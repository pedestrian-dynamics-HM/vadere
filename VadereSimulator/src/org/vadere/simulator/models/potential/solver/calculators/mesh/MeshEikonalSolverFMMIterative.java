package org.vadere.simulator.models.potential.solver.calculators.mesh;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.gen.IncrementalTriangulation;
import org.vadere.meshing.mesh.inter.IFace;
import org.vadere.meshing.mesh.inter.IHalfEdge;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.meshing.mesh.inter.IMesh;
import org.vadere.meshing.mesh.inter.ITriEventListener;
import org.vadere.meshing.mesh.inter.IVertex;
import org.vadere.meshing.mesh.triangulation.triangulator.gen.GenRegularRefinement;
import org.vadere.meshing.utils.math.GeometryUtilsMesh;
import org.vadere.simulator.models.potential.solver.calculators.EikonalSolver;
import org.vadere.simulator.models.potential.solver.timecost.ITimeCostFunction;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.logging.Logger;
import org.vadere.util.math.IDistanceFunction;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;


// TODO this is experimental code!
/**
 * This code solves the eikonal equation by iteratively refine the mesh as described in the PhD thesis of B. Zoennchen (Section 9.4.3).
 * The implementation is experimental and not jet ready for general usage, i.e. the code is still not very well structured
 * and rather hard to read.
 *
 * @author Benedikt Zoennchen
 *
 * @param <V>   the type of the vertices of the triangulation
 * @param <E>   the type of the half-edges of the triangulation
 * @param <F>   the type of the faces of the triangulation
 *
 * @author Benedikt Zoennchen
 */
public class MeshEikonalSolverFMMIterative<V extends IVertex, E extends IHalfEdge, F extends IFace> implements EikonalSolver, ITriEventListener<V, E, F> {

	private static Logger logger = Logger.getLogger(MeshEikonalSolverFMMIterative.class);

	public static String nameCurvature = "curvature";

	private MeshEikonalSolverFMM<V, E, F> solver;

	private boolean calculationFinished;

	private double maxCurrentCurvature = Double.MAX_VALUE;

	private static final double MIN_CURVATURE = 0.1;

	private static final double MAX_CURVATURE = 0.3;

	private static final double MIN_EDGE_LEN = 0.001;

	private static final int MAX_ITERATIONS = 3;

	private int level = 0;

	private GenRegularRefinement<V, E, F> refiner;

	private @NotNull IDistanceFunction distFunc;

	private Collection<V> points = null;

	private Collection<? extends IPoint> pointList;

	private ITimeCostFunction timeCostFunction;

	private Collection<VShape> targetShapes = null;


	public MeshEikonalSolverFMMIterative(@NotNull final String identifier,
	                             @NotNull final Collection<VShape> targetShapes,
	                             @NotNull final ITimeCostFunction timeCostFunction,
	                             @NotNull final IIncrementalTriangulation<V, E, F> triangulation
	) {
		this(new MeshEikonalSolverFMM<>(identifier, targetShapes, timeCostFunction, triangulation));
		this.targetShapes = targetShapes;
		this.timeCostFunction = timeCostFunction;
	}

	public MeshEikonalSolverFMMIterative(@NotNull final ITimeCostFunction timeCostFunction,
	                                     @NotNull final IIncrementalTriangulation<V, E, F> triangulation,
	                                     @NotNull final Collection<V> targetVertices,
	                                     @NotNull final IDistanceFunction distFunc
	) {
		this(new MeshEikonalSolverFMM<>(timeCostFunction, triangulation, targetVertices));
		this.distFunc = distFunc;
		this.points = targetVertices;
		this.timeCostFunction = timeCostFunction;
	}

	public MeshEikonalSolverFMMIterative(@NotNull final ITimeCostFunction timeCostFunction,
	                                     @NotNull final List<? extends IPoint> points,
	                                     @NotNull final IIncrementalTriangulation<V, E, F> triangulation,
	                                     @NotNull final IDistanceFunction distFunc
	) {
		this(new MeshEikonalSolverFMM<>(timeCostFunction, points, triangulation));
		this.distFunc = distFunc;
		this.pointList = points;
		this.timeCostFunction = timeCostFunction;
	}

	public MeshEikonalSolverFMMIterative(@NotNull final MeshEikonalSolverFMM<V, E, F> solver) {
		this.solver = solver;
	}

	private IIncrementalTriangulation<V, E, F> refine(@NotNull final IIncrementalTriangulation<V, E, F> triangulation) {

		// (1) copy triangulation
		IIncrementalTriangulation<V, E, F> clone = new IncrementalTriangulation<>(triangulation.getMesh().clone(), e -> true);

		// (2) refine if necessary
		final Predicate<E> predicate = new PredicateRefinement<>(triangulation, clone, MIN_EDGE_LEN, 0.3);
		//final Predicate<E> predicate = new PredicateEdgeRefinement<>(solver, MIN_EDGE_LEN, MAX_CURVATURE);
		//refiner = new GenRegularRefinement<>(triangulation, predicate, level);
		refiner = new GenRegularRefinement<>(clone, predicate, level);
		//refiner = new GenRegularRefinement<>(clone, level);
		refiner.refine();
		//refiner.coarse();

		// TODO destroy triangulation
		// triangulation.destroy();
		return refiner.getTriangulation();
	}

	/**
	 * Calculate the fast marching solution. This is called only once,
	 * subsequent calls only return the result of the first.
	 */
	@Override
	public void solve() {

		if (!calculationFinished) {
			List<V> list = solver.getTriangulation().getMesh().getBoundaryVertices();
			solver.solve();
			System.out.println(solver.getTriangulation().getMesh().toPythonTriangulation(v -> solver.getPotential(v)));
			level = 1;
			double lastMaxCurvature = 0;
			maxCurrentCurvature = Double.MAX_VALUE;

			while (level < MAX_ITERATIONS) {
			//while(maxCurrentCurvature > MAX_CURVATURE && !hasConverged(lastMaxCurvature, maxCurrentCurvature) && level < MAX_ITERATIONS) {
				lastMaxCurvature = maxCurrentCurvature;
				//System.out.println(solver.getTriangulation().getMesh().toPythonTriangulation(v -> solver.getPotential(v)));
				maxCurrentCurvature = computeCurvature(solver.getTriangulation());
				logger.debug("max curvature = " + maxCurrentCurvature);
				solver.getTriangulation().removeTriEventListener(solver);
				IIncrementalTriangulation<V, E, F> refinedTriangulation = refine(solver.getTriangulation());
				//solver = new MeshEikonalSolverFMM<>(getTimeCostFunction(), refinedTriangulation, refinedTriangulation.getMesh().getBoundaryVertices());
				//
				if(targetShapes == null) {
					solver = new MeshEikonalSolverFMM<>(getTimeCostFunction(), pointList, refinedTriangulation);
				} else {
					solver = new MeshEikonalSolverFMM<>("", targetShapes, timeCostFunction, refinedTriangulation);
				}
				solver.solve();

				System.out.println(solver.getTriangulation().getMesh().toPythonTriangulation(v -> solver.getPotential(v)));
				level++;
			}

			/*var background = solver.getTriangulation().clone();
			computeCurvature(background);
			double minEdgeLen = background.getMesh().getMinEdgeLen();
			double maxEdgeLen = background.getMesh().getMaxEdgeLen();

			EdgeLengthFunctionTri h = new EdgeLengthFunctionTri(refiner.getTriangulation().clone());
			h.smooth(0.3);

			//IEdgeLengthFunction h1 = p -> Math.min(maxEdgeLen, minEdgeLen + 0.3 * (1 / background.getInterpolatedValue(p.getX(), p.getY(), MeshEikonalSolverFMMIterative.nameCurvature)));

			GenEikMesh<V, E, F> eikMesh = new GenEikMesh<>(h, solver.getTriangulation());
			MeshPanel<V, E, F> panel = new MeshPanel<>(eikMesh.getMesh(), 1000, 1000);
			panel.display();
			while (!eikMesh.isFinished()) {
				eikMesh.improve();
				panel.repaint();
			}

			solver = new MeshEikonalSolverFMM<>(solver, eikMesh.getTriangulation(), eikMesh.getTriangulation().getMesh().getBoundaryVertices());
			solver.solve();

			System.out.println(solver.getTriangulation().getMesh().toPythonTriangulation(v -> solver.getPotential(v)));*/

			calculationFinished = true;
		}
	}

	@Override
	public ITimeCostFunction getTimeCostFunction() {
		return timeCostFunction;
	}

	private boolean hasConverged(double lastMaxCurvature, double thisMaxCurvature) {
		return Math.abs(lastMaxCurvature - thisMaxCurvature) < 0.01;
	}

	private double computeCurvature(@NotNull final IIncrementalTriangulation<V, E, F> background) {
		double maxCurvature = 0;
		for(var v : background.getMesh().getVertices()) {
			double[] result = GeometryUtilsMesh.curvature(background.getMesh(), v, vertex -> solver.getPotential(vertex));
			background.getMesh().setDoubleData(v, MeshEikonalSolverFMMIterative.nameCurvature, result[0]);
			//System.out.println("Curvature: " + result[0]);
			//System.out.println("Gaussian curvature: " + result[1]);
			maxCurvature = Math.max(maxCurvature, result[0]);
		}

		return maxCurvature;
	}

	// include this into an interface
	public IIncrementalTriangulation<V, E, F> getTriangulation() {
		return solver.getTriangulation();
	}

	@Override
	public double getPotential(IPoint pos, double unknownPenalty, double weight) {
		return solver.getPotential(pos, unknownPenalty, weight);
	}

	@Override
	public Function<IPoint, Double> getPotentialField() {
		return solver.getPotentialField();
	}

	@Override
	public double getPotential(double x, double y) {
		return solver.getPotential(x, y);
	}

	@Override
	public IMesh<?, ?, ?> getDiscretization() {
		return solver.getDiscretization();
	}

	public double getPotential(@NotNull final V vertex) {
		return solver.getPotential(vertex);
	}

	@Override
	public void postSplitTriangleEvent(F original, F f1, F f2, F f3, V v) {

	}

	@Override
	public void postSplitHalfEdgeEvent(E originalEdge, F original, F f1, F f2, V v) {
		/*final String name = solver.identifier + "_" + EikonalSolverFMMTriangulation.nameInitialVertex;
		if(refiner.getMesh().streamVertices(v).allMatch(u -> refiner.getMesh().getBooleanData(u, name))) {
			refiner.getMesh().setBooleanData(v, name, true);
		}*/
	}

	@Override
	public void postFlipEdgeEvent(F f1, F f2) {

	}

	@Override
	public void postInsertEvent(V vertex) {

	}

}
