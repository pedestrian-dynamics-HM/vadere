package org.vadere.meshing.mesh.inter;

import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Streams;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vadere.meshing.mesh.gen.AMesh;
import org.vadere.meshing.mesh.gen.DelaunayHierarchy;
import org.vadere.meshing.mesh.iterators.AdjacentFaceIterator;
import org.vadere.meshing.mesh.iterators.AdjacentVertexIterator;
import org.vadere.meshing.mesh.iterators.EdgeIterator;
import org.vadere.meshing.mesh.iterators.EdgeOfVertexIterator;
import org.vadere.meshing.mesh.iterators.IncidentEdgeIterator;
import org.vadere.meshing.mesh.iterators.PointIterator;
import org.vadere.meshing.mesh.iterators.SurroundingFaceIterator;
import org.vadere.meshing.mesh.iterators.VertexIterator;
import org.vadere.meshing.mesh.triangulation.triangulator.inter.ITriangulator;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.meshing.mesh.gen.PMesh;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VPolygon;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VTriangle;
import org.vadere.util.logging.Logger;

import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * <p>
 * A {@link IMesh} is a set of {@link IFace}, their half-edges {@link IHalfEdge} and vertices {@link IVertex}
 * defining a geometry. It also is a factory for those geometric base elements: vertices, half-edges and faces. The user should use one mesh
 * for exactly one geometric and the user should never create any base element without calling its mesh. Furthermore, the user is responsible for the
 * correctness of the mesh definition e.g. no overlapping edges. There are some classes for automatic mesh generation like
 * {@link ITriangulator} or other factory methods like {@link IMesh#createSimpleTriMesh}
 * </p>
 It uses the half-edge data structure to store all information and is a generic interface to provide different implementations such as:
 * <ul>
 *     <li>A pointer based version which implements a doubled-linked-list data structure {@link PMesh}</li>
 *     <li>An index based version which implements an array data structure {@link AMesh}</li>
 * </ul>
 * <p>
 * It should be impossible to create faces, edges, and vertices of the mesh without using the mesh i.e. IMesh is a factory for faces, edges and vertices.
 * A boundary can be a hole or the border. A hole is surrounded by faces and the border is the infinite large face representing the space which is not
 * part of any finite face.
 * </p>
 * <p>
 * For all iterators and stream usage it should be clear that if one manipulates the mesh during iteration the result is not clear. Therefore, use those
 * iterators and streams only if no manipulation is done while iterating. If you want to manipulate the data structure, construct a list {@link List} beforehand and
 * iterate over the list {@link List} while changing elements in the mesh. The mesh offers a large set of different iterators and streams to iterate over all neighbouring
 * faces of a face, vertices of a vertex, edges of a vertex or over all edges / vertices / points of a face.
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
 *
 * @author Benedikt Zoennchen
 *
 * @param <V> the type of the vertices
 * @param <E> the type of the half-edges
 * @param <F> the type of the faces
 */
public interface IMesh<V extends IVertex, E extends IHalfEdge, F extends IFace> extends Iterable<F>, Cloneable {

	Logger logger = Logger.getLogger(IMesh.class);

	/**
	 * construct a new empty mesh.
	 *
	 * @return a new fresh empty mesh
	 */
	IMesh<V, E, F> construct();

	/**
	 * Removes deleted base elements from this data structure.
	 * This might be used if removing an element from the mesh does not removes
	 * this element from the data structure i.e. the mesh representing the geometry.
	 * This operation might be expensive O(n) for n points.
	 */
	void garbageCollection();

	/**
	 * Returns the successor of the half-edge {@link E} in O(1).
	 *
	 * @param halfEdge the half-edge
	 * @return the successor of the half-edge {@link E}.
	 */
	E getNext(@NotNull E halfEdge);

	/**
	 * Returns the predecessor of the half-edge {@link E} in O(1).
	 *
	 * @param halfEdge the half-edge
	 * @return the predecessor of the half-edge {@link E}.
	 */
	E getPrev(@NotNull E halfEdge);

	/**
	 * Returns the twin of the half-edge {@link E} in O(1).
	 *
	 * @param halfEdge the half-edge
	 * @return the twin of the half-edge {@link E}.
	 */
	E getTwin(@NotNull E halfEdge);

	/**
	 * Returns the vertex of the twin of the half-edge {@link E} in O(1).
	 *
	 * @param halfEdge the half-edge
	 * @return the vertex of the twin of the half-edge {@link E}.
	 */
	default V getTwinVertex(@NotNull E halfEdge) {
		return getVertex(getTwin(halfEdge));
	}

	/**
	 * Returns the face of the half-edge {@link E} in O(1).
	 *
	 * @param halfEdge the half-edge
	 * @return the face of the half-edge {@link E}.
	 */
	F getFace(@NotNull E halfEdge);

	/**
	 * Returns the face which is not a boundary, i.e. no hole and no border
	 * of the faces of a full-edge which the half-edge is part of in O(1).
	 *
	 * @param halfEdge the half-edge
	 * @return the face which is not a boundary and the face of the half-edge or its twin
	 */
	default F getNonBoundaryFace(@NotNull E halfEdge) {
        if(!isBoundary(halfEdge)) {
            return getFace(halfEdge);
        }
        else {
            return getFace(getTwin(halfEdge));
        }
    }

	/**
	 * Transforms an half-edge into a line segment (p, q) {@link VLine} where p is the point of the half-edge
	 * and q is the point of its predecessor in O(1).
	 *
	 * @param halfEdge the half-edge
	 * @return a line segment (p, q) {@link VLine} which is the transformation of the half-edge
	 */
	default VLine toLine(@NotNull E halfEdge) {
		return new VLine(new VPoint(getVertex(getPrev(halfEdge))), new VPoint(getVertex(halfEdge)));
	}

	/**
	 * Transforms a vertex into a immutable point {@link VPoint}. This might be useful of one wants to
	 * use the vertex in a calculation in O(1).
	 *
	 * @param vertex    the vertex
	 * @return an immutable point
	 */
	default VPoint toPoint(@NotNull V vertex) {
		return new VPoint(vertex);
	}

	/**
	 * Transforms an edge into a immutable point {@link VPoint}. This might be useful of one wants to
	 * use the vertex in a calculation in O(1).
	 *
	 * @param edge  the edge
	 * @return an immutable point
	 */
	default VPoint toPoint(@NotNull E edge) {
		return toPoint(getVertex(edge));
	}

	default VPoint toPoint(@NotNull IPoint p) {
		return new VPoint(p.getX(), p.getY());
	}



	/**
	 * Returns the half-edge of the vertex i.e. one half-edge which ends in the vertex in O(1).
	 *
	 * @param vertex the vertex
	 * @return the half-edge of the vertex.
	 */
	E getEdge(@NotNull V vertex);

	double getX(@NotNull V vertex);

	double getY(@NotNull V vertex);

	void setCoords(@NotNull V vertex, double x, double y);

	/**
	 * Returns a half-edge of the face this can be any half-edge of this face in O(1).
	 *
	 * @param face the face
	 * @return an arbitrary half-edge of the face
	 */
	E getEdge(@NotNull F face);

	/**
	 * Returns the (end-)point of the half-edge in O(1).
	 *
	 * @param halfEdge the half-edge
	 * @return the (end-)point of the half-edge
	 */
	IPoint getPoint(@NotNull E halfEdge);

	/**
	 * Returns the (end-)vertex of the half-edge in O(1).
	 *
	 * @param halfEdge the half-edge
	 * @return the (end-)vertex of the half-edge
	 */
	V getVertex(@NotNull E halfEdge);


	default V getOpposite(@NotNull E edge) {
		assert getEdges(getFace(edge)).size() == 3;
		return getVertex(getNext(edge));
	}

	/**
	 * Returns the degree of a vertex i.e. the number of connected full-edges
	 * in O(d) where d is the degree of the vertex.
	 *
	 * @param vertex the vertex
	 * @return the degree of a vertex
	 */
	default int degree(@NotNull V vertex) {
		return Iterators.size(getAdjacentVertexIt(vertex).iterator());
	}


	// TODO: this is for the delaunay-hierarchy only!
	/**
	 * This is specifically used by {@link DelaunayHierarchy}
	 * to establish the link of the different hierarchies in O(1).
	 *
	 * @param v a vertex of hierarchy k
	 * @return the vertex connected to v which is at the hierarchy k-1.
	 */
	V getDown(@NotNull V v);

	// TODO: this is for the delaunay-hierarchy only!
	/**
	 * This is specifically used by {@link DelaunayHierarchy}
	 * to establish the link of the different hierarchies. Connects two vertices up and down such that
	 * up is at the hierarchy k and down is at hierarchy k+1 in O(1).
	 *
	 * @param up    vertex at hierarchy k
	 * @param down  vertex at hierarchy k+1
	 */
	void setDown(@NotNull V up, @NotNull V down);

	/**
	 * Returns the point (i.e. the data saved on the vertex) of a vertex in O(1).
	 *
	 * @param vertex the vertex
	 * @return the point of vertex
	 */
	IPoint getPoint(@NotNull V vertex);

	<CV> Optional<CV> getData(@NotNull final V vertex, @NotNull final String name, @NotNull final Class<CV> clazz);

	default boolean getBooleanData(@NotNull final V vertex, @NotNull final String name) {
		return getData(vertex, name, Boolean.class).orElse(false);
	}

	default double getDoubleData(@NotNull final V vertex, @NotNull final String name) {
		return getData(vertex, name, Double.class).orElse(0.0);
	}

	default double getDoubleData(@NotNull final V vertex, @NotNull final int index) {
		return getData(vertex, index+"", Double.class).orElse(0.0);
	}

	<CV> void setData(@NotNull final V vertex, @NotNull final String name, CV data);

	default void setBooleanData(@NotNull final V vertex, @NotNull final String name, boolean data) {
		setData(vertex, name, data);
	}

	default void setDoubleData(@NotNull final V vertex, @NotNull final String name, double data) {
		setData(vertex, name, data);
	}

	default void setDoubleData(@NotNull final V vertex, @NotNull final int index, final double data) {
		setData(vertex, index+"", data);
	}

	default void setIntegerData(@NotNull final V vertex, @NotNull final String name, int data) {
		setData(vertex, name, data);
	}

	//default void setBooleanNull(@NotNull final V vertex, @NotNull final String name, boolean nil) {}

	//default void setDoubleNull(@NotNull final V vertex, @NotNull final String name, double nil) {}

	/**
	 * Returns the data saved on the half-edge in O(1) if there is any and otherwise <tt>Optional.empty()</tt>.
	 *
	 * @param edge  the half-edge
	 * @param name  name of the property
	 * @param clazz type of the property
	 * @return the data saved on the half-edge or <tt>Optional.empty()</tt> if there is no data saved
	 */
	<CE> Optional<CE> getData(@NotNull E edge, @NotNull final String name, @NotNull final Class<CE> clazz);

	default boolean getBooleanData(@NotNull E edge, @NotNull final String name) {
		return getData(edge, name, Boolean.class).orElse(false);
	}

	default double getDoubleData(@NotNull E edge, @NotNull final String name) {
		return getData(edge, name, Double.class).orElse(0.0);
	}

	default int getIntegerData(@NotNull E edge, @NotNull final String name) {
		return getData(edge, name, Integer.class).orElse(0);
	}

	default int getIntegerData(@NotNull V vertex, @NotNull final String name) {
		return getData(vertex, name, Integer.class).orElse(0);
	}

	/**
	 * Sets the data for a specific half-edge in O(1).
	 *
	 * @param edge the half-edge
	 * @param name of the property
	 * @param data the data
	 */
	<CE> void setData(@NotNull E edge, @NotNull final String name, @Nullable CE data);

	default void setBooleanData(@NotNull E edge, @NotNull final String name, boolean data) {
		setData(edge, name, data);
	}

	default void setDoubleData(@NotNull E edge, @NotNull final String name, double data) {
		setData(edge, name, data);
	}

	default void setIntegerData(@NotNull E edge, @NotNull final String name, int data) {
		setData(edge, name, data);
	}

	/**
	 * Returns the data saved on the face in O(1) if there is any and otherwise <tt>Optional.empty()</tt>
	 *
	 * @param face the face
	 * @param clazz
	 * @return the data saved on the face or <tt>Optional.empty()</tt> if there is no data saved
	 */
	<CF> Optional<CF> getData(@NotNull F face, @NotNull final String name, @NotNull final Class<CF> clazz);

	default boolean getBooleanData(@NotNull F face, @NotNull final String name) {
		return getData(face, name, Boolean.class).orElse(false);
	}

	default double getDoubleData(@NotNull F face, @NotNull final String name) {
		return getData(face, name, Double.class).orElse(0.0);
	}

	default int getIntegerData(@NotNull F face, @NotNull final String name) {
		return getData(face, name, Integer.class).orElse(0);
	}

	/**
	 * Sets the data for a specific face in O(1).
	 *
	 * @param face the face
	 * @param data the data
	 */
	<CF> void setData(@NotNull F face, @NotNull final String name, @Nullable final CF data);

	default void setBooleanData(@NotNull F face, @NotNull final String name, final boolean data) {
		setData(face, name, data);
	}

	default void setDoubleData(@NotNull F face, @NotNull final String name, final double data) {
		setData(face, name, data);
	}

	default void setIntegerData(@NotNull F face, @NotNull final String name, final int data) {
		setData(face, name, data);
	}

	/**
	 * Returns the face of the twin of the half-edge, i.e. its twin face in O(1).
	 *
	 * @param halfEdge the half-edge
	 * @return the face of the twin of the half-edge
	 */
	default F getTwinFace(@NotNull E halfEdge) {
		return getFace(getTwin(halfEdge));
	}

	/**
	 * Returns an arbitrary face of the mesh in O(1).
	 *
	 * @return an arbitrary face of the mesh
	 */
	F getFace();

	/**
	 * Returns an arbitrary face which is neighbouring the vertex and which is
	 * not a boundary, i.e. no hole and no border in O(1).
	 *
	 * @param vertex the vertex
	 * @return an arbitrary non-boundary face which is neighbouring the vertex
	 */
	default F getNonBoundaryFace(@NotNull V vertex) {
		return getNonBoundaryFace(getEdge(vertex));
	}

	/**
	 * Returns a neighbouring face of the vertex in O(1).
	 *
	 * @param vertex the vertex
	 * @return a neighbouring face of the vertex
	 */
	default F getFace(@NotNull V vertex) {
		return getFace(getEdge(vertex));
	}

	/**
	 * Returns true if the face is the boundary in O(1).
	 *
	 * @param face the face
	 * @return true if the face is the boundary, false otherwise
	 */
	boolean isBoundary(@NotNull F face);

	/**
	 * Returns true if the face is the border in O(1) (there is only one border
	 * and the border is also a boundary).
	 *
	 * @param face the face
	 * @return true if the face is the border, false otherwise
	 */
	default boolean isBorder(@NotNull F face) {
		return isBoundary(face) && !isHole(face);
	}

	/**
	 * Returns true if the face is the hole in O(1) (there might be multiple holes
	 * and each hole is a boundary).
	 *
	 * @param face the face
	 * @return true if the face is a hole, false otherwise
	 */
	boolean isHole(@NotNull F face);

	/**
	 * Returns true if the edge is a hole edge i.e. part of a hole in O(1) (there might be multiple holes
	 * and each hole is a boundary).
	 *
	 * @param edge the edge
	 * @return true if the edge is a hole edge, false otherwise
	 */
	default boolean isHole(@NotNull E edge) {
		return isHole(getFace(edge));
	}

	/**
	 * Returns true if the vertex is a boundary vertex in O(d) worst case where d
	 * is the degree of the vertex. In general this should only cost O(1) if the data
	 * structure well maintained and this method returns true (otherwise it will check each
	 * neighbouring face).
	 *
	 * @param vertex the vertex
	 * @return true if the vertex is a boundary vertex, false otherwise
	 */
	default boolean isAtBoundary(@NotNull final V vertex) {
		return getAtBoundaryEdge(vertex).isPresent();
	}

	/**
	 * (Optional) returns a half-edge which is at the boundary (itself or its twin is a boundary edge)
	 * (if the vertex is a boundary vertex) in O(d) worst case where d
	 * is the degree of the vertex. In general this should only cost O(1) if the data
	 * structure well maintained and this method returns true (otherwise it will check each
	 * neighbouring face).
	 *
	 * @param vertex the vertex
	 * @return (optional) a boundary edge
	 */
	default Optional<E> getAtBoundaryEdge(@NotNull final V vertex) {
		return streamEdges(vertex).filter(e -> isAtBoundary(e)).findAny();
	}

	/**
	 * (Optional) returns a boundary edge (if the vertex is a boundary vertex) in O(d) worst case where d
	 * is the degree of the vertex. In general this should only cost O(1) if the data
	 * structure well maintained and this method returns true (otherwise it will check each
	 * neighbouring face).
	 *
	 * @param vertex the vertex
	 * @return (optional) a boundary edge
	 */
	default Optional<E> getBoundaryEdge(@NotNull final V vertex) {
		if(isBoundary(getEdge(vertex))) {
			return Optional.of(getEdge(vertex));
		}
		return streamEdges(vertex).filter(e -> isBoundary(e)).findAny();
	}

	/**
	 * Returns true if this face is completely surrounded by the same boundary face, i.e. a hole
	 * or the border.
	 *
	 * @param face  the face
	 * @return true if this face is completely surrounded by the same boundary face
	 */
	default boolean isSeparated(@NotNull final F face) {
		F neighbouringFace = getTwinFace(getEdge(face));
		if(isBoundary(neighbouringFace)) {
			return false;
		}
		return streamEdges(face).map(e -> getTwinFace(e)).allMatch(f -> f.equals(neighbouringFace));
	}

	/**
	 * (Optional) returns an arbitrary boundary edge of the face if one of its neighbouring face is
	 * a boundary face or itself is a boundary face in O(k) where k is the number of neighbouring faces.
	 *
	 * @param face the face
	 * @return (optional) a boundary edge
	 */
	default Optional<E> getBoundaryEdge(@NotNull final F face) {
		if(isBoundary(face)) {
			return Optional.of(getEdge(face));
		}
		return streamEdges(face).filter(e -> isAtBoundary(e)).map(e -> getTwin(e)).findAny();
	}

	/**
	 * Returns true if the vertex is at the border in O(d) where d is the degree of the vertex.
	 *
	 * @param vertex the vertex
	 * @return true if the vertex is at the border, otherwise false
	 */
	default boolean isAtBorder(@NotNull final V vertex) {
		return streamEdges(vertex).anyMatch(e -> isAtBorder(e));
	}

	/**
	 * Returns true if the face is at the border i.e. if any of its half-edges
	 * is at the border in O(k) where k is the number of neighbouring faces,
	 * i.e. number of edges of the face.
	 *
	 * @param face the face
	 * @return true if the face is at the border, otherwise false
	 */
	default boolean isAtBorder(@NotNull final F face) {
		return streamEdges(face).anyMatch(e -> isAtBorder(e));
	}

	/**
	 * Returns true if the face is at the boundary i.e. if any of its half-edges
	 * is at the border or a hole in O(k) where k is the number of neighbouring faces,
	 * i.e. number of edges of the face.
	 *
	 * @param face the face
	 * @return true if the face is at the boundary, otherwise false
	 */
	default boolean isAtBoundary(@NotNull final F face) {
		return streamEdges(face).anyMatch(e -> isAtBoundary(e));
	}

	/**
	 * Returns true if the half-edge is at the border i.e. if itself or its twin
	 * is a border edge in O(1).
	 *
	 * @param edge the half-edge
	 * @return true if the half-edge is at the border, otherwise false
	 */
	default boolean isAtBorder(@NotNull final E edge) {
		return isBorder(edge) || isBorder(getTwin(edge));
	}

	/**
	 * Returns true if the half-edge is at the boundary (the border or a hole) i.e. if itself or its twin
	 * is a boundary edge in O(1).
	 *
	 * @param edge the half-edge
	 * @return true if the half-edge is at the boundary, otherwise false
	 */
	default boolean isAtBoundary(@NotNull final E edge) {
		return isBoundary(edge) || isBoundary(getTwin(edge));
	}

	/**
	 * Returns true if one of the neighbouring faces of the face is the border.
	 *
	 * @param face the face
	 * @return true if one of the neighbouring faces is the border, false otherwise
	 */
	default boolean isNeighbourBorder(@NotNull final F face){
		for(F neighbourFace : getFaceIt(face)) {
			if(isBorder(neighbourFace)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns true if one of the neighbouring faces of the face is a boundary.
	 *
	 * @param face the face
	 * @return true if one of the neighbouring faces is a boundary, false otherwise
	 */
	default boolean isNeighbourBoundary(@NotNull final F face){
		for(F neighbourFace : getFaceIt(face)) {
			if(isBoundary(neighbourFace)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns true if one of the neighbouring faces of the face is a hole.
	 *
	 * @param face the face
	 * @return true if one of the neighbouring faces is a hole, false otherwise
	 */
	default boolean isNeighbourHole(@NotNull final F face){
		for(F neighbourFace : getFaceIt(face)) {
			if(isHole(neighbourFace)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * (Optional) returns the half-edge of a face which has a twin which is a boundary edge
	 * i.e. the link to the boundary.
	 *
	 * @param face (optional) the link to the boundary
	 *
	 * @return (optional) the half-edge of a face which has a twin which is a boundary edge
	 */
	default Optional<E> getLinkToBoundary(@NotNull final F face){
		for(E edge : getEdgeIt(face)) {
			if(isBoundary(getTwin(edge))) {
				return Optional.of(edge);
			}
		}
		return Optional.empty();
	}

	/**
	 * Returns true if the edge is a boundary (border or hole) edge.
	 *
	 * @param halfEdge the half-edge
	 * @return true if the edge is a boundary edge, false otherwise
	 */
	boolean isBoundary(@NotNull final E halfEdge);

	/**
	 * Returns true if the edge is a border edge.
	 *
	 * @param halfEdge the half-edge
	 * @return true if the edge is a border edge, false otherwise
	 */
	default boolean isBorder(@NotNull final E halfEdge) {
		return isBorder(getFace(halfEdge));
	}

	/**
	 * Returns true if the face is already destroyed i.e. not part of the geometric representation.
	 *
	 * @param face the face
	 * @return true if the face is already destroyed, false otherwise
	 */
	boolean isDestroyed(@NotNull final F face);

	/**
	 * Returns true if the edge is already destroyed i.e. not part of the geometric representation.
	 *
	 * @param edge the half-edge
	 * @return true if the edge is already destroyed, false otherwise
	 */
	boolean isDestroyed(@NotNull final E edge);

	/**
	 * Returns true if the vertex is already destroyed i.e. not part of the geometric representation.
	 *
	 * @param vertex the vertex
	 * @return true if the vertex is already destroyed, false otherwise
	 */
	boolean isDestroyed(@NotNull final V vertex);

	/**
	 * Returns true if the vertex is already not i.e. is part of the geometric representation.
	 *
	 * @param vertex the vertex
	 * @return true if the vertex is not destroyed, false otherwise
	 */
	default boolean isAlive(@NotNull final V vertex) {
		return !isDestroyed(vertex);
	}

	/**
	 * Returns true if the edge is not destroyed i.e. is part of the geometric representation.
	 *
	 * @param edge the half-edge
	 * @return true if the edge is not destroyed, false otherwise
	 */
	default boolean isAlive(@NotNull final E edge) {
		return !isDestroyed(edge);
	}

	/**
	 * Returns true if the face is not destroyed i.e. is part of the geometric representation.
	 *
	 * @param face the face
	 * @return true if the face is not destroyed, false otherwise
	 */
	default boolean isAlive(@NotNull final F face) {
		return !isDestroyed(face);
	}

	/**
	 * Sets the bi-directional relation twin of these two half-edges,
	 * i.e. the halfedge will be the twin of the twin and vise versa.
	 *
	 * @param halfEdge  the half-edge halfedge (the twin of the twin)
	 * @param twin      the half-edge twin (the twin of the half-edge)
	 */
	void setTwin(@NotNull final E halfEdge, @NotNull final E twin);

	/**
	 * Sets the bi-directional relation next-prev of these two half-edges,
	 * i.e. the next will be the next (successor) of the halfedge and the halfedge will
	 * be the prev (predecessor) of next.
	 *
	 * @param halfEdge  the half-edge halfedge (the next of the next)
	 * @param next      the half-edge next (the next of the half-edge)
	 */
	void setNext(@NotNull final E halfEdge, @NotNull final E next);

	/**
	 * Sets the bi-directional relation next-prev of these two half-edges,
	 * i.e. the prev will be the prev (predecessor) of the halfedge and the halfedge will
	 * be the next (successor) of prev.
	 *
	 * @param halfEdge  the half-edge halfedge (the next of the next)
	 * @param prev      the half-edge prev (the next of the half-edge)
	 */
	void setPrev(@NotNull final E halfEdge, @NotNull final E prev);

	/**
	 * Sets (uni-directional) the face of a half-edge. This is uni-directional,
	 * i.e. this will not set the half-edge of the face!
	 *
	 * @param halfEdge  the half-edge
	 * @param face      the face
	 */
	void setFace(@NotNull final E halfEdge, @NotNull final F face);

	/**
	 * Sets (uni-directional) the half-edge of the face. This is uni-directional,
	 * i.e. this will not set the face of the half-edge!
	 *
	 * @param edge  the half-edge
	 * @param face  the face
	 */
	void setEdge(@NotNull final F face, @NotNull final E edge);

	/**
	 * Sets (uni-directional) the half-edge of a vertex. This is uni-directional,
	 * i.e. this will not set the vertex of the half-edge!
	 *
	 * @param vertex    the half-edge
	 * @param edge      the face
	 */
	void setEdge(@NotNull final V vertex, @NotNull final E edge);

	/**
	 * Sets (uni-directional) the vertex of a half-edge. This is uni-directional,
	 * i.e. this will not set the half-edge of the vetex!
	 *
	 * @param halfEdge    the half-edge
	 * @param vertex      the vertex
	 */
	void setVertex(@NotNull final E halfEdge, @NotNull final V vertex);

	/**
	 * A factory method which creates a new half-edge with a established vertex relationship.
	 * The the half-edge of the vertex will not change.
	 * This edge will be added to the mesh data structure.
	 *
	 * @param vertex the vertex of the edge
	 * @return a half-edge
	 */
	E createEdge(@NotNull final V vertex);

	/**
	 * A factory method which creates a new half-edge with a established vertex and face relationship.
	 * The the half-edge of the face or the vertex will not change.
	 * This edge will be added to the mesh data structure.
	 *
	 * @param vertex the vertex of the edge
	 * @param face the face of the edge
	 * @return a half-edge
	 */
	E createEdge(@NotNull final V vertex, @NotNull final F face);

	/**
	 * A factory method which creates a new face which will be added to the mesh data structure.
	 *
	 * @return a face
	 */
	F createFace();

	/**
	 * A factory method which creates a new face which will be added to the mesh data structure.
	 *
	 * @param hole if true the face is hole, otherwise it isn't
	 * @return a face
	 */
	F createFace(final boolean hole);

	/**
	 * A factory method which creates a new point. The point will not be inserted into the mesh data
	 * structure.
	 *
	 * @param x x-coordinate
	 * @param y y-coordinate
	 * @return a point.
	 */
	IPoint createPoint(final double x, final double y);

	/**
	 * A factory method which creates a new vertex. The vertex will not be inserted into the mesh data
	 * structure.
	 *
	 * @param x x-coordinate
	 * @param y y-coordinate
	 * @return a vertex.
	 */
	V createVertex(final double x, final double y);

	/**
	 * A factory method which creates a new vertex. The vertex will not be inserted into the mesh data
	 * structure.
	 *
	 * @param point a container supporting 2D-coordinates
	 * @return a vertex.
	 */
	V createVertex(@NotNull final IPoint point);

	/**
	 * Returns the border of the mesh in O(1).
	 *
	 * @return the border of the mesh
	 */
	F getBorder();

	/**
	 * Inserts the vertex into the mesh data structure.
	 *
	 * @param vertex the vertex
	 */
	default void insert(@NotNull final V vertex) {
		insertVertex(vertex);
	}

	/**
	 * Inserts the vertex into the mesh data structure.
	 *
	 * @param vertex the vertex
	 */
	void insertVertex(@NotNull final V vertex);

	/**
	 * Inserts the point into the mesh data structure, returning its vertex
	 *
	 * @param point the point
	 * @return the vertex of the point
	 */
	default V insertPoint(final IPoint point) {
		V vertex = createVertex(point);
		insertVertex(vertex);
		return vertex;
	}

	/**
	 * Inserts the vertex into the mesh data structure.
	 *
	 * @param x x-coordinate
	 * @param y y-coordinate
	 * @return the vertex of the point defiend by (x,y)
	 */
	default V insertVertex(final double x, final double y) {
		V vertex = createVertex(x, y);
		insertVertex(vertex);
		return vertex;
	}

	/**
	 * <p>A factory method which creates a new face from a list {@link List}
	 * of vertices which forms a simple (non-intersecting) polygon.
	 * All base elements of the face and the face itself will be
	 * added to the mesh data structure.</p>
	 *
	 * <p>Assumption: points (p1, ..., pn) is a valid simple polygon
	 * and p1 != pn and vertices are already added to the mesh data structure.</p>
	 *
	 * @param points a list {@link List} of vertices representing a simple polygon (non-intersecting)
	 * @return a face
	 */
	default F createFace(@NotNull final List<V> points) {
		assert points.stream().allMatch(v -> getVertices().contains(v));

		F face = createFace();
		F borderFace = getBorder();

		LinkedList<E> edges = new LinkedList<>();
		LinkedList<E> borderEdges = new LinkedList<>();
		for(V p : points) {
			//insertVertex(p);
			E edge = createEdge(p, face);
			setEdge(p, edge);
			E borderEdge = createEdge(p, borderFace);
			edges.add(edge);
			borderEdges.add(borderEdge);
		}

		E edge = null;
		for(E halfEdge : edges) {
			if(edge != null) {
				setNext(edge, halfEdge);
			}
			edge = halfEdge;
		}
		setNext(edges.peekLast(), edges.peekFirst());

		edge = null;
		for(E halfEdge : borderEdges) {
			if(edge != null) {
				setPrev(edge, halfEdge);
			}
			edge = halfEdge;
		}
		setPrev(borderEdges.peekLast(), borderEdges.peekFirst());

		for(int i = 0; i < edges.size(); i++) {
			E halfEdge = edges.get(i);
			E twin = borderEdges.get((i + edges.size() - 1) % edges.size());
			setTwin(halfEdge, twin);
		}

		setEdge(face, edges.peekFirst());
		setEdge(borderFace, borderEdges.peekFirst());

		return face;
	}

	/**
	 * A factory method which creates a new face from an array
	 * of vertices which forms a simple (non-intersecting) polygon.
	 * All base elements of the face and the face itself will be
	 * added to the mesh data structure.
	 *
	 * Assumption: points (p1, ..., pn) is a valid simple polygon
	 * and p1 != pn and vertices are not added to the mesh data structure already.
	 *
	 * @param points an array of vertices representing a simple polygon (non-intersecting)
	 * @return a face
	 */
	default F createFace(@NotNull final V... points) {
		return createFace(Lists.newArrayList(points));
	}

	/**
	 * A factory method which creates a new face from an array
	 * of points which forms a simple (non-intersecting) polygon.
	 * All base elements of the face and the face itself will be
	 * added to the mesh data structure.
	 *
	 * Assumption: points (p1, ..., pn) is a valid simple polygon
	 * and p1 != pn.
	 *
	 * @param points an array of points representing a simple polygon (non-intersecting)
	 * @return a face
	 */
	default F toFace(@NotNull final IPoint... points) {
		return createFace(Arrays.stream(points).map(p -> insertPoint(p)).collect(Collectors.toList()));
	}

	/**
	 * A factory method which creates a new face from an list  {@link List}
	 * of points which forms a simple (non-intersecting) polygon.
	 * All base elements of the face and the face itself will be
	 * added to the mesh data structure.
	 *
	 * Assumption: points (p1, ..., pn) is a valid simple polygon
	 * and p1 != pn.
	 *
	 * @param points a list {@link List} of points representing a simple polygon (non-intersecting)
	 * @return a face
	 */
	default F toFace(@NotNull final List<IPoint> points) {
		return createFace(points.stream().map(p -> insertPoint(p)).collect(Collectors.toList()));
	}

	/**
	 * Marks the face to be a hole.
	 *
	 * @param face a face
	 */
	void toHole(@NotNull final F face);

	/**
	 * Marks the face to be destroyed.
	 * @param face a face
	 */
	void destroyFace(@NotNull final F face);

	/**
	 * Marks the edge to be destroyed.
	 *
	 * @param edge a half-edge
	 */
	void destroyEdge(@NotNull final E edge);

	/**
	 * Marks the vertex to be destroyed.
	 *
	 * @param vertex a vertex
	 */
	void destroyVertex(@NotNull final V vertex);

	/**
	 * Returns a list {@link List} of all non-boundary and not already destroyed faces.
	 * Therefore the list does not contain holes or the border.
	 *
	 * @return a list {@link List} of all non-boundary and not already destroyed faces.
	 */
	default List<F> getFaces() {
		return streamFaces().filter(face -> !isBoundary(face)).filter(face -> isAlive(face)).collect(Collectors.toList());
	}

	default List<F> getBoundaryAndHoles() {
		return Stream.concat(streamHoles(), Stream.of(getBorder())).collect(Collectors.toList());
	}

	default List<F> getFacesWithBoundary() {
		return streamFacesWithBoundary().filter(face -> isAlive(face)).collect(Collectors.toList());
	}

	default Stream<F> streamFacesWithBoundary() {
		return Streams.concat(streamBoundaries(), streamFaces());
	}

	default Stream<F> streamBoundaries() {
		return Streams.concat(streamHoles(), Stream.of(getBorder()));
	}

	default Stream<E> streamBoundaryEdges() {
		return streamBoundaries().flatMap(f -> streamEdges(f));
	}

	/**
	 * Returns a list {@link List} of all boundary edges (which are alive) of this mesh.
	 * This requires O(n) where n is the number of boundary edges.
	 *
	 * @return a list {@link List} of all boundary edges
	 */
	default List<E> getBoundaryEdges() {
		return streamBoundaryEdges().collect(Collectors.toList());
	}

	/**
	 * Returns a list {@link List} of all boundary points (which are alive) of this mesh.
	 * This requires O(n) where n is the number of boundary edges.
	 *
	 * @return a list {@link List} of all boundary points
	 */
	default List<IPoint> getBoundaryPoints() {
		return streamBoundaryEdges().map(e -> getPoint(e)).collect(Collectors.toList());
	}


	/**
	 * Sets the point of a vertex. This should only be used with great care since
	 * this will re-position the vertex and may destroy a valid connectivity! So in
	 * general this can only be done if the new point is contained in the convex hull
	 * of the neighbouring vertices of the vertex.
	 *  @param vertex    the vertex
	 * @param point     its new point
	 */
	void setPoint(@NotNull final V vertex, @NotNull final IPoint point);

	/**
	 * Returns a list {@link List} of all adjacent points of the vertex in O(d)
	 * where d is the degree of the vertex.
	 *
	 * @param vertex the vertex
	 * @return a list {@link List} of all adjacent points of the vertex
	 */
	default List<IPoint> getPoints(@NotNull final V vertex) {
		List<IPoint> points = new ArrayList<>();
		for(V v : getAdjacentVertexIt(vertex)) {
			points.add(toPoint(v));
		}

		return points;
	}

	/**
	 * Returns a list {@link List} of all faces of the mesh data structure with the exception of the border.
	 * This requires O(n) where n is the number of faces.
	 *
	 * @return a list {@link List} of all faces of the mesh data structure with the exception of the border.
	 */
	default List<F> getFacesWithHoles() {
		return streamFaces().filter(face -> isAlive(face)).collect(Collectors.toList());
	}

	// TODO: this can be done much faster: only filter the edges of holes and the border!
	/**
	 * Returns a list {@link List} of all (alive) boundary vertices, i.e. vertices which have a boundary edge as their neighbours.
	 * This requires O(n) where n is the number of edges.
	 *
	 * @return a list {@link List} of all boundary vertices
	 */
	default List<V> getBoundaryVertices() {
		return streamEdges().filter(edge -> isBoundary(edge)).filter(edge -> isAlive(edge)).map(edge -> getVertex(edge)).collect(Collectors.toList());
	}

	/**
	 * Returns a filtered stream {@link Stream} of (all alive) interior faces such that each face fulfill the predicate.
	 *
	 * @param predicate the predicate
	 * @return a filtered stream {@link Stream} of (all alive) interior faces
	 */
	Stream<F> streamFaces(@NotNull final Predicate<F> predicate);

	/**
	 * Returns a parallel stream {@link Stream} of (all alive) interior faces. Note that the required synchronization has to
	 * be done by the user.
	 *
	 * @return a parallel stream {@link Stream} of (all alive) interior faces
	 */
	default Stream<F> streamFacesParallel() {
		return streamFaces(f -> true).parallel();
	}

	/**
	 * Returns a stream {@link Stream} of (all alive) interior faces. Note that the required synchronization has to
	 * be done by the user.
	 *
	 * @return a parallel stream {@link Stream} of (all alive) interior faces
	 */
	default Stream<F> streamFaces() {
		return streamFaces(f -> !isBoundary(f));
	}

	/**
	 * Clears the mesh data structure i.e. after this call the mesh is empty.
	 */
	void clear();

	/**
	 * (Optional) returns a point of the mesh fulfilling the predicate if there is any.
	 * This requires O(n) where n is the number of points / vertices.
	 *
	 * @param predicate the predicate
	 * @return (optional) returns a point of the mesh fulfilling the predicate
	 */
	default Optional<IPoint> findAny(@NotNull final Predicate<IPoint> predicate) {
		return streamPoints().filter(predicate).findAny();
	}

	/**
	 * (Optional) returns a point of the mesh fulfilling the predicate if there is any.
	 * This requires O(n) where n is the number of half-edges.
	 *
	 * @param predicate the predicate
	 * @return (optional) returns a point of the mesh fulfilling the predicate
	 */
	default Optional<E> findAnyEdge(@NotNull final Predicate<IPoint> predicate) {
		return streamEdges().filter(edge -> predicate.test(getPoint(edge))).findAny();
	}

	/**
	 * Returns true if there is any point inside the mesh fulfilling the predicate.
	 *
 	 * @param predicate the predicate
	 * @return true if there is any point inside the mesh fulfilling the predicate, false otherwise
	 */
	default boolean findMatch(@NotNull final Predicate<IPoint> predicate) {
		return streamPoints().anyMatch(predicate);
	}

	/**
	 * Returns a stream {@link Stream} of (alive) holes.
	 *
	 * @return a stream {@link Stream} of holes
	 */
	Stream<F> streamHoles();

	/**
	 * Returns a list {@link List} of (alive) holes.
	 *
	 * @return a list {@link List} of holes
	 */
	default List<F> getHoles() {
		return streamHoles().collect(Collectors.toList());
	}

	/**
	 * Returns a stream {@link Stream} of all (alive) half-edges.
	 *
	 * @return a stream {@link Stream} of all half-edges.
	 */
	Stream<E> streamEdges();

	/**
	 * Returns a parallel stream {@link Stream} of all (alive) half-edges.
	 * Synchronization has to be done by the user.
	 *
	 * @return a parallel stream {@link Stream} of all half-edges.
	 */
	Stream<E> streamEdgesParallel();

	/**
	 * Returns a stream {@link Stream} of all (alive) vertices.
	 *
	 * @return a stream {@link Stream} of all vertices.
	 */
	Stream<V> streamVertices();

	/**
	 * Returns a parallel stream {@link Stream} of all (alive) vertices.
	 * Synchronization has to be done by the user.
	 *
	 * @return a parallel stream {@link Stream} of all vertices.
	 */
	Stream<V> streamVerticesParallel();

	/**
	 * Returns a stream {@link Stream} of all (alive) points.
	 *
	 * @return a stream {@link Stream} of all points.
	 */
	default Stream<IPoint> streamPoints() {
		return streamVertices().map(v -> getPoint(v));
	}

	/**
	 * Returns a stream {@link Stream} of all of a specific face.
	 *
	 * @param face the specific face
	 * @return a stream {@link Stream} of all points of a specific face.
	 */
	default Stream<IPoint> streamPoints(@NotNull final F face) {
		return streamVertices(face).map(v -> getPoint(v));
	}

	/**
	 * Returns a parallel stream {@link Stream} of all (alive) points.
	 * Synchronization has to be done by the user.
	 *
	 * @return a parallel stream {@link Stream} of all points.
	 */
	default Stream<IPoint> streamPointsParallel() {
		return streamEdgesParallel().map(e -> getPoint(e));
	}

	/**
	 * Returns a immutable polygon {@link VPolygon} by transforming the face to a polygon.
	 * Assumption: The face represents a simple polygon (no intersecting lines). This requires
	 * O(k) time where k is the number of points of the face / polygon. In a first step a {@link Path2D}
	 * is created.
	 *
	 * @param face the face.
	 * @return a immutable polygon {@link VPolygon} representing the face
	 */
	default VPolygon toPolygon(@NotNull final F face) {
		Path2D path2D = new Path2D.Double();
		E edge = getEdge(face);
		E prev = getPrev(edge);

		path2D.moveTo(getVertex(prev).getX(), getVertex(prev).getY());
		path2D.lineTo(getVertex(edge).getX(), getVertex(edge).getY());

		while (!edge.equals(prev)) {
			edge = getNext(edge);
			V p = getVertex(edge);
			path2D.lineTo(p.getX(), p.getY());
		}

		//path2D.closePath();

		return new VPolygon(path2D);
	}

	// TODO speed up by avoiding the creation of a list!
	/**
	 * Returns a immutable triangle {@link VTriangle} by transforming the face to a triangle.
	 * Assumption: The face represents a triangle, i.e. it has exactly 3 distinct points. This
	 * requires O(1) time.
	 *
	 * @param face the face.
	 * @return a immutable triangle {@link VTriangle} representing the face
	 */
	default VTriangle toTriangle(@NotNull final F face) {
		List<V> vertices = getVertices(face);
		assert vertices.size() == 3 : "number of vertices of " + face + " is " + vertices.size();
		return new VTriangle(new VPoint(vertices.get(0)), new VPoint(vertices.get(1)), new VPoint(vertices.get(2)));
	}

	default VPoint toMidpoint(@NotNull final F face) {
		assert getVertices(face).size() == 3 : "number of vertices of " + face + " is " + getVertices(face).size();
		E edge = getEdge(face);
		V v1 = getVertex(edge);
		V v2 = getVertex(getNext(edge));
		V v3 = getVertex(getPrev(edge));
		return GeometryUtils.getTriangleMidpoint(getX(v1), getY(v1), getX(v2), getY(v2), getX(v3), getY(v3));
	}

	default VPoint toCircumcenter(@NotNull final F face) {
		assert getVertices(face).size() == 3 : "number of vertices of " + face + " is " + getVertices(face).size();
		E edge = getEdge(face);
		V v1 = getVertex(edge);
		V v2 = getVertex(getNext(edge));
		V v3 = getVertex(getPrev(edge));
		return GeometryUtils.getCircumcenter(getX(v1), getY(v1), getX(v2), getY(v2), getX(v3), getY(v3));
	}

	/**
	 * Returns the midpoint {@link VPoint} of a triangle defined by the face.
	 * Assumption: The face represents a triangle, i.e. it has exactly 3 distinct points. This
	 * requires O(1) time.
	 *
	 * @param face the face.
	 * @return the midpoint {@link VPoint} of a triangle defined by the face.
	 */
	default VPoint getTriangleMidPoint(@NotNull final F face) {
		assert getVertices(face).size() == 3 : "number of vertices of " + face + " is " + getVertices(face).size();
		E e1 = getEdge(face);
		E e2 = getNext(e1);
		E e3 = getNext(e2);
		V v1 = getVertex(e1);
		V v2 = getVertex(e2);
		V v3 = getVertex(e3);
		return GeometryUtils.getTriangleMidpoint(getX(v1), getY(v1), getX(v2), getY(v2), getX(v3), getY(v3));
	}

	/**
	 * Returns a triple {@link Triple} which represents a triangle by transforming the face to a triangle.
	 * Assumption: The face represents a triangle, i.e. it has exactly 3 distinct points. This requires O(1) time.
	 *
	 * @param face the face.
	 * @return a triple {@link Triple} representing the face
	 */
	default Triple<IPoint, IPoint, IPoint> toTriple(@NotNull final F face) {
		List<V> vertices = getVertices(face);
		assert vertices.size() == 3;
		return Triple.of(getPoint(vertices.get(0)), getPoint(vertices.get(1)), getPoint(vertices.get(2)));
	}

	/**
	 * (Optional) returns the face containing the point (x, y) by testing each face. This is
	 * a brute force strategy requiring O(n) time where n is the number of faces to
	 * compare results with more sophisticated strategies. It should not be used aside from
	 * testing since it is very slow!
	 *
	 * @param x the x-coordinate of the point
	 * @param y the y-coordinate of the point
	 * @return (optional) returns the face containing the point (x, y)
	 */
	default Optional<F> locate(final double x, final double y) {
		for(F face : getFaces()) {
			VPolygon polygon = toPolygon(face);
			if(polygon.contains(new VPoint(x, y))) {
				return Optional.of(face);
			}
		}
		return Optional.empty();
	}

	/**
	 * This method will triangulate all holes and mark them as holes.
	 * Note that a triangulated hole becomes invalid if a vertex or edge of
	 * this hole gets changes (moved, split, collapsed, removed, inserted)!
	 */
	/*default void fillHoles() {

	}*/

	/**
	 * <p>Returns vertex of the triangulation of the face with the smallest distance to point.</p>
	 *
	 * @param face          the face of the trianuglation
	 * @param point         the point
	 * @return vertex of the triangulation of the face with the smallest distance to point
	 */
	default V getNearestPoint(@NotNull final F face, @NotNull final IPoint point) {
		return getNearestPoint(face, point.getX(), point.getY());
	}

	/**
	 * <p>Returns the vertex of a face which is closest to (x, y).</p>
	 *
	 * @param face  the face
	 * @param x     the x-coordinate of the point
	 * @param y     the y-coordinate of the point
	 * @return  the vertex of a face which is closest to (x, y).
	 */
	default V getNearestPoint(final F face, final double x, final double y) {
		return streamEdges(face).map(edge -> getVertex(edge)).reduce((p1, p2) -> p1.distance(x,y) > p2.distance(x,y) ? p2 : p1).get();
	}


	/**
	 * Returns an iterator {@link Iterator} which iterates over all (alive) faces of this mesh.
	 *
	 * @return an iterator which iterates over all faces of this mesh
	 */
	@Override
	default Iterator<F> iterator() {
		return getFaces().iterator();
	}

	/**
	 * Returns a list {@link List} of faces which are adjacent to the vertex of this edge.
	 *
	 * @param edge the edge holding the vertex
	 * @return a list {@link List} of faces which are adjacent to the vertex of this edge
	 */
	default List<F> getFaces(@NotNull E edge) {
		return Lists.newArrayList(new AdjacentFaceIterator(this, edge));
	}

	/**
	 * Returns a list {@link List} of faces which are adjacent to the vertex.
	 *
	 * @param vertex the vertex
	 * @return a list {@link List} of faces which are adjacent to the vertex of this edge
	 */
	default List<F> getFaces(@NotNull V vertex) {
		return Lists.newArrayList(new AdjacentFaceIterator(this, getEdge(vertex)));
	}

	/**
	 * Returns a Iterable {@link Iterable} which can be used to iterate over all edges which end point is the vertex that is adjacent to the vertex of this edge.
	 *
	 * @param edge the edge which holds the vertex
	 * @return a Iterable {@link Iterable} which can be used to iterate over all edges which are adjacent to the vertex of this edge.
	 */
	default Iterable<E> getIncidentEdgesIt(@NotNull final E edge) {
		return () -> new IncidentEdgeIterator(this, edge);
	}

	/**
	 * Returns a Iterable {@link Iterable} which can be used to iterate over adjacent vertices of this vertex.
	 *
	 * @param vertex the vertex
	 * @return a Iterable {@link Iterable} which can be used to iterate over all adjacent vertices.
	 */
	default Iterable<V> getAdjacentVertexIt(@NotNull final V vertex) {
		return () -> new AdjacentVertexIterator<>(this, vertex);
	}

	default List<V> getAdjacentVertices(@NotNull final V vertex) {
		return Lists.newArrayList(new AdjacentVertexIterator<>(this, vertex));
	}

	/**
	 * Returns an Iterable {@link Iterable} which can be used to iterate over all edges of a face.
	 *
	 * @param face the face the iterable iterates over
	 * @return an Iterable {@link Iterable} which can be used to iterate over all edges of a face.
	 */
	default Iterable<E> getEdgeIt(@NotNull final F face) {
		return () -> new EdgeIterator<>(this, face);
	}

	/**
	 * Returns an Iterable {@link Iterable} which can be used to iterate over all edges of a face which the edge is part of.
	 *
	 * @param edge the edge which is part of the face the iterable iterates over
	 * @return an Iterable {@link Iterable} which can be used to iterate over all edges of a face.
	 */
	default Iterable<E> getEdgeIt(@NotNull final E edge) {
		return () -> new EdgeIterator<>(this, edge);
	}


	default Iterable<E> getEdgeItReverse(@NotNull final E edge) {
		return () -> new EdgeIterator<>(this, edge, true);
	}

	/**
	 * Returns an Iterable {@link Iterable} which can be used to iterate over all vertices of a face.
	 *
	 * @param face the face the iterable iterates over
	 * @return an Iterable {@link Iterable} which can be used to iterate over all vertices of a face
	 */
	default Iterable<V> getVertexIt(@NotNull final F face) {
		return () -> new VertexIterator<>(this, face);
	}

	/**
	 * Returns an Iterable {@link Iterable} which can be used to iterate over all vertices of a face.
	 *
	 * @param face the face the iterable iterates over
	 * @return an Iterable {@link Iterable} which can be used to iterate over all vertices of a face
	 */
	default Iterable<IPoint> getPointIt(@NotNull final F face) {
		return () -> new PointIterator<>(this, face);
	}

	/**
	 * Returns a Stream {@link Stream} of edges of a face.
	 *
	 * @param face the faces of which edges the stream consist
	 * @return a Stream {@link Stream} of edges of a face
	 */
	default Stream<E> streamEdges(@NotNull final F face) {
		Iterable<E> iterable = getEdgeIt(face);
		return StreamSupport.stream(iterable.spliterator(), false);
	}

	/**
	 * Returns a Stream {@link Stream} of all adjacent vertices of the vertex.
	 *
	 * @param v the vertex
	 * @return a Stream {@link Stream} of all adjacent vertices
	 */
	default Stream<V> streamVertices(@NotNull final V v) {
		Iterable<V> iterable = getAdjacentVertexIt(v);
		return StreamSupport.stream(iterable.spliterator(), false);
	}

	/**
	 * Returns a Stream {@link Stream} of edges of a face.
	 *
	 * @param edge the edge of the face of which edges the stream consist
	 * @return a Stream {@link Stream} of edges of a face specified by the edge
	 */
	default Stream<E> streamEdges(@NotNull final E edge) {
		Iterable<E> iterable = getEdgeIt(edge);
		return StreamSupport.stream(iterable.spliterator(), false);
	}

	default Stream<E> streamEdgesReverse(@NotNull final E edge) {
		Iterable<E> iterable = getEdgeItReverse(edge);
		return StreamSupport.stream(iterable.spliterator(), false);
	}

	/**
	 * Returns a Stream {@link Stream} of vertices of a face.
	 *
	 * @param face the faces of which edges the stream consist
	 * @return a Stream {@link Stream} of edges of a face
	 */
	default Stream<V> streamVertices(@NotNull final F face) {
		return streamEdges(face).map(edge -> getVertex(edge));
	}

	/**
	 * Returns an Iterable {@link Iterable} which can be used to iterate over surrounding faces of the face.
	 *
	 * @param face the face the iterable iterates over
	 * @return an Iterable {@link Iterable} which can be used to iterate over all surrounding faces
	 */
	default Iterable<F> getFaceIt(@NotNull final F face) { return () -> new SurroundingFaceIterator<>(this, face);}

	/**
	 * Returns an Iterable {@link Iterable} which can be used to iterate over surrounding faces of the vertex.
	 *
	 * @param vertex the vertex the iterable iterates over
	 * @return an Iterable {@link Iterable} which can be used to iterate over all surrounding faces
	 */
	default Iterable<F> getFaceIt(@NotNull final V vertex) { return () -> new AdjacentFaceIterator(this, getEdge(vertex));}

	default List<F> getFaces(@NotNull final F face) { return Lists.newArrayList(new SurroundingFaceIterator<>(this, face)); }

	/**
	 * Returns a Stream {@link Stream} consisting of all surrounding faces of the face.
	 *
	 * @param face the face of which surrounding faces the stream consist.
	 * @return a Stream {@link Stream} consisting of all surrounding faces of the face
	 */
	default Stream<F> streamFaces(@NotNull final F face) {
		Iterable<F> iterable = getFaceIt(face);
		return StreamSupport.stream(iterable.spliterator(), false);
	}

	/**
	 * Returns a Stream {@link Stream} consisting of all surrounding faces of the vertex.
	 *
	 * @param vertex the face of which surrounding faces the stream consist.
	 * @return a Stream {@link Stream} consisting of all surrounding faces of the vertex
	 */
	default Stream<F> streamFaces(@NotNull final V vertex) {
		Iterable<F> iterable = getFaceIt(vertex);
		return StreamSupport.stream(iterable.spliterator(), false);
	}

	/**
	 * Returns a Stream {@link Stream} consisting of all edges which are incident to the edge
	 *
	 * @param edge the edge of which the edges are incident
	 * @return a Stream {@link Stream} consisting of all edges which are incident to the edge.
	 */
	default Stream<E> streamIncidentEdges(@NotNull final E edge) {
		Iterable<E> iterable = getIncidentEdgesIt(edge);
		return StreamSupport.stream(iterable.spliterator(), false);
	}

	/**
	 * Returns an Iterable {@link Iterable} which can be used to iterate over all faces which are adjacent to the vertex of the edge
	 *
	 * @param edge the edge of which adjacent faces
	 * @return an Iterable {@link Iterable} which can be used to iterate over all faces which are adjacent to the vertex of the edge
	 */
	default Iterable<F> getAdjacentFacesIt(@NotNull final E edge) { return () -> new AdjacentFaceIterator<>(this, edge); }

	/**
	 * Returns an Iterable {@link Iterable} which can be used to iterate over all faces which are adjacent to the vertex.
	 *
	 * @param vertex the vertex
	 * @return an Iterable {@link Iterable} which can be used to iterate over all faces which are adjacent to the vertex
	 */
	default Iterable<F> getAdjacentFacesIt(@NotNull final V vertex) { return () -> new AdjacentFaceIterator<>(this, getEdge(vertex)); }

	/**
	 * Returns a list {@link List} of all faces which are adjacent to the vertex of the edge
	 *
	 * @param edge the edge of which adjacent faces
	 * @return a list {@link List} of all faces which are adjacent to the vertex of the edge
	 */
	default List<F> getAdjacentFaces(@NotNull final E edge) {
		return Lists.newArrayList(new AdjacentFaceIterator(this, edge));
	}

	/**
	 * Returns a list {@link List} of edges which are incident to the vertex of this edge.
	 * They hold the vertices which are adjacent to vertex of the edge.
	 *
	 * @param edge the edge which holds the vertex
	 * @return a list {@link List} of edges which are incident to the vertex of this edge.
	 */
	default List<E> getIncidentEdges(@NotNull final E edge) { return Lists.newArrayList(new IncidentEdgeIterator(this, edge)); }


	/**
	 * Returns an iterable {@link Iterable} that can be used to iterate over all edges which end-point is equal to the vertex, i.e. all edges connected to the vertex.
	 *
	 * @param vertex the end-point of all the edges
	 * @return an iterable {@link Iterable} that can be used to iterate over all edges which end-point is equal to the vertex
	 */
	default Iterable<E> getEdgeIt(@NotNull final V vertex) {
		return () -> new EdgeOfVertexIterator(this, vertex);
	}

	/**
	 * Returns a list {@link List} of all edges which end-point is equal to the vertex, i.e. all edges connected to the vertex
	 *
	 * @param vertex the end-point of all the edges
	 * @return a list {@link List} of all edges which end-point is equal to the vertex
	 */
	default List<E> getEdges(@NotNull final V vertex) {
		return Lists.newArrayList(new EdgeOfVertexIterator(this, vertex));
	}

	/**
	 * Returns a {@link Stream} of edges which end-point is equal to the vertex, i.e. all edges connected to the vertex
	 *
	 * @param vertex the end-point of all the edges
	 * @return a {@link Stream} of edges which end-point is equal to the vertex
	 */
	default Stream<E> streamEdges(@NotNull final V vertex) {
		Iterable<E> iterable = getEdgeIt(vertex);
		return StreamSupport.stream(iterable.spliterator(), false);
	}

	/**
	 * Returns a list {@link List} of vertices which are adjacent to the vertex of this edge.
	 *
	 * @param edge the edge which holds the vertex
	 * @return a list {@link List} of vertices which are adjacent to the vertex of this edge.
	 */
	default List<V> getAdjacentVertices(@NotNull final E edge) {
		return streamIncidentEdges(edge).map(this::getVertex).collect(Collectors.toList());
	}

	/**
	 * Returns a list {@link List} of all (alive) edges in O(n) where n is the number of edges.
	 *
	 * @return a list {@link List} of all (alive) edges.
	 */
	default List<E> getEdges() {
		List<E> edges = new ArrayList<>();
		for (E edge : getEdgeIt()) {
			if(isAlive(edge)) {
				edges.add(edge);
			}
		}

		return edges;
	}

	/**
	 * Returns a list {@link Set} of all (alive) edges (only one of the half-edge) transformed into lines
	 * in O(n) where n is the number of half-edges.
	 *
	 * @return a list {@link Set} of all (alive) edges transformed into lines
	 */
	default Set<VLine> getLines() {
		return streamEdges().map(edge -> toLine(edge)).collect(Collectors.toSet());
	}

	/**
	 * Returns a list {@link Set} of all unique (alive) points transformed into immutable {@link VPoint}
	 * in O(n) where n is the number of points.
	 *
	 * @return a list {@link Set} of all unique (alive) edges transformed into lines
	 */
	default Set<VPoint> getUniquePoints() {
		return streamVertices().map(vertex -> toPoint(vertex)).collect(Collectors.toSet());
	}

	/**
	 * Returns a list {@link Collection} of all (alive) points in O(n) where n is the number of points.
	 *
	 * @return a list {@link Collection} of all (alive) points
	 */
	default Collection<IPoint> getPoints() {
		return streamVertices().map(vertex -> getPoint(vertex)).collect(Collectors.toList());
	}

	/**
	 * Returns an iterator {@link Iterable} iterating over all edges (this includes destroyed edges).
	 *
	 * @return an iterator {@link Iterable} iterating over all edges (this includes destroyed edges)
	 */
	Iterable<E> getEdgeIt();

	/**
	 * Returns a list {@link List} of all edges of a face in O(k) where k is the number of
	 * points / vertices / half-edges of the face.
	 *
	 * @param face the face
	 * @return a list {@link List} of all edges of a face.
	 */
	default List<E> getEdges(@NotNull final F face) {
		return Lists.newArrayList(new EdgeIterator(this, face));
	}

	/**
	 * Returns a list {@link List} of all edges of a face in O(k) where k is the number of
	 * points / vertices / half-edges of the face.
	 *
	 * @param edge some edge of the face
	 * @return a list {@link List} of all edges of a face.
	 */
	default List<E> getEdges(@NotNull final E edge) {
		return Lists.newArrayList(new EdgeIterator(this, edge));
	}

	/**
	 * Returns a list {@link List} of all vertices of a face in O(n) where n is the
	 * number of faces.
	 *
	 * @param face the face
	 * @return a list {@link List} of all vertices of a face.
	 */
	default List<V> getVertices(@NotNull final F face) {
		EdgeIterator<V, E, F> edgeIterator = new EdgeIterator<>(this, face);

		List<V> vertices = new ArrayList<>();
		while (edgeIterator.hasNext()) {
			vertices.add(getVertex(edgeIterator.next()));
		}

		return vertices;
	}

	/**
	 * Returns a list {@link List} of all points of a face in O(k), where k is the number of points
	 * of the face.
	 *
	 * @param face the face
	 * @return a list {@link List} of all points of a face.
	 */
	default List<IPoint> getPoints(@NotNull final F face) {
		EdgeIterator<V, E, F> edgeIterator = new EdgeIterator<>(this, face);

		List<IPoint> points = new ArrayList<>();
		while (edgeIterator.hasNext()) {
			points.add(getPoint(edgeIterator.next()));
		}

		return points;
	}

	/**
	 * Returns true if the two half-edges represent the same line segment,
	 * i.e. their set of points (start- and end-point) is equals in O(1).
	 *
	 * @param e1 the first half-edge
	 * @param e2 the second half-edge
	 * @return true if the two half-edges represent the same line segment, false otherwise
	 */
	default boolean isSameLineSegment(@NotNull final E e1, @NotNull final E e2) {
		if(e1.equals(e2)) {
			return true;
		}

		IPoint p11 = getPoint(e1);
		IPoint p12 = getPoint(getPrev(e1));

		IPoint p21 = getPoint(e2);
		IPoint p22 = getPoint(getPrev(e2));

		return p11.equals(p21) && p12.equals(p22) || p11.equals(p22) && p12.equals(p21);
	}

	/**
	 * Tests whether the point (x,y) is a vertex of the face in O(k),
	 * where k is the number of points /vertices of the face.
	 *
	 * @param face  the face
	 * @param x     the x-coordinate of the point
	 * @param y     the y-coordinate of the point
	 * @return true if the point (x,y) is a vertex of the face, false otherwise
	 */
	default boolean isCloseTo(@NotNull final F face, final double x, final double y) {
		return getMember(face, x, y).isPresent();
	}

	/**
	 * Tests whether the point (x,y) is very close to one of the points of the face
	 * in O(k) where k is the number of points /vertices of the face.
	 *
	 * @param face      the face
	 * @param x         the x-coordinate of the point
	 * @param y         the y-coordinate of the point
	 * @param distance  the maximal distance of the point
	 *
	 * @return true if the point (x,y) is very close to a vertex of the face, false otherwise
	 */
	default boolean isCloseTo(@NotNull final F face, final double x, final double y, final double distance) {
		return getClose(face, x, y, distance).isPresent();
	}

	/**
	 * (Optional) returns an arbitrary edge with an end point having the same coordinates as the point (x, y)
	 * in O(k) where k is the number of points /vertices of the face.
	 *
	 * @param face  the face
	 * @param x     the x-coordinate of the point
	 * @param y     the y-coordinate of the point
	 * @return (optional) an arbitrary edge with an end point having the same coordinates as the point (x, y)
	 */
	default Optional<E> getMember(@NotNull final F face, final double x, final double y) {
		return streamEdges(face).filter(e -> getVertex(e).getX() == x && getVertex(e).getY() == y).findAny();
	}

	/**
	 * (Optional) returns an arbitrary edge with an end point close to the point (x, y) in O(k),
	 * where k is the number of points /vertices of the face.
	 *
	 * @param face      the face
	 * @param x         the x-coordinate of the point
	 * @param y         the y-coordinate of the point
	 * @param distance  the maximal distance
	 * @return (optional) an arbitrary edge with an end point having the same coordinates as the point (x, y)
	 */
	default Optional<E> getClose(@NotNull final F face, final double x, final double y, final double distance) {
		return streamEdges(face).filter(e -> getVertex(e).distance(x, y) <= distance).findAny();
	}

	/**
	 * Returns a list {@link List} of all (alive) vertices of the mesh in O(n),
	 * where n is the number of vertices.
	 *
	 * @return a list {@link List} of all (alive) vertices of the mesh
	 */
	default List<V> getVertices() {
		return streamVertices().collect(Collectors.toList());
	}

	/**
	 * Returns valid vertex uniformly randomly chosen from the set of all vertices.
	 *
	 * @param random a pseudo-random number generator
	 *
	 * @return valid vertex uniformly randomly chosen from the set of all vertices.
	 */
	V getRandomVertex(@NotNull final Random random);

	/**
	 * Returns the number of alive vertices in O(1).
	 *
	 * @return the number of alive vertices
	 */
	int getNumberOfVertices();

	/**
	 * Returns the number of alive interior faces in O(1), i.e. holes and the border are excluded.
	 *
	 * @return the number of alive faces
	 */
	int getNumberOfFaces();

	/**
	 * Returns the number of alive holes in O(1).
	 *
	 * @return the number of alive faces
	 */
	int getNumberOfHoles();

	/**
	 * Returns the number of alive edges in O(1).
	 *
	 * @return the number of alive edges
	 */
	int getNumberOfEdges();

	// TODO duplcated code see getNearestPoint.
	/**
	 * Returns the closest (Euklidean distance) vertex of the face with respect to (x ,y) in
	 * O(k), where k is the number of vetices of the face.
	 *
	 * @param face  the face
	 * @param x     the x-coordinate of the point
	 * @param y     the y-coordinate of the point
	 * @return the closest vertex of the face with respect to (x ,y)
	 */
	default V closestVertex(@NotNull final F face, final double x, final double y) {
		V result = null;
		double distance = Double.MAX_VALUE;
		for (V vertex : getVertexIt(face)) {
			if(getPoint(vertex).distance(x, y) < distance) {
				result = vertex;
			}
		}

		return result;
	}

	default String getMeshInformations() {
		// here we divide the number of half-edges by 2 because each edge is represented by 2 half-edges
		return "#vertices = " + getNumberOfVertices() +
				", #edges = " + getNumberOfEdges() / 2 +
				", #faces = " + getNumberOfFaces();
	}

	/**
	 * This method is for synchronizing resources if multiple threads are used.
	 * It tries to lock the vertex which might be uses to modify the mesh data structure
	 * by multiple threads e.g. one can flip an edge see {@link ITriConnectivity#flipSync(IHalfEdge)}
	 * in parallel by locking all 4 involved vertices beforehand.
	 *
	 * @param vertex the vertex for which the lock is acquired
	 * @return true if the lock was successfully acquired, false otherwise
	 */
	boolean tryLock(@NotNull final V vertex);

	/**
	 * This method is for synchronizing resources if multiple threads are used.
	 * It releases the lock if it was acquired otherwise this method has no effect.
	 *
	 * @param vertex the vertex for which the lock is released
	 */
	void unlock(@NotNull final V vertex);

	/**
	 * Returns the edge of a given face which is the closest edge of the face in respect to the point defined
	 * by (x,y). The point might be outside or inside the face or even on an specific edge.
	 *
	 * @param face  the face
	 * @param x     x-coordinate of the point
	 * @param y     y-coordinate of the point
	 * @return the edge of a given face which is closest to a point p = (x,y)
	 */
	default E closestEdge(@NotNull final F face, final double x, final double y) {
		E result = null;
		double minDistance = Double.MAX_VALUE;
		for (E edge : getEdgeIt(face)) {
			double distance = GeometryUtils.distanceToLineSegment(getPoint(getPrev(edge)), getPoint(edge), x, y);
			if(distance < minDistance) {
				result = edge;
				minDistance = distance;
			}
		}
		return result;
	}

	default double getMinEdgeLen() {
		return streamEdges().map(e -> toLine(e)).mapToDouble(l -> l.length()).min().orElse(0.0);
	}

	default double getMaxEdgeLen() {
		return streamEdges().map(e -> toLine(e)).mapToDouble(l -> l.length()).max().orElse(0.0);
	}

	/**
	 * Returns the half-edge which ends in v1 and starts in v2 if there is any, otherwise empty.
	 *
	 * @param v1 the end vertex
	 * @param v2 the start vertex
	 * @return the half-edge which ends in v1 and starts in v2 if there is any, empty otherwise
	 */
	default Optional<E> getEdge(@NotNull V v1, @NotNull V v2){
		for(E edge : getEdgeIt(v1)) {
			if(getTwinVertex(edge).equals(v2)) {
				return Optional.of(edge);
			}
		}
		return Optional.empty();
	}

	default Optional<E> getEdge(@NotNull final F face, @NotNull final VPoint v1, @NotNull final VPoint v2){
		for(E edge : getEdgeIt(face)) {
			if(toPoint(getTwinVertex(edge)).equals(v1) && toPoint(getVertex(edge)).equals(v2)) {
				return Optional.of(edge);
			}
		}
		return Optional.empty();
	}

	//default Optional<E> getBoundaryEdge()

	/**
	 * Returns a deep clone of this mesh.
	 *
	 * @return a deep clone of this mesh
	 */
	IMesh<V, E, F> clone();

	/**
	 * Returns a rectangular bound containing all vertices of the mesh.
	 *
	 * @return a rectangular bound containing all vertices of the mesh
	 */
	default VRectangle getBound() {

		if(getNumberOfVertices() <= 2) {
			return new VRectangle(0,0,1,1);
		}

		double minX = Double.MAX_VALUE;
		double maxX = Double.MIN_VALUE;
		double minY = Double.MAX_VALUE;
		double maxY = Double.MIN_VALUE;

		for(IPoint p : getPoints()) {
			minX = Math.min(minX, p.getX());
			minY = Math.min(minY, p.getY());
			maxX = Math.max(maxX, p.getX());
			maxY = Math.max(maxY, p.getY());
		}

		return new VRectangle(minX, minY, maxX-minX, maxY-minY);
	}

	/**
	 * Transforms the mesh into a rich triangulation {@link IIncrementalTriangulation}.
	 * There will be no connectivity changes performed!
	 *
	 * Assumption: The mesh is a valid triangulation.
	 *
	 * @param type  specifies the used {@link IPointLocator}
	 * @return a triangulation {@link IIncrementalTriangulation} of this mesh
	 */
	IIncrementalTriangulation<V, E, F> toTriangulation(@NotNull final IPointLocator.Type type);

	/**
	 * Rearranges the memory location of faces, vertices and halfEdges of the mesh according to
	 * the {@link Iterable} faceOrder. I.e. edges, vertices and faces which are close the faceOrder
	 * will be close in the memory!
	 *
	 * Assumption: faceOrder contains all faces of this mesh.
	 * Invariant: the geometry i.e. the connectivity and the vertex positions will not change.
	 *
	 * @param faceOrder the new order
	 */
	void arrangeMemory(@NotNull final Iterable<F> faceOrder);

	/**
	 * This method is for logging information. It returns a string of the path defining the polygon of a face.
	 *
	 * @param face  the face
	 * @return a string of the path defining the polygon of a face
	 */
	default String toPath(@NotNull final F face) {
		return streamPoints(face).map(p -> p.toString()).reduce((s1, s2) -> s1 + " -> " + s2).orElse("");
	}

	/**
	 * Tests whether the half-edge is the longest edge of its two faces (excluding boundary faces).
	 * This requires O(n + m) where n and m are the number of edges of the two neighbouring faces.
	 *
	 * @param edge  the half-edge
	 * @return true if the half-edge is the longest edge of its two faces (excluding boundary faces), false otherwise
	 */
	default boolean isLongestEdge(@NotNull final E edge) {
		if(!isAtBoundary(edge)) {
			E longestEdge1 = streamEdges(getFace(edge)).reduce((e1, e2) -> toLine(e1).length() > toLine(e2).length() ? e1 : e2).get();
			E longestEdge2 = streamEdges(getTwinFace(edge)).reduce((e1, e2) -> toLine(e1).length() > toLine(e2).length() ? e1 : e2).get();
			return isSameLineSegment(longestEdge1, edge) && isSameLineSegment(longestEdge2, edge);
		}
		else {
			E nonBoundaryEdge = edge;
			if(isBoundary(edge)) {
				nonBoundaryEdge = getTwin(edge);
			}

			E longestEdge = streamEdges(getFace(nonBoundaryEdge)).reduce((e1, e2) -> toLine(e1).length() > toLine(e2).length() ? e1 : e2).get();
			return isSameLineSegment(longestEdge, edge);
		}
	}

	/**
	 * Tests if the mesh is a valid mesh, i.e. all relations between edges, faces and vertices are correct,
	 * e.g. <tt>getFace(getEdge(face)) == face</tt>.
	 *
	 * @return true if the mesh is valid, false otherwise
	 */
	default boolean isValid() {
		String message = "invalid mesh: ";
		for(F face : getFacesWithBoundary()) {
			int count = 0;
			for(E edge : getEdgeIt(face)) {
				count++;
				if(count > getNumberOfEdges()) {
					logger.warn(message + "endless loop in face");
					return false;
				}

				F f = getFace(edge);

				if(f == null) {
					logger.warn(message + "null face of edge " + edge);
					return false;
				}

				if(!f.equals(face)) {
					logger.warn(message + "wrong edge face " + face + "!=" + getFace(edge));
					return false;
				}
			}

			if(count < 3) {
				logger.warn(message + "number of edges smaller 2");
				return false;
			}

			count = 0;
			for(V vertex : getVertexIt(face)) {
				if(count > getNumberOfVertices()) {
					logger.warn(message + "endless loop in face");
					return false;
				}
			}
		}

		for(V vertex : getVertices()) {
			int count = 0;
			E edge = getEdge(vertex);
			if(edge == null) {
				logger.warn(message + "null edge of vertex " + vertex);
				return false;
			}

			if(!vertex.equals(getVertex(edge))) {
				logger.warn(message + "wrong edge vertex " + vertex + "!=" + getVertex(edge));
				return false;
			}

			for(E e : getEdgeIt(vertex)) {
				if(count > getNumberOfVertices()) {
					logger.warn(message + "endless loop around vertex " + vertex);
					return false;
				}

				if(!vertex.equals(getVertex(e))) {
					logger.warn(message + "wrong edge vertex " + vertex + "!=" + getVertex(e));
					return false;
				}
			}
		}

		for(E edge : getEdges()) {
			E twin = getTwin(edge);
			E next = getNext(edge);
			E prev = getPrev(edge);
			V v = getVertex(edge);
			F face = getFace(edge);
			F twinFace = getFace(twin);

			if(twin == null) {
				logger.warn(message + "twin is null for " + edge);
				return false;
			}

			if(next == null) {
				logger.warn(message + "next is null for " + edge);
				return false;
			}

			if(prev == null) {
				logger.warn(message + "prev is null for " + edge);
				return false;
			}

			if(v == null) {
				logger.warn(message + "vertex is null for " + edge);
				return false;
			}

			E twinTwin = getTwin(twin);

			if(twinTwin == null) {
				logger.warn(message + "twin of the twin is null for " + edge);
				return false;
			}

			if(!twinTwin.equals(edge)) {
				logger.warn(message + "twin of the twin is not equal to the edge " + edge);
				return false;
			}

			V twinVertex = getVertex(twin);

			if(twinVertex == null) {
				logger.warn(message + "vertex of the twin is null for " + edge);
				return false;
			}

			if(twinVertex.equals(v)) {
				logger.warn(message + "edge ends and starts at the same vertex " + v);
				return false;
			}

			if(twinFace.equals(face)) {
				logger.warn(message + "the faces of the edge and its twin are equals");
				return false;
			}

			if(isBoundary(edge) && isBoundary(twin)) {
				logger.warn(message + "the faces of the edge and its twin are boundaries");
				return false;
			}
		}

		return true;
	}

	/**
	 * Creates a very simple mesh consisting of two triangles ((-100, 0), (100, 0), (0, 1)) and ((0, -1), (-100, 0), (100, 0))
	 *
	 * @param mesh  the mesh used to create the triangle. This mesh should be empty.
	 * @param <V>   the type of the vertex
	 * @param <E>   the type of the edge
	 * @param <F>   the type of the face
	 */
	static <V extends IVertex, E extends IHalfEdge, F extends IFace> void createSimpleTriMesh(
			@NotNull final IMesh<V, E, F> mesh
	) {
		F face1;
		F face2;
		F border;
		V x, y, z, w;
		E zx ;
		E xy;
		E yz;

		E wx;
		E xz;
		E yw;
		E zy;

		border = mesh.getBorder();

		// first triangle xyz
		face1 = mesh.createFace();
		x = mesh.insertVertex(-100, 0);
		y = mesh.insertVertex(100, 0);
		z = mesh.insertVertex(0, 1);

		zx = mesh.createEdge(x, face1);
		mesh.setEdge(x, zx);
		xy = mesh.createEdge(y, face1);
		mesh.setEdge(y, xy);
		yz = mesh.createEdge(z, face1);
		mesh.setEdge(z, yz);

		mesh.setNext(zx, xy);
		mesh.setNext(xy, yz);
		mesh.setNext(yz, zx);

		mesh.setEdge(face1, xy);


		// second triangle yxw
		face2 = mesh.createFace();
		w = mesh.insertVertex(0, -1);

		E yx = mesh.createEdge(x, face2);
		E xw = mesh.createEdge(w, face2);
		E wy = mesh.createEdge(y, face2);

		mesh.setNext(yx, xw);
		mesh.setNext(xw, wy);
		mesh.setNext(wy, yx);

		mesh.setEdge(face2, yx);

		mesh.setTwin(xy, yx);

		// border twins
		zy = mesh.createEdge(y, border);
		xz = mesh.createEdge(z, border);

		mesh.setTwin(yz, zy);
		mesh.setTwin(zx, xz);

		wx = mesh.createEdge(x, border);
		yw = mesh.createEdge(w, border);
		mesh.setEdge(w, yw);

		mesh.setEdge(border, wx);
		mesh.setTwin(xw, wx);
		mesh.setTwin(wy, yw);


		mesh.setNext(zy, yw);
		mesh.setNext(yw, wx);
		mesh.setNext(wx, xz);
		mesh.setNext(xz, zy);
	}


	default String toPythonValues(@NotNull final Function<V, Double> evalPoint) {
		StringBuilder builder = new StringBuilder();
		List<V> vertices = getVertices();
		for(V v : vertices) {
			builder.append(evalPoint.apply(v) + ",");
		}
		builder.delete(builder.length()-1, builder.length());
		builder.append("\n");
		return builder.toString();
	}

	/**
	 * Constructs and returns a string which can be used to construct a matplotlib Triangulation
	 * which is helpful to plot the mesh.
	 *
	 * @param evalPoint a function to extract double values from vertices.
	 *
	 * @return a string representing the mesh
	 */
	default String toPythonTriangulation(@Nullable final Function<V, Double> evalPoint) {
		garbageCollection();
		StringBuilder builder = new StringBuilder();
		List<V> vertices = getVertices();
		Map<V, Integer> indexMap = new HashMap<>();

		// [x1, x2, ...]
		builder.append("X.append([");
		for(int i = 0; i < vertices.size(); i++) {
			V v = vertices.get(i);
			indexMap.put(v, i);
			builder.append(v.getX() + ",");
		}
		builder.delete(builder.length()-1, builder.length());
		builder.append("])\n");

		// [y1, y2, ...]
		builder.append("Y.append([");
		for(V v : vertices) {
			builder.append(v.getY() + ",");
		}
		builder.delete(builder.length()-1, builder.length());
		builder.append("])\n");

		// [z1, z2, ...] z = value
		if(evalPoint != null) {
			builder.append("Z.append([");
			for(V v : vertices) {
				builder.append(evalPoint.apply(v) + ",");
			}
			builder.delete(builder.length()-1, builder.length());
			builder.append("])\n");
		}

		// [[vId1, vId2, vId3], ...]
		List<F> faces = getFaces();
		builder.append("TRIS.append([");
		for(F face : faces) {
			builder.append("[");
			for(V v : getVertexIt(face)) {
				int index = indexMap.get(v);
				builder.append(index + ",");
			}
			builder.delete(builder.length()-1, builder.length());
			builder.append("],");
		}
		builder.delete(builder.length()-1, builder.length());
		builder.append("])\n");

		return builder.toString();
	}

	// delete
	default void getVirtualSupport(@NotNull final V v, @NotNull final E edge, @NotNull final List<Pair<V, V>> virtualSupport) {
		//assert isNonAcute(getMesh().getVertex(edge), getMesh().getVertex(getMesh().getNext(edge)), getMesh().getVertex(getMesh().getPrev(edge)));

		if(isAtBoundary(edge)) {
			return;
		}

		E prev = getPrev(edge);
		E twin = getTwin(edge);

		V v1 = getVertex(prev);
		V v2 = getVertex(edge);
		V u = getVertex(getNext(twin));

		if(!isNonAcute(u, v, v1)) {
			virtualSupport.add(Pair.of(v1, u));
		} else {
			getVirtualSupport(v, getNext(twin), virtualSupport);
		}

		if(!isNonAcute(v2, v, u)) {
			virtualSupport.add(Pair.of(v2, u));
		} else {
			getVirtualSupport(v, getPrev(twin), virtualSupport);
		}
	}

	// delete
	default boolean isNonAcute(V v1, V v2, V v3) {
		double angle1 = GeometryUtils.angle(v1, v2, v3);

		// non-acute triangle
		double rightAngle = Math.PI/2;
		return angle1 > rightAngle + GeometryUtils.DOUBLE_EPS;
	}

	default boolean isNonAcute(@NotNull final E edge) {
		VPoint p1 = toPoint(getPrev(edge));
		VPoint p2 = toPoint(edge);
		VPoint p3 = toPoint(getNext(edge));

		double angle1 = GeometryUtils.angle(p1, p2, p3);

		// non-acute triangle
		double rightAngle = Math.PI/2;
		return angle1 > rightAngle + GeometryUtils.DOUBLE_EPS;
	}


	// Default-Container setter and getter
	/*default double getCurvature(@NotNull final V vertex) {
		return getDoubleData(vertex, "curvature");
	}

	default void setCurvature(@NotNull final V vertex, final double curvature) {
		setDoubleData(vertex, "curvature", curvature);
	}

	default double getPotential(@NotNull final V vertex) {
		return getDoubleData(vertex, "potential");
	}

	default void setPotential(@NotNull final V vertex, final double potential) {
		setDoubleData(vertex, "potential", potential);
	}

	default boolean isBurned(@NotNull final V vertex) {
		return getBooleanData(vertex, "burned");
	}

	default void setBurned(@NotNull final V vertex, final boolean burned) {
		setBooleanData(vertex, "burned", burned);
	}

	default boolean isBurning(@NotNull final V vertex) {
		return getBooleanData(vertex, "burning");
	}

	default void setBurning(@NotNull final V vertex, final boolean burning) {
		setBooleanData(vertex, "burning", burning);
	}

	default boolean isTarget(@NotNull final V vertex) {
		return getBooleanData(vertex, "target");
	}

	default void setTarget(@NotNull V vertex, boolean target) {
		setBooleanData(vertex, "target", target);
	}*/

	default IEdgeContainerDouble<V, E, F> getDoubleEdgeContainer(@NotNull final String name) {
		return new IEdgeContainerDouble<>() {
			@Override
			public double getValue(@NotNull final E edge) {
				return getDoubleData(edge, name);
			}

			@Override
			public void setValue(@NotNull final E edge, double value) {
				setDoubleData(edge, name, value);
			}
		};
	}

	default IVertexContainerDouble<V, E, F> getDoubleVertexContainer(@NotNull final String name) {
		return new IVertexContainerDouble<>() {
			@Override
			public double getValue(@NotNull V vertex) {
				return getDoubleData(vertex, name);
			}

			@Override
			public void setValue(@NotNull V vertex, double value) {
				setDoubleData(vertex, name, value);
			}

			@Override
			public void reset() {
				for(V v : getVertices()) {
					setValue(v, 0.0);
				}
			}
		};
	}

	default IVertexContainerBoolean<V, E, F> getBooleanVertexContainer(@NotNull final String name) {
		return new IVertexContainerBoolean<>() {
			@Override
			public boolean getValue(@NotNull final V vertex) {
				return getBooleanData(vertex, name);
			}

			@Override
			public void setValue(@NotNull final V vertex, final boolean value) {
				setBooleanData(vertex, name, value);
			}
		};
	}

	default <CV> IVertexContainerObject<V, E, F, CV> getObjectVertexContainer(@NotNull final String name, final Class<CV> clazz) {

		return new IVertexContainerObject<>() {

			@Override
			public CV getValue(@NotNull final V v) {
				return getData(v, name, clazz).get();
			}

			@Override
			public void setValue(@NotNull final V v, final CV value) {
				setData(v, name, value);
			}

		};
	}

	default IEdgeContainerBoolean<V, E, F> getBooleanEdgeContainer(@NotNull final String name) {
		return new IEdgeContainerBoolean<>() {
			@Override
			public boolean getValue(@NotNull final E edge) {
				return getBooleanData(edge, name);
			}

			@Override
			public void setValue(@NotNull final E edge, final boolean value) {
				setBooleanData(edge, name, value);
			}
		};
	}

	default <CV> IEdgeContainerObject<V, E, F, CV> getObjectEdgeContainer(@NotNull final String name, final Class<CV> clazz) {

		return new IEdgeContainerObject<>() {

			@Override
			public CV getValue(@NotNull final E edge) {
				return getData(edge, name, clazz).get();
			}

			@Override
			public void setValue(@NotNull final E edge, final CV value) {
				setData(edge, name, value);
			}

		};
	}

}
