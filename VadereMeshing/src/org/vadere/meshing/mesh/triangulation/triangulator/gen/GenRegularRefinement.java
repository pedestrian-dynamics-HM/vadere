package org.vadere.meshing.mesh.triangulation.triangulator.gen;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.gen.MeshPanel;
import org.vadere.meshing.mesh.inter.IFace;
import org.vadere.meshing.mesh.inter.IHalfEdge;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.meshing.mesh.inter.IMesh;
import org.vadere.meshing.mesh.inter.IVertex;
import org.vadere.meshing.mesh.triangulation.IEdgeLengthFunction;
import org.vadere.meshing.mesh.triangulation.triangulator.inter.IRefiner;
import org.vadere.util.geometry.shapes.VLine;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Implementation of the regular refinement described in Mesh Generation by Marshall Bern
 *
 * @param <V> the type of the vertices
 * @param <E> the type of the half-edges
 * @param <F> the type of the faces
 *
 * @author Benedikt Zoennchen
 */
public class GenRegularRefinement<V extends IVertex, E extends IHalfEdge, F extends IFace> implements IRefiner<V, E, F> {

	private final IIncrementalTriangulation<V, E, F> triangulation;
	private final Predicate<F> faceRefinementPredicate;
	private boolean finished;
	private boolean refined;
	public final String propertyNumberOfSplits = "nsplits";

	public GenRegularRefinement(
			@NotNull final IIncrementalTriangulation<V, E, F> triangulation,
			@NotNull final Predicate<F> faceRefinementPredicate
			) {
		this.triangulation = triangulation;
		this.faceRefinementPredicate = faceRefinementPredicate;
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
			for(F face : getMesh().getFaces()) {
				if(!getMesh().isBoundary(face)) {
					if(faceRefinementPredicate.test(face)) {
						refined = true;
						refine(face);
						break;
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
		assert !triangulation.getMesh().isBoundary(face);

		MeshPanel<V, E, F> debugPanel = new MeshPanel<>(triangulation.getMesh(), 800, 800);
		debugPanel.display("debug");

		// some arbitrary edge
		LinkedList<E> toComplete = new LinkedList<>();
		Set<F> toRefine = new HashSet<>();
		toComplete.addLast(getMesh().getEdge(face));

		E edge = toComplete.removeFirst();
		E thisNext = getMesh().getNext(edge);
		E thisPrev = getMesh().getPrev(edge);

		boolean refineTwin = !getMesh().isAtBoundary(edge) && faceRefinementPredicate.test(getMesh().getTwinFace(edge));
		triangulation.splitEdge(edge, false);

		debugPanel.repaint();
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		if(refineTwin) {
			toComplete.addLast(getMesh().getNext(getMesh().getTwin(getMesh().getPrev(thisNext))));
		}

		E thisFlipEdge = getMesh().getPrev(thisPrev);
		getMesh().setIntegerData(thisFlipEdge, propertyNumberOfSplits, 0);
		getMesh().setIntegerData(getMesh().getTwin(thisFlipEdge), propertyNumberOfSplits, 0);
		toComplete.addLast(thisFlipEdge);

		while (!toComplete.isEmpty()) {
			E flipEdge = toComplete.removeFirst();
			E next = getMesh().getPrev(getMesh().getTwin(flipEdge));
			E prev = getMesh().getNext(flipEdge);

			//toRefine.remove(getMesh().getFace(flipEdge));
			//toRefine.remove(getMesh().getTwinFace(flipEdge));

			if(getMesh().getIntegerData(next, propertyNumberOfSplits) >= 1) {
				refineTwin = !getMesh().isAtBoundary(next) && faceRefinementPredicate.test(getMesh().getTwinFace(next));
				E nnext = getMesh().getNext(next);
				triangulation.splitEdge(next, false);

				debugPanel.repaint();
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				if (refineTwin) {
					E e = getMesh().getNext(getMesh().getTwin(getMesh().getPrev(nnext)));
					E twin = getMesh().getTwin(e);

					getMesh().setIntegerData(e, propertyNumberOfSplits, 0);
					getMesh().setIntegerData(twin, propertyNumberOfSplits, 0);

					getMesh().setIntegerData(getMesh().getPrev(e), propertyNumberOfSplits, 1);
					getMesh().setIntegerData(getMesh().getTwin(getMesh().getPrev(e)), propertyNumberOfSplits, 1);

					getMesh().setIntegerData(getMesh().getNext(twin), propertyNumberOfSplits, 1);
					getMesh().setIntegerData(getMesh().getTwin(getMesh().getNext(twin)), propertyNumberOfSplits, 1);

					toComplete.addLast(e);
				}
			}

			if(getMesh().getIntegerData(next, propertyNumberOfSplits) >= 1) {

				refineTwin = !getMesh().isAtBoundary(prev) && faceRefinementPredicate.test(getMesh().getTwinFace(prev));
				E pnext = getMesh().getNext(prev);
				triangulation.splitEdge(prev, false);

				debugPanel.repaint();
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				if(refineTwin) {
					E e = getMesh().getNext(getMesh().getTwin(getMesh().getPrev(pnext)));
					E twin = getMesh().getTwin(e);

					getMesh().setIntegerData(e, propertyNumberOfSplits, 0);
					getMesh().setIntegerData(twin, propertyNumberOfSplits, 0);

					getMesh().setIntegerData(getMesh().getPrev(e), propertyNumberOfSplits, 1);
					getMesh().setIntegerData(getMesh().getTwin(getMesh().getPrev(e)), propertyNumberOfSplits, 1);

					getMesh().setIntegerData(getMesh().getNext(twin), propertyNumberOfSplits, 1);
					getMesh().setIntegerData(getMesh().getTwin(getMesh().getNext(twin)), propertyNumberOfSplits, 1);

					toComplete.addLast(e);
					toRefine.add(getMesh().getFace(e));
					toRefine.add(getMesh().getTwinFace(e));
				}

				triangulation.flip(flipEdge);

				debugPanel.repaint();
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}/* else {
				toComplete.addLast(flipEdge);
			}*/
		}
	}
}
