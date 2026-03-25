package org.vadere.meshing.mesh.gen;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vadere.meshing.mesh.inter.mesh.IFace;
import org.vadere.meshing.mesh.inter.mesh.IHalfEdge;
import org.vadere.meshing.mesh.inter.mesh.IVertex;
import org.vadere.meshing.mesh.inter.mesh.builder.*;
import org.vadere.meshing.mesh.inter.mesh.triangle.ITriangleMesh;
import org.vadere.meshing.mesh.inter.mesh.triangle.ITriangleMeshEdges;
import org.vadere.meshing.mesh.inter.mesh.triangle.ITriangleMeshFaces;
import org.vadere.meshing.mesh.inter.mesh.triangle.ITriangleMeshVertices;
import org.vadere.meshing.mesh.inter.meshConnectivity.IReadOnlyTriConnectivity;
import org.vadere.meshing.mesh.inter.meshConnectivity.ITriConnectivity;
import org.vadere.util.data.Node;
import org.vadere.util.data.NodeLinkedList;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VTriangle;
import org.vadere.util.logging.Logger;
import org.vadere.util.math.IDistanceFunction;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class TriangleConnectivity<V extends IVertex, E extends IHalfEdge, F extends IFace> extends PolyConnectivity<V, E, F> implements ITriConnectivity<V, E, F> {
    /**
     * A logger to debug some code.
     */
    Logger log = Logger.getLogger(TriangleConnectivity.class);

    private final IReadOnlyTriConnectivity<V, E, F> readOnlyTriConnectivity;
    private final ITriangleMesh<V, E, F> mesh;
    private final ITriangleMeshBuilder<V, E, F> meshBuilder;
    private final IMeshBuilderEdges<V, E, F> edgeBuilder;
    private final IMeshBuilderFaces<V, E, F> faceBuilder;
    private final IMeshBuilderVertices<V, E, F> vertexBuilder;

    private final ITriangleMeshVertices<V, E, F> vertices;
    private final ITriangleMeshFaces<V, E, F> faces;
    private final ITriangleMeshEdges<V, E, F> edges;

    private Predicate<Pair<E, V>> isIllegalPredicate;
    private Predicate<Triple<E, V, Double>> isIllegalPredicateWithEps;

    public TriangleConnectivity(ITriangleMeshBuilder<V, E, F> meshBuilder) {
        super(meshBuilder);
        this.meshBuilder = meshBuilder;
        this.mesh = meshBuilder.getMesh();
        readOnlyTriConnectivity = this.meshBuilder.getMesh().readConnectivity();

        isIllegalPredicate = evPair -> false;
        isIllegalPredicateWithEps = triple -> false;

        edgeBuilder = meshBuilder.edges();
        faceBuilder = meshBuilder.faces();
        vertexBuilder = meshBuilder.vertices();

        vertices = meshBuilder.getMesh().vertices();
        faces = meshBuilder.getMesh().faces();
        edges = meshBuilder.getMesh().edges();
    }

    @Override
    public void setIsIllegalPredicate(Predicate<Pair<E, V>> isIllegalPredicate, Predicate<Triple<E, V, Double>> isIllegalPredicateWithEps) {
        this.isIllegalPredicate = isIllegalPredicate;
        this.isIllegalPredicateWithEps = isIllegalPredicateWithEps;
    }

    public void replacePoint(@NotNull final V vertex, @NotNull final IPoint point) {
        assert readOnlyTriConnectivity.ringContainsPoint(vertex, point);
        vertexBuilder.setPoint(vertex, point);
    }

    @Override
    public ITriangleMeshBuilder<V, E, F> getMeshBuilder() {
        return meshBuilder;
    }

    @Override
    public boolean isIllegal(@NotNull E edge, @NotNull V p) {
        return isIllegalPredicate.test(Pair.of(edge, p));
    }

    @Override
    public boolean isIllegal(@NotNull E edge, @NotNull V p, double eps) {
        return isIllegalPredicateWithEps.test(Triple.of(edge, p, eps));
    }

    @Override
    public boolean isIllegal(@NotNull final E edge) {
        return isIllegal(edge, vertices.getEndOf(edges.getNext(edge)));
    }

    @Override
    public boolean isIllegal(@NotNull final E edge, final double eps) {
        return isIllegal(edge, vertices.getEndOf(edges.getNext(edge)), eps);
    }

    @Override
    public boolean isDelaunayIllegal(@NotNull final E edge) {
        return isDelaunayIllegal(edge, vertices.getEndOf(edges.getNext(edge)));
    }

    @Override
    public boolean isDelaunayIllegal(@NotNull final E edge, @NotNull final V p, final double eps) {
        //assert mesh.getVertex(mesh.getNext(edge)).equals(p);
        //V p = mesh.getVertex(mesh.getNext(edge));
        E t0 = edges.getTwin(edge);
        E t1 = edges.getNext(t0);
        E t2 = edges.getNext(t1);

        V x = vertices.getEndOf(t0);
        V y = vertices.getEndOf(t1);
        V z = vertices.getEndOf(t2);

        //return Utils.angle3D(x, y, z) + Utils.angle3D(x, p, z) > Math.PI;

        //return Utils.isInCircumscribedCycle(x, y, z, p);
        //if(Utils.ccw(z,x,y) > 0) {
        return GeometryUtils.isInsideCircle(z, x, y, p, eps);
        //}
        //else {
        //	return Utils.isInsideCircle(x, z, y, p);
        //}
    }

    @Override
    public Pair<E, E> splitEdge(@NotNull V v, @NotNull E halfEdge, boolean legalize) {
        vertexBuilder.insert(v);

        /*
         * Situation: h0 = halfEdge
         * h1 -> h2 -> h0
         *       f0
         * o2 <- o1 <- o0
         *       f3
         *
         * After splitEdge:
         * h0 -> h1 -> t0
         *       f0
         * t1 <- h2 <- e0
         *       f1
         *
         * e1 -> o1 -> t2
         *       f2
         * o0 <- o2 <- e2
         *       f3
         */

        //h0,(t0),t1
        //e2,(o0,

        E h0 = halfEdge;
        E o0 = edges.getTwin(h0);

        V v2 = vertices.getEndOf(o0);
        F f0 = faces.getOf(h0);
        F f3 = faces.getOf(o0);

        // faces correct?
        //mesh.createEdge(v2, mesh.getFace(o0));

        E e1 = edgeBuilder.createAndInsert(v2, faces.getOf(o0));
        E t2 = null;
        E t1 = edgeBuilder.createAndInsert(v, faces.getOf(h0));

        vertexBuilder.setEdge(v, t1);
        edgeBuilder.setTwin(e1, t1);

        /*
         * These two operations are strongly connected.
         * Before these operations the vertex of o0 is v2.
         * If the edge of v2 is equal to o0, the edge becomes
         * invalid after calling mesh.setVertex(o0, v);
         */
        edgeBuilder.setVertex(o0, v);
        if (edges.getOf(v2).equals(o0)) {
            vertexBuilder.setEdge(v2, e1);
        }

        F f1 = null;
        if (!edges.isBoundary(h0)) {
            f1 = meshBuilder.faces().createAndInsert();

            E h1 = edges.getNext(h0);
            E h2 = edges.getNext(h1);

            V v1 = vertices.getEndOf(h1);
            E e0 = edgeBuilder.createAndInsert(v1, f1);
            E t0 = edgeBuilder.createAndInsert(v, f0);

            edgeBuilder.setTwin(e0, t0);

            faceBuilder.setEdge(f0, h0);
            faceBuilder.setEdge(f1, h2);

            edgeBuilder.setFace(h1, f0);
            edgeBuilder.setFace(t0, f0);
            edgeBuilder.setFace(h0, f0);

            edgeBuilder.setFace(h2, f1);
            edgeBuilder.setFace(t1, f1);
            edgeBuilder.setFace(e0, f1);

            edgeBuilder.setNext(h0, h1);
            edgeBuilder.setNext(h1, t0);
            edgeBuilder.setNext(t0, h0);

            edgeBuilder.setNext(e0, h2);
            edgeBuilder.setNext(h2, t1);
            edgeBuilder.setNext(t1, e0);
        } else {
            edgeBuilder.setNext(edges.getPrev(h0), t1);
            edgeBuilder.setNext(t1, h0);
        }

        F f2 = null;
        if (!edges.isBoundary(o0)) {
            E o1 = edges.getNext(o0);
            E o2 = edges.getNext(o1);

            V v3 = vertices.getEndOf(o1);
            f2 = faceBuilder.createAndInsert();

            // face
            E e2 = edgeBuilder.createAndInsert(v3, faces.getOf(o0));
            t2 = edgeBuilder.createAndInsert(v, f2);
            edgeBuilder.setTwin(e2, t2);

            faceBuilder.setEdge(f2, o1);
            faceBuilder.setEdge(f3, o0);

            edgeBuilder.setFace(o1, f2);
            edgeBuilder.setFace(t2, f2);
            edgeBuilder.setFace(e1, f2);

            edgeBuilder.setFace(o2, f3);
            edgeBuilder.setFace(o0, f3);
            edgeBuilder.setFace(e2, f3);

            edgeBuilder.setNext(e1, o1);
            edgeBuilder.setNext(o1, t2);
            edgeBuilder.setNext(t2, e1);

            edgeBuilder.setNext(o0, e2);
            edgeBuilder.setNext(e2, o2);
            edgeBuilder.setNext(o2, o0);
        } else {
            edgeBuilder.setNext(e1, edges.getNext(o0));
            edgeBuilder.setNext(o0, e1);
        }

        // Event after the mesh connectivity is valid!
        mesh.insertEvent(t1);
        if (!edges.isBoundary(h0)) {
            mesh.splitEdgeEvent(h0, f0, f0, f1, v);
        }

        if (!edges.isBoundary(o0)) {
            mesh.splitEdgeEvent(o0, f3, f3, f2, v);
        }


        if (legalize) {
            if (!edges.isBoundary(h0)) {
                E h1 = edges.getNext(h0);
                E h2 = edges.getPrev(t1);
                legalize(h1, v);
                legalize(h2, v);
            }

            if (!edges.isBoundary(o0)) {
                E o1 = edges.getNext(e1);
                E o2 = edges.getPrev(o0);
                legalize(o1, v);
                legalize(o2, v);
            }
        }

        return org.apache.commons.lang3.tuple.Pair.of(t1, t2);
    }

    @Override
    public Pair<E, E> splitEdge(@NotNull IPoint p, @NotNull E halfEdge, boolean legalize) {
        var vertexBuilder = getMeshBuilder().vertices();

        V v = vertexBuilder.create(p);
        return splitEdge(v, halfEdge, legalize);
    }

    @Override
    public List<E> splitEdgeAndReturn(@NotNull final V v, @NotNull E halfEdge, boolean legalize) {
        vertexBuilder.insert(v);

        /*
         * Situation: h0 = halfEdge
         * h1 -> h2 -> h0
         *       f0
         * o2 <- o1 <- o0
         *       f3
         *
         * After splitEdge:
         * h0 -> h1 -> t0
         *       f0
         * t1 <- h2 <- e0
         *       f1
         *
         * e1 -> o1 -> t2
         *       f2
         * o0 <- o2 <- e2
         *       f3
         */

        //h0,(t0),t1
        //e2,(o0,

        E h0 = halfEdge;
        E o0 = edges.getTwin(h0);

        V v2 = vertices.getEndOf(o0);
        F f0 = faces.getOf(h0);
        F f3 = faces.getOf(o0);

        // faces correct?
        //mesh.createEdge(v2, mesh.getFace(o0));
        E e1 = edgeBuilder.createAndInsert(v2, faces.getOf(o0));
        E t2 = null;
        E t1 = edgeBuilder.createAndInsert(v, faces.getOf(h0));
        vertexBuilder.setEdge(v, t1);

        edgeBuilder.setTwin(e1, t1);

        /*
         * These two operations are strongly connected.
         * Before these operations the vertex of o0 is v2.
         * If the edge of v2 is equal to o0, the edge becomes
         * invalid after calling mesh.setVertex(o0, v);
         */
        edgeBuilder.setVertex(o0, v);
        if (edges.getOf(v2).equals(o0)) {
            vertexBuilder.setEdge(v2, e1);
        }

        if (!edges.isBoundary(h0)) {
            F f1 = faceBuilder.createAndInsert();

            E h1 = edges.getNext(h0);
            E h2 = edges.getNext(h1);

            V v1 = vertices.getEndOf(h1);
            E e0 = edgeBuilder.createAndInsert(v1, f1);
            E t0 = edgeBuilder.createAndInsert(v, f0);

            edgeBuilder.setTwin(e0, t0);

            faceBuilder.setEdge(f0, h0);
            faceBuilder.setEdge(f1, h2);

            edgeBuilder.setFace(h1, f0);
            edgeBuilder.setFace(t0, f0);
            edgeBuilder.setFace(h0, f0);

            edgeBuilder.setFace(h2, f1);
            edgeBuilder.setFace(t1, f1);
            edgeBuilder.setFace(e0, f1);

            edgeBuilder.setNext(h0, h1);
            edgeBuilder.setNext(h1, t0);
            edgeBuilder.setNext(t0, h0);

            edgeBuilder.setNext(e0, h2);
            edgeBuilder.setNext(h2, t1);
            edgeBuilder.setNext(t1, e0);

            mesh.splitEdgeEvent(h0, f0, f0, f1, v);
        } else {
            edgeBuilder.setNext(edges.getPrev(h0), t1);
            edgeBuilder.setNext(t1, h0);
        }

        if (!edges.isBoundary(o0)) {
            E o1 = edges.getNext(o0);
            E o2 = edges.getNext(o1);

            V v3 = vertices.getEndOf(o1);
            F f2 = faceBuilder.createAndInsert();

            // face
            E e2 = edgeBuilder.createAndInsert(v3, faces.getOf(o0));
            t2 = edgeBuilder.createAndInsert(v, f2);
            edgeBuilder.setTwin(e2, t2);

            faceBuilder.setEdge(f2, o1);
            faceBuilder.setEdge(f3, o0);

            edgeBuilder.setFace(o1, f2);
            edgeBuilder.setFace(t2, f2);
            edgeBuilder.setFace(e1, f2);

            edgeBuilder.setFace(o2, f3);
            edgeBuilder.setFace(o0, f3);
            edgeBuilder.setFace(e2, f3);

            edgeBuilder.setNext(e1, o1);
            edgeBuilder.setNext(o1, t2);
            edgeBuilder.setNext(t2, e1);

            edgeBuilder.setNext(o0, e2);
            edgeBuilder.setNext(e2, o2);
            edgeBuilder.setNext(o2, o0);

            mesh.splitEdgeEvent(o0, f3, f3, f2, v);
        } else {
            edgeBuilder.setNext(e1, edges.getNext(o0));
            edgeBuilder.setNext(o0, e1);
        }

        List<E> toLegalize = new ArrayList<>(4);

        if (!edges.isBoundary(h0)) {
            E h1 = edges.getNext(h0);
            E h2 = edges.getPrev(t1);
            toLegalize.add(h1);
            toLegalize.add(h2);
        }

        if (!edges.isBoundary(o0)) {
            E o1 = edges.getNext(e1);
            E o2 = edges.getPrev(o0);
            toLegalize.add(o1);
            toLegalize.add(o2);
        }

        if (legalize) {
            if (!edges.isBoundary(h0)) {
                E h1 = edges.getNext(h0);
                E h2 = edges.getPrev(t1);
                legalize(h1, v);
                legalize(h2, v);
            }

            if (!edges.isBoundary(o0)) {
                E o1 = edges.getNext(e1);
                E o2 = edges.getPrev(o0);
                legalize(o1, v);
                legalize(o2, v);
            }
        }

        return toLegalize;
    }

    @Override
    public Pair<E, E> splitEdge(@NotNull final E halfEdge, final boolean legalize, @NotNull final Consumer<V> action) {
        VPoint midPoint = edges.toLine(halfEdge).midPoint();
        V v = vertexBuilder.create(midPoint.getX(), midPoint.getY());
        Pair<E, E> result = splitEdge(v, halfEdge, legalize);
        action.accept(v);
        return result;
    }

    @Override
    public void flipSync(@NotNull final E edge) {
        E a0 = edge;
        E a1 = edges.getNext(a0);
        E a2 = edges.getNext(a1);

        E b0 = edges.getTwin(edge);
        E b1 = edges.getNext(b0);

        V v1 = vertices.getEndOf(a0);
        V v2 = vertices.getEndOf(a1);
        V v3 = vertices.getEndOf(a2);
        V v4 = vertices.getEndOf(b1);

        // TODO: a very first and simple aquire all locks implementation => improve it
        while (true) {
            // lock all 4 involved vertices
            if (vertexBuilder.tryLock(v1)) {
                if (vertexBuilder.tryLock(v2)) {
                    if (vertexBuilder.tryLock(v3)) {
                        if (vertexBuilder.tryLock(v4)) {
                            break;
                        } else {
                            vertexBuilder.unlock(v3);
                            vertexBuilder.unlock(v2);
                            vertexBuilder.unlock(v1);
                        }
                    } else {
                        vertexBuilder.unlock(v2);
                        vertexBuilder.unlock(v1);
                    }
                } else {
                    vertexBuilder.unlock(v1);
                }
            }
        }

        try {
            // if everything is locked flip
            flip(edge);
        }
        // unlock all locks
        finally {
            vertexBuilder.unlock(v4);
            vertexBuilder.unlock(v3);
            vertexBuilder.unlock(v2);
            vertexBuilder.unlock(v1);
        }
    }

    @Override
    public void flip(@NotNull final E edge) {
        // 1. gather all the references required
        E a0 = edge;
        E a1 = edges.getNext(a0);
        E a2 = edges.getNext(a1);

        E b0 = edges.getTwin(edge);
        E b1 = edges.getNext(b0);
        E b2 = edges.getNext(b1);

        F fa = faces.getOf(a0);
        F fb = faces.getOf(b0);

        V va1 = vertices.getEndOf(a1);
        V vb1 = vertices.getEndOf(b1);

        V va0 = vertices.getEndOf(a0);
        V vb0 = vertices.getEndOf(b0);

        if (edges.getAnyOf(fb).equals(b1)) {
            faceBuilder.setEdge(fb, a1);
        }

        if (edges.getAnyOf(fa).equals(a1)) {
            faceBuilder.setEdge(fa, b1);
        }

        // TODO: maybe without if, just do it? its faster?
        assert vertices.getEndOf(b2) == va0;
        assert vertices.getEndOf(a2) == vb0;

        if (edges.getOf(va0).equals(a0)) {
            vertexBuilder.setEdge(va0, b2);
        }

        if (edges.getOf(vb0).equals(b0)) {
            vertexBuilder.setEdge(vb0, a2);
        }

        edgeBuilder.setVertex(a0, va1);
        edgeBuilder.setVertex(b0, vb1);

        edgeBuilder.setNext(a0, a2);
        edgeBuilder.setNext(a2, b1);
        edgeBuilder.setNext(b1, a0);

        edgeBuilder.setNext(b0, b2);
        edgeBuilder.setNext(b2, a1);
        edgeBuilder.setNext(a1, b0);

        edgeBuilder.setFace(a1, fb);
        edgeBuilder.setFace(b1, fa);

        mesh.flipEdgeEvent(fa, fb);
    }

    @Override
    public E splitTriangle(@NotNull final F face, final boolean legalize) {
        VPoint circumcenter = faces.toTriangle(face).getCircumcenter();
        return splitTriangle(face, mesh.createPoint(circumcenter.getX(), circumcenter.getY()), legalize);
    }

    @Override
    public void removeBoundaryVertex(@NotNull final V vertex) {
        assert vertices.isAtBoundary(vertex);
        F boundary = faces.getOf(vertex);
        E boundaryEdge = edges.getBoundaryEdge(vertex).get();
        E next = edges.getNext(boundaryEdge);
        E nnext = edges.getNext(next);

        List<E> ringEdges = edges
                .streamEdgesOf(vertex)
                .map(edge -> edges.getPrev(edge))
                .collect(Collectors.toList());

        var meshBuilder = getMeshBuilder();

        for (int i = 0; i < ringEdges.size() - 1; i++) {
            E edge = ringEdges.get(i);
            V v = vertices.getEndOf(edge);
            meshBuilder.edges().setNext(edge, ringEdges.get(i + 1));
            meshBuilder.edges().setFace(edge, boundary);
            // adjust since the edge is now a boundary edge!
            meshBuilder.vertices().setEdge(v, edge);
        }

        meshBuilder.edges().setNext(ringEdges.get(ringEdges.size() - 1), nnext);
    }

    @Override
    public void removeNonBoundaryVertex(@NotNull final V vertex) {
        assert !vertices.isAtBoundary(vertex);

        // (1) remove the vertex
        // get ringEdges in ccw order!
        List<E> ringEdges = edges
                .streamEdgesOf(vertex)
                .map(edge -> edges.getPrev(edge)).collect(Collectors.toList());

        F face = faces.getOf(ringEdges.get(ringEdges.size() - 1));

        for (int i = 0; i < ringEdges.size(); i++) {
            E edge = ringEdges.get(i);
            E next = edges.getNext(edge);
            E nextTwin = edges.getTwin(next);
            F f = faces.getOf(edge);

            edgeBuilder.destroy(next);
            edgeBuilder.destroy(nextTwin);
            if (i != ringEdges.size() - 1) {
                faceBuilder.destroy(f);
            }
        }

        for (int i = 0; i < ringEdges.size(); i++) {
            E edge = ringEdges.get(i);
            getMeshBuilder().edges().setNext(edge, ringEdges.get((i + 1) % ringEdges.size()));
            edgeBuilder.setFace(edge, face);

            E vEdge = edges.getOf(vertices.getEndOf(edge));
            if (edges.isDestroyed(vEdge)/* || !mesh.isAtBoundary(vEdge)*/) {
                vertexBuilder.setEdge(vertices.getEndOf(edge), edge);
            }
        }

        faceBuilder.setEdge(face, ringEdges.get(ringEdges.size() - 1));
        edgeBuilder.setFace(ringEdges.get(ringEdges.size() - 1), face);
        vertexBuilder.destroy(vertex);

        NodeLinkedList<GenEar<V, E, F>> list = new NodeLinkedList<>();
        GenEar.EarNodeComparator<V, E, F> comparator = new GenEar.EarNodeComparator<>();
        PriorityQueue<Node<GenEar<V, E, F>>> heap = new PriorityQueue<>(comparator);

        assert mesh.isValid();
        // (2) re-triangulate
        for (int i = 0; i < ringEdges.size(); i++) {
            E e1 = ringEdges.get(i % ringEdges.size());
            E e2 = ringEdges.get((i + 1) % ringEdges.size());
            E e3 = ringEdges.get((i + 2) % ringEdges.size());

            GenEar<V, E, F> ear = new GenEar<>(e1, e2, e3, power(e1, e2, e3, vertex));
            Node<GenEar<V, E, F>> earNode = list.add(ear);
            heap.add(earNode);

        }

        while (heap.size() > 3) {
            Node<GenEar<V, E, F>> earNode = heap.poll();
            GenEar<V, E, F> ear = earNode.getElement();

            // create triangle ear and link it to its two or three existing neighbors
            E e1 = ear.getEdges().get(0);
            E e2 = ear.getEdges().get(1);
            E e3 = ear.getEdges().get(2);
            E next = edges.getNext(e3);

            E e = edgeBuilder.createAndInsert(vertices.getEndOf(e1));
            E t = edgeBuilder.createAndInsert(vertices.getEndOf(e3));
            F f = faceBuilder.createAndInsert();
            F tf = faces.getOf(e1);

            faceBuilder.setEdge(f, e);
            edgeBuilder.setTwin(e, t);
            edgeBuilder.setNext(e, e2);
            edgeBuilder.setNext(e3, e);

            edgeBuilder.setFace(e, f);
            edgeBuilder.setFace(e2, f);
            edgeBuilder.setFace(e3, f);

            edgeBuilder.setNext(t, next);
            edgeBuilder.setNext(e1, t);
            edgeBuilder.setFace(t, tf);
            faceBuilder.setEdge(tf, t);
            // end

            if (heap.size() > 3) {
                Node<GenEar<V, E, F>> prevEarNode = earNode.getPrev();
                Node<GenEar<V, E, F>> nextEarNode = earNode.getNext();

                if (prevEarNode == null) {
                    prevEarNode = list.getTail();
                }

                if (nextEarNode == null) {
                    nextEarNode = list.getHead();
                }

                Node<GenEar<V, E, F>> nnextEarNode = nextEarNode.getNext();
                if (nnextEarNode == null) {
                    nnextEarNode = list.getHead();
                }


                heap.remove(earNode);
                heap.remove(prevEarNode);
                heap.remove(nextEarNode);
                heap.remove(nnextEarNode);

                prevEarNode.getElement().setLast(t);

                nextEarNode.getElement().setFirst(e1);
                nextEarNode.getElement().setMiddle(t);
                nnextEarNode.getElement().setFirst(t);
                earNode.remove();

                GenEar<V, E, F> prevEar = prevEarNode.getElement();
                GenEar<V, E, F> nextEar = nextEarNode.getElement();
                GenEar<V, E, F> nnextEar = nnextEarNode.getElement();
                prevEar.setPower(power(prevEar.getEdges().get(0), prevEar.getEdges().get(1), prevEar.getEdges().get(2), vertex));
                nextEar.setPower(power(nextEar.getEdges().get(0), nextEar.getEdges().get(1), nextEar.getEdges().get(2), vertex));
                nnextEar.setPower(power(nnextEar.getEdges().get(0), nnextEar.getEdges().get(1), nnextEar.getEdges().get(2), vertex));
                heap.add(prevEarNode);
                heap.add(nextEarNode);
                heap.add(nnextEarNode);
            }
        }
    }

    @Override
    public E splitTriangle(@NotNull F face, @NotNull final IPoint point, boolean legalize) {
        V p = vertexBuilder.create(point);
        return splitTriangle(face, p, legalize);
    }

    @Override
    public E splitTriangle(@NotNull F face, @NotNull final V p, boolean legalize) {
        vertexBuilder.insert(p);

        F xyp = faceBuilder.createAndInsert();
        F yzp = faceBuilder.createAndInsert();

        //F zxp = mesh.createFace();
        F zxp = face;

        E zx = edges.getAnyOf(face);
        E xy = edges.getNext(zx);
        E yz = edges.getNext(xy);

        V x = vertices.getEndOf(zx);
        V y = vertices.getEndOf(xy);
        V z = vertices.getEndOf(yz);

        E yp = edgeBuilder.createAndInsert(p, xyp);
        vertexBuilder.setEdge(p, yp);

        E py = edgeBuilder.createAndInsert(y, yzp);
        edgeBuilder.setTwin(yp, py);

        E xp = edgeBuilder.createAndInsert(p, zxp);
        E px = edgeBuilder.createAndInsert(x, xyp);
        edgeBuilder.setTwin(xp, px);

        E zp = edgeBuilder.createAndInsert(p, yzp);
        E pz = edgeBuilder.createAndInsert(z, zxp);
        edgeBuilder.setTwin(zp, pz);

        edgeBuilder.setNext(zx, xp);
        edgeBuilder.setNext(xp, pz);
        edgeBuilder.setNext(pz, zx);

        edgeBuilder.setNext(xy, yp);
        edgeBuilder.setNext(yp, px);
        edgeBuilder.setNext(px, xy);

        edgeBuilder.setNext(yz, zp);
        edgeBuilder.setNext(zp, py);
        edgeBuilder.setNext(py, yz);

        faceBuilder.setEdge(xyp, yp);
        faceBuilder.setEdge(yzp, py);
        faceBuilder.setEdge(zxp, xp);

        edgeBuilder.setFace(xy, xyp);
        edgeBuilder.setFace(zx, zxp);
        edgeBuilder.setFace(yz, yzp);


        // we reuse the face for efficiency
        //mesh.destroyFace(face);

        mesh.splitTriangleEvent(face, xyp, yzp, zxp, p);

        if (legalize) {
            legalize(zx, p);
            legalize(xy, p);
            legalize(yz, p);
        }

        return xp;
    }

    @Override
    public V collapseEdge(@NotNull final E edge, final boolean deleteIsolatededVertex) {
        E twin = edges.getTwin(edge);

        // before changing connectivity change vertices.
        V replacedVertex = vertices.getEndOf(twin);
        V survivedVertex = vertices.getEndOf(edge);

        for (E e : edges.iterableFor(replacedVertex)) {
            edgeBuilder.setVertex(e, survivedVertex);
        }

        if (edges.getOf(survivedVertex).equals(edge)) {
            vertexBuilder.setEdge(survivedVertex, edges.getTwin(edges.getNext(edge)));
        }

        F fa = faces.getOf(edge);
        F fb = faces.getOf(twin);

        F f4 = faces.getTwin(edges.getPrev(edge));
        F f5 = faces.getTwin(edges.getNext(twin));

        boolean isF4Boundary = faces.isBoundary(f4);
        boolean isF5Boundary = faces.isBoundary(f5);

        // survives
        E aNext = edges.getNext(edge);
        E bNext = edges.getNext(twin);
        E aPrev = edges.getPrev(edge);
        // survives
        E bPrev = edges.getPrev(twin);

        if (!edges.isBoundary(edge)) {
            E aPrevTwin = edges.getTwin(aPrev);
            E aPrevTwinPrev = edges.getPrev(aPrevTwin);
            E aPrevTwinNext = edges.getNext(aPrevTwin);
            E next = edges.getNext(edge);
            V nextVertex = vertices.getEndOf(next);

            if (edges.getOf(nextVertex).equals(aPrevTwin)) {
                vertexBuilder.setEdge(nextVertex, next);
            }

            // adjust pointers
            edgeBuilder.setNext(aNext, aPrevTwinNext);
            edgeBuilder.setPrev(aNext, aPrevTwinPrev);
            edgeBuilder.setFace(aNext, f4);
            faceBuilder.setEdge(f4, aNext);

            // destroy the rest
            faceBuilder.destroy(fa);
            edgeBuilder.destroy(aPrev);
            edgeBuilder.destroy(aPrevTwin);
        } else {
            edgeBuilder.setNext(edges.getPrev(edge), edges.getNext(edge));
        }

        if (!edges.isBoundary(twin)) {
            E bNextTwin = edges.getTwin(bNext);
            E bNextTwinNext = edges.getNext(bNextTwin);
            E bNextTwinPrev = edges.getPrev(bNextTwin);
            E prevTwin = edges.getTwin(edges.getPrev(twin));
            V nextVertex = vertices.getEndOf(prevTwin);

            if (edges.getOf(nextVertex).equals(bNext)) {
                vertexBuilder.setEdge(nextVertex, prevTwin);
            }

            // adjust pointers
            edgeBuilder.setNext(bPrev, bNextTwinNext);
            edgeBuilder.setPrev(bPrev, bNextTwinPrev);
            edgeBuilder.setFace(bPrev, f5);
            faceBuilder.setEdge(f5, bPrev);

            // destroy the rest
            faceBuilder.destroy(fb);
            edgeBuilder.destroy(bNext);
            edgeBuilder.destroy(bNextTwin);
        } else {
            edgeBuilder.setNext(edges.getPrev(twin), edges.getNext(twin));
        }

        // destroy the rest
        edgeBuilder.destroy(edge);
        edgeBuilder.destroy(twin);

        if (deleteIsolatededVertex) {
            vertexBuilder.destroy(replacedVertex);
        } else {
            vertexBuilder.setEdge(replacedVertex, null);
        }

        adjustVertex(survivedVertex);
        assert mesh.isValid();

        return survivedVertex;
    }

    @Override
    public void collapse3DVertex(@NotNull final V vertex, final boolean deleteIsolatededVertex) {
        assert vertices.degree(vertex) == 3;
        Optional<E> toDeleteEdge = edges.streamEdgesOf(vertex).filter(e -> !edges.isAtBoundary(e)).findAny();

        assert toDeleteEdge.isPresent();

        if (toDeleteEdge.isPresent()) {
            E edge = toDeleteEdge.get();
            assert edges.streamEdgesOf(vertex).filter(e -> edges.isAtBoundary(e)).count() == 2;

            E halfEdge = edge;
            if (!vertices.getEndOf(halfEdge).equals(vertex)) {
                halfEdge = edges.getTwin(halfEdge);
            }

            if (!vertices.getEndOf(halfEdge).equals(vertex)) {
                throw new IllegalArgumentException(halfEdge + " does not end in " + vertex + ".");
            }

            removeSimpleLink(edge);
            remove2DVertex(vertex, deleteIsolatededVertex);
        } else {
            log.warn("Did not found any non-boundary half-edge. Something went wrong!");
        }
    }

    @Override
    public void collapse4DVertex(@NotNull final V vertex, final boolean deleteIsolatededVertex) {
        assert vertices.degree(vertex) == 4;

        E edge = edges.getOf(vertex);
        E opp = edges.getNext(edges.getTwin(edges.getNext(edge)));

        F f1 = removeSimpleLink(edge);
        F f2 = removeSimpleLink(opp);
        remove2DVertex(vertex, deleteIsolatededVertex);
    }

    @Override
    public E remove2DVertex(@NotNull final V vertex, final boolean deleteIsolatededVertex) {
        assert vertices.degree(vertex) == 2;

        E survivor = edges.getOf(vertex);
        E next = edges.getNext(survivor);

        E twin = edges.getTwin(survivor);
        E twinPrev = edges.getPrev(twin);
        edgeBuilder.setNext(survivor, edges.getNext(next));
        edgeBuilder.setPrev(twin, edges.getPrev(twinPrev));

        if (edges.getAnyOf(faces.getOf(survivor)).equals(next)) {
            faceBuilder.setEdge(faces.getOf(survivor), survivor);
        }

        if (edges.getAnyOf(faces.getOf(twin)).equals(twinPrev)) {
            faceBuilder.setEdge(faces.getOf(twin), twin);
        }

        edgeBuilder.setVertex(survivor, vertices.getEndOf(next));

        if (edges.getOf(vertices.getEndOf(next)).equals(next)) {
            vertexBuilder.setEdge(vertices.getEndOf(next), survivor);
        }

        edgeBuilder.destroy(twinPrev);
        edgeBuilder.destroy(next);

        if (deleteIsolatededVertex) {
            vertexBuilder.destroy(vertex);
        }

        return survivor;
    }

    @Override
    public F createFaceAtBoundary(@NotNull final E boundaryEdge) {
        assert edges.isBoundary(boundaryEdge);

        IMeshBuilder<V, E, F> meshBuilder = getMeshBuilder();
        var vertexBuilder = meshBuilder.vertices();
        var edgeBuilder = meshBuilder.edges();
        var faceBuilder = meshBuilder.faces();

        F boundary = faces.getOf(boundaryEdge);
        E next = edges.getNext(boundaryEdge);
        E prev = edges.getPrev(boundaryEdge);

        // can we form a triangle
        assert GeometryUtils.isCCW(edges.endToPoint(prev), edges.endToPoint(boundaryEdge), edges.endToPoint(next));

        E nnext = edges.getNext(next);

        E newEdge = edgeBuilder.createAndInsert(vertices.getEndOf(next));
        E newTwin = edgeBuilder.createAndInsert(vertices.getEndOf(prev));
        F newFace = faceBuilder.createAndInsert();

        edgeBuilder.setFace(newEdge, boundary);
        edgeBuilder.setNext(newEdge, nnext);
        edgeBuilder.setPrev(newEdge, prev);
        edgeBuilder.setTwin(newEdge, newTwin);

        edgeBuilder.setFace(newTwin, newFace);
        edgeBuilder.setNext(newTwin, boundaryEdge);
        edgeBuilder.setPrev(newTwin, next);
        edgeBuilder.setTwin(newTwin, newEdge);

        faceBuilder.setEdge(newFace, newTwin);

        //mesh.setPrev(nnext, newEdge);

        //mesh.setNext(next, newTwin);
        //mesh.setFace(newEdge, newFace);

        //mesh.setPrev(boundaryEdge, newTwin);
        edgeBuilder.setFace(boundaryEdge, newFace);
        edgeBuilder.setFace(next, newFace);

        //mesh.setNext(prev, newEdge);

        if(edges.getAnyOf(boundary).equals(boundaryEdge) || edges.getAnyOf(boundary).equals(next)) {
            faceBuilder.setEdge(boundary, newEdge);
        }

        // to find the boundary edge as quick as possible
        vertexBuilder.setEdge(vertices.getEndOf(next), newEdge);

        assert vertices.getAllOf(newFace).size() == 3;
        return newFace;
    }

    @Override
    public void legalizeNonRecursive(@NotNull final E edge, @NotNull final V p) {
        int flips = 0;
        int its = 0;
        //if(isIllegal(edge, p)) {

        // this should be the same afterwards
        //E halfEdge = getMesh().getNext(edge);

        E startEdge = edges.getPrev(edge);
        E endEdge = edges.getTwin(edges.getPrev(startEdge));
        E currentEdge = edges.getPrev(edge);

        // flipp
        //c.prev.twin

        while(currentEdge != endEdge) {
            while (isIllegal(edges.getNext(currentEdge), p)) {
                flip(edges.getNext(currentEdge));
                flips++;
                its++;
            }
            its++;

            currentEdge = edges.getTwin(edges.getPrev(currentEdge));
        }

        //log.info("#flips = " + flips);
        //log.info("#its = " + its);
        //}
    }

    @Override
    public void legalize(@NotNull  final E edge) {
        legalizeNonRecursive(edge, vertices.getEndOf(edges.getNext(edge)));
    }

   @Override
    public boolean isFlipOkAssertion(@NotNull final E halfEdge) {
        if(edges.isBoundary(halfEdge)) {
            return false;
        }
        else {
            E xy = halfEdge;
            E yx = edges.getTwin(halfEdge);

            if(vertices.getEndOf(edges.getNext(xy)).equals(vertices.getEndOf(edges.getNext(yx)))) {
                return false;
            }

            V vertex = vertices.getEndOf(edges.getNext(yx));
            for(E neigbhour : edges.getIncidentEdgesIt(edges.getNext(xy))) {

                if(vertices.getEndOf(neigbhour).equals(vertex)) {
                    return false;
                }
            }
        }
        return true;
    }

   @Override
    public void smoothBorder() {
       for(E edge : edges.getAllOf(faces.getOuterBorder())) {
            if(edges.isBorder(edge)) {

                VPoint p = edges.endToPoint(edge);
                VPoint q = edges.endToPoint(edges.getNext(edge));
                VPoint r = edges.endToPoint(edges.getPrev(edge));

                if(GeometryUtils.isCCW(r, p, q)) {
                    double angle = GeometryUtils.angle(r, p, q);
                    if(angle < 0.5*Math.PI) {
                        createFaceAtBoundary(edge);
                    }
                }
            }
        }
    }

    @Override
    public void smoothHoles(@NotNull final IDistanceFunction distanceFunction, Predicate<V> isBoundary) {
        for(F hole : faces.getHoles()) {
            for(E edge : edges.getAllOf(hole)) {

                /*
                 * to avoid duplicated smoothing
                 */
                if(faces.getOf(edge).equals(hole)) {
                    V vp = vertices.getEndOf(edge);
                    if(!isBoundary.test(vp)) {
                        VPoint r = edges.endToPoint(edges.getPrev(edge));
                        VPoint p = edges.endToPoint(edge);
                        VPoint q = edges.endToPoint(edges.getNext(edge));
                        VPoint midPoint = new VLine(r, q).midPoint();

                        if((distanceFunction.apply(p) + distanceFunction.apply(midPoint) < 0) && GeometryUtils.isCCW(r, p, q)) {
                            double angle = GeometryUtils.angle(r, p, q);
                            if(angle < 0.5*Math.PI) {
                                createFaceAtBoundary(edge);
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void collapseHoleFaces(@NotNull final Predicate<F> collapsePredicate, @NotNull final Predicate<E> edgeCollapsePredicate, @NotNull final Consumer<V> action) {
        for(F hole : faces.getHoles()) {
            for(E edge : edges.getAllOf(hole)) {
                E twin = edges.getTwin(edge);
                F face = faces.getOf(twin);

                assert !faces.isBoundary(face);

                /*
                 * to avoid duplicated smoothing
                 */
                if(faces.getOf(edge).equals(hole) &&
                        !edges.isAtBoundary(edges.getNext(twin)) && !edges.isAtBoundary(edges.getPrev(twin)) &&
                        collapsePredicate.test(face)) {

                    V vr = vertices.getEndOf(edges.getPrev(edge));
                    V vp = vertices.getEndOf(edges.getNext(twin));
                    V vq = vertices.getEndOf(edge);


                    if(edgeCollapsePredicate.test(edges.getNext(twin))) {
                        VPoint r = vertices.toPoint(vr);
                        VPoint q = vertices.toPoint(vq);

                        VPoint midPoint = new VLine(r, q).midPoint();
                        removeFaceAtBoundary(face, hole,true);
                        vertexBuilder.setPoint(vp, midPoint);
                        action.accept(vp);
                    }
                }
            }
        }
    }

    @Override
    public void collapseBorderFaces(@NotNull final Predicate<F> collapsePredicate, @NotNull final Predicate<E> edgeCollapsePredicate, @NotNull final Consumer<V> action) {
        for(E edge : edges.getAllOf(faces.getOuterBorder())) {
            E twin = edges.getTwin(edge);
            F face = faces.getOf(twin);

            assert !faces.isBoundary(face);

            /*
             * to avoid duplicated smoothing
             */
            if(faces.getOf(edge).equals(faces.getOuterBorder()) &&
                    !edges.isAtBoundary(edges.getNext(twin)) && !edges.isAtBoundary(edges.getPrev(twin)) &&
                    collapsePredicate.test(face)) {

                V vr = vertices.getEndOf(edges.getPrev(edge));
                V vp = vertices.getEndOf(edges.getNext(twin));
                V vq = vertices.getEndOf(edge);


                if(edgeCollapsePredicate.test(edges.getNext(twin))) {
                    VPoint r = vertices.toPoint(vr);
                    VPoint q = vertices.toPoint(vq);

                    VPoint midPoint = new VLine(r, q).midPoint();
                    removeFaceAtBoundary(face, faces.getOuterBorder(), true);
                    vertexBuilder.setPoint(vp, midPoint);
                    action.accept(vp);
                }
            }
        }
    }

    @Override
    public void smoothBorder(@Nullable final IDistanceFunction distanceFunction, @NotNull final Predicate<V> isBoundary) {
        for(E edge : edges.getAllOf(faces.getOuterBorder())) {

            /*
             * to avoid duplicated smoothing
             */
            if(faces.getOf(edge).equals(faces.getOuterBorder())) {
                V vr = vertices.getEndOf(edges.getPrev(edge));
                V vp = vertices.getEndOf(edge);
                V vq = vertices.getEndOf(edges.getNext(edge));

                if(!isBoundary.test(vp)) {
                    VPoint r = vertices.toPoint(vr);
                    VPoint p = vertices.toPoint(vp);
                    VPoint q = vertices.toPoint(vq);


                    VPoint midPoint = new VLine(r, q).midPoint();
                    if(distanceFunction != null && (distanceFunction.apply(p) + distanceFunction.apply(midPoint) < 0)) {
                        if(GeometryUtils.isCCW(r, p, q)) {
                            double angle = GeometryUtils.angle(r, p, q);
                            if(angle < 0.5*Math.PI) {
                                //System.out.println(triangle);
                                F newFace = createFaceAtBoundary(edge);
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void smoothBorder(@NotNull final IDistanceFunction distanceFunction) {
        for(E edge : edges.getAllOf(faces.getOuterBorder())) {

            /*
             * to avoid duplicated smoothing
             */
            if(faces.getOf(edge).equals(faces.getOuterBorder())) {
                VPoint r = edges.endToPoint(edges.getPrev(edge));
                VPoint p = edges.endToPoint(edge);
                VPoint q = edges.endToPoint(edges.getNext(edge));

                VPoint midPoint = new VLine(r, q).midPoint();

                VTriangle triangle = new VTriangle(r,p,q);
                if((distanceFunction.apply(p) + distanceFunction.apply(midPoint) < 0) && GeometryUtils.isCCW(r, p, q)) {
                    double angle = GeometryUtils.angle(r, p, q);
                    if(angle < 0.5*Math.PI) {
                        //System.out.println(triangle);
                        F newFace = createFaceAtBoundary(edge);
                    }
                }
            }
        }
    }

    @Override
    public void remove(@NotNull final V vertex) {
        if (vertices.isAtBoundary(vertex)) {
            removeBoundaryVertex(vertex);
        } else {
            removeNonBoundaryVertex(vertex);
        }
    }

    private double power(@NotNull final E e1, @NotNull final E e2, @NotNull final E e3, @NotNull final IPoint p) {
        IPoint point = edges.getMutableEndPoint(e1);
        if (!readOnlyTriConnectivity.isLeftOf(point.getX(), point.getY(), e3)) {
            return Double.MAX_VALUE;
        }

        VPoint p1 = edges.endToPoint(e1);
        VPoint p2 = edges.endToPoint(e2);
        VPoint p3 = edges.endToPoint(e3);
        VTriangle triangle = new VTriangle(p1, p2, p3);
        VPoint x = triangle.getCircumcenter();
        double r = triangle.getCircumscribedRadius();
        double xpSq = x.distanceSq(p);
        double power = (xpSq - r * r);
        return -power;
    }
}
