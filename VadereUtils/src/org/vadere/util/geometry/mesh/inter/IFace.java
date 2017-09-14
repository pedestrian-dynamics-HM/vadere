package org.vadere.util.geometry.mesh.inter;

import org.vadere.util.geometry.mesh.gen.*;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VPoint;

/**
 * @author Benedikt Zoennchen
 * @param <P> the type of the vertices
 */
public interface IFace<P extends IPoint> {

    static AMesh<VPoint> createSimpleTriMesh() {
        AMesh<VPoint> mesh;
        AFace<VPoint> face1;
        AFace<VPoint> face2;
        AFace<VPoint> border;
        AVertex<VPoint> x, y, z, w;
        AHalfEdge<VPoint> zx ;
        AHalfEdge<VPoint> xy;
        AHalfEdge<VPoint> yz;

        AHalfEdge<VPoint> wx;
        AHalfEdge<VPoint> xz;
        AHalfEdge<VPoint> yw;
        AHalfEdge<VPoint> zy;

        mesh = new AMesh<>((x1, y1) -> new VPoint(x1, y1));
        border = mesh.createFace(true);

        // first triangle xyz
        face1 = mesh.createFace();
        x = mesh.insertVertex(0, 0);
        y = mesh.insertVertex(3, 0);
        z = mesh.insertVertex(1.5,3.0);

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
        w = mesh.insertVertex(1.5,-1.5);

        AHalfEdge<VPoint> yx = mesh.createEdge(x, face2);
        AHalfEdge<VPoint> xw = mesh.createEdge(w, face2);
        AHalfEdge<VPoint> wy = mesh.createEdge(y, face2);

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

        return mesh;
    }
}
