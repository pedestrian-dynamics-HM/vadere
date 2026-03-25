package org.vadere.meshing.mesh.gen.mesh;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.inter.mesh.*;
import org.vadere.meshing.mesh.inter.meshConnectivity.IReadOnlyPolyConnectivity;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VPolygon;

import java.util.*;
import java.util.function.Predicate;

public class ReadOnlyPolyConnectivity<V extends IVertex, E extends IHalfEdge, F extends IFace> implements IReadOnlyPolyConnectivity<V, E, F> {
    private final IMesh<V, E, F> mesh;
    private final IMeshEdges<V, E, F> edges;
    private final IMeshFaces<V, E, F> faces;
    private final IMeshVertices<V, E, F> vertices;

    public ReadOnlyPolyConnectivity(IMesh<V, E, F> mesh) {
        this.mesh = mesh;
        edges = this.mesh.edges();
        faces = this.mesh.faces();
        vertices = this.mesh.vertices();
    }

    @Override
    public boolean isAtBoundary(@NotNull final E halfEdge) {
        return edges.isBoundary(halfEdge) || edges.isBoundary(edges.getTwin(halfEdge));
    }

    @Override
    public Optional<F> locateNonBoundaryByFullScan(final double x, final double y, final Object caller) {
        return locateNonBoundaryByFullScan(x, y);
    }

    @Override
    public Optional<V> locateNonBoundaryPointByFullScan(final double x, final double y) {
        Optional<F> optFace = locateNonBoundaryByFullScan(x, y);
        if(optFace.isPresent()) {
            for(V v :vertices.iterableFor(optFace.get())) {
                if(v.getX() == x && v.getY() == y) {
                    return Optional.of(v);
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<F> locateNonBoundaryByFullScan(double x, final double y) {
        for(F face : faces.getAll()) {
            VPolygon polygon = faces.toPolygon(face);
            if(polygon.contains(new VPoint(x, y))) {
                return Optional.of(face);
            }
        }
        return Optional.empty();
    }

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
    public Optional<E> findEdge(@NotNull final V begin, @NotNull final V end) {
        return edges.getIncidentEdges(edges.getOf(begin)).stream()
                .filter(edge -> edges.getPrev(edge).equals(end))
                .map(edge -> edges.getTwin(edge)).findAny();
    }

    @Override
    public boolean isSimpleLink(@NotNull final E halfEdge) {
        E edge = halfEdge;
        E twin = edges.getTwin(halfEdge);
        F twinFace = faces.getOf(twin);

        E next = edges.getNext(edge);

        while (!edge.equals(next)) {
            if (twinFace.equals(faces.getTwin(next))) {
                return false;
            }
            next = edges.getNext(next);
        }
        return true;
    }

    // TODO: improve performance by remembering faces
    @Override
    public List<F> findFaces(
            @NotNull final F face,
            @NotNull final Predicate<F> markCondition,
            final int maxDept) {
        int dept = 0;

        if(!markCondition.test(face)) {
            return Collections.EMPTY_LIST;
        }

        Set<F> markedFaces = new HashSet<>();
        List<F> result = new ArrayList<>();
        result.add(face);
        markedFaces.add(face);

        List<E> toProcess = edges.getAllOf(face);

        while (!toProcess.isEmpty()) {
            dept++;

            List<E> newToProcess = new ArrayList<>();

            for(E edge : toProcess) {
                // the face might be destroyed by an operation before
                F candidate = faces.getTwin(edge);
                if(!markedFaces.contains(candidate) &&  markCondition.test(candidate)) {
                    result.add(candidate);
                    markedFaces.add(candidate);
                    for(E e : edges.iterableFor(candidate)) {
                        newToProcess.add(e);
                    }
                }
            }

            if(maxDept > 0 && dept >= maxDept) {
                return result;
            }

            toProcess = newToProcess;
        }

        return result;
    }

    @Override
    public boolean faceContains(final double x, final double y, @NotNull final F face) {
        for(E e : edges.iterableFor(face)) {
            if(isRightOf(x, y, e)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean faceContains(final double x, final double y, @NotNull final V v1, @NotNull final V v2, @NotNull final V v3) {
        return !GeometryUtils.isRightOf(v1.getX(), v1.getY(), v2.getX(), v2.getY(), x, y)
                && !GeometryUtils.isRightOf(v2.getX(), v2.getY(), v3.getX(), v3.getY(), x, y)
                && !GeometryUtils.isRightOf(v3.getX(), v3.getY(), v1.getX(), v1.getY(), x, y);
    }

    @Override
    public Optional<E> getMemberEdge(@NotNull final F face, final double x1, final double y1) {

        for(E e : edges.iterableFor(face)) {
            IPoint p = edges.getMutableEndPoint(e);
            if(p.getX() == x1 && p.getY() == y1) {
                return Optional.of(e);
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<E> getCloseEdge(@NotNull final F face, double x1, double y1, double distance) {
        assert distance > 0;
        for(E e : edges.iterableFor(face)) {
            IPoint p = edges.getMutableEndPoint(e);
            if(p.distance(x1, y1) <= distance) {
                return Optional.of(e);
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean isRightOf(final double x1, final double y1, @NotNull final E edge) {
        V v1 = vertices.getEndOf(edges.getPrev(edge));
        V v2 = vertices.getEndOf(edge);
        return GeometryUtils.isRightOf(vertices.getX(v1), vertices.getY(v1), vertices.getX(v2), vertices.getY(v2), x1, y1);
    }

    @Override
    public boolean isLeftOfRobust(final double x1, final double y1, @NotNull final E edge) {
        V v1 = vertices.getEndOf(edges.getPrev(edge));
        V v2 = vertices.getEndOf(edge);
        return GeometryUtils.isLeftOf(v1.getX(), v1.getY(), v2.getX(), v2.getY(), x1, y1);
    }

    @Override
    public boolean isLeftOf(final double x1, final double y1, @NotNull final E edge) {
        V v1 = vertices.getEndOf(edges.getPrev(edge));
        V v2 = vertices.getEndOf(edge);
        return GeometryUtils.isLeftOf(v1.getX(), v1.getY(), v2.getX(), v2.getY(), x1, y1);
    }

    @Override
    public boolean intersects(@NotNull final IPoint p1, @NotNull final IPoint p2, @NotNull final E edge) {
        V v1 = vertices.getEndOf(edges.getPrev(edge));
        V v2 = vertices.getEndOf(edge);
        return intersects(p1, p2, v1, v2);
    }

    @Override
    public boolean intersects(@NotNull final IPoint p1, @NotNull final IPoint p2, @NotNull final V v1, @NotNull final V v2) {
        return GeometryUtils.intersectLine(p1.getX(), p1.getY(), p2.getX(), p2.getY(), v1.getX(), v1.getY(), v2.getX(), v2.getY());
    }

    @Override
    public boolean intersectsDirectional(@NotNull final IPoint p1, @NotNull final IPoint p2, E edge) {
        V v1 = vertices.getEndOf(edges.getPrev(edge));
        V v2 = vertices.getEndOf(edge);
        return GeometryUtils.intersectHalfLineSegment(
                p1.getX(), p1.getY(), p2.getX(), p2.getY(),
                vertices.getX(v1), vertices.getY(v1), vertices.getX(v2), vertices.getY(v2));
    }

    @Override
    public boolean isSimpleConnected(@NotNull final V vertex) {
        if(vertices.isDestroyed(vertex)) {
            return true;
        }
        // test if degree of the vertex is <= 2
        E edge0 = edges.getOf(vertex);
        E edge1 = edges.getTwin(edges.getNext(edge0));
        E edge2 = edges.getTwin(edges.getNext(edge1));
        return edge0 == edge1 || edge0 == edge2;
    }

    @Override
    public Optional<E> findTwins(@NotNull final F face1, @NotNull final F face2) {
        for(E halfEdge1 : edges.iterableFor(face1)) {
            for(E halfEdge2 : edges.iterableFor(face2)) {
                if(edges.getTwin(halfEdge1).equals(halfEdge2)) {
                    return Optional.of(halfEdge1);
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean isSimpleConnected(@NotNull final F face) {
        Set<F> faceSet = new HashSet<>();
        E edge = edges.getAnyOf(face);
        E next = edges.getNext(edge);
        faceSet.add(faces.getTwin(edge));

        while (!edge.equals(next)) {
            if(faceSet.contains(faces.getTwin(next))) {
                return false;
            }
            else {
                faceSet.add(faces.getTwin(next));
            }
            next = edges.getNext(next);
        }
        return true;
    }
}
