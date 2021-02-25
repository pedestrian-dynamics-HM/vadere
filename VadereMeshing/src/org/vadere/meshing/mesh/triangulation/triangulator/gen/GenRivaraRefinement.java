package org.vadere.meshing.mesh.triangulation.triangulator.gen;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.inter.IFace;
import org.vadere.meshing.mesh.inter.IHalfEdge;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.meshing.mesh.inter.IMesh;
import org.vadere.meshing.mesh.inter.IVertex;
import org.vadere.meshing.mesh.triangulation.triangulator.inter.IRefiner;

import java.util.LinkedList;
import java.util.function.Predicate;

/**
 * Implementation of Rivara's bisection of longest edge algorithm for triangular mesh refinement.
 *
 *
 * <b>References:</b>
 * <ol>
 *     <li>
 *           <a href="https://onlinelibrary.wiley.com/doi/abs/10.1002/nme.1620200412">Algorithm of Rivara</a>
 *     </li>
 * </ol>
 * @param <V> the type of the vertices
 * @param <E> the type of the half-edges
 * @param <F> the type of the faces
 *
 * @author Benedikt Zoennchen
 */
public class GenRivaraRefinement<V extends IVertex, E extends IHalfEdge, F extends IFace> implements IRefiner<V, E, F> {

	private final IIncrementalTriangulation<V, E, F> triangulation;
	private final Predicate<E> edgeRefinePredicates;
	private boolean finished;
	private boolean refined;

	public GenRivaraRefinement(
			@NotNull final IIncrementalTriangulation<V, E, F> triangulation,
			@NotNull final Predicate<E> edgeRefinePredicates
			) {
		this.triangulation = triangulation;
		this.edgeRefinePredicates = edgeRefinePredicates;
		this.finished = false;
	}

	@Override
	public IIncrementalTriangulation<V, E, F> generate() {
		return generate(true);
	}

	@Override
	public IIncrementalTriangulation<V, E, F> generate(boolean finalize) {
		if(!finished) {
			do {
				refine();
			} while(isRefined());
		}
		if(finalize) {
			triangulation.finish();
			finished = true;
		}
		return triangulation;
	}

	@Override
	public IIncrementalTriangulation<V, E, F> getTriangulation() {
		return triangulation;
	}

	@Override
	public void refine() {
		refined = false;
		if(!finished) {
			for(E edge : getMesh().getEdges()) {
				if(!getMesh().isBoundary(edge)) {
					if(edgeRefinePredicates.test(edge)) {
						refined = true;
						refine(getMesh().getFace(edge));
					}
				}
			}
		}

		if(!refined) {
			finished = true;
		}
	}

	public boolean isRefined() {
		return refined;
	}

	public boolean isFinished() {
		return finished;
	}

	@Override
	public IMesh<V, E, F> getMesh() {
		return triangulation.getMesh();
	}

	private void refine(@NotNull final F face) {
		assert !getMesh().isBoundary(face);
		refine(triangulation.getLongestHalfEdge(face));
	}

	private void refine(@NotNull final E edge) {
		assert triangulation.isLongestHalfEdge(edge);
		LinkedList<E> longestEdges = new LinkedList<>();
		longestEdges.addFirst(edge);

		int count = 0;
		while (!longestEdges.isEmpty()) {
			//System.out.println(count++);
			E longestHe = longestEdges.peekFirst();
			if(getMesh().isBoundary(longestHe)) {
				longestEdges.removeFirst();
				triangulation.splitEdge(longestHe, false);
			} else {
				E twin = getMesh().getTwin(longestHe);
				if(triangulation.isLongestHalfEdge(twin)) {
					longestEdges.removeFirst();
					triangulation.splitEdge(longestHe, false);
				}
				else {
					F twinFace = getMesh().getTwinFace(longestHe);
					E e = triangulation.getLongestHalfEdge(twinFace);
					longestEdges.addFirst(e);
				}
			}
		}
	}
}
