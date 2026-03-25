package org.vadere.meshing.mesh.inter.mesh;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.inter.mesh.builder.IMeshBuilderEdges;
import org.vadere.meshing.mesh.inter.mesh.builder.IMeshBuilderFaces;
import org.vadere.meshing.mesh.inter.mesh.builder.IMeshBuilderVertices;
import org.vadere.meshing.mesh.inter.mesh.builder.ITriangleMeshBuilder;

public final class MeshUtils {
    /**
     * Creates a very simple mesh consisting of two triangles ((-100, 0), (100, 0), (0, 1)) and ((0, -1), (-100, 0), (100, 0))
     *
     *       z
     *      / \
     *     /   \
     *   x ----- y
     *     \   /
     *      \ /
     *       w
     *
     * @param meshBuilder the mesh used to create the triangle. This mesh should be empty.
     * @param <V>         the type of the vertex
     * @param <E>         the type of the edge
     * @param <F>         the type of the face
     */
    public static <V extends IVertex, E extends IHalfEdge, F extends IFace> void createSimpleTriMesh(
            @NotNull final ITriangleMeshBuilder<V, E, F>  meshBuilder
    ) {
        V x, y, z, w;

        F border = meshBuilder.getMesh().faces().getOuterBorder();

        // first triangle xyz
        IMeshBuilderFaces<V, E, F> faces = meshBuilder.faces();
        IMeshBuilderVertices<V, E, F> vertices = meshBuilder.vertices();
        IMeshBuilderEdges<V, E, F> edges = meshBuilder.edges();

        F face1 = faces.createAndInsert();

        x = vertices.createAndInsert(-100, 0);
        y = vertices.createAndInsert(100, 0);
        z = vertices.createAndInsert(0, 1);

        E zx = edges.createAndInsert(x, face1);
        vertices.setEdge(x, zx);
        E xy = edges.createAndInsert(y, face1);
        vertices.setEdge(y, xy);
        E yz = edges.createAndInsert(z, face1);
        vertices.setEdge(z, yz);

        edges.setNext(zx, xy);
        edges.setNext(xy, yz);
        edges.setNext(yz, zx);

        faces.setEdge(face1, xy);


        // second triangle yxw
        F face2 = faces.createAndInsert();
        w = vertices.createAndInsert(0, -1);

        E yx = edges.createAndInsert(x, face2);
        E xw = edges.createAndInsert(w, face2);
        E wy = edges.createAndInsert(y, face2);

        edges.setNext(yx, xw);
        edges.setNext(xw, wy);
        edges.setNext(wy, yx);

        faces.setEdge(face2, yx);

        edges.setTwin(xy, yx); // edge shared by both triangles

        // construct border
        E zy = edges.createAndInsert(y, border);
        E xz = edges.createAndInsert(z, border);

        edges.setTwin(yz, zy);
        edges.setTwin(zx, xz);

        E wx = edges.createAndInsert(x, border);
        E yw = edges.createAndInsert(w, border);
        vertices.setEdge(w, yw);

        faces.setEdge(border, wx);
        edges.setTwin(xw, wx);
        edges.setTwin(wy, yw);


        edges.setNext(zy, yw);
        edges.setNext(yw, wx);
        edges.setNext(wx, xz);
        edges.setNext(xz, zy);
    }
}
