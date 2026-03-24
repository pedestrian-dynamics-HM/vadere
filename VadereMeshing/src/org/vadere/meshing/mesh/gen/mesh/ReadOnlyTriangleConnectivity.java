package org.vadere.meshing.mesh.gen.mesh;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vadere.meshing.mesh.inter.IVertexContainerDouble;
import org.vadere.meshing.mesh.inter.mesh.data.IMeshDataStorage;
import org.vadere.meshing.mesh.inter.mesh.triangle.ITriangleMesh;
import org.vadere.meshing.mesh.inter.mesh.triangle.ITriangleMeshEdges;
import org.vadere.meshing.mesh.inter.mesh.triangle.ITriangleMeshFaces;
import org.vadere.meshing.mesh.inter.mesh.triangle.ITriangleMeshVertices;
import org.vadere.meshing.mesh.inter.meshConnectivity.IReadOnlyTriConnectivity;
import org.vadere.meshing.mesh.inter.mesh.*;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.logging.Logger;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

public class ReadOnlyTriangleConnectivity <V extends IVertex, E extends IHalfEdge, F extends IFace> extends ReadOnlyPolyConnectivity<V, E, F> implements IReadOnlyTriConnectivity<V, E, F> {
    private final ITriangleMesh<V, E, F> mesh;
    private final ITriangleMeshEdges<V, E, F> edges;
    private final ITriangleMeshFaces<V, E, F> faces;
    private final ITriangleMeshVertices<V, E, F> vertices;

    public ReadOnlyTriangleConnectivity(ITriangleMesh<V, E, F> mesh) {
        super(mesh);
        this.mesh = mesh;

        edges = this.mesh.edges();
        faces = this.mesh.faces();
        vertices = this.mesh.vertices();
    }

    /**
     * A logger for debug and information reasons.
     */
    Logger log = Logger.getLogger(IReadOnlyTriConnectivity.class);

    /**
     * A Random number generator to randomly walk through the trinagulation.
     */
    private Random random = new Random();

    @Override
    public int getDimension() {
        return vertices.count() - 2;
    }

    @Override
    public Optional<F> locateMarch(final double x, final double y, @NotNull final F startFace) {
        boolean hasNoFaces = getDimension() <= 0;
        if(hasNoFaces){
            return Optional.empty();
        }

        boolean onlyOneFace = getDimension() == 1;
        if(onlyOneFace) {
            return marchLocate1D(x, y, startFace);
        }
        else {
            return Optional.of(straightWalk2D(x, y, startFace));
        }
    }

    @Override
    public Optional<F> locate(final double x, final double y) {
        Optional<F> optFace;
        if(faces.count() > 1) {
            optFace = locateMarch(x, y, faces.getFirst());
        }
        else if(faces.count() == 1) {
            optFace = Optional.of(faces.getFirst());
        }
        else {
            optFace = Optional.empty();
        }

        return optFace;
    }

    @Override
    public LinkedList<E> getIntersectingEdges(@NotNull final V vStart, @NotNull final V vEnd) {
        VPoint q = vertices.toPoint(vStart);
        VPoint p = vertices.toPoint(vEnd);
        E firstEdge = null;

        LinkedList<E> visitedEdges = new LinkedList<>();
        for(E e : edges.iterableFor(vStart)) {
            E prev = edges.getPrev(e);
            if(intersectsDirectional(q, p, prev)) {
                firstEdge = edges.getTwin(prev);
                visitedEdges.addLast(firstEdge);
            }
        }

        Optional<E> optEdge = Optional.ofNullable(firstEdge);

        // TODO: duplicated code
        while(optEdge.isPresent()) {
            E inEdge = optEdge.get();
            optEdge = straightWalkNext(inEdge, q, p, e -> !isRightOf(vEnd.getX(), vEnd.getY(), e), visitedEdges);
            if(optEdge.isPresent()) {
                inEdge = optEdge.get();
                visitedEdges.addLast(inEdge);

                if(edges.isBorder(inEdge)) {
                    break;
                }
                else if(edges.isHole(inEdge)) {
                    throw new IllegalArgumentException("reach a hole!");
                }
            }
        }

        return visitedEdges;
    }

    @Override
    public LinkedList<E> straightWalk2DGatherDirectional(@NotNull final F face, @NotNull final VPoint direction, @NotNull final Predicate<E> additionalStopCondition) {
        VPoint q = faces.toTriangleMidpoint(face);
        assert faces.toTriangle(face).contains(q);

        Predicate<E> publicStopCondion = e -> isRightOf(q.x, q.y, e);
        LinkedList<E> visitedFaces = straightGatherWalk2DDirectional(q, direction, face, publicStopCondion.or(additionalStopCondition));

        return visitedFaces;
    }

    @Override
    public F straightWalk2D(final double x1, final double y1, @NotNull final F startFace, @NotNull final Predicate<E> stopCondition) {
        return faces.getOf(straightGatherWalk2D(x1, y1, startFace, stopCondition).peekLast());
    }

    @Override
    public LinkedList<E> straightGatherWalk2D(final double x1, final double y1, @NotNull final F startFace, @NotNull final Predicate<E> stopCondition) {
        assert !faces.isOuterBorder(startFace);

        // initialize
        F face = startFace;
        // for convex polygons we could also use: VPoint q = getMesh().toPolygon(startFace).getPolygonCentroid();
        VPoint q = faces.toTriangleMidpoint(startFace); // walk from q to p
        VPoint p = new VPoint(x1, y1);

        return straightGatherWalk2D(q, p, face, stopCondition);
    }

    @Override
    public Optional<F> marchLocate1D(final double x, final double y, @NotNull final F startFace) {
        if(this.faceContains(x, y, startFace)) {
            return Optional.of(startFace);
        }
        else {
            return Optional.empty();
        }
    }

    @Override
    public boolean ringContainsPoint(@NotNull final V vertex, @NotNull final IPoint point)  {
        java.util.List<IPoint> points = vertices.getPoints(vertex);

        if(vertices.isAtBoundary(vertex)) {
            points.add(vertices.toPoint(vertex));
        }

        return points.contains(point)
                || GeometryUtils.toPolygon(points).contains(point)
                || GeometryUtils.toPolygon(points).distance(point) <= GeometryUtils.DOUBLE_EPS;
    }

    @Override
    public boolean isLongestEdge(@NotNull final E edge) {

        E e = edge;
        if(edges.isBoundary(e)) {
            e = edges.getTwin(e);
        }

        VLine line = edges.toLine(e);
        double lenSq = line.lengthSq();

        E next = edges.getNext(e);
        E prev = edges.getPrev(e);

        if(edges.toLine(next).lengthSq() > lenSq || edges.toLine(prev).lengthSq() > lenSq) {
            return false;
        }

        if(edges.isAtBoundary(e)) {
            return true;
        }
        else {
            e = edges.getTwin(e);
            next = edges.getNext(e);
            prev = edges.getPrev(e);
            return edges.toLine(next).lengthSq() < lenSq && edges.toLine(prev).lengthSq() < lenSq;
        }
    }

    @Override
    public E getLongestHalfEdge(@NotNull final F face) {
        assert !faces.isBoundary(face);
        E edge = edges.getAnyOf(face);
        E next = edges.getNext(edge);
        E prev = edges.getPrev(edge);

        double len = edges.toLine(edge).lengthSq();
        double lenN = edges.toLine(next).lengthSq();
        double lenP = edges.toLine(prev).lengthSq();

        if(len >= lenN) {
            if(len >= lenP || lenP <= lenN) {
                return edge;
            } else {
                return prev;
            }
        } else {
            if(lenN > lenP) {
                return next;
            } else {
                return  prev;
            }
        }
    }

    @Override
    public boolean isLongestHalfEdge(@NotNull final E edge) {
        E e = edge;
        if(edges.isBoundary(e)) {
            e = edges.getTwin(e);
        }

        E next = edges.getNext(e);
        E prev = edges.getPrev(e);
        VLine line = edges.toLine(e);
        double lenSq = line.lengthSq();

        return edges.toLine(next).lengthSq() <= lenSq && edges.toLine(prev).lengthSq() <= lenSq;
    }

    @Override
    public boolean isShortestHalfEdge(@NotNull final E edge) {
        E e = edge;
        if(edges.isBoundary(e)) {
            e = edges.getTwin(e);
        }

        E next = edges.getNext(e);
        E prev = edges.getPrev(e);
        VLine line = edges.toLine(e);
        double lenSq = line.lengthSq();

        return edges.toLine(next).lengthSq() >= lenSq && edges.toLine(prev).lengthSq() >= lenSq;
    }

    @Override
    public Set<V> getVertices(@NotNull final double x, final double y, final F startFace, @NotNull final Predicate<V> predicate) {
        assert !faces.isBoundary(startFace) && faces.toTriangle(startFace).contains(x, y);
        Set<V> set = new HashSet<>();
        LinkedList<V> heap = new LinkedList();
        for(V v : vertices.iterableFor(startFace)) {
            heap.addLast(v);
        }

        while (!heap.isEmpty()) {
            V candidate = heap.poll();
            if(predicate.test(candidate) && !set.contains(candidate)) {
                set.add(candidate);
                for(V neighbour : vertices.adjacentIterableFor(candidate)) {
                    heap.addLast(neighbour);
                }
            }
        }

        return set;
    }

    @Override
    public E getAnyEdge(@NotNull final Pair<E, E> pair) {
        if(pair.getLeft() != null) {
            return pair.getLeft();
        }
        else {
            return pair.getRight();
        }
    }

    @Override
    public boolean isCCW(@NotNull final F triangleFace) {
        assert edges.getAllOf(triangleFace).size() == 3;

        E edge = edges.getAnyOf(triangleFace);
        IPoint p1 = edges.endToPoint(edge);
        IPoint p2 = edges.endToPoint(edges.getNext(edge));
        IPoint p3 = edges.endToPoint(edges.getPrev(edge));

        return GeometryUtils.isCCW(p1.getX(), p1.getY(), p2.getX(), p2.getY(), p3.getX(), p3.getY());
    }

    @Override
    public F straightWalk2D(final VPoint q, final VPoint p, final F startFace, final Predicate<E> stopCondition) {
        return faces.getOf(straightGatherWalk2D(q, p, startFace, stopCondition).peekLast());
    }

    @Override
    public E straightWalkSpecialCase(@NotNull final E inEdge,
                                      @NotNull final VPoint q,
                                      @NotNull final VPoint p) {

        /**
         * (1) get the vertex v which intersects with (q,p)
         */
        V vertex = getSpecialVertex(inEdge, q, p);
        E nextOutEdge = null;
        for(E e : edges.iterableFor(vertex)) {
            E prev = edges.getPrev(e);          // v1 -> v3
            E next = edges.getNext(e);          // vertex -> v1
            E twin = edges.getTwin(e);          // vertex -> v3
            E twinNext = edges.getNext(twin);   // v3 -> v2

            V v1 = vertices.getEndOf(next);
            V v2 = vertices.getEndOf(twinNext);
            V v3 = vertices.getEndOf(twin);

            if(faceContains(p.getX(), p.getY(), v2, vertex, v3)) {
                nextOutEdge = twinNext;
            }

            if(faceContains(p.getX(), p.getY(), vertex, v1, v3)) {
                nextOutEdge = prev;
            }

            if(nextOutEdge == null && GeometryUtils.isRightOf(v1.getX(), v1.getY(), v2.getX(), v2.getY(), p.getX(), p.getY()) && intersects(q, p, v1, v2)) {
                if(!inEdge.equals(prev) && intersects(q, p, prev)) {
                    nextOutEdge = prev;
                }
                else if(!inEdge.equals(twinNext) && intersects(q, p, twinNext)) {
                    nextOutEdge = twinNext;
                }
                else {
                    nextOutEdge = prev;
                }
            }

            if(nextOutEdge != null) {
                break;
            }
        }

        if(nextOutEdge == null) {
            throw new IllegalArgumentException("this should never happen!");
        }

        return nextOutEdge;
    }

    @Override
    public V getSpecialVertex(@NotNull final E inEdge,
                               @NotNull final VPoint q,
                               @NotNull final VPoint p) {

        for(V v : vertices.iterableFor(faces.getOf(inEdge))) {
            if(GeometryUtils.distanceToLineSegment(q, p, v) < GeometryUtils.DOUBLE_EPS) {
                return v;
            }
        }

        throw new IllegalArgumentException("no intersection point found " + q + " -> " + p);
    }

    @Override
    public E rayCastingPolygon(@NotNull final E inEdge,
                                @NotNull final VPoint q,
                                @NotNull final VPoint p,
                                @NotNull final Predicate<E> stopCondition) {

        E outEdge = null;
        E outIfInside = null;
        F face = faces.getOf(inEdge);

        // TODO: this seems to be expensive
        int count = 0;
        double distance = Double.MAX_VALUE;
        for(E e : edges.iterableFor(inEdge)) {
            if(intersectsDirectional(p, q, e)) {
                count++;
                //if((!stopCondition.test(e) && !getMesh().isBorder(face)) || (stopCondition.test(e) && getMesh().isBorder(face))) {
                V v1 = vertices.getEndOf(e);
                V v2 = vertices.getTwin(e);
                VPoint iPoint = GeometryUtils.intersectionPoint(q.getX(), q.getY(), p.getX(), p.getY(), v1.getX(), v1.getY(), v2.getX(), v2.getY());
                double dist = p.distance(iPoint);
                if(dist < distance) {
                    outEdge = e;
                    distance = dist;
                }
                //} else {
                //	outIfInside = e;
                //}
            }
        }

        boolean isInside = count % 2 == 1;

        boolean faceIsBorder = faces.isOuterBorder(face);
        return (isInside && !faceIsBorder || !isInside && faceIsBorder) ? null : outEdge;
    }

    @Override
    public E walkAroundBoundaryStraight(@NotNull final E inEdge,
                                         @NotNull final VPoint q,
                                         @NotNull final VPoint p,
                                         @NotNull final Predicate<E> stopCondition) {
        E outEdge = null;
        E outIfInside = null;
        F face = faces.getOf(inEdge);

        V v1 = vertices.getEndOf(inEdge);
        V v2 = vertices.getTwin(inEdge);

        double angle = GeometryUtils.angle2D(p.x-q.x, p.y-q.y,
                vertices.getX(v1) - vertices.getX(v2), vertices.getY(v1) - vertices.getY(v2));

        boolean walkForward = angle <= Math.PI * 0.5;

        Iterable<E> iterable = walkForward ? edges.iterableFor(inEdge) : edges.iterableReversedFor(inEdge);
        for(E e : iterable) {
            if(!e.equals(inEdge) && intersectsDirectional(p, q, e)) {
                outEdge = e;
                break;
            }
        }

        if(outEdge == null) {
            return outEdge;
        }

        if(faces.isBoundary(face) && isLeftOf(p.getX(), p.getY(), outEdge)) {
            return rayCastingPolygon(inEdge, q, p, stopCondition);
        } else {
            return outEdge;
        }
    }

    @Override
    public Optional<E> straightWalkNext(
            @NotNull final E inEdge,
            @NotNull final VPoint q,
            @NotNull final VPoint p,
            @NotNull final Predicate<E> stopCondition,
            @Nullable final LinkedList<E> visitedEdges) {
        E outEdge = null;
        F face = faces.getOf(inEdge);

        /**
         * Special case: the face is a hole or the border!
         */
        if(faces.isBoundary(face)) {
            //return Optional.empty();

            V v1 = vertices.getEndOf(inEdge);
            V v2 = vertices.getTwin(inEdge);
            VPoint iPoint1 = GeometryUtils.intersectionPoint(q.getX(), q.getY(), p.getX(), p.getY(), v1.getX(), v1.getY(), v2.getX(), v2.getY());

            outEdge = walkAroundBoundaryStraight(inEdge, q, p, stopCondition);
            //outEdge = rayCastingPolygon(inEdge, q, p, stopCondition);
            // the point outside
            if(outEdge == null) {
                return Optional.empty();
            }

            v1 = vertices.getEndOf(outEdge);
            v2 = vertices.getTwin(outEdge);
            VPoint iPoint2 = GeometryUtils.intersectionPoint(q.getX(), q.getY(), p.getX(), p.getY(), v1.getX(), v1.getY(), v2.getX(), v2.getY());

            // we did no progress towards p => walking around does not work cause the boundary is not convex, therefore we use the expensive method
            if(iPoint1.distanceSq(p) <= iPoint2.distanceSq(p)) {
                // TODO this is too expensive!
                outEdge = rayCastingPolygon(inEdge, q, p, stopCondition);
            }

            // the point outside
            if(outEdge == null) {
                return Optional.empty();
            } else {
                return Optional.of(edges.getTwin(outEdge));
            }
        } else {
            /**
             * Get the half-edges e which intersects (q, p).
             */
            for(E e : edges.iterableFor(edges.getNext(inEdge))) {
                if(!e.equals(inEdge) && intersects(q, p, e)) {
                    outEdge = e;
                    break;
                }
            }
        }

        /**
         * General case (1): The line defined by (q,p) intersects 2 edges of the convex polygon.
         */
        if(outEdge != null) {
            //log.debug("straight walk: general case");
            boolean stop = stopCondition.test(outEdge);
            if(!stopCondition.test(outEdge)) {
                return Optional.of(edges.getTwin(outEdge));
            }
            else {
                return Optional.empty();
            }
        }
        /**
         * Special case (2): There is one or two points of the polygon which are collinear with the line defined by (q,p).
         */
        else {
            /**
             * Good case (2.1) There are two collinear points but the face contains p => p lies on an edge of the face.
             */
            if(this.faceContains(p.getX(), p.getY(), face) || faces.isCloseTo(face, p.getX(), p.getY())) {
                log.debug("no intersection line but contained or very close.");
                return Optional.empty();
            }
            /**
             * Bad case (2.2): This which should not happen in general: q, the exit point v and p are collinear, therefore there is no exit intersection line!
             * We continue the search with the face which centroid is closest to p! v has to be the closest p as well.
             */
            else {
                log.debug("straight walk: no exit edge found due to collinear exit point.");

                /**
                 * Get the face with the centroid closest to p and which was not visited already.
                 */
                E nextOutEdge = straightWalkSpecialCase(inEdge, q, p);
                if(!stopCondition.test(nextOutEdge)) {
                    return Optional.of(edges.getTwin(nextOutEdge));
                }
                else {
                    if(visitedEdges != null) {
                        visitedEdges.add(nextOutEdge);
                    }
                    return Optional.empty();
                }
            }
        }
    }

    @Override
    public LinkedList<E> straightGatherWalk2D(
            @NotNull final VPoint q,
            @NotNull final VPoint pDirection,
            @NotNull final F startFace,
            @NotNull final Predicate<E> stopCondition,
            final boolean directional,
            final boolean gather) {
        LinkedList<E> visitedEdges = new LinkedList<>();

        assert this.faceContains(q.getX(), q.getY(), startFace);

        /**
         * (1) find the initial in-edge.
         */
        E inEdge = null;

        /**
         * Find the half-edge which intersects the line-segment (q, q+pDirection) but not the half-line (q, q+pDirection).
         * This will be the first in-edge and the other half-edge intersecting (q, q+pDirection) will be the out-edge.
         */
        VPoint p;
        if(directional) {
            p = q.add(pDirection);
            for(E e : edges.iterableFor(startFace)) {
                // line intersection

                if(intersects(q, p, e) && !intersectsDirectional(q, p, e)) {
                    inEdge = e;
                    break;
                }
            }

            /**
             * this might happen if the line intersects a point, in this case both neighbouring edges are feasible
             */
            if(inEdge == null) {
                inEdge = edges.streamEdgesOf(startFace).filter(e -> !intersectsDirectional(q, p, e)).findAny().get();
            }

        }
        /**
         * Find the half-edge which intersects the line-segment (q, pDirection) and has pDirection on its left side.
         * This will be the first in-edge and the other half-edge intersecting (q, q+pDirection) will be the out-edge.
         */
        else {
            p = pDirection;
            // if this is true we are already done
            if(this.faceContains(pDirection.getX(), pDirection.getY(), startFace)) {
                if(!gather) {
                    visitedEdges.clear();
                }
                visitedEdges.add(edges.getAnyOf(startFace));
                return visitedEdges;
            }
            // find the entering edge
            for(E e : edges.iterableFor(startFace)) {
                // line intersection
                if(intersects(q, pDirection, e) && isLeftOfRobust(p.getX(), p.getY(), e)) {
                    inEdge = e;
                    break;
                }
            }

            /**
             * this might happen if the line intersects a point, in this case both neighbouring edges are feasible
             */
            if(inEdge == null) {
                Optional<E> optEdge = edges.streamEdgesOf(startFace).filter(e -> isLeftOf(p.getX(), p.getY(), e)).findAny();
                inEdge = optEdge.get();
            }
        }

        if(inEdge == null) {
            throw new IllegalArgumentException("did not find any edge.");
        }

        if(!gather) {
            visitedEdges.clear();
        }
        visitedEdges.addLast(inEdge);


        Optional<E> optEdge;

        /**
         * (2) find all other in-edges.
         */
        do {
            // TODO: this might be slow
            if(directional) {
                optEdge = straightWalkNext(inEdge, q, p, stopCondition, visitedEdges);
            }
            else {
                optEdge = straightWalkNext(inEdge, q, p, stopCondition, visitedEdges);
            }

            if(optEdge.isPresent()) {
                inEdge = optEdge.get();
                if(!gather) {
                    visitedEdges.clear();
                }
                visitedEdges.addLast(inEdge);
            }
        } while (optEdge.isPresent());

        return visitedEdges;
    }

    @Override
    public F marchRandom2D(final double x1, final double y1, @NotNull final F startFace) {
        assert faces.getHoles().size() == 0;

        boolean first = true;
        F face = startFace;
        F prevFace = null;
        int count = 0;

        while (true) {

            if(faces.isBoundary(face)) {
                if(this.faceContains(x1, y1, face)) {
                    return face;
                }
                else {
                    throw new IllegalArgumentException("marchRandom2D can not walk through holes.");
                }
            }

            count++;
            boolean goLeft = random.nextBoolean();
            //boolean goLeft = true;

            E e1 = edges.getAnyOf(face);
            E e2 = edges.getNext(e1);
            E e3 = edges.getNext(e2);

            V v1 = vertices.getEndOf(e1);
            V v2 = vertices.getEndOf(e2);
            V v3 = vertices.getEndOf(e3);

            // loop unrolling for efficiency!
            if(first) {
                first = false;
                prevFace = face;

                if (GeometryUtils.isRightOf(v3, v1, x1, y1)) {
                    face = faces.getTwin(e1);
                    continue;
                }

                if (GeometryUtils.isRightOf(v1, v2, x1, y1)) {
                    face = faces.getTwin(e2);
                    continue;
                }

                if (GeometryUtils.isRightOf(v2, v3, x1, y1)) {
                    face = faces.getTwin(e3);
                    continue;
                }
            } else if(goLeft) {
                if(prevFace == faces.getTwin(e1)) {
                    prevFace = face;

                    if (GeometryUtils.isRightOf(v2, v3, x1, y1)) {
                        face = faces.getTwin(e3);
                        continue;
                    }

                    if(GeometryUtils.isRightOf(v1, v2, x1, y1)) {
                        face = faces.getTwin(e2);
                        continue;
                    }

                }
                else if(prevFace == faces.getTwin(e2)) {
                    prevFace = face;

                    if (GeometryUtils.isRightOf(v3, v1, x1, y1)) {
                        face = faces.getTwin(e1);
                        continue;
                    }

                    if (GeometryUtils.isRightOf(v2, v3, x1, y1)) {
                        face = faces.getTwin(e3);
                        continue;
                    }

                }
                else {
                    prevFace = face;

                    if(GeometryUtils.isRightOf(v1, v2, x1, y1)) {
                        face = faces.getTwin(e2);
                        continue;
                    }

                    if (GeometryUtils.isRightOf(v3, v1, x1, y1)) {
                        face = faces.getTwin(e1);
                        continue;
                    }

                }
            }
            else {
                if(prevFace == faces.getTwin(e1)) {
                    prevFace = face;

                    if(GeometryUtils.isRightOf(v1, v2, x1, y1)) {
                        face = faces.getTwin(e2);
                        continue;
                    }

                    if (GeometryUtils.isRightOf(v2, v3, x1, y1)) {
                        face = faces.getTwin(e3);
                        continue;
                    }

                }
                else if(prevFace == faces.getTwin(e2)) {
                    prevFace = face;

                    if (GeometryUtils.isRightOf(v2, v3, x1, y1)) {
                        face = faces.getTwin(e3);
                        continue;
                    }

                    if (GeometryUtils.isRightOf(v3, v1, x1, y1)) {
                        face = faces.getTwin(e1);
                        continue;
                    }

                }
                else {
                    prevFace = face;

                    if (GeometryUtils.isRightOf(v3, v1, x1, y1)) {
                        face = faces.getTwin(e1);
                        continue;
                    }

                    if(GeometryUtils.isRightOf(v1, v2, x1, y1)) {
                        face = faces.getTwin(e2);
                        continue;
                    }

                }
            }
            //log.info("#traversed triangles = " + count);
            return face;
        }
    }

    @Override
    public boolean faceContains(final double x, final double y, @NotNull final F face) {
        if(!faces.isBoundary(face)) {
            //return getMesh().toImmutableTriangle(face).contains(x, y);
            E e1 = edges.getAnyOf(face);
            V v1 = vertices.getEndOf(e1);
            V v3 = vertices.getTwin(e1);
            V v2 = vertices.getEndOf(edges.getNext(e1));

            double x1 = vertices.getX(v1);
            double y1 = vertices.getY(v1);
            double x2 = vertices.getX(v2);
            double y2 = vertices.getY(v2);
            double x3 = vertices.getX(v3);
            double y3 = vertices.getY(v3);

            return !GeometryUtils.isRightOf(x1, y1, x2, y2, x, y) &&
                    !GeometryUtils.isRightOf(x2, y2, x3, y3, x, y) &&
                    !GeometryUtils.isRightOf(x3, y3, x1, y1, x, y);
        } else {
            return super.faceContains(x, y, face);
        }
    }

    @Override
    public boolean isInsideCircumscribedCycle(@NotNull final F face, final double x1, final double y1) {
        E e1 = edges.getAnyOf(face);
        E e2 = edges.getNext(e1);
        E e3 = edges.getNext(e2);

        V v1 = vertices.getEndOf(e1);
        V v2 = vertices.getEndOf(e2);
        V v3 = vertices.getEndOf(e3);

        return GeometryUtils.isInsideCircle(v1, v2, v3, x1, y1);
    }

    @Override
    public Optional<E> getClosestEdge(final double x, final double y) {
        Optional<F> optFace = locate(x, y);

        if(optFace.isPresent()) {
            return Optional.of(edges.closestOfFaceTo(optFace.get(), x, y));
        }
        else {
            return Optional.empty();
        }
    }

    @Override
    public Optional<E> getClosestEdge(final double x, final double y, final F startFace) {
        Optional<F> optFace = locateMarch(x, y, startFace);

        if(optFace.isPresent()) {
            return Optional.of(edges.closestOfFaceTo(optFace.get(), x, y));
        }
        else {
            return Optional.empty();
        }
    }

    @Override
    public Optional<V> getClosestVertex(final double x, final double y) {
        Optional<F> optFace = locate(x, y);

        if(optFace.isPresent()) {
            return Optional.of(vertices.closestOfFaceTo(optFace.get(), x, y));
        }
        else {
            return Optional.empty();
        }
    }


    @Override
    public Optional<V> getClosestVertex(final double x, final double y, final F startFace) {
        Optional<F> optFace = locateMarch(x, y, startFace);

        if(optFace.isPresent()) {
            return Optional.of(vertices.closestOfFaceTo(optFace.get(), x, y));
        }
        else {
            return Optional.empty();
        }
    }

    @Override
    public boolean isValid() {
        Predicate<F> orientationPredicate = f -> {
            E edge = edges.getAnyOf(f);
            IPoint p1 = edges.getMutableEndPoint(edges.getPrev(edge));
            IPoint p2 = edges.getMutableEndPoint(edge);
            IPoint p3 = edges.getMutableEndPoint(edges.getNext(edge));
            boolean valid = GeometryUtils.isLeftOf(p1, p2, p3);
            if (!valid) {
                log.info(p1 + ", " + p2 + ", " + p3);
            }
            return valid;
        };

        return faces.stream().filter(f -> !faces.isDestroyed(f)).allMatch(orientationPredicate);
    }

    @Override
    public boolean isValid(@NotNull final F face) {
        Predicate<F> orientationPredicate = f -> {
            E edge = edges.getAnyOf(f);
            IPoint p1 = edges.getMutableEndPoint(edges.getPrev(edge));
            IPoint p2 = edges.getMutableEndPoint(edge);
            IPoint p3 = edges.getMutableEndPoint(edges.getNext(edge));
            return GeometryUtils.isLeftOf(p1, p2, p3);
        };

        return !faces.isBoundary(face) && orientationPredicate.test(face);
    }

    @Override
    public IPoint[] getPoints(@NotNull final E edge) {
        final IPoint[] points = new IPoint[3];
        points[0] = edges.getMutableEndPoint(edge);
        points[1] = edges.getMutableEndPoint(edges.getNext(edge));
        points[2] = edges.getMutableEndPoint(edges.getPrev(edge));
        return points;
    }

    @Override
    public IPoint[] getPoints(F face) {
        return getPoints(edges.getAnyOf(face));
    }

    @Override
    public void getTriPoints(@NotNull final F face, double[] x, double[] y, double[] z, @NotNull final IVertexContainerDouble<V, E, F> distances){
        assert x.length == y.length && y.length == z.length && x.length == 3;

        E edge = edges.getAnyOf(face);
        V v = vertices.getEndOf(edge);
        x[0] = vertices.getX(v);
        y[0] = vertices.getY(v);
        z[0] = distances.getValue(v);

        v = vertices.getEndOf(edges.getNext(edge));
        x[1] = vertices.getX(v);
        y[1] = vertices.getY(v);
        z[1] = distances.getValue(v);

        v = vertices.getEndOf(edges.getPrev(edge));
        x[2] = vertices.getX(v);
        y[2] = vertices.getY(v);
        z[2] = distances.getValue(v);
    }

    @Override
    public void getTriPoints(@NotNull final F face, double[] x, double[] y, double[] z, @NotNull final String name, IMeshDataStorage<V, E, F> dataStorage){
        assert x.length == y.length && y.length == z.length && x.length == 3;

        E edge = edges.getAnyOf(face);
        V v = vertices.getEndOf(edge);
        x[0] = vertices.getX(v);
        y[0] = vertices.getY(v);
        z[0] = dataStorage.getDoubleData(v, name);

        v = vertices.getEndOf(edges.getNext(edge));
        x[1] = vertices.getX(v);
        y[1] = vertices.getY(v);
        z[1] = dataStorage.getDoubleData(v, name);

        v = vertices.getEndOf(edges.getPrev(edge));
        x[2] = vertices.getX(v);
        y[2] = vertices.getY(v);
        z[2] = dataStorage.getDoubleData(v, name);
    }

    @Override
    public void getTriPoints(@NotNull final F face, double[] x, double[] y, double[] z, Function<V, Double> func){
        assert x.length == y.length && y.length == z.length && x.length == 3;

        E edge = edges.getAnyOf(face);
        V v = vertices.getEndOf(edge);
        x[0] = vertices.getX(v);
        y[0] = vertices.getY(v);
        z[0] = func.apply(v);

        v = vertices.getEndOf(edges.getNext(edge));
        x[1] = vertices.getX(v);
        y[1] = vertices.getY(v);
        z[1] = func.apply(v);

        v = vertices.getEndOf(edges.getPrev(edge));
        x[2] = vertices.getX(v);
        y[2] = vertices.getY(v);
        z[2] = func.apply(v);
    }

    @Override
    public double faceToQuality(final F face) {
        assert edges.getAllOf(face).size() == 3;
        E edge = edges.getAnyOf(face);
        IPoint p1 = vertices.getEndOf(edge);
        IPoint p2 = vertices.getEndOf(edges.getNext(edge));
        IPoint p3 = vertices.getEndOf(edges.getPrev(edge));

        return GeometryUtils.qualityInCircleOutCircle(p1, p2, p3);
    }

    @Override
    public double faceToLongestEdgeQuality(final F face) {
        E edge = edges.getAnyOf(face);
        IPoint p1 = vertices.getEndOf(edge);
        IPoint p2 = vertices.getEndOf(edges.getNext(edge));
        IPoint p3 = vertices.getEndOf(edges.getPrev(edge));
        return GeometryUtils.qualityLongestEdgeInCircle(p1, p2, p3);
    }

    public void getVirtualSupport(@NotNull final V v, @NotNull final E edge, @NotNull final List<Pair<V, V>> virtualSupport) {
        if(edges.isAtBoundary(edge)) {
            return;
        }

        E prev = edges.getPrev(edge);
        E twin = edges.getTwin(edge);

        V v1 = vertices.getEndOf(prev);
        V v2 = vertices.getEndOf(edge);
        V u = vertices.getEndOf(edges.getNext(twin));

        if(!isNonAcute(u, v, v1)) {
            virtualSupport.add(Pair.of(v1, u));
        } else {
            getVirtualSupport(v, edges.getNext(twin), virtualSupport);
        }

        if(!isNonAcute(v2, v, u)) {
            virtualSupport.add(Pair.of(v2, u));
        } else {
            getVirtualSupport(v, edges.getPrev(twin), virtualSupport);
        }

    }

    public boolean isNonAcute(V v1, V v2, V v3) {
        double angle1 = GeometryUtils.angle(v1, v2, v3);

        // non-acute triangle
        double rightAngle = Math.PI/2;
        return angle1 > rightAngle + GeometryUtils.DOUBLE_EPS;
    }

    @Override
    public boolean isLargeAngle(@NotNull final E edge, double minAngle) {
        assert !faces.isBoundary(faces.getOf(edge));
        V vp = vertices.getEndOf(edge);
        V vq = vertices.getEndOf(edges.getNext(edge));
        V vr = vertices.getEndOf(edges.getPrev(edge));

        VPoint r = vertices.toPoint(vr);
        VPoint p = vertices.toPoint(vp);
        VPoint q = vertices.toPoint(vq);

        if(GeometryUtils.isCCW(r, p, q)) {
            double angle = GeometryUtils.angle(r, p, q);
            return angle > minAngle;
        }

        return false;
    }
}
