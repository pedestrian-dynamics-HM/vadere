package org.vadere.meshing.mesh.triangulation.triangulator.gen;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.gen.IncrementalTriangulation;
import org.vadere.meshing.mesh.inter.IFace;
import org.vadere.meshing.mesh.inter.IHalfEdge;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.meshing.mesh.inter.IMesh;
import org.vadere.meshing.mesh.inter.IVertex;
import org.vadere.meshing.mesh.triangulation.triangulator.inter.ITriangulator;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VRectangle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Set;
import java.util.function.Predicate;

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

	private final IIncrementalTriangulation<P, CE, CF, V, E, F> triangulation;
	private final Collection<VLine> constrains;
	private final Collection<Pair<V, V>> vConstrains;
	private final Collection<E> eConstrains;
	private final Set<P> points;
	private boolean generated;


	public GenConstrainedDelaunayTriangulator(
			@NotNull final IMesh<P, CE, CF, V, E, F> mesh,
			@NotNull final VRectangle bound,
			@NotNull final Collection<VLine> constrains,
			@NotNull final Set<P> points) {
		this.triangulation = new IncrementalTriangulation<>(mesh, bound, halfEdge -> true);
		this.constrains = constrains;
		this.points = points;
		this.vConstrains = new ArrayList<>(constrains.size());
		this.eConstrains = new ArrayList<>(constrains.size());
	}

	public GenConstrainedDelaunayTriangulator(
			@NotNull final IMesh<P, CE, CF, V, E, F> mesh,
			@NotNull final VRectangle bound,
			@NotNull final Collection<VLine> constrains,
			@NotNull final Set<P> points,
			@NotNull final Predicate<E> illegalPredicate) {
		this.triangulation = new IncrementalTriangulation<>(mesh, bound, illegalPredicate);
		this.constrains = constrains;
		this.points = points;
		this.vConstrains = new ArrayList<>(constrains.size());
		this.eConstrains = new ArrayList<>(constrains.size());
	}

	@Override
	public IIncrementalTriangulation<P, CE, CF, V, E, F> generate() {
		if(!generated) {
			computeDelaunayTriangulation();
			for(Pair<V, V> constrain : vConstrains) {
				LinkedList<E> newEdges = forceConstrain(constrain);
				reinforceDelaunayCriteria(constrain, newEdges);
			}
			triangulation.finish();
			generated = true;
		}

		for(Pair<V, V> constrain : vConstrains) {
			V v1 = constrain.getLeft();
			V v2 = constrain.getRight();
			for(E e : getMesh().getEdgeIt(v1)) {
				if(getMesh().getTwinVertex(e).equals(v2)) {
					eConstrains.add(e);
					break;
				}
			}
		}

		return triangulation;
	}

	@Override
	public IMesh<P, CE, CF, V, E, F> getMesh() {
		return triangulation.getMesh();
	}

	public Collection<E> getConstrains() {
		return eConstrains;
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

	private void computeDelaunayTriangulation() {
		triangulation.init();
		IMesh<P, CE, CF, V, E, F> mesh = triangulation.getMesh();

		triangulation.insert(points);

		for(VLine constrain : constrains) {
			P p1 = mesh.createPoint(constrain.x1, constrain.y1);
			P p2 = mesh.createPoint(constrain.x2, constrain.y2);

			E edge1 = triangulation.insert(p1);
			E edge2 = triangulation.insert(p2);

			V v1 = triangulation.getMesh().getVertex(edge1);
			V v2 = triangulation.getMesh().getVertex(edge2);

			vConstrains.add(Pair.of(v1, v2));
		}
	}
}
