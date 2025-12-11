package org.vadere.meshing.mesh.inter.mesh;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.gen.mesh.arrayBased.elements.AMesh;
import org.vadere.meshing.mesh.gen.pointLocator.DelaunayHierarchyPointLocator;
import org.vadere.meshing.mesh.inter.mesh.data.IMeshDataStorage;
import org.vadere.meshing.mesh.inter.meshConnectivity.IReadOnlyPolyConnectivity;
import org.vadere.meshing.mesh.triangulation.triangulator.inter.ITriangulator;
import org.vadere.meshing.mesh.gen.mesh.pointerBased.elements.PMesh;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.logging.Logger;

import java.util.List;

/**
 * <p>
 * A {@link IMesh} is a set of {@link IFace}, their half-edges {@link IHalfEdge} and vertices {@link IVertex}
 * defining a geometry. It also is a factory for those geometric base elements: vertices, half-edges and faces. The user should use one mesh
 * for exactly one geometric and the user should never create any base element without calling its mesh. Furthermore, the user is responsible for the
 * correctness of the mesh definition e.g. no overlapping edges. There are some classes for automatic mesh generation like
 * {@link ITriangulator} or other factory methods like {@link MeshUtils#createSimpleTriMesh}
 * </p>
 * It uses the half-edge data structure to store all information and is a generic interface to provide different implementations such as:
 * <ul>
 *     <li>A pointer based version which implements a doubled-linked-list data structure {@link PMesh}</li>
 *     <li>An index based version which implements an array data structure {@link AMesh}</li>
 * </ul>
 * <p>
 * {@link org.vadere.meshing.mesh.inter.mesh.builder.IMeshBuilder} is used to create faces, edges, and vertices of the mesh.
 * A boundary can be a hole or the border. A hole is surrounded by faces and the border is the infinite large face representing the space which is not
 * part of any finite face.
 * </p>
 *     We define as base elements: vertices {@link V}, half-edges {@link E} and faces {@link F}.
 *     <ul>
 *         <li>
 *             vertex {@link V}:
 *                  A vertex is the end node / point of a half-edge. A vertex has also a 1 to 1 relation to a half-edge and the half-edge of a vertex can accessed in O(1) time.
 *                  Furthermore, it has a reference to one arbitrary of its half-edges (half-edges ending in it). If the vertex is at the boundary (hole or border) the half-edge should be
 *                  a boundary half-edge but this is not guaranteed but the aim is to have this situation as often as possible to have quick access to boundary half-edges
 *                  to quickly check if the vertex is a boundary vertex! Note that an arbitrary neighbouring face of the vertex can also be accessed in O(1) by fist getting
 *                  its half-edge and extracting from the half-edge the face.
 *         </li>
 *         <li>
 *             half-edge {@link E}:
 *                  A half-edge is part of a full-edge i.e. the half-edge and its twin fully define the full-edge. Each half-edge has a predecessor
 *					and a successor and a twin half-edge {@link E} which can be accessed in O(1). Furthermore, each half-edge is part of exactly one face {@link F} and ends
 *                  in exactly one vertex {@link V} both can be accessed in O(1) time. As one can see the half-edge has the most amount of references (5).
 *         </li>
 *         <li>
 *             face {@link F}:
 *                  A face can be a interior face i.e. a simple polygon, a hole (also a polygon but representing empty space) or the border i.e. the infinite
 *                  face which represents all the space which is not represented by any finite face. An arbitrary half-edge {@link E} can be accessed in O(1).
 *         </li>
 *     </ul>
 * <p>
 * We say a half-edge is a boundary / border / hole edge if it is part of a boundary / hole or the border. A boundary can be a hole or the border (there is only one border).
 * We say a half-edge is at the boundary / border / hole if itself is a boundary / border / hole edge or its twin. Therefore a boundary / border / hole edge is via definition
 * at the boundary / hole / border. Sometimes we say edge instead of half-edge but we try to use full-edge if we explicitly talk about the edge defined by the half-edge and
 * its twin.
 * </p>
 * <p>
 * Note: For all iterators and stream usage it should be clear that if one manipulates the mesh during iteration the result is not clear. Therefore, use those
 * iterators and streams only if no manipulation is done while iterating. If you want to manipulate the data structure, construct a list {@link List} beforehand and
 * iterate over the list {@link List} while changing elements in the mesh. The mesh offers a large set of different iterators and streams to iterate over all neighbouring
 * faces of a face, vertices of a vertex, edges of a vertex or over all edges / vertices / points of a face.
 * </p>
 * @author Benedikt Zoennchen
 *
 * @param <V> the type of the vertices
 * @param <E> the type of the half-edges
 * @param <F> the type of the faces
 */
public interface IMesh<V extends IVertex, E extends IHalfEdge, F extends IFace> extends Cloneable {

	Logger logger = Logger.getLogger(IMesh.class);

	IReadOnlyPolyConnectivity<V, E, F> readConnectivity();

	IMeshEdges<V, E, F> edges();
	IMeshFaces<V, E, F> faces();
	IMeshVertices<V, E, F> vertices();

	// TODO: this is for the delaunay-hierarchy only!
	/**
	 * This is specifically used by {@link DelaunayHierarchyPointLocator}
	 * to establish the link of the different hierarchies in O(1).
	 *
	 * @param v a vertex of hierarchy k
	 * @return the vertex connected to v which is at the hierarchy k-1.
	 */
	V getDown(@NotNull V v);

	// TODO: this is for the delaunay-hierarchy only!
	/**
	 * This is specifically used by {@link DelaunayHierarchyPointLocator}
	 * to establish the link of the different hierarchies. Connects two vertices up and down such that
	 * up is at the hierarchy k and down is at hierarchy k+1 in O(1).
	 *
	 * @param up    vertex at hierarchy k
	 * @param down  vertex at hierarchy k+1
	 */
	void setDown(@NotNull V up, @NotNull V down);

	default String getMeshInformations() {
		// here we divide the number of half-edges by 2 because each edge is represented by 2 half-edges
		return "#vertices = " + vertices().count() +
				", #edges = " + edges().count() / 2 +
				", #faces = " + faces().count();
	}

	/**
	 * Returns a deep clone of this mesh.
	 *
	 * @return a deep clone of this mesh
	 */
	IMesh<V, E, F> copy();

	/**
	 * Returns a rectangular bound containing all vertices of the mesh.
	 *
	 * @return a rectangular bound containing all vertices of the mesh
	 */
	default VRectangle getBound() {

		if(vertices().count() <= 2) {
			return new VRectangle(0,0,1,1);
		}

		double minX = Double.MAX_VALUE;
		double maxX = Double.MIN_VALUE;
		double minY = Double.MAX_VALUE;
		double maxY = Double.MIN_VALUE;

		for(IPoint p : vertices().toPoints()) {
			minX = Math.min(minX, p.getX());
			minY = Math.min(minY, p.getY());
			maxX = Math.max(maxX, p.getX());
			maxY = Math.max(maxY, p.getY());
		}

		return new VRectangle(minX, minY, maxX-minX, maxY-minY);
	}

	IMeshDataStorage<V, E, F> createEmptyDataStorage();

	/**
	 * Tests if the mesh is a valid mesh, i.e. all relations between edges, faces and vertices are correct,
	 * e.g. <tt>getFace(getEdge(face)) == face</tt>.
	 *
	 * @return true if the mesh is valid, false otherwise
	 */
	default boolean isValid() {
		final String warnLogPrefix = "invalid mesh: ";

		var faces = faces();
		var edges = edges();
		var vertices = vertices();

		int vertexCount = vertices.count();

		for(F face : faces.getAllWithBoundary()) {
			int count = 0;
			for(E edge : edges.iterableFor(face)) {
				count++;
				if(count > edges.count()) {
					logger.warn(warnLogPrefix + "endless loop in face");
					return false;
				}

				F f = faces.getOf(edge);

				if(f == null) {
					logger.warn(warnLogPrefix + "null face of edge " + edge);
					return false;
				}

				if(!f.equals(face)) {
					logger.warn(warnLogPrefix + "wrong edge face " + face + "!=" + faces.getOf(edge));
					return false;
				}
			}

			if(count < 3) {
				logger.warn(warnLogPrefix + "number of edges smaller 2");
				return false;
			}

			count = 0;
			for(V v : vertices.iterableFor(face)) {
				count++;

				if(count > vertexCount) {
					logger.warn(warnLogPrefix + "endless loop in face");
					return false;
				}
			}
		}

		for(V vertex : vertices().getAll()) {
			int count = 0;
			E edge = edges().getOf(vertex);
			if(edge == null) {
				logger.warn(warnLogPrefix + "null edge of vertex " + vertex);
				return false;
			}

			if(!vertex.equals(vertices.getEndOf(edge))) {
				logger.warn(warnLogPrefix + "wrong edge vertex " + vertex + "!=" + vertices.getEndOf(edge));
				return false;
			}

			for(E e : edges.iterableFor(vertex)) {
				if(count > vertexCount) {
					logger.warn(warnLogPrefix + "endless loop around vertex " + vertex);
					return false;
				}

				if(!vertex.equals(vertices.getEndOf(e))) {
					logger.warn(warnLogPrefix + "wrong edge vertex " + vertex + "!=" + vertices.getEndOf(e));
					return false;
				}
			}
		}

		for(E edge : edges.getAll()) {
			E twin = edges.getTwin(edge);
			E next = edges.getNext(edge);
			E prev = edges.getPrev(edge);
			V v = vertices().getEndOf(edge);
			F face = faces().getOf(edge);

			if(twin == null) {
				logger.warn(warnLogPrefix + "twin is null for " + edge);
				return false;
			}

			if(next == null) {
				logger.warn(warnLogPrefix + "next is null for " + edge);
				return false;
			}

			if(prev == null) {
				logger.warn(warnLogPrefix + "prev is null for " + edge);
				return false;
			}

			if(v == null) {
				logger.warn(warnLogPrefix + "vertex is null for " + edge);
				return false;
			}

			E twinTwin = edges.getTwin(twin);

			if(twinTwin == null) {
				logger.warn(warnLogPrefix + "twin of the twin is null for " + edge);
				return false;
			}

			if(!twinTwin.equals(edge)) {
				logger.warn(warnLogPrefix + "twin of the twin is not equal to the edge " + edge);
				return false;
			}

			V twinVertex = vertices.getEndOf(twin);

			if(twinVertex == null) {
				logger.warn(warnLogPrefix + "vertex of the twin is null for " + edge);
				return false;
			}

			if(twinVertex.equals(v)) {
				logger.warn(warnLogPrefix + "edge ends and starts at the same vertex " + v);
				return false;
			}

			F twinFace = faces.getOf(twin);
			if(twinFace.equals(face)) {
				logger.warn(warnLogPrefix + "the faces of the edge and its twin are equals");
				return false;
			}

			if(edges.isBoundary(edge) && edges.isBoundary(twin)) {
				logger.warn(warnLogPrefix + "the faces of the edge and its twin are boundaries");
				return false;
			}
		}

		return true;
	}

	/**
	 * A factory method which creates a new point. The point will not be inserted into the mesh data
	 * structure.
	 *
	 * @param x x-coordinate
	 * @param y y-coordinate
	 * @return a point.
	 */
	IPoint createPoint(final double x, final double y);

	// this has been marked for deletion by Bene but is still in use
	default void getVirtualSupport(@NotNull final V v, @NotNull final E edge, @NotNull final List<Pair<V, V>> virtualSupport) {
		//assert isNonAcute(getMesh().getVertex(edge), getMesh().getVertex(getMesh().getNext(edge)), getMesh().getVertex(getMesh().getPrev(edge)));

		var edges = edges();
		if(edges.isAtBoundary(edge)) {
			return;
		}

		var vertices = vertices();

		E prev = edges.getPrev(edge);
		E twin = edges.getTwin(edge);

		V v1 = vertices.getEndOf(prev);
		V v2 = vertices.getEndOf(edge);
		V u = vertices.getEndOf(edges.getNext(twin));

		if(!vertices.isNonAcute(u, v, v1)) {
			virtualSupport.add(Pair.of(v1, u));
		} else {
			getVirtualSupport(v, edges.getNext(twin), virtualSupport);
		}

		if(!vertices.isNonAcute(v2, v, u)) {
			virtualSupport.add(Pair.of(v2, u));
		} else {
			getVirtualSupport(v, edges.getPrev(twin), virtualSupport);
		}
	}
}
