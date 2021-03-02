package org.vadere.simulator.models.potential.solver.calculators.mesh;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.inter.IFace;
import org.vadere.meshing.mesh.inter.IHalfEdge;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.meshing.mesh.inter.IVertex;
import org.vadere.simulator.models.potential.solver.timecost.ITimeCostFunction;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.logging.Logger;
import org.vadere.util.math.IDistanceFunction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;


/**
 * This class computes the traveling time T using the (single threaded) Fast Iterative Method for arbitrary triangulated meshes.
 * The quality of the result depends on the quality of the triangulation. For a high accuracy the triangulation
 * should not contain too many non-acute triangles.
 *
 * @param <V>   the type of the vertices of the triangulation
 * @param <E>   the type of the half-edges of the triangulation
 * @param <F>   the type of the faces of the triangulation
 *
 * @author Benedikt Zoennchen
 */
public class MeshEikonalSolverFIM<V extends IVertex, E extends IHalfEdge, F extends IFace> extends AMeshEikonalSolver<V, E, F> {

	private static Logger logger = Logger.getLogger(MeshEikonalSolverFIM.class);

	private int nThreds = 1;

	final String identifier;

	static {
		logger.setDebug();
	}

	/**
	 * Indicates that the computation of T has been completed.
	 */
	private boolean calculationFinished = false;

	/**
	 * The narrow-band of the fast marching method.
	 */
	private LinkedList<V> activeList;

	private int iteration = 0;
	private int nUpdates = 0;
	private final double epsilon = 0;

	// Note: The updateOrder of arguments in the constructors are exactly as they are since the generic type of a collection is only known at run-time!

	/**
	 * Constructor for certain target shapes.
	 *
	 * @param identifier
	 * @param targetShapes      shapes that define the whole target area.
	 * @param timeCostFunction  the time cost function t(x). Note F(x) = 1 / t(x).
	 * @param triangulation     the triangulation the propagating wave moves on.
	 */
	public MeshEikonalSolverFIM(@NotNull final String identifier,
	                            @NotNull final Collection<VShape> targetShapes,
	                            @NotNull final ITimeCostFunction timeCostFunction,
	                            @NotNull final IIncrementalTriangulation<V, E, F> triangulation
	                            //@NotNull final Collection<VShape> destinations
	) {
		super(identifier, triangulation, timeCostFunction);
		this.identifier = identifier;
		this.activeList = new LinkedList<>();

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

		if(!solved || needsUpdate()) {
			if(!solved) {
				prepareMesh();
				unsolve();
				initialActiveList();
				march();
			} else if(needsUpdate()) {
				unsolve();
				initialActiveList();
				march();
			}
		}

		solved = true;
		double runTime = (System.currentTimeMillis() - ms);
		logger.debug("fim run time = " + runTime);
		logger.debug("#nUpdates = " + nUpdates);
		logger.debug("#nVertices = " + (getMesh().getNumberOfVertices() - (int)getMesh().streamVertices().filter(v -> isInitialVertex(v)).count()));
		iteration++;
	}

	private void initialActiveList() {
		for(V vertex : getInitialVertices()) {
			activeList.addLast(vertex);
			//setPotential(vertex, 0);
		}
	}

	private void march() {
		while(!activeList.isEmpty()) {
			ListIterator<V> listIterator = activeList.listIterator();
			LinkedList<V> newActiveList = new LinkedList<>();
			while(listIterator.hasNext()) {
				V x = listIterator.next();
				double p = getPotential(x);
				double q = p;

				if(!isInitialVertex(x)) {
					q =  Math.min(p, recomputePotential(x));
					setPotential(x, q);
				}

				if (Math.abs(p - q) <= epsilon) {
					setBurned(x);
					setUnburning(x);
					// check adjacent neighbors
					for(V xn : getMesh().getAdjacentVertexIt(x)) {
						if(getPotential(xn) > getPotential(x) && !isInitialVertex(xn) && !isBurining(xn)) {
							p = getPotential(xn);
							q = recomputePotential(xn);
							if(p > q) {
								setPotential(xn, q);
								newActiveList.add(xn);
								setBurning(xn);
							}
						}
					}
					listIterator.remove();
					setBurned(x);
					setUnburning(x);
					if(!isInitialVertex(x)) {
						nUpdates++;
					}
				}
			}
			activeList.addAll(newActiveList);
		}
	}

	private void updatePotential(@NotNull final V vertex) {
		double potential = recomputePotential(vertex);
		if(potential < getPotential(vertex)) {
			if(!isBurining(vertex)) {
				activeList.add(vertex);
			}
			setPotential(vertex, potential);
			setBurning(vertex);
		}

		if(isUndefined(vertex)) {
			logger.debug("could not set neighbour vertex" + vertex);
		}
	}

	@Override
	protected void compute() {
		march();
	}
}
