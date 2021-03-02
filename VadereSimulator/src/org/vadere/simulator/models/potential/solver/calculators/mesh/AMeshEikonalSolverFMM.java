package org.vadere.simulator.models.potential.solver.calculators.mesh;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.inter.IFace;
import org.vadere.meshing.mesh.inter.IHalfEdge;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.meshing.mesh.inter.IVertex;
import org.vadere.simulator.models.potential.solver.timecost.ITimeCostFunction;

import java.util.Comparator;
import java.util.PriorityQueue;

/**
 * @author Benedikt Zoennchen
 *
 */
public abstract class AMeshEikonalSolverFMM<V extends IVertex, E extends IHalfEdge, F extends IFace> extends AMeshEikonalSolver<V, E, F> {

	protected PriorityQueue<V> narrowBand;

	public AMeshEikonalSolverFMM(
			@NotNull final String identifier,
			@NotNull final IIncrementalTriangulation<V, E, F> triangulation,
			@NotNull final ITimeCostFunction timeCostFunction) {
		super(identifier, triangulation, timeCostFunction);

		Comparator<V> pointComparator = (v1, v2) -> {
			double alpha = 0.3;

			double key1 = alpha * getPotential(v1);
			double key2 = alpha * getPotential(v2);

			if (key1 < key2) {
				return -1;
			} else if(key1 > key2) {
				return 1;
			}
			else {
				return 0;
			}
		};

		this.narrowBand = new PriorityQueue<>(pointComparator);
	}

	public AMeshEikonalSolverFMM(
			@NotNull final String identifier,
			@NotNull final IIncrementalTriangulation<V, E, F> triangulation,
			@NotNull final ITimeCostFunction timeCostFunction,
			@NotNull final Comparator<V> comparator) {
		super(identifier, triangulation, timeCostFunction);
		this.narrowBand = new PriorityQueue<>(comparator);
	}

	protected void initializeNarrowBand() {
		for(V vertex : getInitialVertices()) {
			narrowBand.add(vertex);
			/*for(V v : getMesh().getAdjacentVertexIt(vertex)) {
				if(isUndefined(v)) {
					updatePotential(v);
				}
			}*/
		}
	}

	protected boolean isEmpty() {
		return narrowBand.isEmpty();
	}

	@Override
	protected void compute() {
		march();
	}

	@Override
	protected void compute(@NotNull V v) {
		march(v);
	}

	protected V pop() {
		return narrowBand.poll();
	}

	protected void push(@NotNull final V v) {
		narrowBand.add(v);
	}

	protected void march() {
		while (!isEmpty()) {
			V vertex = narrowBand.poll();
			setBurned(vertex);
			updatePotentialOfNeighbours(vertex);
		}
	}

	protected void march(@NotNull final V v) {
		while (!isEmpty() && !isBurned(v)) {
			V vertex = narrowBand.poll();
			setBurned(vertex);
			updatePotentialOfNeighbours(vertex);
		}
	}

	/**
	 * Updates the the traveling times T of all neighbours of <tt>vertex</tt>.
	 *
	 * @param vertex the vertex
	 */
	protected void updatePotentialOfNeighbours(@NotNull final V vertex) {
		for(V neighbour : getMesh().getAdjacentVertexIt(vertex)) {
			if(!isBurned(neighbour) && !isInitialVertex(neighbour)) {
				updatePotential(neighbour);
			}
		}
	}

	/**
	 * Updates the traveling time T of a certain vertex by recomputing it and
	 * updates the narrow band if necessary. If the recomputed value is larger
	 * than the old value, nothing will change.
	 *
	 * @param vertex the vertex for which T will be updated
	 */
	protected void updatePotential(@NotNull final V vertex) {
		double potential = recomputePotential(vertex);
		if(potential < getPotential(vertex)) {
			if(isBurining(vertex)) {
				narrowBand.remove(vertex);
			}
			setPotential(vertex, potential);
			setBurning(vertex);
			narrowBand.add(vertex);
		}

		if(isUndefined(vertex)) {
			logger.debug("could not set neighbour vertex" + vertex);
		}
	}
}