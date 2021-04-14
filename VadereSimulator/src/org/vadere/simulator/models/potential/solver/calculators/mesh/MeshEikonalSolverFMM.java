package org.vadere.simulator.models.potential.solver.calculators.mesh;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.inter.IFace;
import org.vadere.meshing.mesh.inter.IHalfEdge;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.meshing.mesh.inter.IVertex;
import org.vadere.simulator.models.potential.solver.timecost.ITimeCostFunction;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.logging.Logger;
import org.vadere.util.math.IDistanceFunction;

import java.io.BufferedWriter;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;


/**
 * This class computes the traveling time T using the fast marching method for arbitrary triangulated meshes.
 * The quality of the result depends on the quality of the triangulation. For a high accuracy the triangulation
 * should not contain too many non-acute triangles.
 *
 * @param <V>   the type of the vertices of the triangulation
 * @param <E>   the type of the half-edges of the triangulation
 * @param <F>   the type of the faces of the triangulation
 *
 * @author Benedikt Zoennchen
 */
public class MeshEikonalSolverFMM<V extends IVertex, E extends IHalfEdge, F extends IFace> extends AMeshEikonalSolverFMM<V, E, F> {

	private static Logger logger = Logger.getLogger(MeshEikonalSolverFMM.class);

	final String identifier;
	private int nUpdates = 0;

	private boolean calculationFinished = false;

	// Note: The order of arguments in the constructors are exactly as they are since the generic type of a collection is only known at run-time!

	public MeshEikonalSolverFMM(@NotNull final MeshEikonalSolverFMM<V, E, F> solver,
	                            @NotNull final IIncrementalTriangulation<V, E, F> triangulation,
	                            @NotNull final List<V> initialVertices
	) {
		super(solver.identifier, triangulation, solver.getTimeCostFunction());
		this.identifier = solver.identifier;
		setInitialVertices(initialVertices, p -> 0.0);
	}

	/**
	 * Constructor for certain target points.
	 * @param identifier
	 * @param timeCostFunction  the time cost function t(x). Note F(x) = 1 / t(x).
	 * @param targetPoints      Points where the propagating wave starts i.e. points that are part of the target area.
	 * @param triangulation     the triangulation the propagating wave moves on.
	 */
	public MeshEikonalSolverFMM(@NotNull final String identifier,
	                            @NotNull final ITimeCostFunction timeCostFunction,
	                            @NotNull final Collection<? extends IPoint> targetPoints,
	                            @NotNull final IIncrementalTriangulation<V, E, F> triangulation
	) {
		super(identifier, triangulation, timeCostFunction);
		this.identifier = identifier;

		HashSet<V> targetVertices = new HashSet<>();
		IDistanceFunction distFunc = p -> IDistanceFunction.createToTargetPoints(targetPoints).apply(p);

		for(IPoint point : targetPoints) {
			F face = triangulation.locateFace(point).get();
			assert !getMesh().isBoundary(face);

			if(!getMesh().isBoundary(face)) {

				for(V v : getMesh().getVertexIt(face)) {
					targetVertices.add(v);
				}

				for(F neighbourFace : getMesh().getFaceIt(face)) {
					if(!getMesh().isBoundary(neighbourFace)) {
						for(V v : getMesh().getVertexIt(neighbourFace)) {
							targetVertices.add(v);
						}
					}
				}
			}
		}
		setInitialVertices(targetVertices.stream().collect(Collectors.toList()), distFunc);
	}

	public MeshEikonalSolverFMM(@NotNull final ITimeCostFunction timeCostFunction,
	                            @NotNull final Collection<? extends IPoint> targetPoints,
	                            @NotNull final IIncrementalTriangulation<V, E, F> triangulation
	) {
		this("", timeCostFunction, targetPoints, triangulation);
	}

	/**
	 * Constructor for certain target points.
	 *
	 * @param identifier
	 * @param timeCostFunction  the time cost function t(x). Note F(x) = 1 / t(x).
	 * @param triangulation     the triangulation the propagating wave moves on.
	 * @param initialVertices   Points where the propagating wave starts i.e. points that are part of the target area.
	 */
	public MeshEikonalSolverFMM(@NotNull final String identifier,
	                            @NotNull final ITimeCostFunction timeCostFunction,
	                            @NotNull final IIncrementalTriangulation<V, E, F> triangulation,
	                            @NotNull final Collection<V> initialVertices
	) {
		super(identifier, triangulation, timeCostFunction);
		this.identifier = identifier;
		setInitialVertices(initialVertices, p -> 0.0);
	}

	public MeshEikonalSolverFMM(@NotNull final ITimeCostFunction timeCostFunction,
	                            @NotNull final IIncrementalTriangulation<V, E, F> triangulation,
	                            @NotNull final Collection<V> targetVertices
	) {
		this("", timeCostFunction, triangulation, targetVertices);
	}

	/**
	 * Constructor for certain target shapes.
	 *
	 * @param identifier
	 * @param targetShapes      shapes that define the whole target area.
	 * @param timeCostFunction  the time cost function t(x). Note F(x) = 1 / t(x).
	 * @param triangulation     the triangulation the propagating wave moves on.
	 */
	public MeshEikonalSolverFMM(@NotNull final String identifier,
	                            @NotNull final Collection<VShape> targetShapes,
	                            @NotNull final ITimeCostFunction timeCostFunction,
	                            @NotNull final IIncrementalTriangulation<V, E, F> triangulation
	) {
		super(identifier, triangulation, timeCostFunction);
		this.identifier = identifier;
		setInitialVertices(findInitialVertices(targetShapes), IDistanceFunction.createToTargets(targetShapes));
	}

	public MeshEikonalSolverFMM(@NotNull final Collection<VShape> targetShapes,
	                            @NotNull final ITimeCostFunction timeCostFunction,
	                            @NotNull final IIncrementalTriangulation<V, E, F> triangulation
	) {
		this("", targetShapes, timeCostFunction, triangulation);
	}

	/**
	 * Constructor for certain vertices of the triangulation.
	 *
	 * @param identifier
	 * @param timeCostFunction  the time cost function t(x). Note F(x) = 1 / t(x).
	 * @param triangulation     the triangulation the propagating wave moves on.
	 * @param initialVertices   vertices which are part of the triangulation where the propagating wave starts i.e. points that are part of the target area.
	 * @param distFunc          the distance function (distance to the target) which is negative inside and positive outside the area of interest
	 */
	public MeshEikonalSolverFMM(@NotNull final String identifier,
	                            @NotNull final ITimeCostFunction timeCostFunction,
	                            @NotNull final IIncrementalTriangulation<V, E, F> triangulation,
	                            @NotNull final Collection<V> initialVertices,
	                            @NotNull final IDistanceFunction distFunc
	) {
		super(identifier, triangulation, timeCostFunction);
		this.identifier = identifier;
		setInitialVertices(initialVertices, distFunc);
	}

	public MeshEikonalSolverFMM(@NotNull final ITimeCostFunction timeCostFunction,
	                            @NotNull final IIncrementalTriangulation<V, E, F> triangulation,
	                            @NotNull final Collection<V> targetVertices,
	                            @NotNull final IDistanceFunction distFunc
	) {
		this("", timeCostFunction, triangulation, targetVertices, distFunc);
	}

	/**
	 * Calculate the fast marching solution. This is called only once,
	 * subsequent calls only return the result of the first.
	 */
	@Override
	public void solve() {
		double ms = System.currentTimeMillis();
		getTriangulation().enableCache();

		if(!solved || needsUpdate()) {
			if(!solved) {
				prepareMesh();
				unsolve();
				initializeNarrowBand();
				march();
			} else if(needsUpdate()) {
				//prepareMesh();
				unsolve();
				initializeNarrowBand();
				march();
			}
		}

		solved = true;
		double runTime = (System.currentTimeMillis() - ms);
		logger.debug("fmm run time = " + runTime);
	}
}
