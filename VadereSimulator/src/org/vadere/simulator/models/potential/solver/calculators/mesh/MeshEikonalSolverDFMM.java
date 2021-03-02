package org.vadere.simulator.models.potential.solver.calculators.mesh;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.inter.IFace;
import org.vadere.meshing.mesh.inter.IHalfEdge;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.meshing.mesh.inter.IVertex;
import org.vadere.meshing.mesh.inter.IVertexContainerBoolean;
import org.vadere.meshing.mesh.inter.IVertexContainerDouble;
import org.vadere.simulator.models.potential.solver.timecost.ITimeCostFunction;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.logging.Logger;
import org.vadere.util.math.IDistanceFunction;
import org.vadere.util.math.MathUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;


/**
 * This class computes the traveling time T using the fast marching method for arbitrary triangulated meshes.
 * If the equation is solved multiple times, computation time is saved by re-using unchanged values.
 * The quality of the result depends on the quality of the triangulation. For a high accuracy the triangulation
 * should not contain too many non-acute triangles.
 *
 * @param <V>   the type of the vertices of the triangulation
 * @param <E>   the type of the half-edges of the triangulation
 * @param <F>   the type of the faces of the triangulation
 *
 * @author Benedikt Zoennchen
 */
public class MeshEikonalSolverDFMM<V extends IVertex, E extends IHalfEdge, F extends IFace> extends AMeshEikonalSolverFMM<V, E, F> {

	private static Logger logger = Logger.getLogger(MeshEikonalSolverDFMM.class);

	public static final String nameSpeedChanged = "speedChanged";
	public static final String nameOldSpeed = "oldTimeCosts";
	public static final String nameOldPotential = "oldPotential";

	final String identifier;

	/**
	 * The time cost function defined on the geometry.
	 */
	private ITimeCostFunction timeCostFunction;

	/**
	 * The narrow-band of the fast marching method.
	 */
	private LinkedList<V> order;

	private IVertexContainerDouble<V, E, F> oldPotential;
	private IVertexContainerDouble<V, E, F> oldTimeCosts;
	private IVertexContainerBoolean<V, E, F> speedChange;

	private LinkedList<V> prevOrder;
	private int iteration = 0;
	private int nUpdates = 0;
	private int avoidedUpdates = 0;
	private double maxValidOldPotential = 0;

	//private IDistanceFunction distToDest;

	// Note: The prevOrder of arguments in the constructors are exactly as they are since the generic type of a collection is only known at run-time!

	/**
	 * Constructor for certain target shapes.
	 *
	 * @param identifier
	 * @param targetShapes      shapes that define the whole target area.
	 * @param timeCostFunction  the time cost function t(x). Note F(x) = 1 / t(x).
	 * @param triangulation     the triangulation the propagating wave moves on.
	 */
	public MeshEikonalSolverDFMM(@NotNull final String identifier,
	                             @NotNull final Collection<VShape> targetShapes,
	                             @NotNull final ITimeCostFunction timeCostFunction,
	                             @NotNull final IIncrementalTriangulation<V, E, F> triangulation
	                             //@NotNull final Collection<VShape> destinations
	) {
		super(identifier, triangulation, timeCostFunction);
		this.identifier = identifier;
		//this.distToDest = IDistanceFunction.createToTargets(destinations);
		this.oldPotential = getMesh().getDoubleVertexContainer(identifier + "_" + nameOldPotential);
		this.oldTimeCosts = getMesh().getDoubleVertexContainer(identifier + "_" + nameOldSpeed);
		this.speedChange = getMesh().getBooleanVertexContainer(identifier + "_" + nameSpeedChanged);

		this.timeCostFunction = timeCostFunction;
		this.order = new LinkedList<>();
		this.prevOrder = new LinkedList<>();

		//TODO a more clever init!
		List<V> initialVertices = new ArrayList<>();
		for(VShape shape : targetShapes) {
			getMesh().streamVertices()
					.filter(v -> shape.contains(getMesh().toPoint(v)))
					.forEach(v -> {
						for(V u : getMesh().getAdjacentVertexIt(v)) {
							initialVertices.add(u);
							setAsInitialVertex(u);
						}
						initialVertices.add(v);
						setAsInitialVertex(v);
					});
		}

		setInitialVertices(initialVertices, IDistanceFunction.createToTargets(targetShapes));
	}

	@Override
	public void solve() {
		double ms = System.currentTimeMillis();
		getTriangulation().enableCache();
		nUpdates = 0;
		avoidedUpdates = 0;

		if(!solved || needsUpdate()) {
			if(!solved) {
				prepareMesh();
				unsolve();
				initializeNarrowBand();
				march();
			} else if(needsUpdate()) {
				partiallyUnsolve();
				//initializeNarrowBand();
				march();
			}
		}
		solved = true;

		double runTime = (System.currentTimeMillis() - ms);
		logger.debug("fmm run time = " + runTime);
		logger.debug("#nUpdates = " + nUpdates);
		logger.debug("#nUpdates avoided = " + avoidedUpdates);
		logger.debug("#nVertices = " + getMesh().getNumberOfVertices());
	}


	/*@Override
	public void solve() {
		double ms = System.currentTimeMillis();
		getTriangulation().enableCache();
		if (!calculationFinished || needsUpdate()) {
			init();
			if(!lazy) {
				compute();
			}
			calculationFinished = true;
		}

		System.out.println("updates = " + nUpdates);
		System.out.println("nVertices = " + getMesh().getNumberOfVertices());

		iteration++;
		System.out.println("run time = " + (System.currentTimeMillis() - ms));
		//System.out.println(triangulation.getMesh().toPythonTriangulation(v -> getPotential(v)));
		//System.out.println();
	}*/


	protected void march() {
		double currentPotential = 0;
		int count = 0;
		while(!isEmpty() || !prevOrder.isEmpty()) {
			if(isEmpty()) {
				count++;
				partiallyUnsolve(prevOrder, currentPotential);
				reInitialNarrowBand(prevOrder);
				logger.debug("partially unsolve: " + count + ", nb: " + narrowBand.size() + ", order: " + order.size());
				continue;
			}
			V vertex = pop();
			currentPotential = getPotential(vertex);
			setBurned(vertex);
			order.add(vertex);
			//nUpdates++;
			updatePotentialOfNeighbours(vertex);
		}
		prevOrder = order;
		order = new LinkedList<>();
	}

	protected void march(@NotNull final V v) {
		while(!isEmpty() && !isBurned(v)) {
			V vertex = pop();
			prevOrder.addLast(vertex);
			setBurned(vertex);
			order.add(vertex);
			nUpdates++;
			updatePotentialOfNeighbours(vertex);
		}
		System.out.println("updates = " + nUpdates);
	}

	@Override
	protected void unsolve() {
		getMesh().streamVerticesParallel().forEach(v -> {
			if(!isInitialVertex(v)) {
				setUndefined(v);
				setPotential(v, Double.MAX_VALUE);
				setOldPotential(v, Double.MAX_VALUE);
				setOldTimeCost(v, getTimeCost(v));
			}

			setTimeCost(v);
		});
		solved = false;
	}

	protected boolean isNeighbourBurned(@NotNull final V v) {
		if(isBurned(v)) {
			return false;
		}

		for(V neighbour : getMesh().getAdjacentVertexIt(v)) {
			if(isBurned(neighbour)){
				return true;
			}
		}

		return false;
	}

	protected void reInitialNarrowBand(@NotNull final LinkedList<V> preorder) {
		while(!preorder.isEmpty() && isNeighbourBurned(preorder.peek())) {
			V v = preorder.poll();
			updatePotential(v);
			//push(v);
		}
	}

	protected void partiallyUnsolve(@NotNull final LinkedList<V> preorder, final double currentPotential) {
		//maxValidOldPotential = Double.MAX_VALUE;
		while(!preorder.isEmpty() && getOldPotential(preorder.peek()) <= currentPotential) {
			V v = preorder.poll();
			//order.add(v);
		}
		//find the max valid potential
		while(!preorder.isEmpty() && (isInitialVertex(preorder.peek()) || !hasSpeedChanged(preorder.peek()))) {
			V v = preorder.poll();
			if(!isBurned(v)) {
				setBurned(v);
				setPotential(v, getOldPotential(v));
				order.add(v);
			}
		}
		/*if(!preorder.isEmpty()) {
			maxValidOldPotential = getOldPotential(preorder.peek());
		}*/
	}

	protected void partiallyUnsolve() {
		getMesh().streamVerticesParallel().forEach(v -> {

			if(!isInitialVertex(v)) {
				setUndefined(v);
				setOldPotential(v, getPotential(v));
				setPotential(v, Double.MAX_VALUE);
				setOldTimeCost(v, getTimeCost(v));
			}
			// update new time cost
			setTimeCost(v);
			if(!isInitialVertex(v) && Math.abs(getOldTimeCost(v) - getTimeCost(v)) > MathUtil.EPSILON){
				setSpeedChanged(v, true);
			} else {
				setSpeedChanged(v, false);
			}
		});

		/*getMesh().streamVerticesParallel().forEach(v -> setSpeedChanged(v, false));
		getMesh().streamVerticesParallel().forEach(v -> setOldPotential(v, getPotential(v)));

		maxValidOldPotential = Double.MAX_VALUE;

		//TODO: parallel computation due to reduction
		for(V v : getMesh().getVertices()) {
			if(!isInitialVertex(v)) {
				if(Math.abs(getOldTimeCost(v) - getTimeCost(v)) > MathUtil.EPSILON){
					maxValidOldPotential = Math.min(maxValidOldPotential, getPotential(v));
					setSpeedChanged(v, true);
				}

				//setOldPotential(v, getPotential(v));
				setUndefined(v);
				setPotential(v, Double.MAX_VALUE);
			}
		}*/

		getTriangulation().getMesh().streamVerticesParallel().forEach(v -> {
			setOldTimeCost(v, getTimeCost(v));
		});

		/*while (!order.isEmpty() && isInInitialNarrowBand(order.peek(), maxValidOldPotential)) {
			updatePotential(order.poll());
		}*/

		/*LinkedList<V> orderBurned = new LinkedList<>();
		for(V v : order) {
			if(getOldPotential(v) <= maxValidOldPotential) {
				setBurned(v);
				setPotential(v, getOldPotential(v));
				orderBurned.addLast(v);
			} else {
				break;
			}
		}

		Iterator<V> descendingIt =  orderBurned.descendingIterator();
		while (descendingIt.hasNext()) {
			V v = descendingIt.next();
			boolean endOfNarrowBand = true;
			for(V neighbour : getMesh().getAdjacentVertexIt(v)) {
				if(!isBurned(neighbour)) {
					updatePotential(neighbour);
					endOfNarrowBand = false;
				}
			}

			if(endOfNarrowBand) {
				break;
			}
		}*/

		/*for(V v: speedChanged) {
			for(V neighbour : getMesh().getAdjacentVertexIt(v)) {
				updatePotentialOfNeighbours(neighbour);
			}
		}*/

		/*for(Pair<V, Double> bandMember : initialNarrowBand) {
			setPotential(bandMember.getLeft(), bandMember.getValue());
			narrowBand.add(bandMember.getLeft());
		}*/
	}

	private boolean isInInitialNarrowBand(@NotNull final V v, final double maxValidOldPotential) {
		for(V neighbour : getMesh().getAdjacentVertexIt(v)) {
			if(isBurned(neighbour)) {
				return true;
			}
		}
		return false;
	}

	private boolean requiresUpdate(@NotNull final V v) {
		for(V neighbour : getMesh().getAdjacentVertexIt(v)) {
			if(hasChanged(getOldPotential(v), neighbour)) {
				return true;
			}
		}
		return false;
	}

	@Override
	protected double recomputePotential(@NotNull V vertex) {
		if(requiresUpdate(vertex)) {
			nUpdates++;
			return super.recomputePotential(vertex);
		}
		avoidedUpdates++;
		return getOldPotential(vertex);
	}


	private boolean hasChanged(@NotNull final double oldPotential, @NotNull final V neighbor) {
		if(!solved) {
			return true;
		}

		if(isUndefined(neighbor) && oldPotential < getOldPotential(neighbor)) {
			return false;
		}

		return Math.abs(getOldPotential(neighbor) - getPotential(neighbor)) > MathUtil.EPSILON;
	}

	/*private double recomputePotential(@NotNull final V vertex) {
		// loop over all, check whether the point is contained and update its
		// value accordingly
		double potential = Double.MAX_VALUE;
		List<V> neighbours = getValidNeirhgours(vertex);

		double[][] P = new double[neighbours.size()][2];
		double[][] a = new double[neighbours.size()][1];
		double[][] b = new double[neighbours.size()][1];

		for(int i = 0; i < neighbours.size(); i++) {
			double x = getMesh().getX(vertex) - getMesh().getX(neighbours.get(i));
			double y = getMesh().getY(vertex) - getMesh().getY(neighbours.get(i));
			double len = GeometryUtils.length(x, y);
			P[i][0] = x / len;
			P[i][1] = y / len;
			a[i][0] = 1 / len;
			b[i][0] = -getPotential(vertex) / len;
		}

		RealMatrix Pm = MatrixUtils.createRealMatrix(P);
		RealMatrix av = MatrixUtils.createRealMatrix(a);
		RealMatrix bv = MatrixUtils.createRealMatrix(b);
		RealMatrix Pmt = Pm.transpose();
		RealMatrix Qi = Pm.multiply(Pmt);


		RealMatrix Q = MatrixUtils.inverse(Qi);
		double tc = timeCostFunction.costAt(getMesh().getPoint(vertex));

		RealMatrix tmp = av.transpose().multiply(Q).multiply(av);
		double x2 = av.transpose().multiply(Q).multiply(av).getEntry(0,0);

		tmp = av.transpose().multiply(Q).multiply(bv);
		double x1 = 2 * av.transpose().multiply(Q).multiply(bv).getEntry(0, 0);

		tmp = bv.transpose().multiply(Q).multiply(bv);
		double x0 = bv.transpose().multiply(Q).multiply(bv).getEntry(0, 0) - (1 / tc * tc);

			potential = solveQuadratic(x2, x1, x0);


		return potential;
	}*/

	boolean hasSpeedChanged(@NotNull final V v) {
		return speedChange.getValue(v);
	}

	void setSpeedChanged(@NotNull final V v, final boolean value) {
		speedChange.setValue(v, value);
	}

	double getOldPotential(@NotNull final V vertex) {
		return oldPotential.getValue(vertex);
	}

	void setOldPotential(@NotNull final V vertex, final double value) {
		oldPotential.setValue(vertex, value);
	}

	double getOldTimeCost(@NotNull final V vertex) {
		return oldTimeCosts.getValue(vertex);
	}

	void setOldTimeCost(@NotNull final V vertex, final double value) {
		oldTimeCosts.setValue(vertex, value);
	}

	private double computePotentialSethian(final V point, final V point1, final V point2) {

		// see: Sethian, Level Set Methods and Fast Marching Methods, page 124.
		V p1;   // A
		V p2;   // B

		// assuming T(B) > T(A)
		if(getPotential(point1) > getPotential(point2)) {
			p2 = point1;
			p1 = point2;
		}
		else {
			p2 = point2;
			p1 = point1;
		}

		double TA = getPotential(p1);
		double TB = getPotential(p2);

		double u = TB - TA;
		double a = p2.distance(point);
		double b = p1.distance(point);
		double c = p1.distance(p2);

		//double phi = angle3D(p1, point, p2);
		double cosphi = computeCosPhi(p1, point, p2);
		double sinPhi = computeSinPhi(p1, point, p2);


		double F = 1.0 / getTimeCost(point);

		// solve x2 t^2 + x1 t + x0 == 0
		double x2 = a * a + b * b - 2 * a * b * cosphi;
		double x1 = 2 * b * u * (a * cosphi - b);
		double x0 = b * b * (u * u - F * F * a * a * sinPhi * sinPhi);
		double t = solveQuadratic(x2, x1, x0);

		double inTriangle = (b * (t - u) / t);
		if (u < t && a * cosphi < inTriangle && inTriangle < a / cosphi) {
			return t + TA;
		} else {
			return Math.min(b * F + TA, a * F + TB);
		}
	}

	private double computeSinPhi(@NotNull final V p1, @NotNull final V p, @NotNull final V p2) {
		return Math.sin(GeometryUtils.angle(p1, p, p2));
	}

	private double computeCosPhi(@NotNull final E edge) {
		E next = getMesh().getNext(edge);
		E prev = getMesh().getPrev(edge);
		IPoint p = getMesh().toPoint(edge);
		IPoint p1 = getMesh().toPoint(next);
		IPoint p2 = getMesh().toPoint(prev);
		return Math.cos(GeometryUtils.angle(p1, p, p2));
	}

	private double angle(@NotNull final E p1, @NotNull final E p, @NotNull final E p2) {
		double angle =  getMesh().getDoubleData(p, "angle3D");
		if(angle == 0) {
			angle = GeometryUtils.angle(getMesh().getVertex(p1), getMesh().getVertex(p), getMesh().getVertex(p2));
			getMesh().setDoubleData(p, "angle3D", angle);
			return angle;
		}
		else {
			return angle;
		}
	}

	/**
	 * Solves the quadratic equation given by (a*x^2+b*x+c=0).
	 *
	 * @param a a real number in the equation
	 * @parafm b a real number in the equation
	 * @param c a real number in the equation
	 *
	 * @return the maximum of both solutions, if any.
	 *         Double.MIN_VALUE if there is no real solution i.e. the determinant (det = b^2-4ac is negative)
	 */
	private double solveQuadratic(double a, double b, double c) {
		List<Double> solutions = MathUtil.solveQuadratic(a, b, c);
		double result = -Double.MIN_VALUE;
		if (solutions.size() == 2) {
			result =  Math.max(solutions.get(0), solutions.get(1));
		} else if (solutions.size() == 1) {
			result = solutions.get(0);
		}

		return result;

		/*double det = b * b - 4 * a * c;
		if (det < 0) {
			return Double.MIN_VALUE;
		}

		return Math.bound((-b + Math.sqrt(det)) / (2 * a), (-b - Math.sqrt(det))
				/ (2 * a));*/
	}
}
