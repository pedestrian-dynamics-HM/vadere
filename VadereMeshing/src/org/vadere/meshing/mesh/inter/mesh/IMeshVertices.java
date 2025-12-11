package org.vadere.meshing.mesh.inter.mesh;

import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.iterators.AdjacentVertexIterator;
import org.vadere.meshing.mesh.iterators.EdgeIterator;
import org.vadere.meshing.mesh.iterators.PointIterator;
import org.vadere.meshing.mesh.iterators.VertexIterator;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Used to access vertices of an {@link IMesh}
 */
public interface IMeshVertices<V extends IVertex, E extends IHalfEdge, F extends IFace> extends Iterable<V> {
    IMesh<V, E, F> parent();

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

    default VPoint toPoint(@NotNull IPoint p) {
        return new VPoint(p.getX(), p.getY());
    }


    /**
     * Returns the degree of a vertex i.e. the number of connected full-edges
     * in O(d) where d is the degree of the vertex.
     *
     * @param vertex the vertex
     * @return the degree of a vertex
     */
    default int degree(@NotNull V vertex) {
        return Iterators.size(adjacentIterableFor(vertex).iterator());
    }


    /**
     * Returns the vertex of the twin of the half-edge {@link E} in O(1).
     *
     * @param halfEdge the half-edge
     * @return the vertex of the twin of the half-edge {@link E}.
     */
    default V getTwin(@NotNull E halfEdge) {
        return getEndOf(parent().edges().getTwin(halfEdge));
    }

    /**
     * Returns the (end-)vertex of the half-edge in O(1).
     *
     * @param halfEdge the half-edge
     * @return the (end-)vertex of the half-edge
     */
    V getEndOf(@NotNull E halfEdge);

    double getX(@NotNull V vertex);
    double getY(@NotNull V vertex);

    /**
     * Returns the point (i.e. the data saved on the vertex) of a vertex in O(1).
     *
     * @param vertex the vertex
     * @return the point of vertex
     */
    IPoint toMutablePoint(@NotNull V vertex);

    /**
     * Returns a Iterable {@link Iterable} which can be used to iterate over adjacent vertices of this vertex.
     *
     * @param vertex the vertex
     * @return a Iterable {@link Iterable} which can be used to iterate over all adjacent vertices.
     */
    default Iterable<V> adjacentIterableFor(@NotNull final V vertex) {
        return () -> new AdjacentVertexIterator<>(parent(), vertex);
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
        return parent().edges().getAtBoundaryEdge(vertex).isPresent();
    }

    /**
     * Returns true if the vertex is at the border in O(d) where d is the degree of the vertex.
     *
     * @param vertex the vertex
     * @return true if the vertex is at the border, otherwise false
     */
    default boolean isAtBorder(@NotNull final V vertex) {
        return parent().edges().streamEdgesOf(vertex).anyMatch(e -> parent().edges().isAtBorder(e));
    }

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
     * Returns true if the vertex is already destroyed i.e. not part of the geometric representation.
     *
     * @param vertex the vertex
     * @return true if the vertex is already destroyed, false otherwise
     */
    boolean isDestroyed(@NotNull final V vertex);

    /**
     * Returns a stream {@link Stream} of all (alive) vertices.
     *
     * @return a stream {@link Stream} of all vertices.
     */
    Stream<V> stream();

    /**
     * Returns a parallel stream {@link Stream} of all (alive) vertices.
     * Synchronization has to be done by the user.
     *
     * @return a parallel stream {@link Stream} of all vertices.
     */
    Stream<V> streamParallel();

    /**
     * Returns a stream {@link Stream} of all (alive) points.
     *
     * @return a stream {@link Stream} of all points.
     */
    default Stream<IPoint> streamPoints() {
        return stream().map(v -> toMutablePoint(v));
    }

    /**
     * Returns a stream {@link Stream} of all of a specific face.
     *
     * @param face the specific face
     * @return a stream {@link Stream} of all points of a specific face.
     */
    default Stream<IPoint> streamPoints(@NotNull final F face) {
        return streamVerticesOf(face).map(v -> toMutablePoint(v));
    }

    /**
     * Returns a list {@link List} of all adjacent points of the vertex in O(d)
     * where d is the degree of the vertex.
     *
     * @param vertex the vertex
     * @return a list {@link List} of all adjacent points of the vertex
     */
    default List<IPoint> getPoints(@NotNull final V vertex) {
        List<IPoint> points = new ArrayList<>();
        for(V v : adjacentIterableFor(vertex)) {
            points.add(toPoint(v));
        }

        return points;
    }

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

    // TODO: this can be done much faster: only filter the edges of holes and the border!
    /**
     * Returns a list {@link List} of all (alive) boundary vertices, i.e. vertices which have a boundary edge as their neighbours.
     * This requires O(n) where n is the number of edges.
     *
     * @return a list {@link List} of all boundary vertices
     */
    default List<V> getBoundaryVertices() {
        var edges = parent().edges();
        return edges.stream()
                .filter(edges::isBoundary)
                .filter(edges::isAlive)
                .map(v -> this.getEndOf(v))
                .collect(Collectors.toList());
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
     * <p>Returns vertex of the triangulation of the face with the smallest distance to point.</p>
     *
     * @param face          the face of the trianuglation
     * @param point         the point
     * @return vertex of the triangulation of the face with the smallest distance to point
     */
    default V getNearestPoint(@NotNull final F face, @NotNull final IPoint point) {
        return getNearestPoint(face, point.getX(), point.getY());
    }

    default List<V> getAdjacentVertices(@NotNull final V vertex) {
        return Lists.newArrayList(new AdjacentVertexIterator<>(parent(), vertex));
    }

    /**
     * Returns a list {@link List} of vertices which are adjacent to the vertex of this edge.
     *
     * @param edge the edge which holds the vertex
     * @return a list {@link List} of vertices which are adjacent to the vertex of this edge.
     */
    default List<V> getAdjacentVertices(@NotNull final E edge) {
        return parent().edges()
                .streamIncidentEdges(edge)
                .map(halfEdge -> this.getEndOf(halfEdge))
                .collect(Collectors.toList());
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
        return parent().edges().streamEdgesOf(face)
                .map(edge -> this.getEndOf(edge))
                .reduce((p1, p2) -> p1.distance(x,y) > p2.distance(x,y) ? p2 : p1)
                .get();
    }

    /**
     * Returns a Stream {@link Stream} of all adjacent vertices of the vertex.
     *
     * @param v the vertex
     * @return a Stream {@link Stream} of all adjacent vertices
     */
    default Stream<V> streamVerticesOf(@NotNull final V v) {
        Iterable<V> iterable = adjacentIterableFor(v);
        return StreamSupport.stream(iterable.spliterator(), false);
    }

    /**
     * Returns a Stream {@link Stream} of vertices of a face.
     *
     * @param face the faces of which edges the stream consist
     * @return a Stream {@link Stream} of edges of a face
     */
    default Stream<V> streamVerticesOf(@NotNull final F face) {
        return parent().edges().streamEdgesOf(face).map(edge -> this.getEndOf(edge));
    }

    /**
     * Returns an Iterable {@link Iterable} which can be used to iterate over all vertices of a face.
     *
     * @param face the face the iterable iterates over
     * @return an Iterable {@link Iterable} which can be used to iterate over all vertices of a face
     */
    default Iterable<V> iterableFor(@NotNull final F face) {
        return () -> new VertexIterator<>(parent(), face);
    }

    /**
     * Returns an Iterable {@link Iterable} which can be used to iterate over all vertices of a face.
     *
     * @param face the face the iterable iterates over
     * @return an Iterable {@link Iterable} which can be used to iterate over all vertices of a face
     */
    default Iterable<IPoint> pointIterable(@NotNull final F face) {
        return () -> new PointIterator<>(parent(), face);
    }

    /**
     * Returns a list {@link Set} of all unique (alive) points transformed into immutable {@link VPoint}
     * in O(n) where n is the number of points.
     *
     * @return a list {@link Set} of all unique (alive) edges transformed into lines
     */
    default Set<VPoint> getUniquePoints() {
        return stream().map(vertex -> toPoint(vertex)).collect(Collectors.toSet());
    }

    /**
     * Returns a list {@link Collection} of all (alive) points in O(n) where n is the number of points.
     *
     * @return a list {@link Collection} of all (alive) points
     */
    default Collection<IPoint> toPoints() {
        return stream().map(vertex -> toMutablePoint(vertex)).collect(Collectors.toList());
    }

    /**
     * Returns a list {@link List} of all vertices of a face in O(n) where n is the
     * number of faces.
     *
     * @param face the face
     * @return a list {@link List} of all vertices of a face.
     */
    default List<V> getAllOf(@NotNull final F face) {
        EdgeIterator<V, E, F> edgeIterator = new EdgeIterator<>(parent(), face);

        List<V> vertices = new ArrayList<>();
        while (edgeIterator.hasNext()) {
            vertices.add(getEndOf(edgeIterator.next()));
        }

        return vertices;
    }

    /**
     * Returns a list {@link List} of all (alive) vertices of the mesh in O(n),
     * where n is the number of vertices.
     *
     * @return a list {@link List} of all (alive) vertices of the mesh
     */
    default List<V> getAll() {
        return stream().collect(Collectors.toList());
    }

    /**
     * Returns valid vertex uniformly randomly chosen from the set of all vertices.
     *
     * @param random a pseudo-random number generator
     *
     * @return valid vertex uniformly randomly chosen from the set of all vertices.
     */
    V getRandom(@NotNull final Random random);

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
    default V closestOfFaceTo(@NotNull final F face, final double x, final double y) {
        V result = null;
        double distance = Double.MAX_VALUE;
        for (V vertex : iterableFor(face)) {
            if(toMutablePoint(vertex).distance(x, y) < distance) {
                result = vertex;
            }
        }

        return result;
    }

    default boolean isNonAcute(V v1, V v2, V v3) {
        double angle1 = GeometryUtils.angle(v1, v2, v3);

        // non-acute triangle
        double rightAngle = Math.PI/2;
        return angle1 > rightAngle + GeometryUtils.DOUBLE_EPS;
    }

    /**
     * Returns the number of alive vertices in O(1).
     *
     * @return the number of alive vertices
     */
    int count();
}
