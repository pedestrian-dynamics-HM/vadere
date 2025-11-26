package org.vadere.meshing.mesh.triangulation.triangulator.gen;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.gen.MeshPanel;
import org.vadere.meshing.mesh.inter.mesh.*;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.meshing.mesh.inter.mesh.builder.ITriangleMeshBuilder;
import org.vadere.meshing.mesh.inter.mesh.data.IMeshDataStorage;
import org.vadere.meshing.mesh.triangulation.triangulator.inter.IRefiner;
import org.vadere.util.logging.Logger;

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
			toRefine = getMesh().edges().stream().filter(e -> edgeRefinementPredicate.test(e)).collect(Collectors.toCollection(LinkedList::new));
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
	public ITriangleMeshBuilder<V, E, F> getMeshBuilder() {
		return triangulation.getMeshBuilder();
	}

	@Override
	public IMeshDataStorage<V, E, F> getMeshDataStorage() {
		return triangulation.getMeshDataStorage();
	}

	private boolean isGBGBMerge(@NotNull final V v) {
		for(E e : getMesh().edges().iterableFor(v)) {
			F f1 = getMesh().faces().getOf(e);
			F f2 = getMesh().faces().getTwin(e);
			if(!getMesh().edges().isAtBoundary(e) && !(isBlue(f1) && isGreen(f2) || isGreen(f1) && isBlue(f2))) {
				return false;
			}
		}
		return true;
	}

	private boolean isGB(@NotNull final E e) {
		F f1 = getMesh().faces().getOf(e);
		F f2 = getMesh().faces().getTwin(e);
		return !getMesh().edges().isAtBoundary(e) && (isBlue(f1) && isGreen(f2) || isGreen(f1) && isBlue(f2));
	}

	private boolean isRB(@NotNull final E e) {
		F f1 = getMesh().faces().getOf(e);
		F f2 = getMesh().faces().getTwin(e);
		return !getMesh().edges().isAtBoundary(e) && (isRed(f1) && isBlue(f2) || isBlue(f1) && isRed(f2));
	}

	private boolean isRR(@NotNull final E e) {
		F f1 = getMesh().faces().getOf(e);
		F f2 = getMesh().faces().getTwin(e);
		return !getMesh().edges().isAtBoundary(e) && (isRed(f1) && isRed(f2) && isGreen(e));
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
		if(getMesh().edges().isAtBoundary(edge)) {
			return false;
		}

		var faces = getMesh().faces();
		var vertices = getMesh().vertices();

		F f1 = faces.getOf(edge);
		F f2 = faces.getTwin(edge);

		int level = getLevel(vertices.getEndOf(edge));

		return isGreen(f1) && isGreen(f2)
				&& (vertices.streamVerticesOf(f1).allMatch(v -> getLevel(v) == level)) || (vertices.streamVerticesOf(f2).allMatch(v -> getLevel(v) == level));

	}

	private int flipAllToCoarse(@NotNull final V vertex, boolean boundary) {
		List<E> edges = getMesh().edges().getAllOf(vertex);
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
			boolean boundary = getMesh().vertices().isAtBoundary(vertex);
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

			var edges = getMesh().edges();

			if(isGBGBMerge(vertex)) {
				int vertexLevel = getLevel(vertex);

				for(E e : edges.iterableFor(vertex)) {
					if(!edges.isAtBoundary(e) && getLevel(getMesh().vertices().getTwin(e)) == vertexLevel) {
						e1 = e;
						next1 = edges.getNext(e1);
						prev1 = edges.getPrev(e1);
						e2 = edges.getTwin(edges.getNext(edges.getTwin(next1)));
						prev2 = edges.getPrev(e2);
						break;
					}
				}

				level = getLevel(next1);
				triangulation.getMeshBuilder().changeConnectivity().removeSimpleLink(e1);
				if(!boundary) {
					triangulation.getMeshBuilder().changeConnectivity().removeSimpleLink(e2);
				}
				E survivor = triangulation.getMeshBuilder().changeConnectivity().remove2DVertex(vertex, true);
				setLevel(survivor, level - 1);
				setColor(getMesh().faces().getOf(prev1), Coloring.RED);
				if(!boundary) {
					setColor(getMesh().faces().getOf(prev2), Coloring.RED);
				}
			} else {
				for(E e : edges.iterableFor(vertex)) {

					if(!edges.isAtBoundary(e) &&
							(isRed(e) ||
							//(isRed(getMesh().getFace(e)) && isRed(getMesh().getTwinFace(e))) ||
							(isGreen(getMesh().faces().getOf(e)) && isBlue(getMesh().faces().getTwin(e))) ||
							(isBlue(getMesh().faces().getOf(e)) && isGreen(getMesh().faces().getTwin(e))))) {
						e1 = e;
						//et1 = getMesh().getTwin(e1);
						next1 = edges.getNext(e1);
						prev1 = edges.getPrev(e1);
						e2 = edges.getTwin(edges.getNext(edges.getTwin(next1)));
						prev2 = edges.getPrev(e2);
						break;
					}
				}

				level = getLevel(next1);
				triangulation.getMeshBuilder().changeConnectivity().removeSimpleLink(e1);
				if(!boundary) {
					triangulation.getMeshBuilder().changeConnectivity().removeSimpleLink(e2);
				}

				E survivor = triangulation.getMeshBuilder().changeConnectivity().remove2DVertex(vertex, true);
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

			E twin = getMesh().edges().getTwin(edge);
			F f1 = getMesh().faces().getOf(edge);
			F f2 = getMesh().faces().getTwin(edge);

			if(!isRefinable(edge, f1)) {
				if(isBlue(f1)) {
					E redEdge = findRed(f1);
					E greenEdge = findGreen(getMesh().faces().getTwin(redEdge));
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
					E greenEdge = findGreen(getMesh().faces().getTwin(redEdge));
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
		E twin = getMesh().edges().getTwin(edge);
		if(!getMesh().edges().isAtBoundary(edge) && isRed(edge) &&
				(isRed(getMesh().edges().getNext(edge)) || isRed(getMesh().edges().getPrev(edge)) || isRed(getMesh().edges().getNext(twin)) || isRed(getMesh().edges().getPrev(twin)))) {
			return true;
		}
		return false;
	}

	private List<E> valid() {
		return getMesh().edges().stream().filter(e -> invalid(e)).collect(Collectors.toList());
	}

	/**
	 * Splits a green edge.
	 * @param edge
	 */
	private void splitGreen(@NotNull final E edge) {
		boolean isBoundary1 = getMesh().edges().isBoundary(edge);
		boolean isBoundary2 = getMesh().edges().isBoundary(getMesh().edges().getTwin(edge));

		boolean isGreen1 = !isBoundary1 && isGreen(getMesh().faces().getOf(edge));
		boolean isGreen2 = !isBoundary2 && isGreen(getMesh().faces().getTwin(edge));

//		boolean isRed1 = isRed(getMesh().getFace(edge));
//		boolean isRed2 = isRed(getMesh().getTwinFace(edge));

		int level = getLevel(edge);

		V v1 = !isBoundary1 ? getMesh().vertices().getOpposite(edge) : null;
		V v2 = !isBoundary2 ? getMesh().vertices().getOpposite(getMesh().edges().getTwin(edge)) : null;

		// split operation
		V v = split(edge);
		toCoarse.addFirst(v);
		// end split operation

		// adjust the possible two edges that split the old faces
		if(!isBoundary1) {
			adjustMiddleEdge(getMesh().edges().getOf(v1, v).get(), level, isGreen1);
		}

		if(!isBoundary2) {
			adjustMiddleEdge(getMesh().edges().getOf(v2, v).get(), level, isGreen2);
		}

		for(E e : getMesh().edges().iterableFor(v)) {
			if(edgeRefinementPredicate.test(e)) {
				toRefine.addLast(e);
			}

			//TODO: required?
			E pe = getMesh().edges().getPrev(e);
			if(edgeRefinementPredicate.test(pe)) {
				toRefine.addLast(getMesh().edges().getPrev(pe));
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

			if(canFlipToRefine(getMesh().edges().getNext(edge))) {
				flipToRefine(getMesh().edges().getNext(edge));
			}

			E e1Twin = getMesh().edges().getTwin(edge);
			if(canFlipToRefine(getMesh().edges().getPrev(e1Twin))) {
				flipToRefine(getMesh().edges().getPrev(e1Twin));
			}
		}
	}

	private V split(@NotNull final E edge) {
		boolean isBoundary1 = getMesh().edges().isBoundary(edge);
		boolean isBoundary2 = getMesh().edges().isBoundary(getMesh().edges().getTwin(edge));

		E prev = getMesh().edges().getPrev(edge);
		E next = getMesh().edges().getNext(edge);
		E twin = getMesh().edges().getTwin(edge);
		E twinNext = getMesh().edges().getNext(twin);
		boolean flipable = isFlipable(edge);

		V v1 = getMesh().vertices().getEndOf(next);
		V v2 = getMesh().vertices().getEndOf(twinNext);
		int level = getLevel(edge);

		Pair<E, E> split = triangulation.getMeshBuilder().changeConnectivity().splitEdge(edge, false);
		V v = getMesh().vertices().getEndOf(split.getLeft());

		setLevel(v, level + 1);

		setLevel(getMesh().edges().getNext(prev), level + 1);
		setLevel(getMesh().edges().getPrev(next), level + 1);
		setColor(getMesh().edges().getNext(prev), Coloring.GREEN);
		setColor(getMesh().edges().getPrev(next), Coloring.GREEN);
		setFlipable(getMesh().edges().getNext(prev), flipable);
		setFlipable(getMesh().edges().getPrev(next), flipable);

		if(!isBoundary1) {
			E e1 = getMesh().edges().getOf(v1, v).get();
			setFlipable(e1, true);
		}

		if(!isBoundary2) {
			E e2 = getMesh().edges().getOf(v2, v).get();
			setFlipable(e2, true);
		}
		return v;
	}

	private void flipToRefine(@NotNull final E edge) {
		if(canFlipToRefine(edge) && isFlipable(edge)) {

			F f1 = getMesh().faces().getOf(edge);
			F f2 = getMesh().faces().getTwin(edge);
			E twin = getMesh().edges().getTwin(edge);


			int level = getLevel(edge);
			triangulation.getMeshBuilder().changeConnectivity().flip(edge);

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
			F f1 = getMesh().faces().getOf(edge);
			F f2 = getMesh().faces().getTwin(edge);
			E twin = getMesh().edges().getTwin(edge);

			int level = getLevel(edge);

			if(canGGSwap(edge)) {
				triangulation.getMeshBuilder().changeConnectivity().flip(edge);
				setLevel(edge, level - 1);
				setColor(edge, Coloring.RED);
				return true;
			} else if(isBlue(f1) && isBlue(f2) && isRed(edge)) {
				triangulation.getMeshBuilder().changeConnectivity().flip(edge);
				setLevel(edge, level + 1);
				setColor(edge, Coloring.GREEN);
				return true;
			} else if(isRed(edge) && ((isBlue(f2) && isRed(f1)) || (isBlue(f1) && isRed(f2)))) {
				triangulation.getMeshBuilder().changeConnectivity().flip(edge);
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
		for(E e : getMesh().edges().iterableFor(face)) {
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
		for(E e : getMesh().edges().iterableFor(face)) {
			if(isRed(e) && getLevel(e) < level) {
				level = getLevel(e);
				result = e;
			}
		}
		return result;
	}

	private boolean canFlipToRefine(@NotNull final E edge) {
		F f1 = getMesh().faces().getOf(edge);
		F f2 = getMesh().faces().getTwin(edge);
		return !getMesh().edges().isAtBoundary(edge) && isRed(edge) && isBlue(f1) && isBlue(f2) && getLevel(edge) == getLevel(f1) && getLevel(edge) == getLevel(f2);
	}

	private boolean isRemoveable(@NotNull V vertex) {
		int level = getLevel(vertex);
		return level > 0 && level >= getMesh().vertices().streamVerticesOf(vertex).mapToInt(v -> getLevel(v)).max().orElse(0);
	}

	/**
	 * An edge at level l is refinable (i.e. it can be split) if and only if it is green and its two adjacent triangles t0 and t1
	 * are both at level l. In case of a boundary edge, only one such triangle exists.
	 *
	 * @param edge the edge
	 * @return true if the edge is refinable, false otherwise
	 */
	private boolean isRefinable(@NotNull E edge) {
		boolean refinable1 = isRefinable(edge, getMesh().faces().getOf(edge));
		boolean refinable2 = isRefinable(getMesh().edges().getTwin(edge), getMesh().faces().getTwin(edge));
		return  refinable1 && refinable2;
	}

	private boolean isRefinable(@NotNull E edge, @NotNull F face) {
		int level = getLevel(edge);
		return isGreen(edge) && (getMesh().edges().isBoundary(edge) || getLevel(face) == level);
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
		assert getMesh().vertices().getAllOf(face).size() == 3;
		E e1 = getMesh().edges().getAnyOf(face);
		E e2 = getMesh().edges().getNext(e1);
		E e3 = getMesh().edges().getNext(e2);
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
		E e1 = getMesh().edges().getAnyOf(face);
		E e2 = getMesh().edges().getNext(e1);
		E e3 = getMesh().edges().getNext(e2);
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
		assert getMesh().vertices().getAllOf(face).size() == 3;
		E e1 = getMesh().edges().getAnyOf(face);
		E e2 = getMesh().edges().getNext(e1);
		E e3 = getMesh().edges().getNext(e2);
		return getLevel(e1) == getLevel(e2) && getLevel(e1) == getLevel(e3);
	}

	private int getLevel(@NotNull final V vertex) {
		return getMeshDataStorage().getIntegerData(vertex, propertyLevel);
	}

	public int getLevel(@NotNull final E edge) {
		int level = getMeshDataStorage().getIntegerData(edge, propertyLevel);;
		return level;
	}

	/**
	 * The level of a triangle is defined to be the lowest amongst the levels of its edges.
	 *
	 * @param face a triangle
	 * @return the level of the triangle
	 */
	private int getLevel(@NotNull final F face) {
		assert getMesh().vertices().getAllOf(face).size() == 3;
		E e1 = getMesh().edges().getAnyOf(face);
		E e2 = getMesh().edges().getNext(e1);
		E e3 = getMesh().edges().getNext(e2);
		return Math.min(getLevel(e1), Math.min(getLevel(e2), getLevel(e3)));
	}

	public void setLevel(@NotNull final V vertex, final int level) {
		getMeshDataStorage().setIntegerData(vertex, propertyLevel, level);
	}

	public void setLevel(@NotNull final E edge, final int level) {
		getMeshDataStorage().setIntegerData(edge, propertyLevel, level);
		getMeshDataStorage().setIntegerData(getMesh().edges().getTwin(edge), propertyLevel, level);
	}

	public void setFlipable(@NotNull final E edge, final boolean flipable) {
		getMeshDataStorage().setBooleanData(edge, propertyFlipable, flipable);
		getMeshDataStorage().setBooleanData(getMesh().edges().getTwin(edge), propertyFlipable, flipable);
	}

	/**
	 * Make sure no original edges will be flipped.
	 * Such that the structure of the base triangulation is still there.
	 *
	 * @param edge the edge
	 * @return true if the edge is not part of any edge of the base triangulation, false otherwise
	 */
	private boolean isFlipable(@NotNull final E edge){
		return getMeshDataStorage().getBooleanData(edge, propertyFlipable);
	}

	private Coloring getColor(@NotNull final V vertex) {
		return getMeshDataStorage().getData(vertex, propertyColor, Coloring.class).orElse(Coloring.GREEN);
	}

	private Coloring getColor(@NotNull final E edge) {
		Optional<Coloring> color = getMeshDataStorage().getData(edge, propertyColor, Coloring.class);
		return color.orElse(Coloring.GREEN);
	}

	private Coloring getColor(@NotNull final F face) {
		return getMeshDataStorage().getData(face, propertyColor, Coloring.class).orElse(Coloring.GREEN);
	}

	private void setColor(@NotNull final V vertex, final Coloring coloring) {
		getMeshDataStorage().setData(vertex, propertyColor, coloring);
	}

	private void setColor(@NotNull final E edge, final Coloring coloring) {
		getMeshDataStorage().setData(edge, propertyColor, coloring);
		getMeshDataStorage().setData(getMesh().edges().getTwin(edge), propertyColor, coloring);
	}

	private void setColor(@NotNull final F face, final Coloring coloring) {
		getMeshDataStorage().setData(face, propertyColor, coloring);
	}

	public void setMaxLevel(int maxLevel) {
		this.maxLevel = maxLevel;
	}
}
