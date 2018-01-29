package org.vadere.util.geometry.mesh.gen;

import org.jetbrains.annotations.NotNull;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.triangulation.IPointConstructor;

import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Collection;
import java.util.List;

/**
 * @author Benedikt Zoennchen
 */
public class CLGatherer {

    public static <P extends IPoint> DoubleBuffer getVerticesD(@NotNull final AMesh<P> mesh, @NotNull final DoubleBuffer vertexBuffer) {
        Collection<AVertex<P>> vertices = mesh.getVertices();
        int index = 0;
        for(AVertex<P> vertex : vertices) {
            assert index/2.0 == vertex.getId();
            vertexBuffer.put(index, vertex.getX());
            index++;
            vertexBuffer.put(index, vertex.getY());
            index++;
        }
        return vertexBuffer;
    }


    public static <P extends IPoint> DoubleBuffer getVerticesD(@NotNull final AMesh<P> mesh) {
        Collection<AVertex<P>> vertices = mesh.getVertices();
        return getVerticesD(mesh, MemoryUtil.memAllocDouble(vertices.size()*2));
    }

    public static <P extends IPoint> FloatBuffer getVerticesF(@NotNull final AMesh<P> mesh, @NotNull final FloatBuffer vertexBuffer) {
        Collection<AVertex<P>> vertices = mesh.getVertices();
        int index = 0;
        for(AVertex<P> vertex : vertices) {
            assert index/2.0 == vertex.getId();
            vertexBuffer.put(index, (float)vertex.getX());
            index++;
            vertexBuffer.put(index, (float)vertex.getY());
            index++;
        }
        return vertexBuffer;
    }

    public static <P extends IPoint> FloatBuffer getVerticesF(@NotNull final AMesh<P> mesh) {
        Collection<AVertex<P>> vertices = mesh.getVertices();
        return getVerticesF(mesh, MemoryUtil.memAllocFloat(vertices.size()*2));
    }

    public static <P extends IPoint> void scatterHalfEdges(@NotNull final AMesh<P> mesh, @NotNull final IntBuffer edgeBuffer) {
        List<AHalfEdge<P>> edges = mesh.getEdges();

        int index = 0;
        for(AHalfEdge<P> edge : edges) {
            int edgeId = index / 4;
            int vertexId = edgeBuffer.get(index);
            int nextId = edgeBuffer.get(index+1);
            int twinId = edgeBuffer.get(index+2);
            int faceId = edgeBuffer.get(index+3);

            edge.setEnd(vertexId);
            edge.setNext(nextId);
            edge.setTwin(twinId);
            edge.setFace(faceId);

            edges.get(nextId).setPrevious(edgeId);
            index += 4;
        }
    }

    public static <P extends IPoint> IntBuffer getHalfEdges(@NotNull final AMesh<P> mesh) {
        Collection<AHalfEdge<P>> edges = mesh.getEdges();
        IntBuffer edgeBuffer =  MemoryUtil.memAllocInt(edges.size()*4);

        int index = 0;
        for(AHalfEdge<P> edge : edges) {
            edgeBuffer.put(index, edge.getEnd());
            edgeBuffer.put(index+1, edge.getNext());
            edgeBuffer.put(index+2, edge.getTwin());
            edgeBuffer.put(index+3, edge.getFace());
            index += 4;
        }
        return edgeBuffer;
    }

    public static <P extends IPoint> void scatterFaces(@NotNull final AMesh<P> mesh, @NotNull final IntBuffer faceBuffer) {
        Collection<AFace<P>> faces = mesh.getFaces();

        int index = 0;
        for(AFace<P> face : faces) {
            face.setEdge(faceBuffer.get(index));
            index += 2;
        }
    }

    public static <P extends IPoint> IntBuffer getFaces(@NotNull final AMesh<P> mesh) {
        Collection<AFace<P>> faces = mesh.getFaces();
        IntBuffer faceBuffer =  MemoryUtil.memAllocInt(faces.size()*2);

        int index = 0;
        for(AFace<P> face : faces) {
            faceBuffer.put(index, face.getEdge());
            faceBuffer.put(index+1, -1);
            index += 2;
        }
        return faceBuffer;
    }

    public static <P extends IPoint> void scatterVertexToEdge(@NotNull final AMesh<P> mesh, @NotNull final  IntBuffer vertexBuffer) {
        Collection<AVertex<P>> vertices = mesh.getVertices();

        int index = 0;
        for(AVertex<P> vertex : vertices) {
            assert vertex.getId() == index;
            vertex.setEdge(vertexBuffer.get(index));
            index++;
        }
    }

    // TODO: better name
    public static <P extends IPoint> IntBuffer getVertexToEdge(@NotNull final AMesh<P> mesh) {
        Collection<AVertex<P>> vertices = mesh.getVertices();
        IntBuffer vertexBuffer =  MemoryUtil.memAllocInt(vertices.size());

        int index = 0;
        for(AVertex<P> vertex : vertices) {
            assert vertex.getId() == index;
            vertexBuffer.put(index, vertex.getEdge());
            index++;
        }
        return vertexBuffer;
    }

    // TODO: maybe remove duplicated edges
    public static <P extends IPoint> IntBuffer getEdges(@NotNull final AMesh<P> mesh) {
        Collection<AHalfEdge<P>> edges = mesh.getEdges();
        IntBuffer edgeBuffer =  MemoryUtil.memAllocInt(edges.size()*4);
        int index = 0;
        for(AHalfEdge<P> edge : edges) {
            edgeBuffer.put(index, mesh.getPrev(edge).getEnd());
            index++;
            edgeBuffer.put(index, edge.getEnd());
            index++;
            if(mesh.isBoundary(mesh.getFace(edge))) {
                edgeBuffer.put(index, -1);
                index++;
                edgeBuffer.put(index, mesh.getFace(mesh.getTwin(edge)).getId());
                index++;
                if(mesh.isBoundary(mesh.getFace(mesh.getTwin(edge)))) {
                    throw new IllegalArgumentException("invalid mesh!");
                }
            }
            else if(mesh.isBoundary(mesh.getFace(mesh.getTwin(edge)))) {
                edgeBuffer.put(index, mesh.getFace(edge).getId());
                index++;
                edgeBuffer.put(index, -1);
                index++;
                if(mesh.isBoundary(mesh.getFace(edge))) {
                    throw new IllegalArgumentException("invalid mesh!");
                }
            }
            else {
                edgeBuffer.put(index, mesh.getFace(edge).getId());
                index++;
                edgeBuffer.put(index, mesh.getFace(mesh.getTwin(edge)).getId());
                index++;
            }
        }
        return edgeBuffer;
    }

    public static <P extends IPoint> IntBuffer getTwins(@NotNull final AMesh<P> mesh) {
        Collection<AHalfEdge<P>> edges = mesh.getEdges();
        IntBuffer edgeBuffer =  MemoryUtil.memAllocInt(edges.size());
        int index = 0;
        for(AHalfEdge<P> edge : edges) {
            edgeBuffer.put(index, mesh.getTwin(edge).getId());
            index++;
        }
        return edgeBuffer;
    }

    public static <P extends IPoint> IntBuffer getTriangles(@NotNull final AMesh<P> mesh) {
        Collection<AFace<P>> faces = mesh.getFaces();
        IntBuffer faceBuffer = MemoryUtil.memAllocInt(faces.size()*4);
        int index = 0;
        for(AFace<P> face : faces) {
            AHalfEdge<P> edge = mesh.getEdge(face);
            faceBuffer.put(index, edge.getEnd());
            index++;
            edge = mesh.getNext(edge);
            faceBuffer.put(index, edge.getEnd());
            index++;
            edge = mesh.getNext(edge);
            faceBuffer.put(index, edge.getEnd());
            index++;
            faceBuffer.put(index, 0);
            index++;
        }
        return faceBuffer;
    }

}
