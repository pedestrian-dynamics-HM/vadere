

package org.vadere.meshing.mesh.triangulation.triangulator.gen;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.gen.IncrementalTriangulation;
import org.vadere.meshing.mesh.impl.PSLG;
import org.vadere.meshing.mesh.inter.mesh.*;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.meshing.mesh.inter.mesh.builder.ITriangleMeshBuilder;
import org.vadere.meshing.mesh.triangulation.triangulator.inter.ITriangulator;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.logging.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * This class is an implementation of the algorithm of Sloan [1]
 * to compute the Constrained Delaunay Triangulation (CDT).
 *
 * @param <V> the type of the vertices
 * @param <E> the type of the half-edges
 * @param <F> the type of the faces
 *
 * <b>References:</b>
 * <ol>
 *     <li>
 *           <a href="https://www.newcastle.edu.au/__data/assets/pdf_file/0019/22519/23_A-fast-algortithm-for-generating-constrained-Delaunay-triangulations.pdf">Algorithm of Sloan</a>
 *     </li>
 * </ol>
 *
 * @author Benedikt Zoennchen
 */
public class GenConstrainedDelaunayTriangulator<V extends IVertex, E extends IHalfEdge, F extends IFace> implements ITriangulator<V, E, F> {
	private final static Logger logger = Logger.getLogger(GenConstrainedDelaunayTriangulator.class);
	private final IIncrementalTriangulation<V, E, F> triangulation;
	private final Collection<VLine> constrains;
	private final Collection<Pair<V, V>> vConstrains;
	private final Set<E> eConstrains;
	private final Collection<IPoint> points;
	private final Map<V, VLine> projectionMap;
	private boolean generated;
	private boolean conforming;
	private boolean allowSegmentFaces;

	public GenConstrainedDelaunayTriangulator(
			@NotNull final Supplier<ITriangleMeshBuilder<V, E, F>> meshSupply,
			@NotNull final PSLG pslg,
			final boolean confirming) {
		this(IncrementalTriangulation.fromBuilderFactory(meshSupply, pslg.getBoundingBox()), pslg, confirming);
	}

	public GenConstrainedDelaunayTriangulator(
			@NotNull final IIncrementalTriangulation<V, E, F> triangulation,
			@NotNull final PSLG pslg,
			final boolean confirming) {
		this(triangulation, pslg.getAllSegments(), confirming);
	}

	public GenConstrainedDelaunayTriangulator(
			@NotNull final IIncrementalTriangulation<V, E, F> triangulation,
			@NotNull final Collection<VLine> constrains,
			final boolean confirming) {
		this(triangulation, constrains, confirming, true);
	}

	public GenConstrainedDelaunayTriangulator(
			@NotNull final IIncrementalTriangulation<V, E, F> triangulation,
			@NotNull final Collection<VLine> constrains,
			final boolean confirming,
			final boolean allowSegmentFaces) {

		this.conforming = confirming;
		this.constrains = constrains;
		this.allowSegmentFaces = allowSegmentFaces;
		this.points = Collections.EMPTY_LIST;
		this.vConstrains = new ArrayList<>(constrains.size());
		this.eConstrains = new HashSet<>(constrains.size());
		this.projectionMap = new HashMap<>();

		/**
		 * This prevent the flipping of constrained edges
		 */
		Predicate<E> canIllegal = e -> !eConstrains.contains(e) && !eConstrains.contains(getMesh().edges().getTwin(e));
		this.triangulation = triangulation;
		this.triangulation.setCanIllegalPredicate(canIllegal);
	}

	@Override
	public IIncrementalTriangulation<V, E, F> generate(final boolean finalize) {
		if(!generated) {
			computeDelaunayTriangulation(false);
			for(Pair<V, V> constrain : vConstrains) {
				//System.out.println("force constrains for " + constrain.getLeft() + ", " + constrain.getRight());
				LinkedList<E> newEdges = forceConstrain(constrain);
				//System.out.println("reinforce Delaunay criteria " + constrain.getLeft() + ", " + constrain.getRight());
				reinforceDelaunayCriteria(constrain, newEdges);
			}
			generated = true;
		}

		for(Pair<V, V> constrain : vConstrains) {
			V v1 = constrain.getLeft();
			V v2 = constrain.getRight();
			for(E e : getMesh().edges().iterableFor(v1)) {
				if(getMesh().vertices().getTwin(e).equals(v2)) {
					eConstrains.add(e);
					eConstrains.add(getMesh().edges().getTwin(e));
					break;
				}
			}
		}

		if(!allowSegmentFaces) {
			split();
		}

		if(conforming) {
			reinforceConformingCriteria();
		}

		if(finalize) {
			getTriangulation().finish();
		}

		return triangulation;
	}

	private void split() {
		List<E> edges = getMesh().edges().getAll();
		for(E edge : edges) {
			if(!eConstrains.contains(edge)) {
				V v1 = getMesh().vertices().getEndOf(edge);
				V v2 = getMesh().vertices().getTwin(edge);
				if(isSegmentVertex(v1) && isSegmentVertex(v2)) {
					getTriangulation().getMeshBuilder().changeConnectivity().splitEdge(edge, true);
				}
			}
		}
	}

	private boolean isSegmentVertex(@NotNull final V v) {
		return getMesh().edges().streamEdgesOf(v).anyMatch(e -> eConstrains.contains(e));
	}

	// TODO: this is slow!
	private void reinforceConformingCriteria() {
		/*
		 * TODO: remember the 2 vertices and connect them to all created vertices by splitting!
		 *
		 * corner vertices have 2 possible split lines!
		 */
		Map<E, VLine> projectionLines = new HashMap<>();
		var vertices = getMesh().vertices();
		for(E constrain : eConstrains) {
			VLine projectionLine = new VLine(
					vertices.toPoint(vertices.getEndOf(constrain)),
					vertices.toPoint(vertices.getTwin(constrain))
			);
			projectionLines.put(constrain, projectionLine);
		}


		Optional<E> nonConformingEdge;
		do {
			// TODO this seems expensive!
			nonConformingEdge = eConstrains.stream()
					.filter(edge -> !getMesh().edges().isAtBoundary(edge))
					.filter(edge -> getTriangulation().getMeshBuilder().changeConnectivity().isDelaunayIllegal(edge))
					.findAny();

			if(nonConformingEdge.isPresent()) {
				// this call will remove 2 element from eConstrains and will add 4 new ones
				VLine line = projectionLines.get(nonConformingEdge.get());
				if(line == null) {
					line = projectionMap.get(getMesh().vertices().getEndOf(nonConformingEdge.get()));
				}
				V splitVertex = split(nonConformingEdge.get(), eConstrains);
				projectionMap.put(splitVertex, line);
			}
		} while (nonConformingEdge.isPresent());
	}

	// remove again!
	/*public void step(boolean finalize) {
		if(!generated) {
			computeDelaunayTriangulation(finalize);
			for(Pair<V, V> constrain : vConstrains) {
				LinkedList<E> newEdges = forceConstrain(constrain);
				reinforceDelaunayCriteria(constrain, newEdges);
			}
			//triangulation.finish();
			generated = true;
		}

		if(!vConstrains.isEmpty()) {
			Pair<V, V> constrain = vConstrains.iterator().next();
			vConstrains.remove(constrain);
			V v1 = constrain.getLeft();
			V v2 = constrain.getRight();
			for(E e : getMesh().getEdgeIt(v1)) {
				if(getMesh().getTwinVertex(e).equals(v2)) {
					eConstrains.add(e);
					eConstrains.add(getMesh().getTwin(e));
					break;
				}
			}
		}
	}*/

	@Override
	public ITriangleMeshBuilder<V, E, F> getMeshBuilder() {
		return triangulation.getMeshBuilder();
	}

	public Collection<E> getConstrains() {
		return eConstrains;
	}

	public Map<V, VLine> getProjections() {
		return projectionMap;
	}

	@Override
	public IIncrementalTriangulation<V, E, F> getTriangulation() {
		return triangulation;
	}

	private void reinforceDelaunayCriteria(@NotNull final Pair<V, V> contrain, @NotNull final LinkedList<E> newEdges) {
		V v1 = contrain.getLeft();
		V v2 = contrain.getRight();

		while (!newEdges.isEmpty()) {
			E edge = newEdges.removeFirst();
			VLine vEdge = getMesh().edges().toLine(edge);

			// the edge is not actually equal to the constrain
			if(GeometryUtils.intersectLine(v1.getX(), v1.getY(), v2.getX(), v2.getY(), vEdge.x1, vEdge.y1, vEdge.x2, vEdge.y2, GeometryUtils.DOUBLE_EPS)) {
				if(triangulation.getMeshBuilder().changeConnectivity().isIllegal(edge)) {
					triangulation.getMeshBuilder().changeConnectivity().flip(edge);
					newEdges.addLast(edge);
				}
			}

		}
	}

	private LinkedList<E> forceConstrain(@NotNull final Pair<V, V> contrain) {
		LinkedList<E> newEdges = new LinkedList<>();
		V v1 = contrain.getLeft();
		V v2 = contrain.getRight();
		LinkedList<E> intersectingEdges = getMesh().readConnectivity().getIntersectingEdges(v1, v2);

		var edges = getMesh().edges();
		while (!intersectingEdges.isEmpty()) {
			E edge = intersectingEdges.removeFirst();

			VLine vEdge = edges.toLine(edge);

			// to be save, TODO inconsistent geometry check which may lead to a deadlock (isLeftOfRobust is "robust" while intersectLineSegment is not")
			if(GeometryUtils.intersectLineSegment(vEdge.x1, vEdge.y1, vEdge.x2, vEdge.y2, v1.getX(), v1.getY(), v2.getX(), v2.getY())) {
				E next = edges.getNext(edge);
				E prev = edges.getPrev(edge);
				E twin = edges.getTwin(edge);
				IPoint q = edges.getMutableEndPoint(edges.getNext(twin));

				// convex quadrilateral
				if(getMesh().readConnectivity().isLeftOf(q.getX(), q.getY(), prev) && getMesh().readConnectivity().isLeftOf(q.getX(), q.getY(), next)) {
					getMeshBuilder().changeConnectivity().flip(edge);

					vEdge = getMesh().edges().toLine(edge);
					if(GeometryUtils.intersectLineSegment(vEdge.x1, vEdge.y1, vEdge.x2, vEdge.y2, v1.getX(), v1.getY(), v2.getX(), v2.getY())) {
						intersectingEdges.addLast(edge);
					} else {
						newEdges.add(edge);
					}
				}
				else {
					intersectingEdges.addLast(edge);
				}
			}
		}

		return newEdges;
	}

	private void computeDelaunayTriangulation(final boolean finalize) {
		triangulation.init();
		triangulation.insert(points);

		for(VLine constrain : constrains) {
			boolean insertPair = true;
			IPoint p1 = getMesh().createPoint(constrain.x1, constrain.y1);
			IPoint p2 = getMesh().createPoint(constrain.x2, constrain.y2);

			E edge1 = triangulation.insert(p1);
			V v1 = getMesh().vertices().getEndOf(edge1);
			// could not insertVertex p1
			if(!getMesh().vertices().toMutablePoint(v1).equals(p1)) {
				logger.warn("could not insertVertex " + p1);
				insertPair = false;
			}

			E edge2 = triangulation.insert(p2);
			V v2 = getMesh().vertices().getEndOf(edge2);
			// could not insertVertex p2
			if(!getMesh().vertices().toMutablePoint(v2).equals(p2)) {
				logger.warn("could not insertVertex " + p2);
				insertPair = false;
			}

			if(insertPair) {
				vConstrains.add(Pair.of(v1, v2));
			}
		}

		if(finalize) {
			triangulation.finish();
		}
	}
}
