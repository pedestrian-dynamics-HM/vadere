package org.vadere.meshing.mesh.inter.mesh;

import org.jetbrains.annotations.NotNull;

public final class MeshUtils {
    /**
     * Creates a very simple mesh consisting of two triangles ((-100, 0), (100, 0), (0, 1)) and ((0, -1), (-100, 0), (100, 0))
     *
     * @param mesh  the mesh used to create the triangle. This mesh should be empty.
     * @param <V>   the type of the vertex
     * @param <E>   the type of the edge
     * @param <F>   the type of the face
     */
   public static <V extends IVertex, E extends IHalfEdge, F extends IFace> void createSimpleTriMesh(
            @NotNull final IMesh<V, E, F> mesh
    ) {
        F face1;
        F face2;
        F border;
        V x, y, z, w;
        E zx ;
        E xy;
        E yz;

        E wx;
        E xz;
        E yw;
        E zy;

        border = mesh.getBorder();

        // first triangle xyz
        face1 = mesh.createFace();
        x = mesh.insertVertex(-100, 0);
        y = mesh.insertVertex(100, 0);
        z = mesh.insertVertex(0, 1);

        zx = mesh.createEdge(x, face1);
        mesh.setEdge(x, zx);
        xy = mesh.createEdge(y, face1);
        mesh.setEdge(y, xy);
        yz = mesh.createEdge(z, face1);
        mesh.setEdge(z, yz);

        mesh.setNext(zx, xy);
        mesh.setNext(xy, yz);
        mesh.setNext(yz, zx);

        mesh.setEdge(face1, xy);


        // second triangle yxw
        face2 = mesh.createFace();
        w = mesh.insertVertex(0, -1);

        E yx = mesh.createEdge(x, face2);
        E xw = mesh.createEdge(w, face2);
        E wy = mesh.createEdge(y, face2);

        mesh.setNext(yx, xw);
        mesh.setNext(xw, wy);
        mesh.setNext(wy, yx);

        mesh.setEdge(face2, yx);

        mesh.setTwin(xy, yx);

        // border twins
        zy = mesh.createEdge(y, border);
        xz = mesh.createEdge(z, border);

        mesh.setTwin(yz, zy);
        mesh.setTwin(zx, xz);

        wx = mesh.createEdge(x, border);
        yw = mesh.createEdge(w, border);
        mesh.setEdge(w, yw);

        mesh.setEdge(border, wx);
        mesh.setTwin(xw, wx);
        mesh.setTwin(wy, yw);


        mesh.setNext(zy, yw);
        mesh.setNext(yw, wx);
        mesh.setNext(wx, xz);
        mesh.setNext(xz, zy);
    }
}
