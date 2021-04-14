package org.vadere.simulator.models.potential.solver.calculators.mesh;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vadere.meshing.mesh.inter.IEdgeContainerBoolean;
import org.vadere.meshing.mesh.inter.IEdgeContainerDouble;
import org.vadere.meshing.mesh.inter.IEdgeContainerObject;
import org.vadere.meshing.mesh.inter.IFace;
import org.vadere.meshing.mesh.inter.IHalfEdge;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.meshing.mesh.inter.IMesh;
import org.vadere.meshing.mesh.inter.ITriEventListener;
import org.vadere.meshing.mesh.inter.IVertex;
import org.vadere.meshing.mesh.inter.IVertexContainerBoolean;
import org.vadere.meshing.mesh.inter.IVertexContainerDouble;
import org.vadere.simulator.models.potential.solver.timecost.ITimeCostFunction;
import org.vadere.simulator.models.potential.solver.timecost.ITimeCostFunctionMesh;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.math.IDistanceFunction;
import org.vadere.util.math.InterpolationUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Benedikt Zoennchen
 *
 * Abstract class that solves the eikonal equation on a Mesh i.e. 2-D triangular mesh.
 * It is the basis for the implementation of the FMM, FIM, FSM and IFIM on triangular meshes.
 *
 * @author Benedikt Zoennchen
 *
 * @param <V>   the type of the vertices of the triangulation
 * @param <E>   the type of the half-edges of the triangulation
 * @param <F>   the type of the faces of the triangulation
 */
public abstract class AMeshEikonalSolver<V extends IVertex, E extends IHalfEdge, F extends IFace> implements MeshEikonalSolver<V, E, F>, ITriEventListener<V, E, F> {

	private ITimeCostFunctionMesh<V> meshTimeCostFunction;

	@Nullable IDistanceFunction distanceFunction;
	private final IIncrementalTriangulation<V, E, F> triangulation;
	private Collection<V> initialVertices;

	public static final String namePotential = "potential";
	public static final String nameCosPhi = "computeCosPhi";
	public static final String nameBurned = "burned";
	public static final String nameBurning = "burning";
	public static final String nameInitialVertex = "initialVertex";
	public static final String nameNonAccuteEdge = "nonAccuteEdge";
	public static final String nameVirtualSupport = "virtualSupport";
	public static final String nameVirtualSupportCosPhi = "virtualSupportCosPhi";
	public static final String nameTimeCost = "timeCost";

	private IVertexContainerBoolean<V, E, F> burned;
	private IVertexContainerBoolean<V, E, F> burning;
	private IVertexContainerDouble<V, E, F> potential;
	private IEdgeContainerDouble<V, E, F> cosPhi;
	private IVertexContainerBoolean<V, E, F> initialVertex;
	private IEdgeContainerBoolean<V, E, F> nonAccute;
	private IEdgeContainerObject<V, E, F, List> virtualSupport;
	private IEdgeContainerObject<V, E, F, DoubleArrayList> virtualSupportCosPhi;
	private IVertexContainerDouble<V, E, F> timeCosts;


	protected boolean solved = false;

	final String identifier;

	private MeshEikonalSolver.LocalSover localSover = MeshEikonalSolver.LocalSover.SETHIAN;

	/**
	 * Default constructor.
	 *
	 * @param identifier        the identifier is used for all container names. A container is basically a bijective function, that maps
	 *                          a data point (Object or primitive data type) to a vertex, halfe-edge or face.
	 * @param triangulation     the 2-D triangular mesh i.e. the discretization the solution is based on.
	 * @param timeCostFunction  the timeCostFunction of the eikonal equation, i.e. 1/F(x).
	 */
	public AMeshEikonalSolver(@NotNull final String identifier,
	                          @NotNull final IIncrementalTriangulation<V, E, F> triangulation,
	                          @NotNull final ITimeCostFunction timeCostFunction) {
		this.identifier = identifier;
		if(timeCostFunction instanceof ITimeCostFunctionMesh) {
			// unsave cast
			this.meshTimeCostFunction = (ITimeCostFunctionMesh<V>)timeCostFunction;
		} else {
			this.meshTimeCostFunction = ITimeCostFunctionMesh.convert(timeCostFunction);
		}

		this.triangulation = triangulation;
		this.triangulation.removeTriEventListener(this);
		this.triangulation.addTriEventListener(this);

		// get access to containers
		this.burned = getMesh().getBooleanVertexContainer(identifier + "_" + nameBurned);
		this.burning = getMesh().getBooleanVertexContainer(identifier + "_" + nameBurning);
		this.potential = getMesh().getDoubleVertexContainer(identifier + "_" + namePotential);
		this.cosPhi = getMesh().getDoubleEdgeContainer(identifier + "_" + nameCosPhi);
		this.initialVertex = getMesh().getBooleanVertexContainer(identifier + "_" + nameInitialVertex);
		this.nonAccute = getMesh().getBooleanEdgeContainer(identifier + "_" + nameNonAccuteEdge);
		this.virtualSupport = getMesh().getObjectEdgeContainer(identifier + "_" + nameVirtualSupport, List.class);
		this.virtualSupportCosPhi = getMesh().getObjectEdgeContainer(identifier + "_" + nameVirtualSupportCosPhi, DoubleArrayList.class);
		this.timeCosts = getMesh().getDoubleVertexContainer(identifier + "_" + nameTimeCost);
	}

	/**
	 * This method sets the distance of the starting vertices and has to be called before starting the solver via {@link AMeshEikonalSolver#solve()}.
	 * Note that for those vertices the solver assumes that F = 1 i.e. the distance is equal to the travel time T.
	 *
	 * @param initialVertices   the list of initial vertices
	 * @param distanceFunction  a signed distance function to set the initial travelling time values
	 */
	public void setInitialVertices(@NotNull final Collection<V> initialVertices, @NotNull final IDistanceFunction distanceFunction) {
		this.initialVertices = initialVertices;
		this.distanceFunction = distanceFunction;
		for(V v : initialVertices) {
			double dist = distanceFunction.apply(new VPoint(getMesh().toPoint(v)));
			setPotential(v, Math.max(0, dist));
			setBurned(v);
			setAsInitialVertex(v);
		}
	}

	/**
	 * Marks and returns the initial vertices, i.e. vertices at which the wavefront propagation starts.
	 *
	 * @param targetShapes  the agent destination/target of the eikonal equation
	 *
	 * @return
	 */
	protected List<V> findInitialVertices(final Collection<VShape> targetShapes) {
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
		return initialVertices;
	}

	/**
	 * Resets all computed travel times but keeps the computed values which are mesh dependent
	 * (and therefore, do not change if the mesh does not change), for example, the angles inside a triangle.
	 */
	protected void unsolve() {
		triangulation.getMesh().streamVerticesParallel().filter(v -> !isInitialVertex(v)).forEach(v -> {
			setUndefined(v);
			setPotential(v, Double.MAX_VALUE);
			setTimeCost(v);
		});
		initialVertices = initialVertices.stream().filter(v -> !getMesh().isDestroyed(v)).collect(Collectors.toList());
		//solved = false;
	}

	/**
	 * Computes all mesh dependent measures such as angles and virtual supports.
	 */
	protected void prepareMesh() {
		triangulation.getMesh().streamEdgesParallel().forEach(e -> setVirtualSupport(e, Collections.EMPTY_LIST));
		triangulation.getMesh().streamEdgesParallel().forEach(e -> setAccuteEdge(e));
		triangulation.getMesh().streamEdgesParallel().filter(e -> !getMesh().isBoundary(e)).forEach(e -> setCosPhi(e, computeCosPhi(e)));
		triangulation.getMesh().streamEdgesParallel().forEach(e -> {
			if(!triangulation.getMesh().isBoundary(e) && computeIsNonAcute(e)) {
				setNonAccuteEdge(e);
				List<Pair<V, V>> virtualSupport = computeVirtualSupport(e);
				setVirtualSupport(e, virtualSupport);
				setVirtualSupportCosPhi(e, computeVirtualSupportCosPhis(e, virtualSupport));
			}
		});
	}

	@Override
	public IMesh<V, E, F> getMesh() {
		return triangulation.getMesh();
	}

	@Override
	public ITimeCostFunction getTimeCostFunction() {
		return meshTimeCostFunction;
	}

	@Override
	public void update() {
		getTimeCostFunction().update();
		solve();
	}

	protected boolean isLazy() {
		return false;
	}

	protected void compute(@NotNull final V v) {
		compute();
	}

	protected abstract void compute();

	/**
	 * Tests whether the triangle / face of <tt>edge</tt> is non-acute. In this case we can not use
	 * the triangle for computation but have to search for numerical support.
	 *
	 * @param edge  the edge defining the face / triangle
	 *
	 * @return true if the face / triangle is non-acute, false otherwise
	 */
	protected boolean computeIsNonAcute(@NotNull final E edge) {
		VPoint p1 = getMesh().toPoint(getMesh().getPrev(edge));
		VPoint p2 = getMesh().toPoint(edge);
		VPoint p3 = getMesh().toPoint(getMesh().getNext(edge));

		double angle1 = GeometryUtils.angle(p1, p2, p3);

		// non-acute triangle
		double rightAngle = Math.PI/2;
		return angle1 > rightAngle + GeometryUtils.DOUBLE_EPS;
	}

	/**
	 * Recomputes traveling time T of a potential point of a certain vertex by
	 * computing all possible traveling times for each neighbouring points
	 * returning the minimum.
	 *
	 * @param vertex the vertex which represents the potential point.
	 *
	 * @return the recomputed potential of a potential point
	 */
	protected double recomputePotential(@NotNull final V vertex) {
		// loop over all, check whether the point is contained and update its
		// value accordingly
		double potential = Double.MAX_VALUE;

		for(E edge : getMesh().getEdgeIt(vertex)) {
			if(!getMesh().isBoundary(edge)) {
				potential = Math.min(computePotential(edge), potential);
			}
		}
		return potential;
	}

	protected Triple<Double, V, V> recomputePotentialAndDefiningSimplex(@NotNull final V vertex) {
		// loop over all, check whether the point is contained and update its
		// value accordingly
		double potential = Double.MAX_VALUE;
		V v1 = null;
		V v2 = null;

		for(E edge : getMesh().getEdgeIt(vertex)) {
			if(!getMesh().isBoundary(edge)) {
				Triple<Double, V, V> triple = computePotentialAndDefinigSimplex(edge);

				if(triple.getLeft() < potential) {
					potential = triple.getLeft();
					v1 = triple.getMiddle();
					v2 = triple.getRight();
				}
			}
		}
		if(potential <= getPotential(v1)) {
			v1 = null;
		}
		if(potential <= getPotential(v2)) {
			v2 = null;
		}

		return Triple.of(potential, v1, v2);
	}

	/**
	 * Updates a point (the point where the edge ends) given a triangle (which is the face of the edge).
	 * The point can only be updated if the triangle triangleContains it and the other two points are in the frozen band.
	 *
	 * @param edge the edge defining the point and the triangle
	 * @return the recomputed potential
	 */
	private double computePotential(@NotNull final E edge) {
		E next = getMesh().getNext(edge);
		E prev = getMesh().getPrev(edge);
		double potential = Double.MAX_VALUE;

		if(isNonAcute(edge)) {
			V v = getMesh().getVertex(edge);
			List<Pair<V, V>> list = getVirtualSupport(edge);
			// this might happen for vertices which are close at the initialVertex!
			if(list.isEmpty()) {
				//logger.warn("could not find virtual support for non-acute triangle.");
				//potential = computePotential(edge, next, prev, -1.0);
				potential = computePotential(edge, next, prev, getCosPhi(edge));
			} else {
				DoubleArrayList cosPhis = getVirtualSupportCosPhi(edge);

				for(int i = 0; i < list.size(); i++) {
					Pair<V, V> pair = list.get(i);
					double cosPhi = cosPhis.getDouble(i);
					potential = Math.min(potential, computePotential(v, pair.getLeft(), pair.getRight(), cosPhi, localSover));
				}
			}

		} else {
			potential = computePotential(edge, next, prev, getCosPhi(edge));
		}

		return potential;
	}

	private Triple<Double, V, V> computePotentialAndDefinigSimplex(@NotNull final E edge) {
		E next = getMesh().getNext(edge);
		E prev = getMesh().getPrev(edge);
		V v1 = getMesh().getVertex(next);
		V v2 = getMesh().getVertex(prev);

		double potential = Double.MAX_VALUE;
		//TODO: the code below should be used to enable virtual simplexes, however this might currently fail because their is no neighboring connectifity
		// for those simplices defined by the mesh and the FIM might stall because it tests the wrong connectivities not part of the DAG!
		/*if(isNonAcute(edge)) {
			V v = getMesh().getVertex(edge);
			List<Pair<V, V>> list = getVirtualSupport(edge);
			// this might happen for vertices which are close at the initialVertex!
			if(list.isEmpty()) {
				//logger.warn("could not find virtual support for non-acute triangle.");
				//potential = computePotential(edge, next, prev, -1.0);
				potential = computePotential(edge, next, prev, getCosPhi(edge));
			} else {
				DoubleArrayList cosPhis = getVirtualSupportCosPhi(edge);

				for(int i = 0; i < list.size(); i++) {
					Pair<V, V> pair = list.get(i);
					double cosPhi = cosPhis.getDouble(i);
					double q = computePotential(v, pair.getLeft(), pair.getRight(), cosPhi, localSover);
					if(q < potential) {
						potential = q;
						v1 = pair.getLeft();
						v2 = pair.getRight();
					}
				}
			}
		} else {*/
			potential = computePotential(edge, next, prev, getCosPhi(edge));
		//}
		return Triple.of(potential, v1, v2);
	}

	protected List<Pair<V, V>> computeVirtualSupport(@NotNull final E edge) {
		V v = getMesh().getVertex(edge);
		List<Pair<V, V>> list = new ArrayList<>();
		getMesh().getVirtualSupport(v, getMesh().getPrev(edge), list);
		return list;
	}

	protected DoubleArrayList computeVirtualSupportCosPhis(@NotNull final E edge, @NotNull final List<Pair<V, V>> virtualSupportList) {
		DoubleArrayList list = new DoubleArrayList(virtualSupportList.size());
		V p = getMesh().getVertex(edge);
		for(Pair<V, V> virtualSupport : virtualSupportList) {
			list.add(computeCosPhi(virtualSupport.getLeft(), p, virtualSupport.getRight()));
		}
		return list;
	}

	/**
	 * Computes the traveling time T at <tt>point</tt> by using the neighbouring points <tt>point1</tt> and <tt>point2</tt>.
	 *
	 * @param edge     the edge / point for which the traveling time is computed
	 * @param edge1    one neighbouring edge
	 * @param edge2    another neighbouring edge
	 * @param cosPhi
	 *
	 * @return the traveling time T at <tt>edge</tt> by using the triangle (edge, edge1, edge2) for the computation
	 */
	private double computePotential(final E edge, final E edge1, final E edge2, double cosPhi) {
		V point = getMesh().getVertex(edge);
		V point1 = getMesh().getVertex(edge1);
		V point2 = getMesh().getVertex(edge2);
		return computePotential(point, point1, point2, cosPhi, localSover);
	}



	private double sinPhi(@NotNull final V p1, @NotNull final V p, @NotNull final V p2) {
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

	/**
	 * Defines the porperties for which a point is used to compute the traveling time of neighbouring points.
	 *
	 * @param v the point which is tested
	 *
	 * @return true if the point can be used for computation, false otherwise
	 */
	@Override
	public boolean isFeasibleForComputation(@NotNull final V v){
		return isBurining(v) || isBurned(v) || isInitialVertex(v);
	}

	protected boolean isUndefined(@NotNull final V vertex) {
		return !isBurining(vertex) && !isBurned(vertex) && !isInitialVertex(vertex);
	}

	protected void setUndefined(@NotNull final V vertex) {
		setUnburned(vertex);
		setUnburning(vertex);
	}

	@Override
	public double getPotential(@NotNull final IPoint pos, final double unknownPenalty, final double weight) {
		return weight * getPotential(pos.getX(), pos.getY());
	}

	@Override
	public double getPotential(IPoint pos, double unknownPenalty, double weight, final Object caller) {
		return weight * getPotential(pos.getX(), pos.getY(), caller);
	}

	@Override
	public Function<IPoint, Double> getPotentialField() {
		IIncrementalTriangulation<V, E, F> clone = triangulation.clone();
		IVertexContainerDouble<V, E, F> containerDouble = clone.getMesh().getDoubleVertexContainer(identifier + "_" + namePotential);
		return p -> getInterpolatedPotential(clone, containerDouble, p.getX(), p.getY(), null);
	}

	@Override
	public double getPotential(final double x, final double y) {
		return getInterpolatedPotential(triangulation, potential, x, y, null);
	}

	@Override
	public double getPotential(double x, double y, final Object caller) {
		return getInterpolatedPotential(triangulation, potential, x, y, caller);
	}

	@Override
	public IMesh<?, ?, ?> getDiscretization() {
		return triangulation.getMesh().clone();
	}

	@Override
	public boolean needsUpdate() {
		return getTimeCostFunction().needsUpdate();
	}

	/**
	 * Returns barycentric interpolated value at (x,y) based on the {@link IVertexContainerDouble} containerDouble.
	 *
	 * @param triangulation     the triangular mesh (this is most of the time {@link this#triangulation})
	 * @param containerDouble   some vertex container
	 * @param x                 x-coordinate of the request point
	 * @param y                 y-coordinate of the request point
	 * @param caller            the caller object to optimize the triangle walk
	 *
	 * @return the barycentric interpolated value at (x,y)
	 */
	protected double getInterpolatedPotential(
			@NotNull final IIncrementalTriangulation<V, E, F> triangulation,
			@NotNull final IVertexContainerDouble<V, E, F> containerDouble,
			final double x,
			final double y,
			@Nullable final Object caller) {
		Optional<F> optFace;
		if(caller != null) {
			optFace = triangulation.locateFace(x, y, caller);
		} else {
			optFace = triangulation.locateFace(x, y);
		}

		double result = Double.MAX_VALUE;
		if(!optFace.isPresent()) {
			//logger.warn("no face found for coordinates (" + x + "," + y + ")");
		}
		else if(optFace.isPresent() && triangulation.getMesh().isBoundary(optFace.get())) {
			//	logger.warn("no triangle found for coordinates (" + x + "," + y + ")");
		}
		/*else if(!triangulation.contains(x, y, optFace.get())) {
			// the face which was found does not contain the point this happens if we had to walk through an obstacle and abortAtBoundary == true!
		}*/
		else {
			E edge = triangulation.getMesh().getEdge(optFace.get());
			V v1 = triangulation.getMesh().getVertex(edge);
			V v2 = triangulation.getMesh().getVertex(triangulation.getMesh().getNext(edge));
			V v3 = triangulation.getMesh().getVertex(triangulation.getMesh().getPrev(edge));

			double[] xs = new double[3];
			xs[0] = triangulation.getMesh().getX(v1);
			xs[1] = triangulation.getMesh().getX(v2);
			xs[2] = triangulation.getMesh().getX(v3);

			double[] ys = new double[3];
			ys[0] = triangulation.getMesh().getY(v1);
			ys[1] = triangulation.getMesh().getY(v2);
			ys[2] = triangulation.getMesh().getY(v3);

			double[] zs = new double[3];

			if(isLazy()) {
				if(!isBurned(v1)) {
					compute(v1);
				}

				if(!isBurned(v2)) {
					compute(v2);
				}

				if(!isBurned(v3)) {
					compute(v3);
				}
			}

			//System.out.println(triangulation.getMesh().toPythonTriangulation(v -> getPotential(v)));
			//System.out.println();

			zs[0] = containerDouble.getValue(v1);
			zs[1] = containerDouble.getValue(v2);
			zs[2] = containerDouble.getValue(v3);

			double area = GeometryUtils.areaOfPolygon(xs, ys);

			result = InterpolationUtil.barycentricInterpolation(xs, ys, zs, area, x, y);
		}
		return result;
	}


	protected double getPotential(
			@NotNull final IIncrementalTriangulation<V, E, F> triangulation,
			@NotNull final IVertexContainerDouble<V, E, F> containerDouble,
			final double x,
			final double y) {
		return getInterpolatedPotential(triangulation, containerDouble, x, y, null);
	}


	// getter and setter
	protected void updatePotential(@NotNull final V vertex, final double potential) {
		setPotential(vertex, Math.min(getPotential(vertex), potential));
	}

	protected IIncrementalTriangulation<V, E, F> getTriangulation() {
		return triangulation;
	}

	@Override
	public double getTimeCost(@NotNull final V vertex) {
		return timeCosts.getValue(vertex);
		//return timeCostFunction.costAt(getMesh().toPoint(vertex));
		//return 1 + density.getValue(vertex);
	}

	protected Collection<V> getInitialVertices() {
		return initialVertices;
	}

	@Override
	public double getPotential(@NotNull final V vertex) {
		/*if(!isBurned(vertex)) {
			compute(vertex);
		}*/
		return potential.getValue(vertex);
	}

	public void setPotential(@NotNull final V vertex, final double value) {
		potential.setValue(vertex, value);
	}

	protected double getCosPhi(@NotNull final E edge) {
		return cosPhi.getValue(edge);
	}

	protected void setCosPhi(@NotNull final E edge, final double value) {
		cosPhi.setValue(edge, value);
	}

	protected boolean isInitialVertex(@NotNull final V vertex) {
		return initialVertex.getValue(vertex);
	}

	protected void seInitialVertex(@NotNull final V vertex, boolean targetVertex) {
		initialVertex.setValue(vertex, targetVertex);
	}

	protected void setAsInitialVertex(@NotNull final V vertex) {
		seInitialVertex(vertex, true);
	}

	protected boolean isBurned(@NotNull final V vertex) {
		return burned.getValue(vertex);
	}

	protected void setBurned(@NotNull final V vertex) {
		burned.setValue(vertex, true);
	}

	protected void setUnburned(@NotNull final V vertex) {
		burned.setValue(vertex, false);
	}

	protected boolean isBurining(@NotNull final V vertex) {
		return burning.getValue(vertex);
	}

	protected void setBurning(@NotNull final V vertex) {
		burning.setValue(vertex, true);
	}

	protected void setUnburning(@NotNull final V vertex) {
		burning.setValue(vertex, false);
	}

	protected void setNonAccuteEdge(@NotNull final E edge) {
		nonAccute.setValue(edge, true);
	}

	protected void setAccuteEdge(@NotNull final E edge) {
		nonAccute.setValue(edge, false);
	}

	protected boolean isNonAcute(@NotNull final E edge) {
		return nonAccute.getValue(edge);
	}

	protected void setVirtualSupport(@NotNull final E edge, @NotNull final List<Pair<V, V>> list) {
		virtualSupport.setValue(edge, list);
	}

	protected void setTimeCost(@NotNull final V v) {
		setTimeCost(v, meshTimeCostFunction.costAt(v));
	}

	protected void setTimeCost(@NotNull final V v, final double value) {
		timeCosts.setValue(v, value);
	}

	protected List<Pair<V, V>> getVirtualSupport(@NotNull final E edge) {
		return virtualSupport.getValue(edge);
	}

	protected void setVirtualSupportCosPhi(@NotNull final E edge, @NotNull final DoubleArrayList list) {
		virtualSupportCosPhi.setValue(edge, list);
	}

	protected DoubleArrayList getVirtualSupportCosPhi(@NotNull final E edge) {
		return virtualSupportCosPhi.getValue(edge);
	}

	@Override
	public void postInsertEvent(@NotNull final V vertex) {
		double dist = distanceFunction.apply(getMesh().toPoint(vertex));
		if(distanceFunction.apply(getMesh().toPoint(vertex)) <= 0) {
			initialVertices.add(vertex);
			setPotential(vertex, dist);
			setBurned(vertex);
			setAsInitialVertex(vertex);
		}
	}
}
