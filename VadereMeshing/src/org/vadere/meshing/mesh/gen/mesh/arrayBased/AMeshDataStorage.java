package org.vadere.meshing.mesh.gen.mesh.arrayBased;

import it.unimi.dsi.fastutil.booleans.BooleanArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vadere.meshing.mesh.inter.*;
import org.vadere.meshing.mesh.inter.mesh.IMesh;
import org.vadere.meshing.mesh.inter.mesh.data.IMeshDataStorage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class AMeshDataStorage implements IMeshDataStorage<AVertex, AHalfEdge, AFace> {
    Map<String, AObjectArrayList<?>> verticesData;
    Map<String, AObjectArrayList<?>> halfEdgesData;
    Map<String, AObjectArrayList<?>> facesData;
    ArrayList<DoubleArrayList> verticesIndexedDoubleData;
    Map<String, DoubleArrayList> verticesDoubleData;
    Map<String, DoubleArrayList> facesDoubleData;
    Map<String, DoubleArrayList> halfEdgesDoubleData;
    Map<String, BooleanArrayList> verticesBooleanData;
    Map<String, BooleanArrayList> facesBooleanData;
    Map<String, BooleanArrayList> halfEdgesBooleanData;

    private IMesh<AVertex, AHalfEdge, AFace> mesh;

    public AMeshDataStorage(IMesh<AVertex, AHalfEdge, AFace> mesh) {
        this.mesh = mesh;
        clear();
    }

    public void clear(){
        verticesData = new HashMap();
        halfEdgesData= new HashMap();
        facesData= new HashMap();
        verticesIndexedDoubleData = new ArrayList<>();
        verticesDoubleData = new HashMap<>();
        facesDoubleData = new HashMap<>();
        halfEdgesDoubleData = new HashMap<>();
        verticesBooleanData = new HashMap<>();
        facesBooleanData = new HashMap<>();
        halfEdgesBooleanData = new HashMap<>();
    }

    public void onEdgeCreated(){
        for (ObjectArrayList edgeProperty : halfEdgesData.values()) {
            edgeProperty.add(null);
        }
        for(DoubleArrayList edgeDoubleProperty : halfEdgesDoubleData.values()) {
            edgeDoubleProperty.add(0.0);
        }
        for(BooleanArrayList edgeBooleanProperty : halfEdgesBooleanData.values()) {
            edgeBooleanProperty.add(false);
        }
    }

    public void onFaceCreated(){
        for (ObjectArrayList faceProperty : facesData.values()) {
            faceProperty.add(null);
        }

        for(DoubleArrayList faceDoubleProperty : facesDoubleData.values()) {
            faceDoubleProperty.add(0.0);
        }

        for(BooleanArrayList faceBooleanProperty : facesBooleanData.values()) {
            faceBooleanProperty.add(false);
        }
    }

    public void onVertexCreated(){
        for (ObjectArrayList vertexProperty : verticesData.values()) {
            vertexProperty.add(null);
        }
        for(DoubleArrayList vertexDoubleProperty : verticesDoubleData.values()) {
            vertexDoubleProperty.add(0.0);
        }
        for(BooleanArrayList vertexBooleanProperty : verticesBooleanData.values()) {
            vertexBooleanProperty.add(false);
        }
    }

    @Override
    public boolean getBooleanData(@NotNull final AVertex vertex, @NotNull final String name) {
        if(!verticesBooleanData.containsKey(name)) {
            return false;
        } else {
            BooleanArrayList dataArray = verticesBooleanData.get(name);
            assert dataArray.size() == mesh.getVertices().size();
            return dataArray.getBoolean(vertex.getId());
        }
    }

    @Override
    public double getDoubleData(@NotNull final AVertex vertex, @NotNull final String name) {
        if(!verticesDoubleData.containsKey(name)) {
            return 0.0;
        } else {
            DoubleArrayList dataArray = verticesDoubleData.get(name);
            assert dataArray.size() == mesh.getVertices().size();
            return dataArray.getDouble(vertex.getId());
        }
    }

    @Override
    public double getDoubleData(@NotNull final AVertex vertex, @NotNull final int index) {
        if(verticesIndexedDoubleData.size() <= index) {
            return 0.0;
        } else {
            DoubleArrayList dataArray = verticesIndexedDoubleData.get(index);
            assert dataArray.size() == mesh.getVertices().size();
            return dataArray.getDouble(vertex.getId());
        }
    }

    @Override
    public boolean getBooleanData(@NotNull final AHalfEdge edge, @NotNull final String name) {
        if(!halfEdgesBooleanData.containsKey(name)) {
            return false;
        } else {
            BooleanArrayList dataArray = halfEdgesBooleanData.get(name);
            assert dataArray.size() == mesh.getEdges().size();
            return dataArray.getBoolean(edge.getId());
        }
    }

    @Override
    public double getDoubleData(@NotNull final AHalfEdge edge, @NotNull final String name) {
        if(!halfEdgesDoubleData.containsKey(name)) {
            return 0.0;
        } else {
            DoubleArrayList dataArray = halfEdgesDoubleData.get(name);
            assert dataArray.size() == mesh.getEdges().size();
            return dataArray.getDouble(edge.getId());
        }
    }

    @Override
    public boolean getBooleanData(@NotNull final AFace face, @NotNull final String name) {
        if(!facesBooleanData.containsKey(name)) {
            return false;
        } else {
            BooleanArrayList dataArray = facesBooleanData.get(name);
            assert dataArray.size() == mesh.getFaces().size();
            return dataArray.getBoolean(face.getId());
        }
    }

    @Override
    public double getDoubleData(@NotNull final AFace face, @NotNull final String name) {
        if(!facesDoubleData.containsKey(name)) {
            return 0.0;
        } else {
            DoubleArrayList dataArray = facesDoubleData.get(name);
            assert dataArray.size() == mesh.getFaces().size();
            return dataArray.getDouble(face.getId());
        }
    }

    @Override
    public <CV> Optional<CV> getData(@NotNull final AVertex vertex, @NotNull final String name, @NotNull Class<CV> clazz) {
        if(!verticesData.containsKey(name)) {
            return Optional.ofNullable(null);
        } else {
            ObjectArrayList<CV> dataArray = (ObjectArrayList<CV>) verticesData.get(name);
            assert dataArray.size() == mesh.getVertices().size();
            return Optional.ofNullable(dataArray.get(vertex.getId()));
        }
    }

    @Override
    public <CV> void setData(@NotNull final AVertex vertex, @NotNull final String name, @Nullable final CV data) {
        if(!verticesData.containsKey(name)) {
            AObjectArrayList<CV> dataArray = new AObjectArrayList<>();
            fill(dataArray, mesh.getVertices().size());
            verticesData.put(name, dataArray);
        }
        AObjectArrayList<CV> dataArray = (AObjectArrayList<CV>) verticesData.get(name);
        assert dataArray.size() == mesh.getVertices().size();
        dataArray.set(vertex.getId(), data);
    }

    @Override
    public <CE> Optional<CE> getData(@NotNull final AHalfEdge edge, @NotNull final String name, @NotNull Class<CE> clazz) {
        if(!halfEdgesData.containsKey(name)) {
            return Optional.ofNullable(null);
        } else {
            AObjectArrayList<CE> dataArray = (AObjectArrayList<CE>) halfEdgesData.get(name);
            assert dataArray.size() == mesh.getEdges().size();
            return Optional.ofNullable(dataArray.get(edge.getId()));
        }
    }

    @Override
    public <CE> void setData(@NotNull final AHalfEdge edge, @NotNull final String name, @Nullable final CE data) {
        if(!halfEdgesData.containsKey(name)) {
            AObjectArrayList<CE> dataArray = new AObjectArrayList<>();
            fill(dataArray, mesh.getEdges().size());
            halfEdgesData.put(name, dataArray);
        }
        AObjectArrayList<CE> dataArray = (AObjectArrayList<CE>) halfEdgesData.get(name);
        assert dataArray.size() == mesh.getEdges().size();
        dataArray.set(edge.getId(), data);
    }

    @Override
    public <CF> Optional<CF> getData(@NotNull final AFace face, @NotNull final String name, @NotNull Class<CF> clazz) {
        if(!facesData.containsKey(name)) {
            return Optional.ofNullable(null);
        } else {
            AObjectArrayList<CF> dataArray = (AObjectArrayList<CF>) facesData.get(name);
            assert dataArray.size() == mesh.getFaces().size();
            return Optional.ofNullable(dataArray.get(face.getId()));
        }
    }

    @Override
    public <CF> void setData(@NotNull final AFace face, @NotNull final String name, @Nullable final CF data) {
        if(!facesData.containsKey(name)) {
            AObjectArrayList<CF> dataArray = new AObjectArrayList<>();
            fill(dataArray, mesh.getFaces().size());
            facesData.put(name, dataArray);
        }
        AObjectArrayList<CF> dataArray = (AObjectArrayList<CF>) facesData.get(name);
        assert dataArray.size() == mesh.getFaces().size();
        dataArray.set(face.getId(), data);
    }

    @Override
    public void setDoubleData(@NotNull final AFace face, @NotNull final String name, final double data) {
        if(!facesDoubleData.containsKey(name)) {
            DoubleArrayList dataArray = new DoubleArrayList(mesh.getFaces().size());
            dataArray.size(mesh.getFaces().size());
            facesDoubleData.put(name, dataArray);
        }
        DoubleArrayList dataArray = facesDoubleData.get(name);
        assert dataArray.size() == mesh.getFaces().size();
        dataArray.set(face.getId(), data);
    }

    @Override
    public void setDoubleData(@NotNull final AVertex vertex, @NotNull final String name, final double data) {
        if(!verticesDoubleData.containsKey(name)) {
            DoubleArrayList dataArray = new DoubleArrayList(mesh.getVertices().size());
            dataArray.size(mesh.getVertices().size());
            verticesDoubleData.put(name, dataArray);
        }
        DoubleArrayList dataArray = verticesDoubleData.get(name);
        assert dataArray.size() == mesh.getVertices().size();
        dataArray.set(vertex.getId(), data);
    }

    @Override
    public void setDoubleData(@NotNull final AVertex vertex, @NotNull final int index, final double data) {
        if(verticesIndexedDoubleData.size() <= index) {
            for(int i = verticesIndexedDoubleData.size(); i <= index; i++) {
                DoubleArrayList dataArray = new DoubleArrayList(mesh.getVertices().size());
                dataArray.size(mesh.getVertices().size());
                verticesIndexedDoubleData.add(dataArray);
            }
        }
        DoubleArrayList dataArray = verticesIndexedDoubleData.get(index);
        assert dataArray.size() == mesh.getVertices().size();
        dataArray.set(vertex.getId(), data);
    }


    @Override
    public void setDoubleData(@NotNull final AHalfEdge edge, @NotNull final String name, final double data) {
        DoubleArrayList dataArray = getDoubleArrayEdge(name);
        assert dataArray.size() == mesh.getEdges().size();
        dataArray.set(edge.getId(), data);
    }

    @Override
    public void setBooleanData(@NotNull final AFace face, @NotNull final String name, final boolean data) {
        if(!facesBooleanData.containsKey(name)) {
            BooleanArrayList dataArray = new BooleanArrayList(mesh.getFaces().size());
            dataArray.size(mesh.getFaces().size());
            facesBooleanData.put(name, dataArray);
        }
        BooleanArrayList dataArray = facesBooleanData.get(name);
        assert dataArray.size() == mesh.getFaces().size();
        dataArray.set(face.getId(), data);
    }

    @Override
    public void setBooleanData(@NotNull final AVertex vertex, @NotNull final String name, final boolean data) {
        if(!verticesBooleanData.containsKey(name)) {
            BooleanArrayList dataArray = new BooleanArrayList(mesh.getVertices().size());
            dataArray.size(mesh.getVertices().size());
            verticesBooleanData.put(name, dataArray);
        }
        BooleanArrayList dataArray = verticesBooleanData.get(name);
        assert dataArray.size() == mesh.getVertices().size();
        dataArray.set(vertex.getId(), data);
    }

    @Override
    public void setBooleanData(@NotNull final AHalfEdge edge, @NotNull final String name, final boolean data) {
        if(!halfEdgesBooleanData.containsKey(name)) {
            BooleanArrayList dataArray = new BooleanArrayList(mesh.getEdges().size());
            dataArray.size(mesh.getEdges().size());
            halfEdgesBooleanData.put(name, dataArray);
        }
        BooleanArrayList dataArray = halfEdgesBooleanData.get(name);
        assert dataArray.size() == mesh.getEdges().size();
        dataArray.set(edge.getId(), data);
    }

    private void fill(@NotNull final ObjectArrayList<?> data, final int n) {
        for(int i = 0; i < n; i++) {
            data.add(null);
        }
    }

    private <CE> AObjectArrayList<CE> getObjectArrayEdge(@NotNull final String name, @NotNull final Class<CE> clazz) {
        if(!halfEdgesData.containsKey(name)) {
            AObjectArrayList<CE> dataArray = new AObjectArrayList<>();
            fill(dataArray, mesh.getEdges().size());
            halfEdgesData.put(name, dataArray);
        }
        return (AObjectArrayList<CE>)halfEdgesData.get(name);
    }

    private <CE> AObjectArrayList<CE> getObjectArrayVertex(@NotNull final String name, @NotNull final Class<CE> clazz) {
        if(!verticesData.containsKey(name)) {
            AObjectArrayList<CE> dataArray = new AObjectArrayList<>();
            fill(dataArray, mesh.getVertices().size());
            verticesData.put(name, dataArray);
        }
        return (AObjectArrayList<CE>)verticesData.get(name);
    }

    private DoubleArrayList getDoubleArrayEdge(@NotNull final String name) {
        if(!halfEdgesDoubleData.containsKey(name)) {
            DoubleArrayList dataArray = new DoubleArrayList(mesh.getEdges().size());
            dataArray.size(mesh.getEdges().size());
            halfEdgesDoubleData.put(name, dataArray);
        }
        return halfEdgesDoubleData.get(name);
    }

    private DoubleArrayList getDoubleArrayVertex(@NotNull final String name) {
        if(!verticesDoubleData.containsKey(name)) {
            DoubleArrayList dataArray = new DoubleArrayList(mesh.getVertices().size());
            dataArray.size(mesh.getVertices().size());
            verticesDoubleData.put(name, dataArray);
        }
        return verticesDoubleData.get(name);
    }

    private DoubleArrayList getDoubleArrayFace(@NotNull final String name) {
        if(!facesDoubleData.containsKey(name)) {
            DoubleArrayList dataArray = new DoubleArrayList(mesh.getFaces().size());
            dataArray.size(mesh.getFaces().size());
            facesDoubleData.put(name, dataArray);
        }
        return facesDoubleData.get(name);
    }

    private BooleanArrayList getBooleanArrayEdge(@NotNull final String name) {
        if(!halfEdgesBooleanData.containsKey(name)) {
            BooleanArrayList dataArray = new BooleanArrayList(mesh.getEdges().size());
            dataArray.size(mesh.getEdges().size());
            halfEdgesBooleanData.put(name, dataArray);
        }
        return halfEdgesBooleanData.get(name);
    }

    private BooleanArrayList getBooleanArrayVertex(@NotNull final String name) {
        if(!verticesBooleanData.containsKey(name)) {
            BooleanArrayList dataArray = new BooleanArrayList(mesh.getVertices().size());
            dataArray.size(mesh.getVertices().size());
            verticesBooleanData.put(name, dataArray);
        }
        return verticesBooleanData.get(name);
    }

    private BooleanArrayList getBooleanArrayFace(@NotNull final String name) {
        if(!facesBooleanData.containsKey(name)) {
            BooleanArrayList dataArray = new BooleanArrayList(mesh.getFaces().size());
            dataArray.size(mesh.getFaces().size());
            facesBooleanData.put(name, dataArray);
        }
        return facesBooleanData.get(name);
    }

    public IMeshDataStorage<AVertex, AHalfEdge, AFace> clone(IMesh<AVertex, AHalfEdge, AFace> mesh){
        AMeshDataStorage clone = new AMeshDataStorage(mesh);

        // no deep copy of object properties
        clone.facesData = facesData;
        clone.verticesData = verticesData;
        clone.halfEdgesData = halfEdgesData;

        // deep copy of primitive properties
        Map<String, DoubleArrayList> clonedFacesDoubleData = new HashMap<>();
        for(var entry : facesDoubleData.entrySet()) {
            clonedFacesDoubleData.put(entry.getKey(), entry.getValue().clone());
        }
        clone.facesDoubleData = clonedFacesDoubleData;

        Map<String, DoubleArrayList> clonedHalfEdgesDoubleData = new HashMap<>();
        for(var entry : halfEdgesDoubleData.entrySet()) {
            clonedHalfEdgesDoubleData.put(entry.getKey(), entry.getValue().clone());
        }
        clone.halfEdgesDoubleData = clonedHalfEdgesDoubleData;

        Map<String, DoubleArrayList> clonedVerticessDoubleData = new HashMap<>();
        for(var entry : verticesDoubleData.entrySet()) {
            clonedVerticessDoubleData.put(entry.getKey(), entry.getValue().clone());
        }
        clone.verticesDoubleData = clonedVerticessDoubleData;

        ArrayList<DoubleArrayList> clonedVerticessIndexedDoubleData = new ArrayList<>();
        for(var entry : verticesIndexedDoubleData) {
            clonedVerticessIndexedDoubleData.add(entry.clone());
        }
        clone.verticesIndexedDoubleData = clonedVerticessIndexedDoubleData;

        Map<String, BooleanArrayList> clonedFacesBooleanData = new HashMap<>();
        for(var entry : facesBooleanData.entrySet()) {
            clonedFacesBooleanData.put(entry.getKey(), entry.getValue().clone());
        }
        clone.facesBooleanData = clonedFacesBooleanData;

        Map<String, BooleanArrayList> clonedHalfEdgesBooleanData = new HashMap<>();
        for(var entry : halfEdgesBooleanData.entrySet()) {
            clonedHalfEdgesBooleanData.put(entry.getKey(), entry.getValue().clone());
        }
        clone.halfEdgesBooleanData = clonedHalfEdgesBooleanData;

        Map<String, BooleanArrayList> clonedVerticessBooleanData = new HashMap<>();
        for(var entry : verticesBooleanData.entrySet()) {
            clonedVerticessBooleanData.put(entry.getKey(), entry.getValue().clone());
        }
        clone.verticesBooleanData = clonedVerticessBooleanData;

        return clone;
    }

    @Override
    public <CV> IVertexContainerObject<AVertex, AHalfEdge, AFace, CV> getObjectVertexContainer(@NotNull final String name, final Class<CV> clazz) {
        return new IVertexContainerObject<>() {
            private final ObjectArrayList<CV> list = getObjectArrayVertex(name, clazz);

            @Override
            public CV getValue(@NotNull final AVertex v) {
                return list.get(v.getId());
            }

            @Override
            public void setValue(@NotNull final AVertex v, CV value) {
                list.set(v.getId(), value);
            }
        };
    }

    @Override
    public <CV> IEdgeContainerObject<AVertex, AHalfEdge, AFace, CV> getObjectEdgeContainer(@NotNull final String name, final Class<CV> clazz) {
        return new IEdgeContainerObject<>() {
            private final ObjectArrayList<CV> list = getObjectArrayEdge(name, clazz);

            @Override
            public CV getValue(@NotNull final AHalfEdge edge) {
                return list.get(edge.getId());
            }

            @Override
            public void setValue(@NotNull final AHalfEdge edge, CV value) {
                list.set(edge.getId(), value);
            }
        };
    }

    @Override
    public IEdgeContainerBoolean<AVertex, AHalfEdge, AFace> getBooleanEdgeContainer(@NotNull final String name) {
        return new IEdgeContainerBoolean<>() {
            private final BooleanArrayList list = getBooleanArrayEdge(name);

            @Override
            public boolean getValue(@NotNull final AHalfEdge vertex) {
                return list.getBoolean(vertex.getId());
            }

            @Override
            public void setValue(@NotNull final AHalfEdge vertex, final boolean value) {
                list.set(vertex.getId(), value);
            }
        };
    }

    @Override
    public IMesh<AVertex, AHalfEdge, AFace> getMesh() {
        return mesh;
    }

    @Override
    public IEdgeContainerDouble<AVertex, AHalfEdge, AFace> getDoubleEdgeContainer(@NotNull final String name) {
        return new IEdgeContainerDouble<>() {
            private final DoubleArrayList list = getDoubleArrayEdge(name);

            @Override
            public double getValue(@NotNull final AHalfEdge edge) {
                return list.getDouble(edge.getId());
            }

            @Override
            public void setValue(@NotNull final AHalfEdge edge, final double value) {
                list.set(edge.getId(), value);
            }
        };
    }

    @Override
    public IVertexContainerDouble<AVertex, AHalfEdge, AFace> getDoubleVertexContainer(@NotNull final String name) {
        return new IVertexContainerDouble<>() {
            private DoubleArrayList list = getDoubleArrayVertex(name);

            @Override
            public double getValue(@NotNull final AVertex vertex) {
                return list.getDouble(vertex.getId());
            }

            @Override
            public void setValue(@NotNull final AVertex vertex, final double value) {
                list.set(vertex.getId(), value);
            }

            @Override
            public void reset() {
                verticesDoubleData.remove(name);
                list = getDoubleArrayVertex(name);
            }
        };
    }

    @Override
    public IVertexContainerBoolean<AVertex, AHalfEdge, AFace> getBooleanVertexContainer(@NotNull String name) {
        return new IVertexContainerBoolean<>() {
            private final BooleanArrayList list = getBooleanArrayVertex(name);

            @Override
            public boolean getValue(@NotNull final AVertex vertex) {
                return list.getBoolean(vertex.getId());
            }

            @Override
            public void setValue(@NotNull final AVertex vertex, final boolean value) {
                list.set(vertex.getId(), value);
            }
        };
    }
}
