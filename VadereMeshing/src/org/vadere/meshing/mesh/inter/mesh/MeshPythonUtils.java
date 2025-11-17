package org.vadere.meshing.mesh.inter.mesh;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vadere.meshing.mesh.inter.mesh.builder.IMeshBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class MeshPythonUtils {
    public static <V extends IVertex, E extends IHalfEdge, F extends IFace> String toPythonValues(IMesh<V, E, F> mesh, @NotNull final Function<V, Double> evalPoint) {
        StringBuilder builder = new StringBuilder();
        List<V> vertices = mesh.getVertices();
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
     * @param doubleValuePropName the property name to extract double values from vertices.
     *
     * @return a string representing the mesh
     */
    public static <V extends IVertex, E extends IHalfEdge, F extends IFace> String toPythonTriangulation(IMeshWithDataStorage<V, E, F> meshWithDataStorage, String doubleValuePropName) {
        return toPythonTriangulation(meshWithDataStorage, v -> meshWithDataStorage.getDataStorage().getDoubleData(v, doubleValuePropName));
    }

    /**
     * Constructs and returns a string which can be used to construct a matplotlib Triangulation
     * which is helpful to plot the mesh.
     *
     * @param evalPoint a function to extract double values from vertices.
     *
     * @return a string representing the mesh
     */
    public static <V extends IVertex, E extends IHalfEdge, F extends IFace> String toPythonTriangulation(IMeshWithDataStorage<V, E, F> toPrint, @Nullable final Function<V, Double> evalPoint) {
        // todo hh: improve after making mesh immutable
        IMeshBuilder<V, E, F> meshBuilder = toPrint.toMutableMesh();
        meshBuilder.getOptimizer().garbageCollection();

        IMeshWithDataStorage<V, E, F> meshWithDataStorage = meshBuilder.build();
        IMesh<V, E, F> mesh = meshWithDataStorage.getMesh();

        StringBuilder builder = new StringBuilder();
        List<V> vertices = mesh.getVertices();
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
        List<F> faces = mesh.getFaces();
        builder.append("TRIS.append([");
        for(F face : faces) {
            builder.append("[");
            for(V v : mesh.getVertexIt(face)) {
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
}
