package org.vadere.meshing.mesh.inter.meshConnectivity;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vadere.meshing.mesh.inter.IVertexContainerDouble;
import org.vadere.meshing.mesh.inter.mesh.*;
import org.vadere.meshing.mesh.inter.mesh.data.IMeshDataStorage;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.logging.Logger;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * <p>A tri-connectivity {@link ITriConnectivity} is the connectivity of a mesh of non-intersecting connected triangles including holes.
 * A hole can be an arbitrary simple polygon. So it is more concrete than a poly-connectivity {@link IPolyConnectivity}.
 * The mesh {@link IMesh} stores all the date of the base elements (vertices {@link V}, half-edges {@link E}
 * and faces {@link F}) and offers factory method to create new base elements.</p>
 *
 * <p>We say a mesh represents a valid triangulation or a triangulation is valid if and only if all triangle-faces are counter-clockwise oriented.</p>
 *
 * <p>We say a mesh represents a feasible triangulation or a triangulation is feasible if and only if all triangle-faces are legal, i.e. all half-edges are legal.
 * The certificate for an edge to be legal relies on the concrete implementation of the triangulation. E. g. for a strict Delaunay-Triangulation the Delaunay-Criterion
 * has to be fulfilled.</p>
 *
 * @param <V> the type of the vertices
 * @param <E> the type of the half-edges
 * @param <F> the type of the faces
 *
 * @author Benedikt Zoennchen
 * @author Hayato Hess refactored from ITriConnectivity
 */
public interface IReadOnlyTriConnectivity<V extends IVertex, E extends IHalfEdge, F extends IFace> extends IReadOnlyPolyConnectivity<V, E, F>{
    /**
     * A logger for debug and information reasons.
     */
    Logger log = Logger.getLogger(IReadOnlyTriConnectivity.class);

    /**
     * A Random number generator to randomly walk through the trinagulation.
     */
    Random random = new Random();

    /**
     * <p>Returns the dimension of a triConnectivity.</p>
     *
     * @return the dimension of a triConnectivity
     */
    int getDimension();

    /**
     * <p>Searches and returns (optional) the face containing the point (x,y) in O(n),
     * where n is the number of faces of the mesh by starting at a specific face.
     * The search uses a robust straight walk such that it can walk through holes, i.e.
     * polygons. If this face is close to (x, y) the search will be fast.</p>
     *
     * <p>Does not change the connectivity.</p>
     *
     * @param point     the point (x, y)
     * @param startFace the face at which the search starts
     * @return the face containing the point or empty() if there is none
     */
    default Optional<F> locateMarch(@NotNull final IPoint point, F startFace) {
        return locateMarch(point.getX(), point.getY(), startFace);
    }

    /**
     * <p>Searches and returns the face containing the point (x,y) in O(n),
     * where n is the number of faces of the mesh by starting at a specific face.
     * The search uses a robust straight walk such that it can walk through holes, i.e.
     * polygons. If this face is close to (x, y) the search will be fast.</p>
     *
     * Assumption: the start-face is contained in the mesh structure.
     *
     * <p>Does not change the connectivity.</p>
     *
     * @param x         x-coordinate of the location point
     * @param y         y-coordinate of the location point
     * @param startFace the face at which the search starts
     * @return the face containing the point or empty() if there is none
     */
    Optional<F> locateMarch(final double x, final double y, @NotNull final F startFace);

    Optional<F> locate(final double x, final double y);


    /**
     * <p>Marching to the face which contains the point defined by (x1, y1) starting inside the startFace.
     * This algorithm also works if there are convex polygon (holes) inside the triangulation.</p>
     *
     * <p>Assumption: (x1, y1) is contained in some face.</p>
     *
     * <p>Does not change the connectivity.</p>
     *
     * @param x1        the x-coordinate of the ending point
     * @param y1        the y-coordinate of the ending point
     * @param startFace the face where the march start containing (x1,y1).
     * @return returns the face containing (x1, y1)
     */
    default F straightWalk2D(final double x1, final double y1, @NotNull final F startFace) {
        return straightWalk2D(x1, y1, startFace, e -> !isRightOf(x1, y1, e));
    }

    /**
     * <p>Marches to the face which contains the point defined by (x1, y1) starting inside the <tt>startFace</tt>.
     * Furthermore this method will gather all visited edges and requires O(n) time. However, if the face is close
     * the amount of time required is small. This algorithm also works if there are convex polygon (holes)
     * inside the triangulation.</p>
     *
     * <p>Assumption: (x1, y1) is contained in some face.</p>
     *
     * <p>Does not change the connectivity.</p>
     *
     * @param x1        the x-coordinate of the point at which the march will start
     * @param y1        the y-coordinate of the point at which the march will start
     * @param startFace the face where the march start containing (x1,y1).
     * @return returns all visited edges in a first visited first in ordered queue, i.e. <tt>LinkedList</tt>.
     */
    default LinkedList<E> straightGatherWalk2D(final double x1, final double y1, @NotNull final F startFace) {
        return straightGatherWalk2D(x1, y1, startFace, e -> !isRightOf(x1, y1, e));
    }

    LinkedList<E> getIntersectingEdges(@NotNull final V vStart, @NotNull final V vEnd);

    /**
     * Marches from the midpoint of a face i.e. <tt>startFace</tt> in the direction (<tt>direction</tt>) until
     * the stop-condition (<tt>stopCondition</tt>) is fulfilled. This requires O(n) worst case time, where n
     * is the number of faces of the mesh.
     *
     * <p>Assumption: The stopCondition will be fulfilled at some point.</p>
     *
     * @param face                      the face at which the march / search starts
     * @param direction                 the direction in which the march will go
     * @param additionalStopCondition   the stop condition at which the march will stop
     * @return all visited faces in a first visited first in ordered queue, i.e. <tt>LinkedList</tt>.
     */
    LinkedList<E> straightWalk2DGatherDirectional(@NotNull final F face, @NotNull final VPoint direction, @NotNull final Predicate<E> additionalStopCondition);

    F straightWalk2D(final double x1, final double y1, @NotNull final F startFace, @NotNull final Predicate<E> stopCondition);

    /**
     * <p>Marches / walks along the line defined by q and p from q to p starting inside the startFace.
     * Furthermore this method will gather all visited faces and requires O(n) time. However, if the face is close
     * the amount of time required is small. This algorithm also works if there are convex polygon (holes)
     * inside the triangulation. A stop condition like (e to !isRightOf(x1, y1, e)) stops the walk if (x1, y1),
     * will stop the walk if the point p = (x1, y1) is contained in the face.</p>
     *
     * Assumption:asd
     * <ol>
     *     <li>q is contained in the start face</li>
     *     <li>the stop condition will be fulfilled at some point</li>
     * </ol>
     *
     * <p>Does not change the connectivity.</p>
     *
     * @param x1            the x-coordinate of the point at which the march will start
     * @param y1            the y-coordinate of the point at which the march will start
     * @param startFace     the face where the march start containing (x1,y1).
     * @param stopCondition the stopCondition at which the march will stop.
     * @return all visited faces in a first visited first in ordered queue, i.e. <tt>LinkedList</tt>.
     */
    LinkedList<E> straightGatherWalk2D(final double x1, final double y1, @NotNull final F startFace, @NotNull final Predicate<E> stopCondition);

    /**
     * <p>Straight walk in the 1D-case, i.e. the mesh consists only one interior-face.</p>
     *
     * <p>Does not change the connectivity.</p>
     *
     * @param x         the x-coordinate of the point
     * @param y         the y-coordinate of the point
     * @param startFace the startFace face of the search
     * @return (optional) the face containing the point or empty() if there is none
     */
    Optional<F> marchLocate1D(final double x, final double y, @NotNull final F startFace);

    /**
     * <p>Tests if the point is contained in the 1-Ring of the vertex, i.e. the polygon spanned by the
     * neighbour points of the point including itself if the point is at the boundary.</p>
     *
     * @param vertex    the vertex
     * @param point     the point
     * @return true if the point is contained, false otherwise
     */
    boolean ringContainsPoint(@NotNull final V vertex, @NotNull final IPoint point);

    /**
     * <p>Returns true if the full-edge of this half-edge is the longest edge of its faces.</p>
     *
     * @param edge the half-edge
     * @return true if the full-edge of this half-edge is the longest edge of its faces
     */
    boolean isLongestEdge(@NotNull final E edge);

    E getLongestHalfEdge(@NotNull final F face);

    boolean isLongestHalfEdge(@NotNull final E edge);

    boolean isShortestHalfEdge(@NotNull final E edge);

    Set<V> getVertices(@NotNull final double x, final double y, final F startFace, @NotNull final Predicate<V> predicate);

    /**
     * <p>Helper method which returns an arbitrary edge of a pair of edges.
     * It returns the left if it is not null otherwise the right.</p>
     *
     * <p>Does not change the connectivity.</p>
     *
     * @param pair a pair of half-edges
     * @return an arbitrary edge of a pair of edges
     */
    E getAnyEdge(@NotNull final Pair<E, E> pair);

    /**
     * <p>Tests if the face is counter-clockwise oriented in O(1) time. If a triangulation is valid
     * all triangle-faces are counter-clockwise oriented.</p>
     *
     * <p>Assumption: The face is a triangle!</p>
     *
     * @param triangleFace the face representing a triangle
     * @return true if the face (triangle) is counter-clockwise oriented, false otherwise
     */
    boolean isCCW(@NotNull final F triangleFace);

    /**
     * <p>Walks along the line defined by q and p. The walking direction should be controlled by the stopCondition e.g.
     * (e to !isRightOf(x1, y1, e)) stops the walk if (x1, y1) is on the left side of each edge which is the case if the
     * point is inside the reached face. The walk starts at the startFace and continues in the direction of line defined
     * by q and p using the any edge which does not fulfill the stopCondition.</p>
     *
     * <p>Does not change the connectivity.</p>
     *
     * @param q             start of the oriented-line
     * @param p             end of the oriented-line
     * @param startFace     at this face the walk starts
     * @param stopCondition fulfilling the stopCondition will stop the walk
     * @return the face containing p
     */
    F straightWalk2D(final VPoint q, final VPoint p, final F startFace, final Predicate<E> stopCondition);

    /**
     * <p>This method is called whenever the special case appear during a 2D-walk which is the following:
     * There is at least one point v of the face which lies on the line (q,p) and p is not contained in the face.</p>
     *
     * In this case we go around all the neighbouring faces of v searching for an intersection edge which is not equals to in-edge.
     * It might be the case that the line (q,p) goes through two points of a neighbouring face. Therefore, we test first for
     * intersection of (p1,p3) where p1,p2,p3 are consecutive points of the ring around the v. If (q,p) intersects (p1,p3) it has to
     * intersect (p1,p2) or (p2,p3), otherwise (q,p) goes through p2. If so the method returns (p1,p2) or (p2,p3). This is more expensive
     * than the general case i.e. O(d) where d is the degree of v. However this should not happen often in a general triangulation and
     * can only occur more often in very degenerated triangulations.
     *
     * @param inEdge    the edge at which we enter the face i.e. this edge intersects (q,p)
     * @param q         the first point of the directed line (q,p)
     * @param p         the second point of the directed line (q,p)
     * @return the next in-edge intersecting (q,p) which is unequal to <tt>inEdge</tt> for the special case
     */
    E straightWalkSpecialCase(@NotNull final E inEdge,
                                      @NotNull final VPoint q,
                                      @NotNull final VPoint p);

    /**
     * Returns the vertex of the face of the <tt>inEdge</tt> which is closest to the line segment
     * (q, p).
     *
     * @param inEdge
     * @param q
     * @param p
     * @return
     */
    V getSpecialVertex(@NotNull final E inEdge,
                               @NotNull final VPoint q,
                               @NotNull final VPoint p);

    /**
     * This method returns the edge of a face (defined by its half-edge inEdge) which
     * 1) intersects the line (q,p) and
     * 2) its intersection point is the closest one with respect to p
     *
     * This is computational expensive because one iterates over all edges of the face (potentially a hole or the border).
     * But it is a robust method to walk through a non-convex hole or border!
     *
     * @param inEdge
     * @param q
     * @param p
     * @param stopCondition
     * @return
     */
    E rayCastingPolygon(@NotNull final E inEdge,
                                @NotNull final VPoint q,
                                @NotNull final VPoint p,
                                @NotNull final Predicate<E> stopCondition);

    E walkAroundBoundaryStraight(@NotNull final E inEdge,
                                         @NotNull final VPoint q,
                                         @NotNull final VPoint p,
                                         @NotNull final Predicate<E> stopCondition);

    /**
     * Walks one step i.e. from a face to immediate / neighbouring next face along the line defined by q and p
     * from q to p. This is done be walking from an in-edge through the face to the out-edge. Both the in-edge and
     * the out-edge intersects the line (q,p). Furthermore this method will gather the visited edges by placing them
     * into the list of visited edges. There are different cases with special cases which make the code complicated:
     * <ol>
     *     <li>general case (1):    the line (q,p) intersects two half-edges.
     *                              In this case the algorithm walks across the correct line by the definition of the direction (i.e. towards p)</li>
     *     <li>special case (2.1):  the line (q,p) goes through a point of the face of the in-edge and therefore the is no out-line intersecting (q,p)
     *                              but the face contains p. In this case the walk is finished.</li>
     *     <li>special case (2.2):  the line (q,p) goes through a point v of the face of the in-edge and therefore the is no out-line intersecting (q,p)
     * 	                            but the face does not contain p. In this case we go around all the neighbouring faces of v searching for an intersection
     * 	                            edge which is not equals to in-edge see {@see ITriangulation#straightWalkSpecialCase}. This is more expensive than the general case i.e. O(d) where d is the degree of v.
     * 	                            However this should not happen often in a general triangulation and can only accure more often in very degenerated triangulations.</li>
     * </ol>
     *
     * <p>Assumption: inEdge intersects (q,p) and it is not the next out-edge and the stop-condition makes sense.</p>
     *
     * <p>Does not change the connectivity.</p>
     *
     * @param q             start point of the march / walk
     * @param p             end point of the march / walk
     * @param inEdge        start face of the walk
     * @param stopCondition stop condition of the walk, i.e. the walk stops if the condition is no longer fulfilled
     * @param visitedEdges  a list which will be filled with the visited faces in order in which they are visited (first visited = first in)
     * @return all visited edges in a first visited first in ordered queue, i.e. {@link LinkedList}.
     */
    Optional<E> straightWalkNext(
            @NotNull final E inEdge,
            @NotNull final VPoint q,
            @NotNull final VPoint p,
            @NotNull final Predicate<E> stopCondition,
            @Nullable final LinkedList<E> visitedEdges);


    /**
     * <p>Marches / walks along the line defined by q and p from q to p starting inside the startFace.
     * Furthermore this method will gather all visited edges and requires O(n) time. However, if the face is close
     * the amount of time required is small. This algorithm also works if there are convex polygon (holes)
     * inside the triangulation. A stop condition like (e to !isRightOf(x1, y1, e)) stops the walk if (x1, y1),
     * will stop the walk if the point p = (x1, y1) is contained in the face.
     * The method goes from one face to the next by calling {@link IReadOnlyTriConnectivity#straightWalkNext(E, VPoint, VPoint, Predicate, LinkedList)}
     * but adds the resulting edge to the list of visited edges and adds some logging to debug the walks / marches.</p>
     *
     * <p>Assumption: q is contained in the start face.</p>
     *
     * <p>Does not change the connectivity.</p>
     *
     * @param q             start point of the march / walk
     * @param p             end point of the march / walk
     * @param startFace     start face of the walk
     * @param stopCondition stop condition of the walk, i.e. the walk stops if the condition is no longer fulfilled
     * @return all visited edges in a first visited first in ordered queue, i.e. <tt>LinkedList</tt>.
     */
    default LinkedList<E> straightGatherWalk2D(final VPoint q, final VPoint p, final F startFace, final Predicate<E> stopCondition) {
        return straightGatherWalk2D(q, p, startFace, stopCondition, false, false);
    }

    /**
     * <p>Marches / walks from q (which is contained in <tt>startFace</tt>) in direction <tt>direction</tt>.
     * Furthermore this method will gather all visited edges and requires O(n) time where n is the number of
     * visited edges and can be as large as the number of triangles. However, if the face is close
     * the amount of time required is small. This algorithm also works if there are convex polygon (holes)
     * inside the triangulation. A stop condition like (e to !isRightOf(p.x, p.y, e)) stops the walk if
     * the point p = (x, y) is contained in the face. The method goes from one face to the next by calling
     * {@link IReadOnlyTriConnectivity#straightWalkNext(E, VPoint, VPoint, Predicate, LinkedList)}.</p>
     *
     * <p>Assumption: q is contained in the start face.</p>
     *
     * <p>Does not change the connectivity.</p>
     *
     * @param q             start point of the march / walk
     * @param direction     direction of the walk
     * @param startFace     start face of the walk
     * @param stopCondition stop condition of the walk, i.e. the walk stops if the condition is no longer fulfilled
     * @return all visited edges in a first visited first in ordered queue, i.e. <tt>LinkedList</tt>.
     */
    default LinkedList<E> straightGatherWalk2DDirectional(final VPoint q, final VPoint direction, final F startFace, final Predicate<E> stopCondition) {
        return straightGatherWalk2D(q, direction, startFace, stopCondition, true, false);
    }

    /**
     * <p>Marches / walks along the line defined by q and p from q to p starting inside the startFace, i.e. q has to be
     * contained in the <tt>startFace</tt>. Furthermore, this method will gather all visited edges and requires O(n) time
     * where n is the number of visited edges which can be as large as the number of triangles. However, if the face is close
     * the amount of time required is small. This algorithm also works if there are convex polygon (holes)
     * inside the triangulation. A stop condition like (e to !isRightOf(p.x, p.y, e)) stops the walk if the point p = (x, y)
     * is contained in the face. The method goes from one face to the next by calling
     * {@link IReadOnlyTriConnectivity#straightWalkNext(E, VPoint, VPoint, Predicate, LinkedList)}.</p>
     *
     * <p>Assumption: q is contained in the start face.</p>
     *
     * <p>Does not change the connectivity.</p>
     *
     * @param q             start point of the march / walk
     * @param pDirection    the point we are walking to or the direction towards we are walking to which is decided by <tt>directional</tt>
     * @param startFace     start face of the walk
     * @param stopCondition stop condition of the walk, i.e. the walk stops if the condition is no longer fulfilled
     * @param directional   if true we walk in the direction of <tt>pDirection</tt> otherwise we walk to the face containing <tt>pDirection</tt>
     * @param gather
     * @return all visited edges in a first visited first in ordered queue, i.e. <tt>LinkedList</tt>.
     */
    LinkedList<E> straightGatherWalk2D(
            @NotNull final VPoint q,
            @NotNull final VPoint pDirection,
            @NotNull final F startFace,
            @NotNull final Predicate<E> stopCondition,
            final boolean directional,
            final boolean gather);

    /**
     * <p>Marches / walks to the face which contains the point defined by (x1, y1) starting the walk
     * inside the start face (<tt>startFace</tt>). The algorithm is an implementation of the
     * Probabilistic / Random walk which is faster in practice, see Walking in a Triangulation by devillers-2001.
     * This algorithm does NOT works if there are convex polygon (holes) inside the triangulation.</p>
     *
     * Assumption:
     * <ol>
     *     <li>(x1, y1) is contained in some face / triangle</li>
     *     <li>the mesh / triangulation does not contain holes</li>
     * </ol>
     *
     *
     * <p>Does not change the connectivity.</p>
     *
     * @param x1        the x-coordinate of the ending point
     * @param y1        the y-coordinate of the ending point
     * @param startFace the face where the march start containing (x1,y1).
     * @return the face containing the point (x1, y1)
     */
    F marchRandom2D(final double x1, final double y1, @NotNull final F startFace);

    boolean faceContains(final double x, final double y, @NotNull final F face);

    /**
     * <p>Tests if the point p = (x1, y1) is inside the circumscribed cycle defined by the triangle of the face.</p>
     *
     * <p>Does not change the connectivity.</p>
     *
     * @param face  the face
     * @param x1    x-coordinate of the point
     * @param y1    y-coordinate of the point
     * @return true if the point p = (x1, y1) is inside the circumscribed cycle defined by the triangle of the face, false otherwise.
     */
    boolean isInsideCircumscribedCycle(@NotNull final F face, final double x1, final double y1);

    /**
     * Returns the closest half-edge (with respect to (x,y)) of a face containing (x,y) if there is any face that contains p, otherwise empty().
     * Three cases are possible:
     * <ol>
     *     <li>p is in the interior of the face</li>
     *     <li>p lies on the edge which will be returned</li>
     *     <li>p is a vertex of the mesh</li>
     * </ol>
     *
     * <p>Does not change the connectivity.</p>
     *
     * @param x x-coordinate of the point p
     * @param y y-coordinate of the point p
     * @return the closest half-edge of a face containing p = (x,y) if there is any face that contains p, otherwise empty().
     */
    Optional<E> getClosestEdge(final double x, final double y);

    /**
     * Returns the closest half-edge (with respect to (x,y)) of a face containing (x,y) if there is any face that contains p, otherwise empty().
     * Three cases are possible:
     * <ol>
     *     <li>p is in the interior of the face</li>
     *     <li>p lies on the edge which will be returned</li>
     *     <li>p is a vertex of the mesh</li>
     * </ol>
     *
     * <p>Does not change the connectivity.</p>
     *
     * @param x         x-coordinate of the point p
     * @param y         y-coordinate of the point p
     * @param startFace the face from which the walk / march / search will start
     * @return the closest half-edge of a face containing p = (x,y) if there is any face that contains p, otherwise empty().
     */
    Optional<E> getClosestEdge(final double x, final double y, final F startFace);

    /**
     * Returns the closest vertex (with respect to (x,y)) of a face containing (x,y) if there is any face that contains p, otherwise empty().
     * Three cases are possible:
     * <ol>
     *     <li>p is in the interior of the face</li>
     *     <li>p lies on the edge which will be returned</li>
     *     <li>p is a vertex of the mesh</li>
     * </ol>
     *
     * <p>Does not change the connectivity.</p>
     *
     * @param x         x-coordinate of the point p
     * @param y         y-coordinate of the point p
     * @return the closest half-edge of a face containing p = (x,y) if there is any face that contains p, otherwise empty().
     */
    Optional<V> getClosestVertex(final double x, final double y);


    /**
     * Returns the closest vertex (with respect to (x,y)) of a face containing (x,y) if there is any face that contains p, otherwise empty().
     * Three cases are possible:
     * <ol>
     *     <li>p is in the interior of the face</li>
     *     <li>p lies on the edge which will be returned</li>
     *     <li>p is a vertex of the mesh</li>
     * </ol>
     *
     * <p>Does not change the connectivity.</p>
     *
     * @param x         x-coordinate of the point p
     * @param y         y-coordinate of the point p
     * @param startFace the face from which the walk / march / search will start
     * @return the closest half-edge of a face containing p = (x,y) if there is any face that contains p, otherwise empty().
     */
    Optional<V> getClosestVertex(final double x, final double y, final F startFace);

    /**
     * <p>Returns true if and only if the mesh of this ITriConnectivity is a valid Triangulation.</p>
     *
     * <p>Does not change the connectivity.</p>
     *
     * @return true if this the mesh of this ITriConnectivity is a valid Triangulation, false otherwise
     */
    boolean isValid();

    /**
     * <p>Returns true if and only if the face is a valid triangle.</p>
     *
     * <p>Does not change the connectivity.</p>
     *
     * @param face the face which will be tested
     *
     * @return true if this the mesh of the face is a valid triangle, false otherwise
     */
    boolean isValid(@NotNull final F face);

    IPoint[] getPoints(@NotNull final E edge);

    IPoint[] getPoints(F face);

    void getTriPoints(@NotNull final F face, double[] x, double[] y, double[] z, @NotNull final IVertexContainerDouble<V, E, F> distances);

    void getTriPoints(@NotNull final F face, double[] x, double[] y, double[] z, @NotNull final String name, IMeshDataStorage<V, E, F> dataStorage);

    void getTriPoints(@NotNull final F face, double[] x, double[] y, double[] z, Function<V, Double> func);

    /**
     * Returns the quality of a face / triangle.
     *
     * @param face the face which has to be a valid triangle
     * @return the quality of a face / triangle
     */
    double faceToQuality(final F face);

    double faceToLongestEdgeQuality(final F face);

    void getVirtualSupport(@NotNull final V v, @NotNull final E edge, @NotNull final List<Pair<V, V>> virtualSupport);

    boolean isNonAcute(V v1, V v2, V v3);

    boolean isLargeAngle(@NotNull final E edge, double minAngle);
}
