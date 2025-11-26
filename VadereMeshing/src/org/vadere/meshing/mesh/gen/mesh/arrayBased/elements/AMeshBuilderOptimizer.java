package org.vadere.meshing.mesh.gen.mesh.arrayBased.elements;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.SpaceFillingCurve;
import org.vadere.meshing.mesh.gen.mesh.MeshBuilderBase;
import org.vadere.meshing.mesh.gen.mesh.arrayBased.AMeshDataStorage;
import org.vadere.meshing.mesh.inter.mesh.builder.IMeshOptimizer;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class AMeshBuilderOptimizer<Vertices extends AMeshVertices, Edges extends AMeshEdges, Faces extends AMeshFaces,
        Mesh extends ArrayBasedMesh<Vertices,Edges,Faces>> implements IMeshOptimizer<AVertex, AHalfEdge, AFace> {
    private final MeshBuilderBase<AVertex, AHalfEdge, AFace, AMeshDataStorage, Mesh> parent;

    private final Mesh mesh;
    private final Faces faces;
    private final Edges edges ;
    private final Vertices vertices;

    public AMeshBuilderOptimizer(MeshBuilderBase<AVertex, AHalfEdge, AFace, AMeshDataStorage, Mesh> parent) {
        this.parent = parent;
        mesh = parent.getMesh();

        faces = mesh.faces();
        edges = mesh.edges();
        vertices = mesh.vertices();
    }

    /**
     * <p>This method rearranges the indices of faces, vertices and edges according to their positions.
     * After the call, neighbouring faces are near arrange inside the face {@link ArrayList}.</p>
     *
     * <p>Note: that any mapping id to vertex or id to halfEdge or id to face has to be recomputed!</p>
     */
    private void spatialSort() {
        // get the bound for the space filling curve!
        double maxX = Double.MIN_VALUE;
        double maxY = Double.MIN_VALUE;
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;

        List<VPoint> centroids = new ArrayList<>(parent.getMesh().faces().getAll().size());

        for (AFace face : parent.getMesh().faces()) {
            VPoint incenter = GeometryUtils.getPolygonCentroid(parent.getMesh().vertices().getAllOf(face));
            centroids.add(incenter);
            maxX = Math.max(maxX, incenter.getX());
            maxY = Math.max(maxY, incenter.getY());

            minX = Math.min(minX, incenter.getX());
            minY = Math.min(minY, incenter.getY());
        }

        SpaceFillingCurve spaceFillingCurve = new SpaceFillingCurve(new VRectangle(minX, minY, maxX-minX, maxY-minY));

        // TODO: implement faster sorting using radix sort see: http://www.diss.fu-berlin.de/diss/servlets/MCRFileNodeServlet/FUDISS_derivate_000000003494/2_kap2.pdf?hosts=
        // page 18
        List<AFace> sortedFaces = new ArrayList<>(parent.getMesh().faces().items.size());
        sortedFaces.addAll(parent.getMesh().faces().items);
        sortedFaces.sort((f1, f2) -> {
            double i1 = spaceFillingCurve.compute(centroids.get(f1.getId()));
            double i2 = spaceFillingCurve.compute(centroids.get(f2.getId()));
            return Double.compare(i1, i2);
        });
        arrangeMemory(sortedFaces);
    }

    /**
     * <p>Rearranges all indices of faces, vertices and halfEdges of the mesh according to
     * the {@link Iterable} faceOrder. All indices start at 0 and will be incremented one by one.
     * For example, the vertices of the first face of faceOrder will receive id 0,1 and 2.</p>
     *
     * <p>Note: that every mapping id to vertex or id to halfEdge or id to face has to be recomputed!</p>
     * <p>Assumption: faceOrder contains all faces of this mesh.</p>
     * <p>Invariant: the geometry i.e. the connectivity and the vertex positions will not change.</p>
     *
     * @param faceOrder the new order
     */
    public void arrangeMemory(@NotNull final Iterable<AFace> faceOrder) {
        Mesh oldMeshCopy = (Mesh) mesh.copy();

        // merge some of them?
        int nullIdentifier = -2;

        // rebuild
        faces.clear();
        edges.clear();
        vertices.clear();
        faces.holes.clear();

        int[] edgeMap = new int[oldMeshCopy.edges().getAll().size()];
        int[] vertexMap = new int[oldMeshCopy.vertices().getAll().size()];
        int[] faceMap = new int[oldMeshCopy.faces().getAll().size()];

        Arrays.fill(edgeMap, nullIdentifier);
        Arrays.fill(vertexMap, nullIdentifier);
        Arrays.fill(faceMap, nullIdentifier);

        // adjust all id's in order of faceOrder
        for(AFace face : faceOrder) {
            copyFace(face, vertexMap, edgeMap, faceMap, oldMeshCopy);
        }

        // adjust all id's not contained in faceOrder in any order
        for(AFace face : oldMeshCopy.faces()) {
            if(!faces.isDestroyed(face)) {
                copyFace(face, vertexMap, edgeMap, faceMap, oldMeshCopy);
            }
        }

        // repair the rest
        for(AFace face : faces) {
            face.setEdge(edgeMap[face.getEdge()]);
        }

        for(AHalfEdge halfEdge : edges) {
            if(vertexMap[halfEdge.getEnd()] == nullIdentifier) {
                vertexMap[halfEdge.getEnd()] = vertices.getAll().size();
            }
            halfEdge.setEnd(vertexMap[halfEdge.getEnd()]);

            // boundary face
            if(halfEdge.getFaceId() != faces.outerBorder.getId()) {
                halfEdge.setFace(faceMap[halfEdge.getFaceId()]);
            }
            else {
                halfEdge.setFace(faces.outerBorder.getId());
            }

            halfEdge.setTwin(edgeMap[halfEdge.getTwin()]);
            halfEdge.setPrevious(edgeMap[halfEdge.getPrevious()]);
            halfEdge.setNext(edgeMap[halfEdge.getNext()]);
        }

        for(AVertex vertex : vertices) {
            vertex.setDown(vertexMap[vertex.getDown()]);
            vertex.setEdge(edgeMap[vertex.getEdge()]);
        }

        // fix the boundary
        faces.outerBorder.setEdge(edgeMap[faces.outerBorder.getEdge()]);

        // fix properties
        rearrangeFacesData(faceMap, nullIdentifier);
        rearrangeHalfEdgesData(edgeMap, nullIdentifier);
        rearrangeVerticesData(vertexMap, nullIdentifier);
    }

    private void rearrangeFacesData(@NotNull int[] faceMap,  int nullIdentifier) {
        int numberOfDestroyed = 0;
        for(int i = 0; i < faceMap.length; i++) {
            if(faceMap[i] != nullIdentifier) {
                for(var list : parent.getDataStorage().facesData.values()) {
                    list.swap(faceMap[i], i);
                }

                for(var list : parent.getDataStorage().facesDoubleData.values()) {
                    double tmp = list.getDouble(faceMap[i]);
                    list.set(faceMap[i], list.getDouble(i));
                    list.set(i, tmp);
                }

                for(var list : parent.getDataStorage().facesBooleanData.values()) {
                    boolean tmp = list.getBoolean(faceMap[i]);
                    list.set(faceMap[i], list.getBoolean(i));
                    list.set(i, tmp);
                }
            } else {
                numberOfDestroyed++;
            }
        }

        for(var list : parent.getDataStorage().facesDoubleData.values()) {
            list.trim(faceMap.length - numberOfDestroyed);
        }

        for(var list : parent.getDataStorage().facesBooleanData.values()) {
            list.trim(faceMap.length - numberOfDestroyed);
        }

        for(var list : parent.getDataStorage().facesData.values()) {
            list.trim(faceMap.length - numberOfDestroyed);
        }
    }

    private void rearrangeHalfEdgesData(@NotNull int[] edgeMap, int nullIdentifier) {
        int numberOfDestroyed = 0;
        for(int i = 0; i < edgeMap.length; i++) {
            if(edgeMap[i] != nullIdentifier) {
                for(var list : parent.getDataStorage().halfEdgesData.values()) {
                    list.swap(edgeMap[i], i);
                }

                for(var list : parent.getDataStorage().halfEdgesDoubleData.values()) {
                    double tmp = list.getDouble(edgeMap[i]);
                    list.set(edgeMap[i], list.getDouble(i));
                    list.set(i, tmp);
                }

                for(var list : parent.getDataStorage().halfEdgesBooleanData.values()) {
                    boolean tmp = list.getBoolean(edgeMap[i]);
                    list.set(edgeMap[i], list.getBoolean(i));
                    list.set(i, tmp);
                }
            } else {
                numberOfDestroyed++;
            }
        }


        for(var list : parent.getDataStorage().halfEdgesDoubleData.values()) {
            list.trim(edgeMap.length - numberOfDestroyed);
        }

        for(var list : parent.getDataStorage().halfEdgesBooleanData.values()) {
            list.trim(edgeMap.length - numberOfDestroyed);
        }

        for(var list : parent.getDataStorage().halfEdgesData.values()) {
            list.trim(edgeMap.length - numberOfDestroyed);
        }
    }

    private void rearrangeVerticesData(@NotNull int[] vertexMap, int nullIdentifier) {
        int numberOfDestroyed = 0;
        for(int i = 0; i < vertexMap.length; i++) {
            if(vertexMap[i] != nullIdentifier) {
                for(var list : parent.getDataStorage().verticesData.values()) {
                    list.swap(vertexMap[i], i);
                }

                for(var list : parent.getDataStorage().verticesDoubleData.values()) {
                    double tmp = list.getDouble(vertexMap[i]);
                    list.set(vertexMap[i], list.getDouble(i));
                    list.set(i, tmp);
                }

                for(var list : parent.getDataStorage().verticesIndexedDoubleData) {
                    double tmp = list.getDouble(vertexMap[i]);
                    list.set(vertexMap[i], list.getDouble(i));
                    list.set(i, tmp);
                }

                for(var list : parent.getDataStorage().verticesBooleanData.values()) {
                    boolean tmp = list.getBoolean(vertexMap[i]);
                    list.set(vertexMap[i], list.getBoolean(i));
                    list.set(i, tmp);
                }
            } else {
                numberOfDestroyed++;
            }
        }

        for(var list : parent.getDataStorage().verticesDoubleData.values()) {
            list.size(vertexMap.length - numberOfDestroyed);
            list.trim(vertexMap.length - numberOfDestroyed);
        }

        for(var list : parent.getDataStorage().verticesIndexedDoubleData) {
            list.size(vertexMap.length - numberOfDestroyed);
            list.trim(vertexMap.length - numberOfDestroyed);
        }

        for(var list : parent.getDataStorage().verticesBooleanData.values()) {
            list.size(vertexMap.length- numberOfDestroyed);
            list.trim(vertexMap.length- numberOfDestroyed);
        }

        for(var list : parent.getDataStorage().verticesData.values()) {
            list.size(vertexMap.length - numberOfDestroyed);
            list.trim(vertexMap.length- numberOfDestroyed);
        }
    }

    /**
     * <p>Removes all destroyed object from this mesh and re-arranges all indices.</p>
     *
     * <p>Note: that any mapping id to vertex or id to halfEdge or id to face has to be recomputed!</p>
     */
    public void garbageCollection() {
        int nullIdentifier = -2;
        Mesh mesh = parent.getMesh();
        int[] faceIdMap = new int[mesh.faces().getAll().size()];
        int[] edgeIdMap = new int[mesh.edges().getAll().size()];
        int[] vertexIdMap = new int[mesh.vertices().getAll().size()];

        Arrays.fill(faceIdMap, nullIdentifier);
        Arrays.fill(edgeIdMap, nullIdentifier);
        Arrays.fill(vertexIdMap, nullIdentifier);

        int i = 0;
        int j = 0;
        for (AFace face : mesh.faces().getAll()) {
            if (face.isDestroyed()) {
                j--;
            } else {
                faceIdMap[i] = j;
            }
            i++;
            j++;
        }

        i = 0;
        j = 0;
        for (AHalfEdge edge : mesh.edges().getAll()) {
            if (edge.isDestroyed()) {
                j--;
            } else {
                edgeIdMap[i] = j;
            }
            i++;
            j++;
        }

        i = 0;
        j = 0;
        for (AVertex vertex : mesh.vertices().getAll()) {
            if (vertex.isDestroyed()) {
                j--;
            } else {
                vertexIdMap[i] = j;
            }
            i++;
            j++;
        }

        mesh.faces().items = mesh.faces().items.stream().filter(f -> !f.isDestroyed()).collect(Collectors.toList());
        mesh.edges().items = mesh.edges().items.stream().filter(e -> !e.isDestroyed()).collect(Collectors.toList());
        mesh.vertices().items = mesh.vertices().items.stream().filter(v -> !v.isDestroyed()).collect(Collectors.toList());

        i = 0;
        for (AFace face : mesh.faces().getAll()) {
            face.setId(faceIdMap[face.getId()]);
            face.setEdge(edgeIdMap[face.getEdge()]);
            assert face.getId() == i;
            i++;
        }

        i = 0;
        for (AVertex vertex : mesh.vertices().getAll()) {
            vertex.setId(vertexIdMap[vertex.getId()]);
            vertex.setEdge(edgeIdMap[vertex.getEdge()]);
            assert vertex.getId() == i;
            i++;
        }

        i = 0;
        for (AHalfEdge edge : mesh.edges().getAll()) {
            edge.setId(edgeIdMap[edge.getId()]);
            edge.setEnd(vertexIdMap[edge.getEnd()]);
            edge.setNext(edgeIdMap[edge.getNext()]);
            edge.setPrevious(edgeIdMap[edge.getPrevious()]);
            edge.setTwin(edgeIdMap[edge.getTwin()]);
            if (edge.getFaceId() != mesh.faces().outerBorder.getId()) {
                edge.setFace(faceIdMap[edge.getFaceId()]);
            }

            assert edge.getId() == i;
            i++;
        }

        // fix properties
        rearrangeFacesData(faceIdMap, nullIdentifier);
        rearrangeHalfEdgesData(edgeIdMap, nullIdentifier);
        rearrangeVerticesData(vertexIdMap, nullIdentifier);

        assert (vertices.count() == mesh.vertices().items.size()) &&
                (edges.count() == mesh.edges().items.size()) &&
                (faces.count() == mesh.faces().items.size()-mesh.faces().holes.size());
    }

    private void copyFace(@NotNull final AFace face, @NotNull int[] vertexMap, @NotNull int[] edgeMap, @NotNull int[] faceMap, @NotNull final Mesh cMesh) {
        // merge some of them?
        int nullIdentifier = -2;

        // face not jet copied
        if(faceMap[face.getId()] == nullIdentifier) {
            AFace fClone = face.clone();

            // 1. face
            faceMap[face.getId()] = faces.items.size();
            fClone.setId(faces.items.size());
            faces.items.add(fClone);

            if(cMesh.faces().isHole(face)){
                faces.holes.add(fClone);
            }

            // 2. vertices
            for(AVertex v : cMesh.vertices().iterableFor(face)) {
                if(vertexMap[v.getId()] == nullIdentifier) {
                    vertexMap[v.getId()] = vertices.items.size();
                    AVertex cVertex = v.clone();
                    cVertex.setId(vertices.items.size());
                    vertices.items.add(cVertex);
                }
            }

            // 3. edges
            for(AHalfEdge halfEdge : cMesh.edges().iterableFor(face)) {

                // origin
                if(edgeMap[halfEdge.getId()] == nullIdentifier) {
                    edgeMap[halfEdge.getId()] = edges.items.size();
                    AHalfEdge cHalfEdge = halfEdge.clone();
                    cHalfEdge.setId(edges.items.size());
                    edges.items.add(cHalfEdge);
                }

                // twin
                halfEdge = cMesh.edges().getTwin(halfEdge);
                if(edgeMap[halfEdge.getId()] == nullIdentifier) {
                    // origin
                    edgeMap[halfEdge.getId()] = edges.items.size();
                    AHalfEdge cHalfEdge = halfEdge.clone();
                    cHalfEdge.setId(edges.items.size());
                    edges.items.add(cHalfEdge);
                }
            }
        }
    }
}
