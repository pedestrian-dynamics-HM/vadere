package org.vadere.meshing.mesh.triangulation.triangulator.gen;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.gen.MeshPanel;
import org.vadere.meshing.mesh.gen.MeshRenderer;
import org.vadere.meshing.mesh.inter.IFace;
import org.vadere.meshing.mesh.inter.IHalfEdge;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.meshing.mesh.inter.IMesh;
import org.vadere.meshing.mesh.inter.IVertex;
import org.vadere.meshing.mesh.triangulation.triangulator.inter.IRefiner;
import org.vadere.meshing.utils.color.Colors;
import org.vadere.util.logging.Logger;

import java.awt.*;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Implementation of the regular refinement described in RGB Subdivision by Enrico Puppo and Daniele Panozzo
 *
 * @param <V> the type of the vertices
 * @param <E> the type of the half-edges
 * @param <F> the type of the faces
 *
 * @author Benedikt Zoennchen
 */
public class GenRegularRefinement<V extends IVertex, E extends IHalfEdge, F extends IFace> implements IRefiner<V, E, F> {

	enum Coloring {
		RED, GREEN, BLUE;
	}

	private final static Logger logger = Logger.getLogger(GenRegularRefinement.class);

	private final IIncrementalTriangulation<V, E, F> triangulation;
	private final Predicate<E> edgeRefinementPredicate;
	private boolean finished;
	private boolean refined;
	private boolean coarse;
	public final String propertyLevel = "level";
	public final String propertyColor = "color";
	public final String propertyFlipable = "flipable";
	private LinkedList<E> toRefine;
	private LinkedList<V> toCoarse;
	private Predicate<V> removePredicate = v -> getLevel(v) > 0;
	private final static int sleepTime = 10;

	private MeshPanel<V, E, F> debugPanel;

	public GenRegularRefinement(
			@NotNull final IIncrementalTriangulation<V, E, F> triangulation,
			@NotNull final Predicate<E> edgeRefinementPredicate
			) {
		this.triangulation = triangulation;
		this.edgeRefinementPredicate = edgeRefinementPredicate;
		this.finished = false;
		this.coarse = false;
		this.toRefine = new LinkedList<>();
		this.toCoarse = new LinkedList<>();

		var meshRenderer = new MeshRenderer<>(getMesh(), f -> false, f -> {
			if(isBlue(f)) {
				return Colors.BLUE;
			}
			if(isRed(f)) {
				return Colors.RED;
			}
			if(isGreen(f)) {
				return Colors.GREEN;
			}
			return Color.WHITE;
		});
		debugPanel = new MeshPanel<>(meshRenderer, 800, 800);
		debugPanel.display("debug");
		debugPanel.paintImmediately(0, 0, debugPanel.getWidth(), debugPanel.getHeight());
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
		//synchronized (getMesh()) {
			if(!finished) {
				refined = false;
				do {
					toRefine = getMesh().streamEdges().filter(e -> edgeRefinementPredicate.test(e)).collect(Collectors.toCollection(LinkedList::new));
					refined = !toRefine.isEmpty();
					refineAll();
				} while (refined);
			}
		//}
	}

	public void coarse() {
		//synchronized (getMesh()) {
			if(!finished) {
				coarse = false;
				//do {
					/*toCoarse = getMesh().streamVertices()
							.filter(v -> isRemoveable(v))
							.filter(v -> removePredicate.test(v))
							.collect(Collectors.toCollection(LinkedList::new));*/
					coarse = !toCoarse.isEmpty();
					coarseAll();
				//} while (coarse);
			}
		//}
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

	private void coarseAll() {

		while (!toCoarse.isEmpty()) {
			//synchronized (getMesh()) {
				V vertex = toCoarse.removeFirst();
				coarse(vertex);
			//}

			try {
				Thread.sleep(sleepTime);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			debugPanel.paintImmediately(0, 0, debugPanel.getWidth(), debugPanel.getHeight());
		}
	}

	private void refineAll() {

		var meshRenderer = new MeshRenderer<>(getMesh(), f -> false, f -> {
			if(isBlue(f)) {
				return Color.BLUE;
			}
			if(isRed(f)) {
				return Color.RED;
			}
			if(isGreen(f)) {
				return Color.GREEN;
			}
			return Color.WHITE;
		});

		while (!toRefine.isEmpty()) {
			//synchronized (getMesh()) {
				E edge = toRefine.removeFirst();
				if(edgeRefinementPredicate.test(edge)) {
					refine(edge);
				}
			//}
		}
	}

	private boolean isGBGBMerge(@NotNull final V v) {
		for(E e : getMesh().getEdgeIt(v)) {
			F f1 = getMesh().getFace(e);
			F f2 = getMesh().getTwinFace(e);
			if(!getMesh().isAtBoundary(e) && !(isBlue(f1) && isGreen(f2) || isGreen(f1) && isBlue(f2))) {
				return false;
			}
		}
		return true;
	}

	private boolean canFlipToCoarse(@NotNull final E edge) {
		if(getMesh().isAtBoundary(edge)) {
			return false;
		}

		E twin = getMesh().getTwin(edge);
		F f1 = getMesh().getFace(edge);
		F f2 = getMesh().getFace(twin);
		//return !(isRed(f1) && isRed(f2) || isBlue(f1) && isGreen(f2));
		return (isGreen(edge) && isGreen(f1) && isGreen(f2)) || (isRed(edge) && isBlue(f1) && isBlue(f2)) || (isRed(edge) && ((isBlue(f1) && isRed(f2)) || (isRed(f1) && isBlue(f2))));
	}

	private void coarse(@NotNull final V vertex) {
		if(isRemoveable(vertex)) {
			// add all removeable vertices to the queue
			List<V> adjacentVertices = getMesh().streamVertices(vertex).filter(v -> !isRemoveable(v) && removePredicate.test(v)).collect(Collectors.toList());

			System.out.println(vertex);

			List<E> edges = getMesh().getEdges(vertex);
			int degree = edges.size();
			boolean boundary = getMesh().isAtBoundary(vertex);

			int requiredDegree = boundary ? 3 : 4;
			for(E e : edges) {
				if(degree > requiredDegree && isFlipable(e) && canFlipToCoarse(e)) {
					flipToCoarse(e);
					degree--;

					debugPanel.paintImmediately(0, 0, debugPanel.getWidth(), debugPanel.getHeight());
					try {
						Thread.sleep(sleepTime);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
				}
			}

			if(degree != requiredDegree) {
				toCoarse.add(vertex);
				return;
			}

			assert degree == requiredDegree : "requiredDegree (" + requiredDegree + ") != " + degree;

			E e1 = null;
			E next1 = null;
			E prev1 = null;
			E e2 = null;
			E prev2 = null;
			int level;


			if(isGBGBMerge(vertex)) {
				int vertexLevel = getLevel(vertex);
				for(E e : getMesh().getEdgeIt(vertex)) {
					if(!getMesh().isAtBoundary(e) && getLevel(getMesh().getTwinVertex(e)) == vertexLevel) {
						e1 = e;
						next1 = getMesh().getNext(e1);
						prev1 = getMesh().getPrev(e1);
						e2 = getMesh().getTwin(getMesh().getNext(getMesh().getTwin(next1)));
						prev2 = getMesh().getPrev(e2);
						break;
					}
				}

				level = getLevel(next1);
				triangulation.removeSimpleLink(e1);
				if(!boundary) {
					triangulation.removeSimpleLink(e2);
				}
				E survivor = triangulation.remove2DVertex(vertex, true);
				setLevel(survivor, level - 1);
				setColor(getMesh().getFace(prev1), Coloring.RED);
				if(!boundary) {
					setColor(getMesh().getFace(prev2), Coloring.RED);
				}
			} else {
				for(E e : getMesh().getEdgeIt(vertex)) {

					if(!getMesh().isAtBoundary(e) &&
							(isRed(e) ||
							//(isRed(getMesh().getFace(e)) && isRed(getMesh().getTwinFace(e))) ||
							(isGreen(getMesh().getFace(e)) && isBlue(getMesh().getTwinFace(e))) ||
							(isBlue(getMesh().getFace(e)) && isGreen(getMesh().getTwinFace(e))))) {
						e1 = e;
						//et1 = getMesh().getTwin(e1);
						next1 = getMesh().getNext(e1);
						prev1 = getMesh().getPrev(e1);
						e2 = getMesh().getTwin(getMesh().getNext(getMesh().getTwin(next1)));
						prev2 = getMesh().getPrev(e2);
						break;
					}
				}

				level = getLevel(next1);
				triangulation.removeSimpleLink(e1);
				if(!boundary) {
					triangulation.removeSimpleLink(e2);
				}

				E survivor = triangulation.remove2DVertex(vertex, true);
				setLevel(survivor, level - 1);
				boolean isRed1 = isRed(e1);
				boolean isRed2 = isRed(e2) && !boundary;

				if(isRed1 || isRed2) {

					if(isRed1) {
						setColor(getMesh().getFace(prev1), Coloring.GREEN);
					} else {
						setColor(getMesh().getFace(prev1), Coloring.RED);
					}

					if(isRed2 && !boundary) {
						setColor(getMesh().getFace(prev2), Coloring.GREEN);
					} else {
						setColor(getMesh().getFace(prev2), Coloring.RED);
					}

					// TODO only flip if further coarsening is required.
					/*if(isRed1 && isRed2 && isFlipable(survivor)) {
						flipToCoarse(survivor);
					}*/

				} else {
					setColor(getMesh().getFace(prev1), Coloring.RED);
					if(!boundary) {
						setColor(getMesh().getFace(prev2), Coloring.RED);
					}
				}
			}

			for(V v : adjacentVertices) {
				if(isRemoveable(v) && removePredicate.test(v)) {
					//toCoarse.add(v);
				}
			}
		}

		debugPanel.paintImmediately(0, 0, debugPanel.getWidth(), debugPanel.getHeight());
		try {
			Thread.sleep(sleepTime);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
	}

	

	private void refine(@NotNull final E edge) {
		if(canFlipToRefine(edge)) {
			flipToRefine(edge, toRefine);
		}

		if(isRefinable(edge)) {
			splitGreen(edge);

		} else {
			E twin = getMesh().getTwin(edge);
			F f1 = getMesh().getFace(edge);
			F f2 = getMesh().getTwinFace(edge);

			if(!isRefinable(edge, f1)) {
				if(isBlue(f1)) {
					E redEdge = findRed(f1);
					E greenEdge = findGreen(getMesh().getTwinFace(redEdge));
					refine(greenEdge);
				} else if(isRed(f1)) {
					E greenEdge = findGreen(f1);
					refine(greenEdge);
				}
			}

			if(!isRefinable(twin, f2)) {
				if(isBlue(f2)) {
					E redEdge = findRed(f2);
					E greenEdge = findGreen(getMesh().getTwinFace(redEdge));
					refine(greenEdge);
				} else if(isRed(f2)) {
					E greenEdge = findGreen(f2);
					refine(greenEdge);
				}
			}
		}
	}

	// RGB-Subdivision

	private void splitGreen(@NotNull final E edge) {
		E prev = getMesh().getPrev(edge);
		E next = getMesh().getNext(edge);
		E twinNext = getMesh().getNext(getMesh().getTwin(edge));
		boolean flipable = isFlipable(edge);

		V v1 = getMesh().getVertex(next);
		V v2 = getMesh().getVertex(twinNext);
		int level = getLevel(edge);

		boolean isBoundary1 = getMesh().isBoundary(getMesh().getFace(edge));
		boolean isBoundary2 = getMesh().isBoundary(getMesh().getTwinFace(edge));

		boolean isGreen1 = !isBoundary1 && isGreen(getMesh().getFace(edge));
		boolean isGreen2 = !isBoundary2 && isGreen(getMesh().getTwinFace(edge));

		Pair<E, E> split = triangulation.splitEdge(edge, false, v -> setLevel(v, level + 1));
		V v = getMesh().getVertex(split.getLeft());

		setLevel(v, level + 1);
		setLevel(getMesh().getNext(prev), level + 1);
		setLevel(getMesh().getPrev(next), level + 1);

		setFlipable(getMesh().getNext(prev), flipable);
		setFlipable(getMesh().getPrev(next), flipable);

		toCoarse.addFirst(v);

		if(!isBoundary1) {
			E e1 = getMesh().getEdge(v1, v).get();
			setFlipable(e1, true);
			if(isGreen1) {
				setColor(e1, Coloring.RED);
				setLevel(e1, level);
			} else { // face is blue
				setColor(e1, Coloring.GREEN);
				setLevel(e1, level + 1);
				toRefine.add(e1);

				if(canFlipToRefine(getMesh().getNext(e1))) {
					flipToRefine(getMesh().getNext(e1), toRefine);
				}

				E e1Twin = getMesh().getTwin(e1);
				if(canFlipToRefine(getMesh().getPrev(e1Twin))) {
					flipToRefine(getMesh().getPrev(e1Twin), toRefine);
				}
			}
		}

		if(!isBoundary2) {
			E e2 = getMesh().getEdge(v2, v).get();
			setFlipable(e2, true);
			if(isGreen2) {
				setColor(e2, Coloring.RED);
				setLevel(e2, level);
			} else { // face is blue
				setColor(e2, Coloring.GREEN);
				setLevel(e2, level + 1);
				toRefine.add(e2);

				if(canFlipToRefine(getMesh().getNext(e2))) {
					flipToRefine(getMesh().getNext(e2), toRefine);
				}

				E e2Twin = getMesh().getTwin(e2);
				if(canFlipToRefine(getMesh().getPrev(e2Twin))) {
					flipToRefine(getMesh().getPrev(e2Twin), toRefine);
				}
			}
		}

		/*for(E e : getMesh().getEdges()) {
			if(canFlipToRefine(e)) {
				flipToCoarse(e, toRefine);
			}
		}*/
	}

	private void flipToRefine(@NotNull final E edge, @NotNull final LinkedList<E> toRefine) {
		if(canFlipToRefine(edge)) {
			F f1 = getMesh().getFace(edge);
			F f2 = getMesh().getTwinFace(edge);
			E twin = getMesh().getTwin(edge);

			int level = getLevel(edge);
			triangulation.flip(edge);

			setLevel(edge, level + 1);
			//setLevel(f1, level + 1);
			//setLevel(f2, level + 1);

			setColor(f1, Coloring.GREEN);
			setColor(f2, Coloring.GREEN);

			setGreen(edge, toRefine);
			//setGreen(getMesh().getNext(edge), toRefine);
			//setGreen(getMesh().getPrev(edge), toRefine);

			setGreen(twin, toRefine);
			//setGreen(getMesh().getNext(twin), toRefine);
			//setGreen(getMesh().getPrev(twin), toRefine);
		}
	}

	private void  flipToCoarse(@NotNull final E edge) {
		//if(canFlipToCoarse(edge)) {
			F f1 = getMesh().getFace(edge);
			F f2 = getMesh().getTwinFace(edge);
			E twin = getMesh().getTwin(edge);

			int level = getLevel(edge);

			if(isGreen(edge)) {
				triangulation.flip(edge);
				setLevel(edge, level - 1);
				setColor(edge, Coloring.RED);
			} else if(isBlue(f1) && isBlue(f2) && isRed(edge)) {
				triangulation.flip(edge);
				setLevel(edge, level + 1);
				setColor(edge, Coloring.GREEN);
			} else {
				triangulation.flip(edge);
			}

			//setLevel(f1, level + 1);
			//setLevel(f2, level + 1);

			//setColor(f1, Coloring.BLUE);
			//setColor(f2, Coloring.BLUE);

		//}
	}

	private void setGreen(E edge, LinkedList<E> toRefine) {
		setColor(edge, Coloring.GREEN);
		toRefine.add(edge);
	}

	private E findGreen(@NotNull final F face) {
		int level = Integer.MAX_VALUE;
		E result = null;
		for(E e : getMesh().getEdgeIt(face)) {
			if(isGreen(e) && getLevel(e) < level) {
				level = getLevel(e);
				result = e;
			}
		}
		return result;
	}

	private E findRed(@NotNull final F face) {
		int level = Integer.MAX_VALUE;
		E result = null;
		for(E e : getMesh().getEdgeIt(face)) {
			if(isRed(e) && getLevel(e) < level) {
				level = getLevel(e);
				result = e;
			}
		}
		return result;
	}

	private boolean canFlipToRefine(@NotNull final E edge) {
		F f1 = getMesh().getFace(edge);
		F f2 = getMesh().getTwinFace(edge);
		return getColor(edge) == Coloring.RED && isBlue(f1) && isBlue(f2) && getLevel(edge) == getLevel(f1) && getLevel(edge) == getLevel(f2);
	}

	private boolean isRemoveable(@NotNull V vertex) {
		int level = getLevel(vertex);
		return level > 0 && level >= getMesh().streamVertices(vertex).mapToInt(v -> getLevel(v)).min().orElse(0);
	}

	private boolean isRefinable(@NotNull E edge) {
		return isRefinable(edge, getMesh().getFace(edge)) && isRefinable(getMesh().getTwin(edge), getMesh().getTwinFace(edge));
	}

	private boolean isRefinable(@NotNull E edge, @NotNull F face) {
		int level = getLevel(edge);
		return isGreen(edge) && (getMesh().isBoundary(edge) || getLevel(face) == level);
	}

	private boolean isGreen(@NotNull E edge) {
		return getColor(edge) == Coloring.GREEN;
	}

	private boolean isRed(@NotNull E edge) {
		return getColor(edge) == Coloring.RED;
	}

	private boolean isBlue(@NotNull final F face) {
		assert getMesh().getVertices(face).size() == 3;
		E e1 = getMesh().getEdge(face);
		E e2 = getMesh().getNext(e1);
		E e3 = getMesh().getNext(e2);
		int level1 = getLevel(e1);
		int level2 = getLevel(e2);
		int level3 = getLevel(e3);
		return level1 == level2 && level3 == level1 - 1 || level1 == level3 && level2 == level1 - 1 || level2 == level3 && level1 == level2 - 1;
	}

	private boolean isRed(@NotNull final F face) {
		assert getMesh().getVertices(face).size() == 3;
		E e1 = getMesh().getEdge(face);
		E e2 = getMesh().getNext(e1);
		E e3 = getMesh().getNext(e2);
		int level1 = getLevel(e1);
		int level2 = getLevel(e2);
		int level3 = getLevel(e3);
		return level1 == level2 && level3 == level1 + 1 || level1 == level3 && level2 == level1 + 1 || level2 == level3 && level1 == level2 + 1;
	}

	private boolean isGreen(@NotNull final F face) {
		assert getMesh().getVertices(face).size() == 3;
		E e1 = getMesh().getEdge(face);
		E e2 = getMesh().getNext(e1);
		E e3 = getMesh().getNext(e2);
		return getLevel(e1) == getLevel(e2) && getLevel(e1) == getLevel(e3);
	}

	private int getLevel(@NotNull final V vertex) {
		return getMesh().getIntegerData(vertex, propertyLevel);
	}

	private int getLevel(@NotNull final E edge) {
		return getMesh().getIntegerData(edge, propertyLevel);
	}

	private int getLevel(@NotNull final F face) {
		assert getMesh().getVertices(face).size() == 3;
		E e1 = getMesh().getEdge(face);
		E e2 = getMesh().getNext(e1);
		E e3 = getMesh().getNext(e2);
		return Math.min(getLevel(e1), Math.min(getLevel(e2), getLevel(e3)));
	}

	private void setLevel(@NotNull final V vertex, final int level) {
		getMesh().setIntegerData(vertex, propertyLevel, level);
	}

	private void setLevel(@NotNull final E edge, final int level) {
		getMesh().setIntegerData(edge, propertyLevel, level);
		getMesh().setIntegerData(getMesh().getTwin(edge), propertyLevel, level);
	}

	private void setFlipable(@NotNull final E edge, final boolean flipable) {
		getMesh().setBooleanData(edge, propertyFlipable, flipable);
		getMesh().setBooleanData(getMesh().getTwin(edge), propertyFlipable, flipable);
	}

	private boolean isFlipable(@NotNull final E edge){
		return getMesh().getBooleanData(edge, propertyFlipable);
	}


	private Coloring getColor(@NotNull final V vertex) {
		return getMesh().getData(vertex, propertyColor, Coloring.class).orElse(Coloring.GREEN);
	}

	private Coloring getColor(@NotNull final E edge) {
		return getMesh().getData(edge, propertyColor, Coloring.class).orElse(Coloring.GREEN);
	}

	private Coloring getColor(@NotNull final F face) {
		return getMesh().getData(face, propertyColor, Coloring.class).orElse(Coloring.GREEN);
	}

	private void setColor(@NotNull final V vertex, final Coloring coloring) {
		getMesh().setData(vertex, propertyColor, coloring);
	}

	private void setColor(@NotNull final E edge, final Coloring coloring) {
		getMesh().setData(edge, propertyColor, coloring);
		getMesh().setData(getMesh().getTwin(edge), propertyColor, coloring);
	}

	private void setColor(@NotNull final F face, final Coloring coloring) {
		getMesh().setData(face, propertyColor, coloring);
	}
}
