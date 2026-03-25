package org.vadere.meshing.mesh.inter.meshConnectivity;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.inter.mesh.*;
import org.vadere.util.geometry.shapes.IPoint;

import java.util.*;
import java.util.function.Predicate;

/**
 * <p>
 * A poly-connectivity {@link IPolyConnectivity} is the connectivity of a mesh of non-intersecting connected polygons including holes.
 * So it is more abstract than a tri-connectivity {@link ITriConnectivity}. The mesh {@link IMesh} stores all the
 * date of the base elements, vertices {@link V}, half-edges {@link E} and faces {@link F}).
 * </p>
 *
 * @param <V> the type of the vertices
 * @param <E> the type of the half-edges
 * @param <F> the type of the faces
 *
 * @author Benedikt Zoennchen
 * @author Hayato Hess refactored from ITriConnectivity
 */
public interface IReadOnlyPolyConnectivity<V extends IVertex, E extends IHalfEdge, F extends IFace> {
    boolean isAtBoundary(@NotNull final E halfEdge);

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
    Optional<F> locateNonBoundaryByFullScan(final double x, final double y);

    Optional<F> locateNonBoundaryByFullScan(final double x, final double y, final Object caller);

    /**
     * <p>Searches and returns the face containing the point (x,y) in O(n),
     * where n is the number of faces of the mesh.</p>
     *
     * <p>Does not change the connectivity.</p>
     *
     * @param point the location point
     * @return the face containing the point or empty() if there is none
     */
    default Optional<F> locateNonBoundaryByFullScan(@NotNull final IPoint point) {
        return locateNonBoundaryByFullScan(point.getX(), point.getY());
    }

    default Optional<F> locateNonBoundaryByFullScan(@NotNull final IPoint point, final Object caller) {
        return locateNonBoundaryByFullScan(point.getX(), point.getY());
    }

    Optional<V> locateNonBoundaryPointByFullScan(final double x, final double y);

    /**
     * Returns a half-edge (begin, end) where end is its end point
     * and begin is the end point of its predecessor. This requires
     * O(d), where d is the degree of begin.
     *
     * Does not change the connectivity.
     *
     * @param begin the end point of the predecessor of the searched half-edge
     * @param end   the end point of the searched half-edge
     * @return a half-edge (begin, end) if there is any, otherwise empty()
     */
    Optional<E> findEdge(@NotNull final V begin, @NotNull final V end);

    /**
     * Tests if the half-edge is the only link / part of the full-edge
     * between the face of the half-edge and the face of its twin.
     * This requires O(n), where n is the number of edges of the face
     * of the edge.
     *
     * Does not change the connectivity.
     *
     * @param halfEdge a half-edge to test
     * @return true if the half-edge is a simple link, false otherwise
     */
    boolean isSimpleLink(@NotNull final E halfEdge);

    // TODO: improve performance by remembering faces
    /**
     * <p>A virus like working algorithm which searches for neighbouring faces by starting at the face until
     * the <tt>markCondition</tt> does no longer hold or the maximal dept is reached.
     * This requires in the worst case O(n), where n is the number of edges of all involved faces
     * (i.e. the face and the merged faces).</p>
     *
     * <p>Does not changes the connectivity.</p>
     *
     * @param face                      the face
     * @param markCondition             the mark condition.
     * @param maxDept                   the maximum dept / neighbouring distance at which faces can be marked / found
     *
     * @return the merge result i.e. the resulting face.
     */
    List<F> findFaces(
            @NotNull final F face,
            @NotNull final Predicate<F> markCondition,
            final int maxDept);

    /**
     * Tests whether the point (x, y) is contained in the face. This requires O(n), where
     * n is the number of edges of the face.
     *
     * Assumption: The ordering of the mesh is CCW and the face is a valid simple polyon.
     *
     * Does not change the connectivity.
     *
     * @param x     x-coordinate of the point
     * @param y     y-coordinate of the point
     * @param face  the face
     * @return true if the point (x, y) is contained in the face, false otherwise
     */
    boolean faceContains(final double x, final double y, @NotNull final F face);

    /**
     * Returns true if (x,y) is contained in the ccw triangle defined by (v1, v2, v3).
     *
     * Assumption: v1 -> v2 -> v3 is ccw oriented.
     *
     * @param x
     * @param y
     * @param v1
     * @param v2
     * @param v3
     * @return
     */
    boolean faceContains(final double x, final double y, @NotNull final V v1, @NotNull final V v2, @NotNull final V v3);

    /**
     * Returns true if the point (x1, y1) is part of the face in O(n),
     * where n is the number of edges of the face.
     *
     * Does not change the connectivity.
     *
     * @param face  the face
     * @param x1    the x-coordinate of the point
     * @param y1    the y-coordinate of the point
     * @return true if the (x1, y1) is part of the face, false otherwise
     */
    default boolean isMember(final double x1, final double y1, @NotNull final F face) {
        return getMemberEdge(face, x1, y1).isPresent();
    }

    /**
     * Returns true if the point (x1, y1) is close to a point of the face in O(n),
     * where n is the number of edges of the face.
     *
     * Does not change the connectivity.
     *
     * @param face      the face
     * @param x1        the x-coordinate of the point
     * @param y1        the y-coordinate of the point
     * @param distance  the maximal distance
     * @return true if the (x1, y1) close to a point of the face, false otherwise
     */
    default boolean isClose(final double x1, final double y1, @NotNull final F face, double distance) {
        return getCloseEdge(face, x1, y1, distance).isPresent();
    }

    /**
     * (Optional) returns the half-edge of a face which ends in (x1, y1) in O(n),
     * where n is the number of edges of the face.
     *
     * Does not change the connectivity.
     *
     * @param face  the face
     * @param x1    the x-coordinate of the point
     * @param y1    the y-coordinate of the point
     * @return (optional) the half-edge of a face which ends in (x1, y1)
     */
    Optional<E> getMemberEdge(@NotNull final F face, final double x1, final double y1);

    /**
     * (Optional) returns the half-edge of a face with an end-point close to (x1, y1) in O(n),
     * where n is the number of edges of the face.
     *
     * Does not change the connectivity.
     *
     * @param face      the face
     * @param x1        the x-coordinate of the point
     * @param y1        the y-coordinate of the point
     * @param distance  the maximal distance
     * @return (optional) the half-edge of a face which ends in (x1, y1)
     */
    Optional<E> getCloseEdge(@NotNull final F face, double x1, double y1, double distance);

    /**
     * Returns true if the point (x1, y1) is right of the half-edge in O(1). The half-edge is directed
     * and ends in its point.
     *
     * Does not change the connectivity.
     *
     * @param x1    the x-coordinate of the point
     * @param y1    the y-coordinate of the point
     * @param edge  the half-edge
     * @return true if the point (x1, y1) is right of the half-edge, false otherwise
     */
    boolean isRightOf(final double x1, final double y1, @NotNull final E edge);

    /**
     * Returns true if the point (x1, y1) is left of the half-edge in O(1). The half-edge is directed
     * and ends in its point.
     *
     * Does not change the connectivity.
     *
     * @param x1    the x-coordinate of the point
     * @param y1    the y-coordinate of the point
     * @param edge  the half-edge
     * @return true if the point (x1, y1) is left of the half-edge, false otherwise
     */
    boolean isLeftOfRobust(final double x1, final double y1, @NotNull final E edge);

    /**
     * Returns true if the point (x1, y1) is left of the half-edge in O(1). The half-edge is directed
     * and ends in its point.
     *
     * Does not change the connectivity.
     *
     * @param x1    the x-coordinate of the point
     * @param y1    the y-coordinate of the point
     * @param edge  the half-edge
     * @return true if the point (x1, y1) is left of the half-edge, false otherwise
     */
    boolean isLeftOf(final double x1, final double y1, @NotNull final E edge);

    /**
     * Tests if the line-segment defined by the half-edge intersects the line defined by p1 and p2 in O(1).
     *
     * Does not change the connectivity.
     *
     * @param p1    the first point of the undirected line
     * @param p2    the second point of the undirected line
     * @param edge  the half-edge defining the line-segment
     * @return true if the line-segment defined by the half-edge intersects the line (p1, p2)
     */
    boolean intersects(@NotNull final IPoint p1, @NotNull final IPoint p2, @NotNull final E edge);

    /**
     * Tests if the line-segment defined by the (v1,v2) intersects the line defined by p1 and p2 in O(1).
     *
     * Does not change the connectivity.
     *
     * @param p1 the first point of the undirected line
     * @param p2 the second point of the undirected line
     * @param v1 the first point of the line-segment
     * @param v2 the second point of the line-segment
     * @return true if the line-segment defined by (v1,v2) intersects the line (p1, p2)
     */
    boolean intersects(@NotNull final IPoint p1, @NotNull final IPoint p2, @NotNull final V v1, @NotNull final V v2);

    /**
     * Tests if the half-line-segment starting at p1 in the direction (p2-p1) intersects the line-segment defined by the half-edge in O(1).
     *
     * Does not change the connectivity.
     *
     * @param p1    the start point of the directed half-line-segment
     * @param p2    the second point of the directed half-line-segment of direction (p2-p1).
     * @param edge  the half-edge defining the line-segment
     * @return true if the half-line-segment starting at p1 in the direction (p2-p1) intersects the line-segment defined by the half-edge, false otherwise
     */
    boolean intersectsDirectional(@NotNull final IPoint p1, @NotNull final IPoint p2, E edge);

    /**
     * Tests whether the vertex has degree smaller or equals 2.
     * If an edge gets deleted and the vertex is simple connected
     * the vertex becomes isolated.
     *
     * Does not change the connectivity.
     *
     * @param vertex    the vertex
     * @return true if the vertex has degree smaller or equals 2, false otherwise.
     */
    boolean isSimpleConnected(@NotNull final V vertex);

    /**
     * Returns a half-edge such that it is part of face1 and the twin of this half-edge
     * is part of face2.
     *
     * Does not change the connectivity.
     *
     * @param face1 the first face
     * @param face2 the second face that might be a neighbour of face1
     * @return  the half-edge of face1 such that its twin is part of face2
     */
    Optional<E> findTwins(@NotNull final F face1, @NotNull final F face2);

    /**
     * Tests if there is any face which shares more than one edge with the face
     * we are checking. This requires O(n), where n is the number of edges of the face.
     *
     * Does not change the connectivity.
     *
     * @param face the face we are checking
     * @return true if there is no face which shares more than one edge with this face, false otherwise
     */
    boolean isSimpleConnected(@NotNull final F face);
}
