package org.vadere.meshing.mesh.gen.mesh.arrayBased;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.SpaceFillingCurve;
import org.vadere.meshing.mesh.inter.mesh.builder.IMeshOptimizer;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

class AMeshBuilderOptimizer implements IMeshOptimizer<AVertex, AHalfEdge, AFace> {
    private final AMeshBuilder parent;

    public AMeshBuilderOptimizer(AMeshBuilder parent) {
        this.parent = parent;
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

        List<VPoint> centroids = new ArrayList<>(parent.mesh.getNumberOfFaces());

        for (AFace face : parent.mesh.getFaces()) {
            VPoint incenter = GeometryUtils.getPolygonCentroid(parent.mesh.getVertices(face));
            centroids.add(incenter);
            maxX = Math.max(maxX, incenter.getX());
            maxY = Math.max(maxY, incenter.getY());

            minX = Math.min(minX, incenter.getX());
            minY = Math.min(minY, incenter.getY());
        }

        SpaceFillingCurve spaceFillingCurve = new SpaceFillingCurve(new VRectangle(minX, minY, maxX-minX, maxY-minY));

        // TODO: implement faster sorting using radix sort see: http://www.diss.fu-berlin.de/diss/servlets/MCRFileNodeServlet/FUDISS_derivate_000000003494/2_kap2.pdf?hosts=
        // page 18
        List<AFace> sortedFaces = new ArrayList<>(parent.mesh.faces.size());
        sortedFaces.addAll(parent.mesh.faces);
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
        // clone the old one!
        AMesh cMesh = parent.mesh.clone();

        // merge some of them?
        int nullIdentifier = -2;

        // rebuild
        parent.mesh.faces.clear();
        parent.mesh.edges.clear();
        parent.mesh.vertices.clear();
        parent.mesh.holes.clear();

        int[] edgeMap = new int[cMesh.edges.size()];
        int[] vertexMap = new int[cMesh.vertices.size()];
        int[] faceMap = new int[cMesh.faces.size()];

        Arrays.fill(edgeMap, nullIdentifier);
        Arrays.fill(vertexMap, nullIdentifier);
        Arrays.fill(faceMap, nullIdentifier);

        // adjust all id's in order of faceOrder
        for(AFace face : faceOrder) {
            copyFace(face, vertexMap, edgeMap, faceMap, cMesh);
        }

        // adjust all id's not contained in faceOrder in any order
        for(AFace face : cMesh.faces) {
            if(!parent.mesh.isDestroyed(face)) {
                copyFace(face, vertexMap, edgeMap, faceMap, cMesh);
            }
        }

        // repair the rest
        for(AFace face : parent.mesh.faces) {
            face.setEdge(edgeMap[face.getEdge()]);
        }

        for(AHalfEdge halfEdge : parent.mesh.edges) {
            if(vertexMap[halfEdge.getEnd()] == nullIdentifier) {
                vertexMap[halfEdge.getEnd()] = parent.mesh.vertices.size();
            }
            halfEdge.setEnd(vertexMap[halfEdge.getEnd()]);

            // boundary face
            if(halfEdge.getFace() != parent.mesh.boundary.getId()) {
                halfEdge.setFace(faceMap[halfEdge.getFace()]);
            }
            else {
                halfEdge.setFace(parent.mesh.boundary.getId());
            }

            halfEdge.setTwin(edgeMap[halfEdge.getTwin()]);
            halfEdge.setPrevious(edgeMap[halfEdge.getPrevious()]);
            halfEdge.setNext(edgeMap[halfEdge.getNext()]);
        }

        for(AVertex vertex : parent.mesh.vertices) {
            vertex.setDown(vertexMap[vertex.getDown()]);
            vertex.setEdge(edgeMap[vertex.getEdge()]);
        }

        // fix the boundary
        parent.mesh.boundary.setEdge(edgeMap[parent.mesh.boundary.getEdge()]);

        // fix properties
        rearrangeFacesData(faceMap, nullIdentifier);
        rearrangeHalfEdgesData(edgeMap, nullIdentifier);
        rearrangeVerticesData(vertexMap, nullIdentifier);
    }


    private void rearrangeVerticesData(@NotNull int[] vertexMap, int nullIdentifier) {
        int numberOfDestroyed = 0;
        for(int i = 0; i < vertexMap.length; i++) {
            if(vertexMap[i] != nullIdentifier) {
                for(var list : parent.meshDataStorage.verticesData.values()) {
                    list.swap(vertexMap[i], i);
                }

                for(var list : parent.meshDataStorage.verticesDoubleData.values()) {
                    double tmp = list.getDouble(vertexMap[i]);
                    list.set(vertexMap[i], list.getDouble(i));
                    list.set(i, tmp);
                }

                for(var list : parent.meshDataStorage.verticesIndexedDoubleData) {
                    double tmp = list.getDouble(vertexMap[i]);
                    list.set(vertexMap[i], list.getDouble(i));
                    list.set(i, tmp);
                }

                for(var list : parent.meshDataStorage.verticesBooleanData.values()) {
                    boolean tmp = list.getBoolean(vertexMap[i]);
                    list.set(vertexMap[i], list.getBoolean(i));
                    list.set(i, tmp);
                }
            } else {
                numberOfDestroyed++;
            }
        }

        for(var list : parent.meshDataStorage.verticesDoubleData.values()) {
            list.size(vertexMap.length - numberOfDestroyed);
            list.trim(vertexMap.length - numberOfDestroyed);
        }

        for(var list : parent.meshDataStorage.verticesIndexedDoubleData) {
            list.size(vertexMap.length - numberOfDestroyed);
            list.trim(vertexMap.length - numberOfDestroyed);
        }

        for(var list : parent.meshDataStorage.verticesBooleanData.values()) {
            list.size(vertexMap.length- numberOfDestroyed);
            list.trim(vertexMap.length- numberOfDestroyed);
        }

        for(var list : parent.meshDataStorage.verticesData.values()) {
            list.size(vertexMap.length - numberOfDestroyed);
            list.trim(vertexMap.length- numberOfDestroyed);
        }
    }

    private void rearrangeHalfEdgesData(@NotNull int[] edgeMap, int nullIdentifier) {
        int numberOfDestroyed = 0;
        for(int i = 0; i < edgeMap.length; i++) {
            if(edgeMap[i] != nullIdentifier) {
                for(var list : parent.meshDataStorage.halfEdgesData.values()) {
                    list.swap(edgeMap[i], i);
                }

                for(var list : parent.meshDataStorage.halfEdgesDoubleData.values()) {
                    double tmp = list.getDouble(edgeMap[i]);
                    list.set(edgeMap[i], list.getDouble(i));
                    list.set(i, tmp);
                }

                for(var list : parent.meshDataStorage.halfEdgesBooleanData.values()) {
                    boolean tmp = list.getBoolean(edgeMap[i]);
                    list.set(edgeMap[i], list.getBoolean(i));
                    list.set(i, tmp);
                }
            } else {
                numberOfDestroyed++;
            }
        }


        for(var list : parent.meshDataStorage.halfEdgesDoubleData.values()) {
            list.trim(edgeMap.length - numberOfDestroyed);
        }

        for(var list : parent.meshDataStorage.halfEdgesBooleanData.values()) {
            list.trim(edgeMap.length - numberOfDestroyed);
        }

        for(var list : parent.meshDataStorage.halfEdgesData.values()) {
            list.trim(edgeMap.length - numberOfDestroyed);
        }
    }

    private void rearrangeFacesData(@NotNull int[] faceMap,  int nullIdentifier) {
        int numberOfDestroyed = 0;
        for(int i = 0; i < faceMap.length; i++) {
            if(faceMap[i] != nullIdentifier) {
                for(var list : parent.meshDataStorage.facesData.values()) {
                    list.swap(faceMap[i], i);
                }

                for(var list : parent.meshDataStorage.facesDoubleData.values()) {
                    double tmp = list.getDouble(faceMap[i]);
                    list.set(faceMap[i], list.getDouble(i));
                    list.set(i, tmp);
                }

                for(var list : parent.meshDataStorage.facesBooleanData.values()) {
                    boolean tmp = list.getBoolean(faceMap[i]);
                    list.set(faceMap[i], list.getBoolean(i));
                    list.set(i, tmp);
                }
            } else {
                numberOfDestroyed++;
            }
        }

        for(var list : parent.meshDataStorage.facesDoubleData.values()) {
            list.trim(faceMap.length - numberOfDestroyed);
        }

        for(var list : parent.meshDataStorage.facesBooleanData.values()) {
            list.trim(faceMap.length - numberOfDestroyed);
        }

        for(var list : parent.meshDataStorage.facesData.values()) {
            list.trim(faceMap.length - numberOfDestroyed);
        }
    }

    /**
     * <p>Removes all destroyed object from this mesh and re-arranges all indices.</p>
     *
     * <p>Note: that any mapping id to vertex or id to halfEdge or id to face has to be recomputed!</p>
     */
    public void garbageCollection() {
        int nullIdentifier = -2;
        int[] faceIdMap = new int[parent.mesh.faces.size()];
        int[] edgeIdMap = new int[parent.mesh.edges.size()];
        int[] vertexIdMap = new int[parent.mesh.vertices.size()];

        Arrays.fill(faceIdMap, nullIdentifier);
        Arrays.fill(edgeIdMap, nullIdentifier);
        Arrays.fill(vertexIdMap, nullIdentifier);

        int i = 0;
        int j = 0;
        for (AFace face : parent.mesh.faces) {
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
        for (AHalfEdge edge : parent.mesh.edges) {
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
        for (AVertex vertex : parent.mesh.vertices) {
            if (vertex.isDestroyed()) {
                j--;
            } else {
                vertexIdMap[i] = j;
            }
            i++;
            j++;
        }

        parent.mesh.faces = parent.mesh.faces.stream().filter(f -> !f.isDestroyed()).collect(Collectors.toList());
        parent.mesh.edges = parent.mesh.edges.stream().filter(e -> !e.isDestroyed()).collect(Collectors.toList());
        parent.mesh.vertices = parent.mesh.vertices.stream().filter(v -> !v.isDestroyed()).collect(Collectors.toList());

        i = 0;
        for (AFace face : parent.mesh.faces) {
            face.setId(faceIdMap[face.getId()]);
            face.setEdge(edgeIdMap[face.getEdge()]);
            assert face.getId() == i;
            i++;
        }

        i = 0;
        for (AVertex vertex : parent.mesh.vertices) {
            vertex.setId(vertexIdMap[vertex.getId()]);
            vertex.setEdge(edgeIdMap[vertex.getEdge()]);
            assert vertex.getId() == i;
            i++;
        }

        i = 0;
        for (AHalfEdge edge : parent.mesh.edges) {
            edge.setId(edgeIdMap[edge.getId()]);
            edge.setEnd(vertexIdMap[edge.getEnd()]);
            edge.setNext(edgeIdMap[edge.getNext()]);
            edge.setPrevious(edgeIdMap[edge.getPrevious()]);
            edge.setTwin(edgeIdMap[edge.getTwin()]);
            if (edge.getFace() != parent.mesh.boundary.getId()) {
                edge.setFace(faceIdMap[edge.getFace()]);
            }

            assert edge.getId() == i;
            i++;
        }

        // fix properties
        rearrangeFacesData(faceIdMap, nullIdentifier);
        rearrangeHalfEdgesData(edgeIdMap, nullIdentifier);
        rearrangeVerticesData(vertexIdMap, nullIdentifier);

        assert (parent.mesh.getNumberOfVertices() == parent.mesh.vertices.size()) &&
                (parent.mesh.getNumberOfEdges() == parent.mesh.edges.size()) &&
                (parent.mesh.getNumberOfFaces() == parent.mesh.faces.size()-parent.mesh.holes.size());
    }

    private void copyFace(@NotNull final AFace face, @NotNull int[] vertexMap, @NotNull int[] edgeMap, @NotNull int[] faceMap, @NotNull final AMesh cMesh) {
        // merge some of them?
        int nullIdentifier = -2;

        // face not jet copied
        if(faceMap[face.getId()] == nullIdentifier) {
            AFace fClone = face.clone();

            // 1. face
            faceMap[face.getId()] = parent.mesh.faces.size();
            fClone.setId(parent.mesh.faces.size());
            parent.mesh.faces.add(fClone);

            if(cMesh.isHole(face)){
                parent.mesh.holes.add(fClone);
            }

            // 2. vertices
            for(AVertex v : cMesh.getVertexIt(face)) {
                if(vertexMap[v.getId()] == nullIdentifier) {
                    vertexMap[v.getId()] = parent.mesh.vertices.size();
                    AVertex cVertex = v.clone();
                    cVertex.setId(parent.mesh.vertices.size());
                    parent.mesh.vertices.add(cVertex);
                }
            }

            // 3. edges
            for(AHalfEdge halfEdge : cMesh.getEdgeIt(face)) {

                // origin
                if(edgeMap[halfEdge.getId()] == nullIdentifier) {
                    edgeMap[halfEdge.getId()] = parent.mesh.edges.size();
                    AHalfEdge cHalfEdge = halfEdge.clone();
                    cHalfEdge.setId(parent.mesh.edges.size());
                    parent.mesh.edges.add(cHalfEdge);
                }

                // twin
                halfEdge = cMesh.getTwin(halfEdge);
                if(edgeMap[halfEdge.getId()] == nullIdentifier) {
                    // origin
                    edgeMap[halfEdge.getId()] = parent.mesh.edges.size();
                    AHalfEdge cHalfEdge = halfEdge.clone();
                    cHalfEdge.setId(parent.mesh.edges.size());
                    parent.mesh.edges.add(cHalfEdge);
                }
            }
        }
    }
}
