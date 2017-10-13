package org.vadere.util.geometry.mesh.gen;

import org.jetbrains.annotations.NotNull;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.vadere.util.geometry.shapes.IPoint;

import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Collection;

/**
 * @author Benedikt Zoennchen
 */
public class CLGatherer {

    public static <P extends IPoint> DoubleBuffer getVerticesD(@NotNull final AMesh<P> mesh) {
        Collection<AVertex<P>> vertices = mesh.getVertices();
        DoubleBuffer vertexBuffer = MemoryUtil.memAllocDouble(vertices.size()*2);
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

    public static <P extends IPoint> FloatBuffer getVerticesF(@NotNull final AMesh<P> mesh) {
        Collection<AVertex<P>> vertices = mesh.getVertices();
        FloatBuffer vertexBuffer = MemoryUtil.memAllocFloat(vertices.size()*2);
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

    public static <P extends IPoint> IntBuffer getEdges(@NotNull final AMesh<P> mesh) {
        Collection<AHalfEdge<P>> edges = mesh.getEdges();
        IntBuffer edgeBuffer =  MemoryUtil.memAllocInt(edges.size()*4);
        int index = 0;
        for(AHalfEdge<P> edge : edges) {
            edgeBuffer.put(index, mesh.getPrev(edge).getEnd());
            index++;
            edgeBuffer.put(index, edge.getEnd());
            index++;
            edgeBuffer.put(index, mesh.getFace(edge).getId());
            index++;
            edgeBuffer.put(index, mesh.getFace(mesh.getTwin(edge)).getId());
            index++;
        }
        return edgeBuffer;
    }

    public static <P extends IPoint> IntBuffer getFaces(@NotNull final AMesh<P> mesh) {
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