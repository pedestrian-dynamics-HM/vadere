package org.vadere.meshing.mesh.gen.mesh.arrayBased.elements;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.gen.mesh.MeshBuilderBase;
import org.vadere.meshing.mesh.gen.mesh.arrayBased.AMeshDataStorage;
import org.vadere.meshing.mesh.inter.mesh.builder.MeshBuilderFacesBase;

public class AMeshBuilderFaces<Vertices extends AMeshVertices, Edges extends AMeshEdges, Faces extends AMeshFaces,
        Mesh extends ArrayBasedMesh<Vertices,Edges,Faces>> extends MeshBuilderFacesBase<AVertex, AHalfEdge, AFace, Mesh> {
    private final MeshBuilderBase<AVertex, AHalfEdge, AFace, AMeshDataStorage, Mesh> parent;
    private final AMeshFaces faces;

    public AMeshBuilderFaces(MeshBuilderBase<AVertex, AHalfEdge, AFace, AMeshDataStorage, Mesh> parent) {
        super(parent);
        this.parent = parent;
        faces = this.parent.getMesh().faces();
    }

    @Override
    public AFace createAndInsert() {
        return createAndInsert(false);
    }

    @Override
    public AFace createAndInsertHole() {
        return createAndInsert(true);
    }

    private AFace createAndInsert(boolean hole) {
        int id = faces.items.size();
        AFace face = new AFace(id, -1, hole);
        faces.items.add(face);
        parent.getDataStorage().onFaceCreated();

        if(!hole) {
            faces.numberOfFaces++;
        }
        else {
            faces.holes.add(face);
            faces.numberOfHoles++;
        }
        return face;
    }

    @Override
    public void setEdge(@NotNull AFace face, @NotNull AHalfEdge edge) {
        face.setEdge(edge.getId());
    }

    @Override
    public void convertToHole(@NotNull AFace face) {
        assert !faces.isDestroyed(face);
        if(!faces.isHole(face)) {
            faces.holes.add(face);
            face.setBorder(true);
            faces.numberOfHoles++;
            faces.numberOfFaces--;
        }
    }

    @Override
    public void destroy(@NotNull final AFace face) {
        if (!faces.isDestroyed(face)) {
            parent.getMesh().setElementRemoved(true);
            faces.numberOfFaces--;

            if(faces.isHole(face)) {
                faces.numberOfHoles--;
            }

            face.destroy();
        }
    }
}
