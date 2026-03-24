package org.vadere.meshing.mesh.inter.mesh.triangle;

import org.apache.commons.lang3.tuple.Triple;
import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.inter.mesh.*;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VTriangle;

import java.util.List;

public interface ITriangleMeshFaces<V extends IVertex, E extends IHalfEdge, F extends IFace> extends IMeshFaces<V, E, F> {

    /**
     * Returns a immutable triangle {@link VTriangle} by transforming the face to a triangle.
     * Assumption: The face represents a triangle, i.e. it has exactly 3 distinct points. This
     * requires O(1) time.
     *
     * @param face the face.
     * @return a immutable triangle {@link VTriangle} representing the face
     */
    default VTriangle toTriangle(@NotNull final F face) {
        List<V> vertices = base().vertices().getAllOf(face); // TODO speed up by avoiding the creation of a list!
        assert vertices.size() == 3 : "number of vertices of " + face + " is " + vertices.size();
        return new VTriangle(new VPoint(vertices.get(0)), new VPoint(vertices.get(1)), new VPoint(vertices.get(2)));
    }

    /**
     * Returns the midpoint {@link VPoint} of a triangle defined by the face.
     * Assumption: The face represents a triangle, i.e. it has exactly 3 distinct points. This
     * requires O(1) time.
     *
     * @param face the face.
     * @return the midpoint {@link VPoint} of a triangle defined by the face.
     */
    default VPoint toTriangleMidpoint(@NotNull final F face) {
        var vertices = base().vertices();
        var edges = base().edges();
        assert vertices.getAllOf(face).size() == 3 : "number of vertices of " + face + " is " + vertices.getAllOf(face).size();
        E e1 = edges.getAnyOf(face);
        E e2 = edges.getNext(e1);
        E e3 = edges.getPrev(e1);
        V v1 = vertices.getEndOf(e1);
        V v2 = vertices.getEndOf(e2);
        V v3 = vertices.getEndOf(e3);
        return GeometryUtils.getTriangleMidpoint(v1.getX(), v1.getY(), v2.getX(), v2.getY(), v3.getX(), v3.getY());
    }

    default VPoint toTriangleCircumcenter(@NotNull final F face) {
        var vertices = base().vertices();
        assert vertices.getAllOf(face).size() == 3 : "number of vertices of " + face + " is " + vertices.getAllOf(face).size();
        E edge = base().edges().getAnyOf(face);
        V v1 = vertices.getEndOf(edge);
        V v2 = vertices.getEndOf(base().edges().getNext(edge));
        V v3 = vertices.getEndOf(base().edges().getPrev(edge));
        return GeometryUtils.getCircumcenter(v1.getX(), v1.getY(), v2.getX(), v2.getY(), v3.getX(), v3.getY());
    }

    /**
     * Returns a triple {@link Triple} which represents a triangle by transforming the face to a triangle.
     * Assumption: The face represents a triangle, i.e. it has exactly 3 distinct points. This requires O(1) time.
     *
     * @param face the face.
     * @return a triple {@link Triple} representing the face
     */
    default Triple<IPoint, IPoint, IPoint> toTriple(@NotNull final F face) {
        var verticesOfMesh = base().vertices();

        List<V> vertices = verticesOfMesh.getAllOf(face);
        assert vertices.size() == 3;

        return Triple.of(
                verticesOfMesh.toMutablePoint(vertices.get(0)),
                verticesOfMesh.toMutablePoint(vertices.get(1)),
                verticesOfMesh.toMutablePoint(vertices.get(2))
        );
    }
}
