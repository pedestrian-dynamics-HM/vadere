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
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.logging.Logger;

import java.awt.*;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Implementation of the regular refinement described in RGB Subdivision by Enrico Puppo and Daniele Panozzo
 * see https://cims.nyu.edu/gcl/papers/TVCG08-PuppoPanozzo.pdf.
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

	/**
	 * the base (mutable) triangulation this refiner is working on.
	 */
	private final IIncrementalTriangulation<V, E, F> triangulation;

	/**
	 * a predicate that decides whether or not an edge should be refined.
	 */
	private Predicate<E> edgeRefinementPredicate;

	private boolean finished;
	private boolean refined;
	private boolean coarse;
	public final String propertyLevel = "level";
	public final String propertyColor = "color";
	public final String propertyFlipable = "flipable";

	/**
	 * contains all edges that have to be refined.
	 */
	private LinkedList<E> toRefine;

	/**
	 * contains all vertices that have to be coarsen.
	 */
	private LinkedList<V> toCoarse;

	private Predicate<V> coarsePredicate = v -> getLevel(v) > 0;
	private final static int sleepTime = 1;
	private int maxLevel;

	private MeshPanel<V, E, F> debugPanel;

	public GenRegularRefinement(
			@NotNull final IIncrementalTriangulation<V, E, F> triangulation,
			@NotNull final Predicate<E> edgeRefinementPredicate) {
		this(triangulation, edgeRefinementPredicate, Integer.MAX_VALUE);
	}

	public void setEdgeRefinementPredicate(Predicate<E> edgeRefinementPredicate) {
		this.edgeRefinementPredicate = edgeRefinementPredicate;
	}

	public void setCoarsePredicate(Predicate<V> coarsePredicate) {
		this.coarsePredicate = coarsePredicate;
	}

	public GenRegularRefinement(
			@NotNull final IIncrementalTriangulation<V, E, F> triangulation,
			final int level
			) {

		this.triangulation = triangulation;
		this.maxLevel = level;
		//this.edgeRefinementPredicate = e -> getLevel(e) == (maxLevel-1) && edgeRefinementPredicate.test(e);
		//this.edgeAddToRefine = e -> getLevel(e) == (maxLevel-1) && edgeRefinementPredicate.test(e);

		//VPoint p = new VPoint(5,5);

		//this.edgeRefinementPredicate = e -> !getMesh().isBoundary(e) && getMesh().toTriangle(getMesh().getFace(e)).midPoint().distance(p) < 3.0 && (!isGreen(e) || getMesh().toLine(e).length() > 0.5);
		this.edgeRefinementPredicate = e -> getLevel(e) < level;
		this.finished = false;
		this.coarse = false;
		this.toRefine = new LinkedList<>();
		this.toCoarse = new LinkedList<>();
	}

	public GenRegularRefinement(
			@NotNull final IIncrementalTriangulation<V, E, F> triangulation,
			@NotNull final Predicate<E> edgeRefinementPredicate,
			int maxLevel
			) {
		this.triangulation = triangulation;
		//this.edgeRefinementPredicate = e -> getLevel(e) == (maxLevel-1) && edgeRefinementPredicate.test(e);
		//this.edgeAddToRefine = e -> getLevel(e) == (maxLevel-1) && edgeRefinementPredicate.test(e);

		//VPoint p = new VPoint(5,5);

		//this.edgeRefinementPredicate = e -> !getMesh().isBoundary(e) && getMesh().toTriangle(getMesh().getFace(e)).midPoint().distance(p) < 3.0 && (!isGreen(e) || getMesh().toLine(e).length() > 0.5);
		this.edgeRefinementPredicate = e -> getLevel(e) == (maxLevel-1) && (edgeRefinementPredicate.test(e));
		this.finished = false;
		this.coarse = false;
		this.toRefine = new LinkedList<>();
		this.toCoarse = new LinkedList<>();

		/*var meshRenderer = new MeshRenderer<>(getMesh(), f -> false, f -> {
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
		}, e -> {
			if(isRed(e)) {
				return Color.RED;
			}
			if(isGreen(e)) {
				return Color.GREEN;
			}
			return Color.BLACK;
		});
		debugPanel = new MeshPanel<>(meshRenderer, 1000, 1000);
		debugPanel.display("debug");
		debugPanel.paintImmediately(0, 0, debugPanel.getWidth(), debugPanel.getHeight());*/
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
		if(!finished) {
			toRefine = getMesh().streamEdges().filter(e -> edgeRefinementPredicate.test(e)).collect(Collectors.toCollection(LinkedList::new));
			refined = false;
			if(toRefine.isEmpty()) {
				return;
			}

			do {
				/*toRefine = getMesh().streamEdges().filter(e -> edgeRefinementPredicate.test(e)).collect(Collectors.toCollection(LinkedList::new));
				if(toRefine.isEmpty()) {
					return;
				}*/
				E edge = toRefine.removeFirst();
				if(edgeRefinementPredicate.test(edge)) {
					refine(edge, 0);
				}

				/*debugPanel.paintImmediately(0, 0, debugPanel.getWidth(), debugPanel.getHeight());
				try {
					Thread.sleep(sleepTime);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}*/
			} while (!toRefine.isEmpty());

			//getMesh().getEdges().stream().filter(e -> isRR(e)).forEach(e -> refine(e));

			// TODO make these steps optional: remove blue triangles and quad red triangles
			//getMesh().getEdges().stream().filter(e -> isRB(e)).forEach(e -> refine(e));
			/*getMesh().getVertices().stream()
					.filter(v -> getMesh().degree(v) == 4)
					.filter(v -> getMesh().streamFaces(v).allMatch(f -> isRed(f)))
					.forEach(v -> coarse(v));*/

		}
	}


	public boolean refine(@NotNull final Collection<E> edges) {
		toRefine.addAll(edges);
		boolean refined = false;
		//int level = edges.stream().mapToInt(e -> getLevel(e)).max().getAsInt();
		//setMaxLevel(level+1);

		/*for(E edge : edges) {
			refine(edge);
			refined = true;
		}*/

		do {
			E edge = toRefine.removeFirst();
			if(edgeRefinementPredicate.test(edge)) {
				refine(edge, 0);
				refined = true;
			}
		} while (!toRefine.isEmpty());

		return refined;
	}

	public void coarse(@NotNull final Collection<V> vertices) {
		toCoarse.addAll(vertices);
		coarse();
	}

	public void coarse() {
		if(!finished) {
			coarse = false;
			while (!toCoarse.isEmpty()) {
				V vertex = toCoarse.removeFirst();
				if(coarsePredicate.test(vertex)) {
					coarse(vertex);
				}


				/*try {
					Thread.sleep(sleepTime);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				debugPanel.paintImmediately(0, 0, debugPanel.getWidth(), debugPanel.getHeight());*/
			}
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

	private boolean isGB(@NotNull final E e) {
		F f1 = getMesh().getFace(e);
		F f2 = getMesh().getTwinFace(e);
		return !getMesh().isAtBoundary(e) && (isBlue(f1) && isGreen(f2) || isGreen(f1) && isBlue(f2));
	}

	private boolean isRB(@NotNull final E e) {
		F f1 = getMesh().getFace(e);
		F f2 = getMesh().getTwinFace(e);
		return !getMesh().isAtBoundary(e) && (isRed(f1) && isBlue(f2) || isBlue(f1) && isRed(f2));
	}

	private boolean isRR(@NotNull final E e) {
		F f1 = getMesh().getFace(e);
		F f2 = getMesh().getTwinFace(e);
		return !getMesh().isAtBoundary(e) && (isRed(f1) && isRed(f2) && isGreen(e));
	}

	/*private boolean canFlipToCoarse(@NotNull final E edge) {
		if(getMesh().isAtBoundary(edge)) {
			return false;
		}

		E twin = getMesh().getTwin(edge);
		F f1 = getMesh().getFace(edge);
		F f2 = getMesh().getFace(twin);
		//return !(isRed(f1) && isRed(f2) || isBlue(f1) && isGreen(f2));
		return canGGSwap(edge) ||
				//(isRed(edge) && isBlue(f1) && isBlue(f2)) ||
				//(isGreen(edge) && isRed(f1) && isRed(f2) && getLevel(f1) == getLevel(f2)) || // check it
				(isRed(edge) && ((isBlue(f1) && isRed(f2)) || (isRed(f1) && isBlue(f2))));
	}*/

	private boolean canGGSwap(@NotNull final E edge) {
		if(getMesh().isAtBoundary(edge)) {
			return false;
		}

		F f1 = getMesh().getFace(edge);
		F f2 = getMesh().getTwinFace(edge);

		int level = getLevel(getMesh().getVertex(edge));

		return isGreen(f1) && isGreen(f2)
				&& (getMesh().streamVertices(f1).allMatch(v -> getLevel(v) == level)) || (getMesh().streamVertices(f2).allMatch(v -> getLevel(v) == level));

	}

	private int flipAllToCoarse(@NotNull final V vertex, boolean boundary) {
		List<E> edges = getMesh().getEdges(vertex);
		int degree = edges.size();
		int requiredDegree = boundary ? 3 : 4;
		for(E e : edges) {
			if(degree > requiredDegree) {
				if(flipToCoarse(e)) {
					degree--;
				}
			}
		}

		return degree;
	}

	private void coarse(@NotNull final V vertex) {
		if(isRemoveable(vertex)) {
			boolean boundary = getMesh().isAtBoundary(vertex);
			int requiredDegree = boundary ? 3 : 4;

			// flip as much as possible
			int degree = flipAllToCoarse(vertex, boundary);
			/*if(!getMesh().streamEdges(vertex).allMatch(e -> getLevel(e) >= getLevel(vertex))) {
				toCoarse.add(vertex);
				return;
			}*/
			///assert getMesh().streamEdges(vertex).allMatch(e -> getLevel(e) == getLevel(vertex));
			// check the star!
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
			}
		} else {
			logger.warn("we have a problem.");
		}

		/*debugPanel.paintImmediately(0, 0, debugPanel.getWidth(), debugPanel.getHeight());
		try {
			Thread.sleep(sleepTime);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}*/
	}

	/**
	 * Refinement of an edge
	 * @param edge the edge
	 */
	private void refine(@NotNull final E edge, int dept) {
		if(canFlipToRefine(edge)) {
			flipToRefine(edge);
		}

		if(isRefinable(edge)) {
			splitGreen(edge);
		} else {
			if(edgeRefinementPredicate.test(edge)) {
				toRefine.addLast(edge);
			}

			E twin = getMesh().getTwin(edge);
			F f1 = getMesh().getFace(edge);
			F f2 = getMesh().getTwinFace(edge);

			if(!isRefinable(edge, f1)) {
				if(isBlue(f1)) {
					E redEdge = findRed(f1);
					E greenEdge = findGreen(getMesh().getTwinFace(redEdge));
					if(!greenEdge.equals(edge)) {
						refine(greenEdge, dept+1);
					}
				} else if(isRed(f1)) {
					E greenEdge = findGreen(f1);
					if(!greenEdge.equals(edge)) {
						refine(greenEdge, dept+1);
					}
				}
			}

			if(!isRefinable(twin, f2)) {
				if(isBlue(f2)) {
					E redEdge = findRed(f2);
					E greenEdge = findGreen(getMesh().getTwinFace(redEdge));
					if(!greenEdge.equals(edge)) {
						refine(greenEdge, dept+1);
					}
				} else if(isRed(f2)) {
					E greenEdge = findGreen(f2);
					if(!greenEdge.equals(edge)) {
						refine(greenEdge, dept+1);
					}
				}
			}
		}
	}

	// RGB-Subdivision

	private boolean invalid(@NotNull final E edge) {
		E twin = getMesh().getTwin(edge);
		if(!getMesh().isAtBoundary(edge) && isRed(edge) &&
				(isRed(getMesh().getNext(edge)) || isRed(getMesh().getPrev(edge)) || isRed(getMesh().getNext(twin)) || isRed(getMesh().getPrev(twin)))) {
			return true;
		}
		return false;
	}

	private List<E> valid() {
		return getMesh().streamEdges().filter(e -> invalid(e)).collect(Collectors.toList());
	}

	/**
	 * Splits a green edge.
	 * @param edge
	 */
	private void splitGreen(@NotNull final E edge) {
		boolean isBoundary1 = getMesh().isBoundary(edge);
		boolean isBoundary2 = getMesh().isBoundary(getMesh().getTwin(edge));

		boolean isGreen1 = !isBoundary1 && isGreen(getMesh().getFace(edge));
		boolean isGreen2 = !isBoundary2 && isGreen(getMesh().getTwinFace(edge));

//		boolean isRed1 = isRed(getMesh().getFace(edge));
//		boolean isRed2 = isRed(getMesh().getTwinFace(edge));

		int level = getLevel(edge);

		V v1 = !isBoundary1 ? getMesh().getOpposite(edge) : null;
		V v2 = !isBoundary2 ? getMesh().getOpposite(getMesh().getTwin(edge)) : null;

		// split operation
		V v = split(edge);
		toCoarse.addFirst(v);
		// end split operation

		// adjust the possible two edges that split the old faces
		if(!isBoundary1) {
			adjustMiddleEdge(getMesh().getEdge(v1, v).get(), level, isGreen1);
		}

		if(!isBoundary2) {
			adjustMiddleEdge(getMesh().getEdge(v2, v).get(), level, isGreen2);
		}

		for(E e : getMesh().getEdgeIt(v)) {
			if(edgeRefinementPredicate.test(e)) {
				toRefine.addLast(e);
			}

			//TODO: required?
			E pe = getMesh().getPrev(e);
			if(edgeRefinementPredicate.test(pe)) {
				toRefine.addLast(getMesh().getPrev(pe));
			}
		}

		/*for(E e : getMesh().getEdges()) {
			if(canFlipToRefine(e)) {
				flipToCoarse(e, toRefine);
			}
		}*/
	}

	/**
	 *
	 *
	 * @param edge      the actual edge
	 * @param level     the level of the former edge that this edge splits
	 * @param isGreen   color of the former triangle the edge splits
	 */
	private void adjustMiddleEdge(@NotNull final E edge, final int level, final boolean isGreen) {
		setFlipable(edge, true);
		if(isGreen) {
			setColor(edge, Coloring.RED);
			setLevel(edge, level);
		} else { // face is red
			setGreen(edge);
			setLevel(edge, level + 1);

			if(canFlipToRefine(getMesh().getNext(edge))) {
				flipToRefine(getMesh().getNext(edge));
			}

			E e1Twin = getMesh().getTwin(edge);
			if(canFlipToRefine(getMesh().getPrev(e1Twin))) {
				flipToRefine(getMesh().getPrev(e1Twin));
			}
		}
	}

	private V split(@NotNull final E edge) {
		boolean isBoundary1 = getMesh().isBoundary(edge);
		boolean isBoundary2 = getMesh().isBoundary(getMesh().getTwin(edge));

		E prev = getMesh().getPrev(edge);
		E next = getMesh().getNext(edge);
		E twin = getMesh().getTwin(edge);
		E twinNext = getMesh().getNext(twin);
		boolean flipable = isFlipable(edge);

		V v1 = getMesh().getVertex(next);
		V v2 = getMesh().getVertex(twinNext);
		int level = getLevel(edge);

		Pair<E, E> split = triangulation.splitEdge(edge, false);
		V v = getMesh().getVertex(split.getLeft());

		setLevel(v, level + 1);

		setLevel(getMesh().getNext(prev), level + 1);
		setLevel(getMesh().getPrev(next), level + 1);
		setColor(getMesh().getNext(prev), Coloring.GREEN);
		setColor(getMesh().getPrev(next), Coloring.GREEN);
		setFlipable(getMesh().getNext(prev), flipable);
		setFlipable(getMesh().getPrev(next), flipable);

		if(!isBoundary1) {
			E e1 = getMesh().getEdge(v1, v).get();
			setFlipable(e1, true);
		}

		if(!isBoundary2) {
			E e2 = getMesh().getEdge(v2, v).get();
			setFlipable(e2, true);
		}
		return v;
	}

	private void flipToRefine(@NotNull final E edge) {
		if(canFlipToRefine(edge) && isFlipable(edge)) {

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

			setGreen(edge);
			//setGreen(getMesh().getNext(edge), toRefine);
			//setGreen(getMesh().getPrev(edge), toRefine);
			setGreen(twin);
			//setGreen(getMesh().getNext(twin), toRefine);
			//setGreen(getMesh().getPrev(twin), toRefine);
			if(edgeRefinementPredicate.test(edge)) {
				toRefine.addLast(edge);
			}

		}
	}

	private boolean flipToCoarse(@NotNull final E edge) {
		if(isFlipable(edge)) {
			F f1 = getMesh().getFace(edge);
			F f2 = getMesh().getTwinFace(edge);
			E twin = getMesh().getTwin(edge);

			int level = getLevel(edge);

			if(canGGSwap(edge)) {
				triangulation.flip(edge);
				setLevel(edge, level - 1);
				setColor(edge, Coloring.RED);
				return true;
			} else if(isBlue(f1) && isBlue(f2) && isRed(edge)) {
				triangulation.flip(edge);
				setLevel(edge, level + 1);
				setColor(edge, Coloring.GREEN);
				return true;
			} else if(isRed(edge) && ((isBlue(f2) && isRed(f1)) || (isBlue(f1) && isRed(f2)))) {
				triangulation.flip(edge);
				return true;
			}

			//setLevel(f1, level + 1);
			//setLevel(f2, level + 1);

			//setColor(f1, Coloring.BLUE);
			//setColor(f2, Coloring.BLUE);

		}
		return false;
	}

	public void setGreen(E edge) {
		setColor(edge, Coloring.GREEN);
		/*if(edgeRefinementPredicate.test(edge)) {
			toRefine.addLast(edge);
		}*/
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
		return !getMesh().isAtBoundary(edge) && isRed(edge) && isBlue(f1) && isBlue(f2) && getLevel(edge) == getLevel(f1) && getLevel(edge) == getLevel(f2);
	}

	private boolean isRemoveable(@NotNull V vertex) {
		int level = getLevel(vertex);
		return level > 0 && level >= getMesh().streamVertices(vertex).mapToInt(v -> getLevel(v)).max().orElse(0);
	}

	/**
	 * An edge at level l is refinable (i.e. it can be split) if and only if it is green and its two adjacent triangles t0 and t1
	 * are both at level l. In case of a boundary edge, only one such triangle exists.
	 *
	 * @param edge the edge
	 * @return true if the edge is refinable, false otherwise
	 */
	private boolean isRefinable(@NotNull E edge) {
		boolean refinable1 = isRefinable(edge, getMesh().getFace(edge));
		boolean refinable2 = isRefinable(getMesh().getTwin(edge), getMesh().getTwinFace(edge));
		return  refinable1 && refinable2;
	}

	private boolean isRefinable(@NotNull E edge, @NotNull F face) {
		int level = getLevel(edge);
		return isGreen(edge) && (getMesh().isBoundary(edge) || getLevel(face) == level);
	}

	public boolean isGreen(@NotNull E edge) {
		return getColor(edge) == Coloring.GREEN;
	}

	public boolean isRed(@NotNull E edge) {
		return getColor(edge) == Coloring.RED;
	}

	/**
	 * A triangle is blue if two of its edges are at the same level l and the third edge is at level (l-1).
	 *
	 * @param face a triangle
	 * @return true if if the triangle is blue, false otherwise
	 */
	public boolean isBlue(@NotNull final F face) {
		assert getMesh().getVertices(face).size() == 3;
		E e1 = getMesh().getEdge(face);
		E e2 = getMesh().getNext(e1);
		E e3 = getMesh().getNext(e2);
		int level1 = getLevel(e1);
		int level2 = getLevel(e2);
		int level3 = getLevel(e3);
		return level1 == level2 && level3 == level1 - 1 || level1 == level3 && level2 == level1 - 1 || level2 == level3 && level1 == level2 - 1;
	}

	/**
	 * A triangle is red if two of its edges are at the same level l and the third edge is at level (l+1).
	 *
	 * @param face a triangle
	 * @return true if the triangle is red, false otherwise
	 */
	public boolean isRed(@NotNull final F face) {
		//assert getMesh().getVertices(face).size() == 3;
		E e1 = getMesh().getEdge(face);
		E e2 = getMesh().getNext(e1);
		E e3 = getMesh().getNext(e2);
		int level1 = getLevel(e1);
		int level2 = getLevel(e2);
		int level3 = getLevel(e3);
		return level1 == level2 && level3 == level1 + 1 || level1 == level3 && level2 == level1 + 1 || level2 == level3 && level1 == level2 + 1;
	}

	/**
	 * A triangle is green if all of its edges are at the same level.
	 *
	 * @param face a triangle
	 * @return true if the triangle is green, false otherwise
	 */
	public boolean isGreen(@NotNull final F face) {
		assert getMesh().getVertices(face).size() == 3;
		E e1 = getMesh().getEdge(face);
		E e2 = getMesh().getNext(e1);
		E e3 = getMesh().getNext(e2);
		return getLevel(e1) == getLevel(e2) && getLevel(e1) == getLevel(e3);
	}

	private int getLevel(@NotNull final V vertex) {
		return getMesh().getIntegerData(vertex, propertyLevel);
	}

	public int getLevel(@NotNull final E edge) {
		int level = getMesh().getIntegerData(edge, propertyLevel);;
		return level;
	}

	/**
	 * The level of a triangle is defined to be the lowest amongst the levels of its edges.
	 *
	 * @param face a triangle
	 * @return the level of the triangle
	 */
	private int getLevel(@NotNull final F face) {
		assert getMesh().getVertices(face).size() == 3;
		E e1 = getMesh().getEdge(face);
		E e2 = getMesh().getNext(e1);
		E e3 = getMesh().getNext(e2);
		return Math.min(getLevel(e1), Math.min(getLevel(e2), getLevel(e3)));
	}

	public void setLevel(@NotNull final V vertex, final int level) {
		getMesh().setIntegerData(vertex, propertyLevel, level);
	}

	public void setLevel(@NotNull final E edge, final int level) {
		getMesh().setIntegerData(edge, propertyLevel, level);
		getMesh().setIntegerData(getMesh().getTwin(edge), propertyLevel, level);
	}

	public void setFlipable(@NotNull final E edge, final boolean flipable) {
		getMesh().setBooleanData(edge, propertyFlipable, flipable);
		getMesh().setBooleanData(getMesh().getTwin(edge), propertyFlipable, flipable);
	}

	/**
	 * Make sure no original edges will be flipped.
	 * Such that the structure of the base triangulation is still there.
	 *
	 * @param edge the edge
	 * @return true if the edge is not part of any edge of the base triangulation, false otherwise
	 */
	private boolean isFlipable(@NotNull final E edge){
		return getMesh().getBooleanData(edge, propertyFlipable);
	}

	private Coloring getColor(@NotNull final V vertex) {
		return getMesh().getData(vertex, propertyColor, Coloring.class).orElse(Coloring.GREEN);
	}

	private Coloring getColor(@NotNull final E edge) {
		Optional<Coloring> color = getMesh().getData(edge, propertyColor, Coloring.class);
		return color.orElse(Coloring.GREEN);
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

	public void setMaxLevel(int maxLevel) {
		this.maxLevel = maxLevel;
	}
}
