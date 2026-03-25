package org.vadere.meshing.mesh.gen.mesh;

import org.vadere.meshing.mesh.inter.mesh.*;
import org.vadere.meshing.mesh.inter.meshConnectivity.IReadOnlyPolyConnectivity;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VPoint;

public abstract class MeshBase<V extends IVertex, E extends IHalfEdge, F extends IFace,
        Vertices extends IMeshVertices<V, E, F>,
        Edges extends IMeshEdges<V, E, F>,
        Faces extends IMeshFaces<V, E, F>
        > implements IMesh<V, E, F> {

    private final ReadOnlyPolyConnectivity<V, E, F> connectivity;

    protected MeshBase(Vertices vertices, Edges edges, Faces faces) {
        this.faces = faces;
        this.edges = edges;
        this.vertices = vertices;
        this.connectivity = new ReadOnlyPolyConnectivity<>(this);
    }

    Faces faces;
    Edges edges;
    Vertices vertices;

    protected abstract void clear();

    @Override
    public Faces faces() {
        return faces;
    }

    @Override
    public Edges edges() {
        return edges;
    }

    @Override
    public Vertices vertices() {
        return vertices;
    }

    @Override
    public IReadOnlyPolyConnectivity<V, E, F> readConnectivity() {
        return connectivity;
    }

    @Override
    public IPoint createPoint(double x, double y) {
        return new VPoint(x, y);
    }
}
