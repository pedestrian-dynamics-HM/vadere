package org.vadere.meshing.mesh.gen.mesh.pointerBased.elements;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.gen.mesh.MeshBuilderBase;
import org.vadere.meshing.mesh.inter.mesh.builder.MeshBuilderFacesBase;

public class PMeshBuilderFaces<Vertices extends PMeshVertices, Edges extends PMeshEdges, Faces extends PMeshFaces,
        Mesh extends PointerBasedMesh<Vertices,Edges,Faces>> extends MeshBuilderFacesBase<PVertex, PHalfEdge, PFace, Mesh>  {
    private final PMeshFaces faces;

    public PMeshBuilderFaces(MeshBuilderBase<PVertex, PHalfEdge, PFace, PMeshDataStorage, Mesh> parent) {
        super(parent);
        faces = parent.getMesh().faces();
    }

    @Override
    public PFace createAndInsert() {
        return createAndInsert(false);
    }

    @Override
    public PFace createAndInsertHole() {
        return createAndInsert(true);
    }

    private PFace createAndInsert(final boolean hole) {
        PFace face = new PFace(hole);

        faces.items.add(face);
        if(hole) {
            faces.numberOfHoles++;
            faces.holes.add(face);
        }
        else {
            faces.numberOfFaces++;
        }
        return face;
    }

    @Override
    public void setEdge(@NotNull final PFace face, @NotNull final PHalfEdge edge) {
        face.setEdge(edge);
    }

    @Override
    public void convertToHole(@NotNull final PFace face) {
        assert !faces.isHole(face);
        if(!faces.isHole(face)) {
            faces.holes.add(face);
            face.setBoundary(true);
            faces.numberOfHoles++;
            faces.numberOfFaces--;
        }
    }

    @Override
    public void destroy(@NotNull final PFace face) {
        //faces.remove(face);
        if(faces.isHole(face)) {
            //holes.remove(face);
            faces.numberOfHoles--;
        }
        else {
            faces.numberOfFaces--;
        }
        face.destroy();
    }
}
