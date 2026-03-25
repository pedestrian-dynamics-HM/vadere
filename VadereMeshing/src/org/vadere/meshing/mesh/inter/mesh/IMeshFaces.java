package org.vadere.meshing.mesh.inter.mesh;

import com.google.common.collect.Lists;
import com.google.common.collect.Streams;
import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.iterators.AdjacentFaceIterator;
import org.vadere.meshing.mesh.iterators.EdgeIterator;
import org.vadere.meshing.mesh.iterators.SurroundingFaceIterator;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VPolygon;
import org.vadere.util.geometry.shapes.VTriangle;

import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public interface IMeshFaces<V extends IVertex, E extends IHalfEdge, F extends IFace> extends Iterable<F> {
    IMesh<V, E, F> base();

    /**
     * Returns the number of alive interior faces in O(1), i.e. holes and the border are excluded.
     *
     * @return the number of alive faces
     */
    int count();

    /**
     * Returns the number of alive holes in O(1).
     *
     * @return the number of alive faces
     */
    int getNumberOfHoles();

    /**
     * Returns an arbitrary face of the mesh in O(1).
     *
     * @return an arbitrary face of the mesh
     */
    F getFirst();

    /**
     * Returns the face of the half-edge {@link E} in O(1).
     *
     * @param halfEdge the half-edge
     * @return the face of the half-edge {@link E}.
     */
    F getOf(@NotNull E halfEdge);
    /**
     * Returns a neighbouring face of the vertex in O(1).
     *
     * @param vertex the vertex
     * @return a neighbouring face of the vertex
     */
    default F getOf(@NotNull V vertex) {
        return getOf(base().edges().getOf(vertex));
    }

    /**
     * Returns the face of the twin of the half-edge, i.e. its twin face in O(1).
     *
     * @param halfEdge the half-edge
     * @return the face of the twin of the half-edge
     */
    default F getTwin(@NotNull E halfEdge) {
        return getOf(base().edges().getTwin(halfEdge));
    }

    /**
     * Returns true if the face is the boundary in O(1).
     *
     * @param face the face
     * @return true if the face is the boundary, false otherwise
     */
    boolean isBoundary(@NotNull F face);

    /**
     * Returns the face which is not a boundary, i.e. no hole and no border
     * of the faces of a full-edge which the half-edge is part of in O(1).
     *
     * @param halfEdge the half-edge
     * @return the face which is not a boundary and the face of the half-edge or its twin
     */
    default F getNonBoundaryFace(@NotNull E halfEdge) {
        if(!base().edges().isBoundary(halfEdge)) {
            return getOf(halfEdge);
        }
        else {
            return getOf(base().edges().getTwin(halfEdge));
        }
    }

    /**
     * Returns the outer border of the mesh in O(1).
     *
     * @return the outer border of the mesh
     */
    F getOuterBorder();

    /**
     * Returns true if the face is the border in O(1) (there is only one border
     * and the border is also a boundary).
     *
     * @param face the face
     * @return true if the face is the border, false otherwise
     */
    default boolean isOuterBorder(@NotNull F face) {
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
     * Returns true if this face is completely surrounded by the same boundary face, i.e. a hole
     * or the border.
     *
     * @param face  the face
     * @return true if this face is completely surrounded by the same boundary face
     */
    default boolean isSeparated(@NotNull final F face) {
        F neighbouringFace = getTwin(base().edges().getAnyOf(face));
        if(isBoundary(neighbouringFace)) {
            return false;
        }
        return base().edges().streamEdgesOf(face).map(this::getTwin).allMatch(f -> f.equals(neighbouringFace));
    }

    /**
     * Returns an arbitrary face which is neighbouring the vertex and which is
     * not a boundary, i.e. no hole and no border in O(1).
     *
     * @param vertex the vertex
     * @return an arbitrary non-boundary face which is neighbouring the vertex
     */
    default F getNonBoundaryFace(@NotNull V vertex) {
        return getNonBoundaryFace(base().edges().getOf(vertex));
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
        return base().edges().streamEdgesOf(face).anyMatch(e -> base().edges().isAtBorder(e));
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
        return base().edges().streamEdgesOf(face).anyMatch(e -> base().edges().isAtBoundary(e));
    }

    /**
     * Returns true if one of the neighbouring faces of the face is the border.
     *
     * @param face the face
     * @return true if one of the neighbouring faces is the border, false otherwise
     */
    default boolean isNeighbourBorder(@NotNull final F face){
        for(F neighbourFace : surroundingIterableFor(face)) {
            if(isOuterBorder(neighbourFace)) {
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
        for(F neighbourFace : surroundingIterableFor(face)) {
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
        for(F neighbourFace : surroundingIterableFor(face)) {
            if(isHole(neighbourFace)) {
                return true;
            }
        }
        return false;
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
     * Returns true if the face is already destroyed i.e. not part of the geometric representation.
     *
     * @param face the face
     * @return true if the face is already destroyed, false otherwise
     */
    boolean isDestroyed(@NotNull final F face);

    /**
     * Returns a list {@link List} of all non-boundary and not already destroyed faces.
     * Therefore the list does not contain holes or the border.
     *
     * @return a list {@link List} of all non-boundary and not already destroyed faces.
     */
    default List<F> getAll() {
        return stream().filter(face -> !isBoundary(face)).filter(face -> isAlive(face)).collect(Collectors.toList());
    }

    default List<F> getBoundaryAndHoles() {
        return Stream.concat(streamHoles(), Stream.of(getOuterBorder())).collect(Collectors.toList());
    }

    default List<F> getAllWithBoundary() {
        return streamFacesWithBoundary().filter(face -> isAlive(face)).collect(Collectors.toList());
    }

    default Stream<F> streamFacesWithBoundary() {
        return Streams.concat(streamBoundaries(), stream());
    }

    default Stream<F> streamBoundaries() {
        return Streams.concat(streamHoles(), Stream.of(getOuterBorder()));
    }

    /**
     * Returns a list {@link List} of all faces of the mesh data structure with the exception of the border.
     * This requires O(n) where n is the number of faces.
     *
     * @return a list {@link List} of all faces of the mesh data structure with the exception of the border.
     */
    default List<F> getFacesWithHoles() {
        return stream().filter(face -> isAlive(face)).collect(Collectors.toList());
    }

    /**
     * Returns a filtered stream {@link Stream} of (all alive) interior faces such that each face fulfill the predicate.
     *
     * @param predicate the predicate
     * @return a filtered stream {@link Stream} of (all alive) interior faces
     */
    Stream<F> stream(@NotNull final Predicate<F> predicate);

    /**
     * Returns a parallel stream {@link Stream} of (all alive) interior faces. Note that the required synchronization has to
     * be done by the user.
     *
     * @return a parallel stream {@link Stream} of (all alive) interior faces
     */
    default Stream<F> streamFacesParallel() {
        return stream(f -> true).parallel();
    }

    /**
     * Returns a stream {@link Stream} of (all alive) interior faces. Note that the required synchronization has to
     * be done by the user.
     *
     * @return a parallel stream {@link Stream} of (all alive) interior faces
     */
    default Stream<F> stream() {
        return stream(f -> !isBoundary(f));
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
     * This method is for logging information. It returns a string of the path defining the polygon of a face.
     *
     * @param face  the face
     * @return a string of the path defining the polygon of a face
     */
    default String toPathString(@NotNull final F face) {
        return base().vertices().
                streamPoints(face).map(Object::toString)
                .reduce((s1, s2) -> s1 + " -> " + s2).orElse("");
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
        E edge = base().edges().getAnyOf(face);
        E prev = base().edges().getPrev(edge);

        V endOfPrev = base().vertices().getEndOf(prev);
        V endOfEdge = base().vertices().getEndOf(edge);
        path2D.moveTo(endOfPrev.getX(), endOfPrev.getY());
        path2D.lineTo(endOfEdge.getX(), endOfEdge.getY());

        while (!edge.equals(prev)) {
            edge = base().edges().getNext(edge);
            V p = base().vertices().getEndOf(edge);
            path2D.lineTo(p.getX(), p.getY());
        }

        //path2D.closePath();

        return new VPolygon(path2D);
    }

    /**
     * Returns a list {@link List} of faces which are adjacent to the vertex of this edge.
     *
     * @param edge the edge holding the vertex
     * @return a list {@link List} of faces which are adjacent to the vertex of this edge
     */
    default List<F> getAdjacentOf(@NotNull E edge) {
        return Lists.newArrayList(new AdjacentFaceIterator(base(), edge));
    }

    /**
     * Returns a list {@link List} of faces which are adjacent to the vertex.
     *
     * @param vertex the vertex
     * @return a list {@link List} of faces which are adjacent to the vertex of this edge
     */
    default List<F> getAdjacentOf(@NotNull V vertex) {
        return Lists.newArrayList(new AdjacentFaceIterator(base(), base().edges().getOf(vertex)));
    }

    /**
     * Returns an Iterable {@link Iterable} which can be used to iterate over surrounding faces of the face.
     *
     * @param face the face the iterable iterates over
     * @return an Iterable {@link Iterable} which can be used to iterate over all surrounding faces
     */
    default Iterable<F> surroundingIterableFor(@NotNull final F face) { return () -> new SurroundingFaceIterator<>(base(), face);}

    /**
     * Returns an Iterable {@link Iterable} which can be used to iterate over surrounding faces of the vertex.
     *
     * @param vertex the vertex the iterable iterates over
     * @return an Iterable {@link Iterable} which can be used to iterate over all surrounding faces
     */
    default Iterable<F> adjacentIterableFor(@NotNull final V vertex) { return () -> new AdjacentFaceIterator(base(), base().edges().getOf(vertex));}

    default List<F> getSurroundingOf(@NotNull final F face) { return Lists.newArrayList(new SurroundingFaceIterator<>(base(), face)); }

    /**
     * Returns a Stream {@link Stream} consisting of all surrounding faces of the face.
     *
     * @param face the face of which surrounding faces the stream consist.
     * @return a Stream {@link Stream} consisting of all surrounding faces of the face
     */
    default Stream<F> streamOf(@NotNull final F face) {
        Iterable<F> iterable = surroundingIterableFor(face);
        return StreamSupport.stream(iterable.spliterator(), false);
    }

    /**
     * Returns a Stream {@link Stream} consisting of all surrounding faces of the vertex.
     *
     * @param vertex the face of which surrounding faces the stream consist.
     * @return a Stream {@link Stream} consisting of all surrounding faces of the vertex
     */
    default Stream<F> streamOf(@NotNull final V vertex) {
        Iterable<F> iterable = adjacentIterableFor(vertex);
        return StreamSupport.stream(iterable.spliterator(), false);
    }

    /**
     * Returns an Iterable {@link Iterable} which can be used to iterate over all faces which are adjacent to the vertex of the edge
     *
     * @param edge the edge of which adjacent faces
     * @return an Iterable {@link Iterable} which can be used to iterate over all faces which are adjacent to the vertex of the edge
     */
    default Iterable<F> getAdjacentFacesIt(@NotNull final E edge) { return () -> new AdjacentFaceIterator<>(base(), edge); }

    /**
     * Returns an Iterable {@link Iterable} which can be used to iterate over all faces which are adjacent to the vertex.
     *
     * @param vertex the vertex
     * @return an Iterable {@link Iterable} which can be used to iterate over all faces which are adjacent to the vertex
     */
    default Iterable<F> getAdjacentFacesIt(@NotNull final V vertex) { return () -> new AdjacentFaceIterator<>(base(), base().edges().getOf(vertex)); }

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
        return base().edges().getClose(face, x, y, distance).isPresent();
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
        return base().edges().getMember(face, x, y).isPresent();
    }

    /**
     * Returns a immutable triangle {@link VTriangle} by transforming the face to a triangle.
     * The face needs to be a triangle or an exception is thrown. This requires O(1) time.
     *
     * @param face the face.
     * @return a immutable triangle {@link VTriangle} representing the face
     */
    default VTriangle toTriangle(@NotNull final F face) {
        List<V> vertices = base().vertices().getAllOf(face); // TODO speed up by avoiding the creation of a list!
        if(vertices.size() != 3){
            throw new IllegalArgumentException("face " + face + " does not represent a triangle");
        }
        return new VTriangle(new VPoint(vertices.get(0)), new VPoint(vertices.get(1)), new VPoint(vertices.get(2)));
    }

    /**
     * Returns a list {@link List} of all points of a face in O(k), where k is the number of points
     * of the face.
     *
     * @param face the face
     * @return a list {@link List} of all points of a face.
     */
    default List<IPoint> getPoints(@NotNull final F face) {
        EdgeIterator<V, E, F> edgeIterator = new EdgeIterator<>(base(), face);

        List<IPoint> points = new ArrayList<>();
        IMeshEdges<V, E, F> edges = base().edges();
        while (edgeIterator.hasNext()) {
            points.add(edges.getMutableEndPoint(edgeIterator.next()));
        }

        return points;
    }
}
