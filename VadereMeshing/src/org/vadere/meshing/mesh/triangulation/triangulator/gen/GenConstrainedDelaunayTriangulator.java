package org.vadere.meshing.mesh.triangulation.triangulator.gen;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.gen.IncrementalTriangulation;
import org.vadere.meshing.mesh.impl.PSLG;
import org.vadere.meshing.mesh.inter.IFace;
import org.vadere.meshing.mesh.inter.IHalfEdge;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.meshing.mesh.inter.IMesh;
import org.vadere.meshing.mesh.inter.IVertex;
import org.vadere.meshing.mesh.triangulation.triangulator.inter.ITriangulator;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VTriangle;
import org.vadere.util.logging.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * This class is an implementation of the algorithm of Sloan [1]
 * to compute the Constrained Delaunay Triangulation (CDT).
 *
 * @param <P> the type of the points (containers)
 * @param <CE> the type of container of the half-edges
 * @param <CF> the type of the container of the faces
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
public class GenConstrainedDelaunayTriangulator<P extends IPoint, CE, CF, V extends IVertex<P>, E extends IHalfEdge<CE>, F extends IFace<CF>> implements ITriangulator<P, CE, CF, V, E, F> {
	private final static Logger logger = Logger.getLogger(GenConstrainedDelaunayTriangulator.class);
	private final IIncrementalTriangulation<P, CE, CF, V, E, F> triangulation;
	private final Collection<VLine> constrains;
	private final Collection<Pair<V, V>> vConstrains;
	private final Collection<E> eConstrains;
	private final Collection<P> points;
	private boolean generated;
	private boolean conforming;

	public GenConstrainedDelaunayTriangulator(
			@NotNull final Supplier<IMesh<P, CE, CF, V, E, F>> meshSupply,
			@NotNull final PSLG pslg,
			final boolean confirming) {

		this.conforming = confirming;
		this.constrains = pslg.getAllSegments();
		this.points = Collections.EMPTY_LIST;
		this.vConstrains = new ArrayList<>(constrains.size());
		this.eConstrains = new ArrayList<>(constrains.size());

		/**
		 * This prevent the flipping of constrained edges
		 */
		Predicate<E> canIllegal = e -> !eConstrains.contains(e) && !eConstrains.contains(getMesh().getTwin(e));
		this.triangulation = new IncrementalTriangulation<>(meshSupply.get(), pslg.getBoundingBox(), canIllegal);
	}

	@Override
	public IIncrementalTriangulation<P, CE, CF, V, E, F> generate(final boolean finalize) {
		if(!generated) {
			computeDelaunayTriangulation(false);
			for(Pair<V, V> constrain : vConstrains) {
				LinkedList<E> newEdges = forceConstrain(constrain);
				reinforceDelaunayCriteria(constrain, newEdges);
			}
			generated = true;
		}

		for(Pair<V, V> constrain : vConstrains) {
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

		if(conforming) {
			reinforceConformingCriteria();
		}

		if(finalize) {
			getTriangulation().finish();
		}

		return triangulation;
	}

	// TODO: this is slow!
	private void reinforceConformingCriteria() {
		Optional<E> nonConformingEdge;
		do {
			nonConformingEdge = eConstrains.stream()
					.filter(edge -> !getMesh().isAtBoundary(edge))
					.filter(edge -> getTriangulation().isDelaunayIllegal(edge))
					.findAny();

			if(nonConformingEdge.isPresent()) {
				split(nonConformingEdge.get(), eConstrains);
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
	public IMesh<P, CE, CF, V, E, F> getMesh() {
		return triangulation.getMesh();
	}

	public Collection<E> getConstrains() {
		return eConstrains;
	}

	@Override
	public IIncrementalTriangulation<P, CE, CF, V, E, F> getTriangulation() {
		return triangulation;
	}

	private void reinforceDelaunayCriteria(@NotNull final Pair<V, V> contrain, @NotNull final LinkedList<E> newEdges) {
		V v1 = contrain.getLeft();
		V v2 = contrain.getRight();

		while (!newEdges.isEmpty()) {
			E edge = newEdges.removeFirst();
			VLine vEdge = triangulation.getMesh().toLine(edge);

			// the edge is not actually equal to the constrain
			if(GeometryUtils.intersectLine(v1.getX(), v1.getY(), v2.getX(), v2.getY(), vEdge.x1, vEdge.y1, vEdge.x2, vEdge.y2, GeometryUtils.DOUBLE_EPS)) {
				if(triangulation.isIllegal(edge)) {
					triangulation.flip(edge);
					newEdges.addLast(edge);
				}
			}

		}
	}

	private LinkedList<E> forceConstrain(@NotNull final Pair<V, V> contrain) {
		LinkedList<E> newEdges = new LinkedList<>();
		V v1 = contrain.getLeft();
		V v2 = contrain.getRight();
		LinkedList<E> intersectingEdges = triangulation.getIntersectingEdges(v1, v2);

		while (!intersectingEdges.isEmpty()) {
			E edge = intersectingEdges.removeFirst();

			VLine vEdge = triangulation.getMesh().toLine(edge);

			// to be save
			if(GeometryUtils.intersectLineSegment(vEdge.x1, vEdge.y1, vEdge.x2, vEdge.y2, v1.getX(), v1.getY(), v2.getX(), v2.getY())) {
				E next = triangulation.getMesh().getNext(edge);
				E prev = triangulation.getMesh().getPrev(edge);
				E twin = triangulation.getMesh().getTwin(edge);
				P q = triangulation.getMesh().getPoint(triangulation.getMesh().getNext(twin));

				// convex quadrilateral
				if(triangulation.isLeftOf(q.getX(), q.getY(), prev) && triangulation.isLeftOf(q.getX(), q.getY(), next)) {
					triangulation.flip(edge);

					vEdge = triangulation.getMesh().toLine(edge);
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
		IMesh<P, CE, CF, V, E, F> mesh = triangulation.getMesh();

		triangulation.insert(points);

		for(VLine constrain : constrains) {
			boolean insertPair = true;
			P p1 = mesh.createPoint(constrain.x1, constrain.y1);
			P p2 = mesh.createPoint(constrain.x2, constrain.y2);

			E edge1 = triangulation.insert(p1);
			V v1 = triangulation.getMesh().getVertex(edge1);
			// could not insert p1
			if(!getMesh().getPoint(v1).equals(p1)) {
				logger.warn("could not insert " + p1);
				insertPair = false;
			}

			E edge2 = triangulation.insert(p2);
			V v2 = triangulation.getMesh().getVertex(edge2);
			// could not insert p2
			if(!getMesh().getPoint(v2).equals(p2)) {
				logger.warn("could not insert " + p2);
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
