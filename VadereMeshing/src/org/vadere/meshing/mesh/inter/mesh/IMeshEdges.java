package org.vadere.meshing.mesh.inter.mesh;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.iterators.EdgeIterator;
import org.vadere.meshing.mesh.iterators.EdgeOfVertexIterator;
import org.vadere.meshing.mesh.iterators.IncidentEdgeIterator;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public interface IMeshEdges<V extends IVertex, E extends IHalfEdge, F extends IFace> extends Iterable<E> {
    IMesh<V, E, F> base();

    /**
     * Returns the number of alive edges in O(1).
     *
     * @return the number of alive edges
     */
    int count();

    /**
     * Returns the half-edge of the vertex i.e. one half-edge which ends in the vertex in O(1).
     *
     * @param vertex the vertex
     * @return the half-edge of the vertex.
     */
    E getOf(@NotNull V vertex);

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
        return base().faces().isOuterBorder(base().faces().getOf(halfEdge));
    }

    /**
     * Returns a half-edge of the face this can be any half-edge of this face in O(1).
     *
     * @param face the face
     * @return an arbitrary half-edge of the face
     */
    E getAnyOf(@NotNull F face);

    /**
     * Returns the (end-)point of the half-edge in O(1).
     *
     * @param halfEdge the half-edge
     * @return the (end-)point of the half-edge
     */
    IPoint getMutableEndPoint(@NotNull E halfEdge);

    /**
     * Returns true if the edge is a hole edge i.e. part of a hole in O(1) (there might be multiple holes
     * and each hole is a boundary).
     *
     * @param edge the edge
     * @return true if the edge is a hole edge, false otherwise
     */
    default boolean isHole(@NotNull E edge) {
        return base().faces().isHole(base().faces().getOf(edge));
    }

    /**
     * Transforms an half-edge into a line segment (p, q) {@link VLine} where p is the point of the half-edge
     * and q is the point of its predecessor in O(1).
     *
     * @param halfEdge the half-edge
     * @return a line segment (p, q) {@link VLine} which is the transformation of the half-edge
     */
    default VLine toLine(@NotNull E halfEdge) {
        return new VLine(new VPoint(base().vertices().getEndOf(getPrev(halfEdge))), new VPoint(base().vertices().getEndOf(halfEdge)));
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
        if(isBoundary(getOf(vertex))) {
            return Optional.of(getOf(vertex));
        }
        return streamEdgesOf(vertex).filter(e -> isBoundary(e)).findAny();
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
        return streamEdgesOf(vertex).filter(e -> isAtBoundary(e)).findAny();
    }

    /**
     * (Optional) returns an arbitrary boundary edge of the face if one of its neighbouring face is
     * a boundary face or itself is a boundary face in O(k) where k is the number of neighbouring faces.
     *
     * @param face the face
     * @return (optional) a boundary edge
     */
    default Optional<E> getBoundaryEdge(@NotNull final F face) {
        if(base().faces().isBoundary(face)) {
            return Optional.of(getAnyOf(face));
        }
        return streamEdgesOf(face).filter(e -> isAtBoundary(e)).map(e -> getTwin(e)).findAny();
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
     * (Optional) returns the half-edge of a face which has a twin which is a boundary edge
     * i.e. the link to the boundary.
     *
     * @param face (optional) the link to the boundary
     *
     * @return (optional) the half-edge of a face which has a twin which is a boundary edge
     */
    default Optional<E> getLinkToBoundary(@NotNull final F face){
        for(E edge : iterableFor(face)) {
            if(isBoundary(getTwin(edge))) {
                return Optional.of(edge);
            }
        }
        return Optional.empty();
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
     * Returns true if the edge is already destroyed i.e. not part of the geometric representation.
     *
     * @param edge the half-edge
     * @return true if the edge is already destroyed, false otherwise
     */
    boolean isDestroyed(@NotNull final E edge);

    /**
     * Returns a stream {@link Stream} of all (alive) half-edges.
     *
     * @return a stream {@link Stream} of all half-edges.
     */
    Stream<E> stream();

    /**
     * Returns a parallel stream {@link Stream} of all (alive) half-edges.
     * Synchronization has to be done by the user.
     *
     * @return a parallel stream {@link Stream} of all half-edges.
     */
    Stream<E> streamParallel();

    /**
     * Returns a parallel stream {@link Stream} of all (alive) points.
     * Synchronization has to be done by the user.
     *
     * @return a parallel stream {@link Stream} of all points.
     */
    default Stream<IPoint> streamPointsParallel() {
        return streamParallel().map(e -> getMutableEndPoint(e));
    }

    default Stream<E> streamBoundaryEdges() {
        return base().faces().streamBoundaries().flatMap(f -> base().edges().streamEdgesOf(f));
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
        return streamBoundaryEdges().map(e -> getMutableEndPoint(e)).collect(Collectors.toList());
    }

    /**
     * Returns a Iterable {@link Iterable} which can be used to iterate over all edges which end point is the vertex that is adjacent to the vertex of this edge.
     *
     * @param edge the edge which holds the vertex
     * @return a Iterable {@link Iterable} which can be used to iterate over all edges which are adjacent to the vertex of this edge.
     */
    default Iterable<E> getIncidentEdgesIt(@NotNull final E edge) {
        return () -> new IncidentEdgeIterator(base(), edge);
    }

    /**
     * (Optional) returns a point of the mesh fulfilling the predicate if there is any.
     * This requires O(n) where n is the number of half-edges.
     *
     * @param predicate the predicate
     * @return (optional) returns a point of the mesh fulfilling the predicate
     */
    default Optional<E> findAnyEdge(@NotNull final Predicate<IPoint> predicate) {
        return stream().filter(edge -> predicate.test(getMutableEndPoint(edge))).findAny();
    }

    /**
     * Returns a Stream {@link Stream} of edges of a face.
     *
     * @param edge the edge of the face of which edges the stream consist
     * @return a Stream {@link Stream} of edges of a face specified by the edge
     */
    default Stream<E> streamEdgesOf(@NotNull final E edge) {
        Iterable<E> iterable = iterableFor(edge);
        return StreamSupport.stream(iterable.spliterator(), false);
    }

    /**
     * Returns a Stream {@link Stream} of edges of a face.
     *
     * @param face the faces of which edges the stream consist
     * @return a Stream {@link Stream} of edges of a face
     */
    default Stream<E> streamEdgesOf(@NotNull final F face) {
        Iterable<E> iterable = iterableFor(face);
        return StreamSupport.stream(iterable.spliterator(), false);
    }

    default Stream<E> streamEdgesReverse(@NotNull final E edge) {
        Iterable<E> iterable = iterableReversedFor(edge);
        return StreamSupport.stream(iterable.spliterator(), false);
    }

    /**
     * Returns an Iterable {@link Iterable} which can be used to iterate over all edges of a face.
     *
     * @param face the face the iterable iterates over
     * @return an Iterable {@link Iterable} which can be used to iterate over all edges of a face.
     */
    default Iterable<E> iterableFor(@NotNull final F face) {
        return () -> new EdgeIterator<>(base(), face);
    }

    /**
     * Returns an Iterable {@link Iterable} which can be used to iterate over all edges of a face which the edge is part of.
     *
     * @param edge the edge which is part of the face the iterable iterates over
     * @return an Iterable {@link Iterable} which can be used to iterate over all edges of a face.
     */
    default Iterable<E> iterableFor(@NotNull final E edge) {
        return () -> new EdgeIterator<>(base(), edge);
    }


    default Iterable<E> iterableReversedFor(@NotNull final E edge) {
        return () -> new EdgeIterator<>(base(), edge, true);
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
     * Returns a list {@link List} of edges which are incident to the vertex of this edge.
     * They hold the vertices which are adjacent to vertex of the edge.
     *
     * @param edge the edge which holds the vertex
     * @return a list {@link List} of edges which are incident to the vertex of this edge.
     */
    default List<E> getIncidentEdges(@NotNull final E edge) { return Lists.newArrayList(new IncidentEdgeIterator<V, E, F>(base(), edge)); }

    /**
     * Returns an iterable {@link Iterable} that can be used to iterate over all edges which end-point is equal to the vertex, i.e. all edges connected to the vertex.
     *
     * @param vertex the end-point of all the edges
     * @return an iterable {@link Iterable} that can be used to iterate over all edges which end-point is equal to the vertex
     */
    default Iterable<E> iterableFor(@NotNull final V vertex) {
        return () -> new EdgeOfVertexIterator<V, E, F>(base(), vertex);
    }

    /**
     * Returns a list {@link List} of all edges which end-point is equal to the vertex, i.e. all edges connected to the vertex
     *
     * @param vertex the end-point of all the edges
     * @return a list {@link List} of all edges which end-point is equal to the vertex
     */
    default List<E> getAllOf(@NotNull final V vertex) {
        return Lists.newArrayList(new EdgeOfVertexIterator<V, E, F>(base(), vertex));
    }

    /**
     * Returns a {@link Stream} of edges which end-point is equal to the vertex, i.e. all edges connected to the vertex
     *
     * @param vertex the end-point of all the edges
     * @return a {@link Stream} of edges which end-point is equal to the vertex
     */
    default Stream<E> streamEdgesOf(@NotNull final V vertex) {
        Iterable<E> iterable = iterableFor(vertex);
        return StreamSupport.stream(iterable.spliterator(), false);
    }

    /**
     * Returns a list {@link List} of all (alive) edges in O(n) where n is the number of edges.
     *
     * @return a list {@link List} of all (alive) edges.
     */
    default List<E> getAll() {
        List<E> edges = new ArrayList<>();
        for (E edge : this) {
            if(isAlive(edge)) {
                edges.add(edge);
            }
        }

        return edges;
    }

    /**
     * Returns a list {@link List} of all edges of a face in O(k) where k is the number of
     * points / vertices / half-edges of the face.
     *
     * @param face the face
     * @return a list {@link List} of all edges of a face.
     */
    default List<E> getAllOf(@NotNull final F face) {
        return Lists.newArrayList(new EdgeIterator<V, E, F>(base(), face));
    }

    /**
     * Returns a list {@link List} of all edges of a face in O(k) where k is the number of
     * points / vertices / half-edges of the face.
     *
     * @param edge some edge of the face
     * @return a list {@link List} of all edges of a face.
     */
    default List<E> getAllOf(@NotNull final E edge) {
        return Lists.newArrayList(new EdgeIterator<V, E, F>(base(), edge));
    }

    /**
     * Returns a list {@link Set} of all (alive) edges (only one of the half-edge) transformed into lines
     * in O(n) where n is the number of half-edges.
     *
     * @return a list {@link Set} of all (alive) edges transformed into lines
     */
    default Set<VLine> getLines() {
        return stream().map(edge -> toLine(edge)).collect(Collectors.toSet());
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

        IPoint p11 = getMutableEndPoint(e1);
        IPoint p12 = getMutableEndPoint(getPrev(e1));

        IPoint p21 = getMutableEndPoint(e2);
        IPoint p22 = getMutableEndPoint(getPrev(e2));

        return p11.equals(p21) && p12.equals(p22) || p11.equals(p22) && p12.equals(p21);
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
        return streamEdgesOf(face).filter(
                e -> base().vertices().getEndOf(e).getX() == x
                && base().vertices().getEndOf(e).getY() == y)
                .findAny();
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
        return streamEdgesOf(face).filter(e -> base().vertices().getEndOf(e).distance(x, y) <= distance).findAny();
    }

    /**
     * Returns the edge of a given face which is the closest edge of the face in respect to the point defined
     * by (x,y). The point might be outside or inside the face or even on an specific edge.
     *
     * @param face  the face
     * @param x     x-coordinate of the point
     * @param y     y-coordinate of the point
     * @return the edge of a given face which is closest to a point p = (x,y)
     */
    default E closestOfFaceTo(@NotNull final F face, final double x, final double y) {
        E result = null;
        double minDistance = Double.MAX_VALUE;
        for (E edge : iterableFor(face)) {
            double distance = GeometryUtils.distanceToLineSegment(getMutableEndPoint(getPrev(edge)), getMutableEndPoint(edge), x, y);
            if(distance < minDistance) {
                result = edge;
                minDistance = distance;
            }
        }
        return result;
    }

    default double getMinEdgeLen() {
        return stream().map(e -> toLine(e)).mapToDouble(l -> l.length()).min().orElse(0.0);
    }

    default double getMaxEdgeLen() {
        return stream().map(e -> toLine(e)).mapToDouble(l -> l.length()).max().orElse(0.0);
    }

    /**
     * Returns the half-edge which ends in v1 and starts in v2 if there is any, otherwise empty.
     *
     * @param v1 the end vertex
     * @param v2 the start vertex
     * @return the half-edge which ends in v1 and starts in v2 if there is any, empty otherwise
     */
    default Optional<E> getOf(@NotNull V v1, @NotNull V v2){
        for(E edge : iterableFor(v1)) {
            if(base().vertices().getTwin(edge).equals(v2)) {
                return Optional.of(edge);
            }
        }
        return Optional.empty();
    }

    default Optional<E> getOf(@NotNull final F face, @NotNull final VPoint v1, @NotNull final VPoint v2){
        var vertices = base().vertices();

        for(E edge : iterableFor(face)) {
            if(vertices.toMutablePoint(vertices.getTwin(edge)).equals(v1) && vertices.toMutablePoint(vertices.getEndOf(edge)).equals(v2)) {
                return Optional.of(edge);
            }
        }
        return Optional.empty();
    }

    /**
     * Transforms an edge into a immutable point {@link VPoint}.
     *
     * @param edge  the edge
     * @return an immutable point
     */
    default VPoint endToPoint(@NotNull E edge) {
        return base().vertices().toPoint(base().vertices().getEndOf(edge));
    }

    /**
     * Tests whether the half-edge is the longest edge of its two faces (excluding boundary faces).
     * This requires O(n + m) where n and m are the number of edges of the two neighbouring faces.
     *
     * @param edge  the half-edge
     * @return true if the half-edge is the longest edge of its two faces (excluding boundary faces), false otherwise
     */
    default boolean isLongestEdge(@NotNull final E edge) {
        var faces = base().faces();
        var edges = base().edges();

        if(!isAtBoundary(edge)) {
            E longestEdge1 = edges.streamEdgesOf(faces.getOf(edge)).reduce((e1, e2) -> toLine(e1).length() > toLine(e2).length() ? e1 : e2).get();
            E longestEdge2 = edges.streamEdgesOf(faces.getTwin(edge)).reduce((e1, e2) -> toLine(e1).length() > toLine(e2).length() ? e1 : e2).get();
            return isSameLineSegment(longestEdge1, edge) && isSameLineSegment(longestEdge2, edge);
        }
        else {
            E nonBoundaryEdge = edge;
            if(isBoundary(edge)) {
                nonBoundaryEdge = getTwin(edge);
            }

            E longestEdge = edges.streamEdgesOf(faces.getOf(nonBoundaryEdge)).reduce((e1, e2) -> toLine(e1).length() > toLine(e2).length() ? e1 : e2).get();
            return isSameLineSegment(longestEdge, edge);
        }
    }

    default boolean isNonAcute(@NotNull final E edge) {
        var edges = base().edges();

        VPoint p1 = edges.endToPoint(getPrev(edge));
        VPoint p2 = edges.endToPoint(edge);
        VPoint p3 = edges.endToPoint(getNext(edge));

        double angle1 = GeometryUtils.angle(p1, p2, p3);

        // non-acute triangle
        double rightAngle = Math.PI/2;
        return angle1 > rightAngle + GeometryUtils.DOUBLE_EPS;
    }
}
