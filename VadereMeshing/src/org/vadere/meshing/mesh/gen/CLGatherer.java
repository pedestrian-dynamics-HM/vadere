package org.vadere.meshing.mesh.gen;

import org.jetbrains.annotations.NotNull;
import org.lwjgl.system.MemoryUtil;
import org.vadere.util.geometry.shapes.IPoint;

import java.nio.Buffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Collection;
import java.util.List;

/**
 * The {@link CLGatherer} gathers and scatters data between a {@link AMesh}
 * and the heap memory represented by buffers ({@link Buffer}) such that one
 * can transfer the {@link Buffer} and therefore the elements of {@link AMesh}
 * to the Graphical Processing Unit (GPU) via OpenCL.
 *
 * @author Benedikt Zoennchen
 */
public class CLGatherer {

	/**
	 * <p>Writes all coordinates of vertices / points p1 = (x1, y1), p2 = (x2, y2), ... pn = (xn, yn) successively,
	 * i.e. (x1,y1,x2,y2,...,xn,yn) into the {@link DoubleBuffer}.</p>
	 *
	 * <p>Assumption: The {@link DoubleBuffer} vertexBuffer is correctly sized,
	 * i.e. 2*n where n is the number of vertices of the mesh.</p>
	 *
	 * @param mesh          the mesh which contains the points.
	 * @param vertexBuffer  the memory in which the coordinates of the vertices / points will be written.
	 *
	 * @return the heap memory {@link DoubleBuffer} containing all coordinates of all all vertices / points in a successive order
	 */
    public static DoubleBuffer getVerticesD(@NotNull final AMesh mesh, @NotNull final DoubleBuffer vertexBuffer) {
        Collection<AVertex> vertices = mesh.getVertices();
        int index = 0;
        for(AVertex vertex : vertices) {
            assert index/2.0 == vertex.getId();
            vertexBuffer.put(index, vertex.getX());
            index++;
            vertexBuffer.put(index, vertex.getY());
            index++;
        }
        return vertexBuffer;
    }

	/**
	 * <p>Writes all coordinates of vertices / points p1 = (x1, y1), p2 = (x2, y2), ... pn = (xn, yn) successively,
	 * i.e. (x1,y1,x2,y2,...,xn,yn) into the {@link DoubleBuffer}.</p>
	 *
	 * @param mesh the mesh which contains the points.
	 *
	 * @return the heap memory {@link DoubleBuffer} containing all coordinates of all all vertices / points in a successive order
	 */
	public static DoubleBuffer getVerticesD(@NotNull final AMesh mesh) {
        Collection<AVertex> vertices = mesh.getVertices();
        return getVerticesD(mesh, MemoryUtil.memAllocDouble(vertices.size()*2));
    }

	/**
	 * <p>Writes all coordinates of vertices / points p1 = (x1, y1), p2 = (x2, y2), ... pn = (xn, yn) successively,
	 * i.e. (x1,y1,x2,y2,...,xn,yn) into the {@link FloatBuffer} by casting double to float</p>
	 *
	 * <p>Assumption: The {@link FloatBuffer} vertexBuffer is correctly sized,
	 * i.e. 2*n where n is the number of vertices of the mesh.</p>
	 *
	 * @param mesh          the mesh which contains the points.
	 * @param vertexBuffer  the memory in which the coordinates of the vertices / points will be written.
	 *
	 * @return the heap memory {@link FloatBuffer} containing all coordinates of all all vertices / points in a successive order
	 */
    public static FloatBuffer getVerticesF(@NotNull final AMesh mesh, @NotNull final FloatBuffer vertexBuffer) {
        Collection<AVertex> vertices = mesh.getVertices();
        int index = 0;
        for(AVertex vertex : vertices) {
            assert index/2.0 == vertex.getId();
            vertexBuffer.put(index, (float)vertex.getX());
            index++;
            vertexBuffer.put(index, (float)vertex.getY());
            index++;
        }
        return vertexBuffer;
    }

	/**
	 * <p>Writes all coordinates of vertices / points p1 = (x1, y1), p2 = (x2, y2), ... pn = (xn, yn) successively,
	 * i.e. (x1,y1,x2,y2,...,xn,yn) into the {@link FloatBuffer} by casting double to float</p>
	 *
	 * @param mesh  the mesh which contains the points.
	 *
	 * @return the heap memory {@link FloatBuffer} containing all coordinates of all all vertices / points in a successive order
	 */
    public static FloatBuffer getVerticesF(@NotNull final AMesh mesh) {
        Collection<AVertex> vertices = mesh.getVertices();
        return getVerticesF(mesh, MemoryUtil.memAllocFloat(vertices.size()*2));
    }

	/**
	 * <p>Scatters / writes / sets the array-indices of half-edges ({@link AHalfEdge}) at the heap memory {@link IntBuffer}
	 * to the mesh in order of the mesh-ordering. This is the reverse operation of {@link CLGatherer#getHalfEdges(AMesh)}</p>
	 *
	 * <p>Assumption: The i-th half-edge in the heap memory represents the i-th half-edge of the {@link AMesh}. The heap memory
	 * has to look like (edgeId1, vertexId1, nextId1, twinId1, faceId1, edgeId2, vertexId2, nextId2, twinId2, faceId2,
	 * ..., edgeIdn, vertexIdn, nextIdn, twinIdn, faceIdn), where n is the number of half-edges of the mesh.</p>
	 *
	 * @param mesh          the mesh receiving the indices
	 * @param edgeBuffer    the heap memory from which the indices will be read
	 */
	public static void scatterHalfEdges(@NotNull final AMesh mesh, @NotNull final IntBuffer edgeBuffer) {
        List<AHalfEdge> edges = mesh.getEdges();

        int index = 0;
        for(AHalfEdge edge : edges) {
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

	/**
	 * <p>Gathers / writes the indices of all half-edges ({@link AHalfEdge}) of a {@link AMesh} to the heap memory {@link IntBuffer}
	 * in order of the mesh-ordering. This is the reverse operation of {@link CLGatherer#scatterHalfEdges(AMesh, IntBuffer)}</p>
	 *
	 * @param mesh  the mesh from which the indices will be received
	 * @return the heap memory {@link IntBuffer} containing all indices of all half-edges
	 */
	public static IntBuffer getHalfEdges(@NotNull final AMesh mesh) {
        Collection<AHalfEdge> edges = mesh.getEdges();
        IntBuffer edgeBuffer =  MemoryUtil.memAllocInt(edges.size()*4);

        int index = 0;
        for(AHalfEdge edge : edges) {
            edgeBuffer.put(index, edge.getEnd());
            edgeBuffer.put(index+1, edge.getNext());
            edgeBuffer.put(index+2, edge.getTwin());
            edgeBuffer.put(index+3, edge.getFace());
            /*if(edge.getFace()==-1){
                System.out.println("border edge" + (index / 4));
            }*/
            index += 4;
        }
        return edgeBuffer;
    }

	/**
	 * <p>Scatters / writes / sets the array-indices of faces ({@link AFace}) at the heap memory {@link IntBuffer}
	 * to the mesh in order of the mesh-ordering. This is the reverse operation of {@link CLGatherer#getFaces(AMesh)}</p>
	 *
	 * <p>Assumption: The i-th face in the heap memory represents the i-th face of the {@link AMesh}. The heap memory
	 * has to look like (faceId1, border1, faceId2, border2 ... faceIdn), where n is the number of faces of the mesh.</p>
	 *
	 * @param mesh          the mesh receiving the indices
	 * @param faceBuffer    the heap memory from which the indices will be read
	 */
    public static void scatterFaces(@NotNull final AMesh mesh, @NotNull final IntBuffer faceBuffer) {
        Collection<AFace> faces = mesh.getFaces();

        int index = 0;
        for(AFace face : faces) {
            face.setEdge(faceBuffer.get(index));
            // TODO: why +2 instead of +1 here?
            index += 2;
        }
    }

	/**
	 * <p>Gathers / writes the indices of all faces ({@link AFace}) of a {@link AMesh} to the heap memory {@link IntBuffer}
	 * in order of the mesh-ordering. This is the reverse operation of {@link CLGatherer#scatterFaces(AMesh, IntBuffer)},
	 * except the border information is set to -1, i.e. ignored</p>
	 *
	 * @param mesh  the mesh from which the indices will be received
	 * @return the heap memory {@link IntBuffer} containing all indices of all faces
	 */
    public static IntBuffer getFaces(@NotNull final AMesh mesh) {
        Collection<AFace> faces = mesh.getFaces();
        IntBuffer faceBuffer =  MemoryUtil.memAllocInt(faces.size()*2);

        int index = 0;
        for(AFace face : faces) {
            faceBuffer.put(index, face.getEdge());
            // TODO: why we set -1 here?
            faceBuffer.put(index+1, -1);
            index += 2;
        }
        return faceBuffer;
    }

	/**
	 * <p>Scatters / writes / sets the array-indices of the edge of each vertex ({@link AVertex}) which is saved at heap memory {@link IntBuffer}
	 * to the mesh in order of the mesh-ordering. This is the reverse operation of {@link CLGatherer#getEdgeOfVertex(AMesh)}</p>
	 *
	 * <p>Assumption: The i-th edge index in the heap memory represents the edge index of the edge of the i-th vertex of the {@link AMesh}.
	 * The heap memory has to look like (edgeId1, edgeId2,..., edgeIdn), where n is the number of vertices of the mesh.</p>
	 *
	 * @param mesh          the mesh receiving the indices
	 * @param edgeOfVertex  the heap memory from which the indices will be read
	 */
    public static void scatterEdgeOfVertex(@NotNull final AMesh mesh, @NotNull final  IntBuffer edgeOfVertex) {
        Collection<AVertex> vertices = mesh.getVertices();

        int index = 0;
        for(AVertex vertex : vertices) {
            assert vertex.getId() == index;
            vertex.setEdge(edgeOfVertex.get(index));
            index++;
        }
    }

	/**
	 * <p>Gathers / writes all edge-indices of all vertices ({@link AVertex}) of a {@link AMesh} to the heap memory {@link IntBuffer}
	 * in order of the mesh-ordering. This is the reverse operation of {@link CLGatherer#scatterEdgeOfVertex(AMesh, IntBuffer)}</p>
	 *
	 * @param mesh  the mesh from which the indices will be received
	 * @return      the heap memory {@link IntBuffer} containing all edge-indices of all vertices
	 */
    public static IntBuffer getEdgeOfVertex(@NotNull final AMesh mesh) {
        Collection<AVertex> vertices = mesh.getVertices();
        IntBuffer vertexBuffer =  MemoryUtil.memAllocInt(vertices.size());

        int index = 0;
        for(AVertex vertex : vertices) {
            assert vertex.getId() == index;
            vertexBuffer.put(index, vertex.getEdge());
            index++;
        }
        return vertexBuffer;
    }

	/**
	 * <p>Gathers / writes for each vertex a 1 if and only if the vertex ({@link AVertex}) is a boundary vertex and 0 otherwise to
	 * to the heap memory {@link IntBuffer} in order of the mesh-ordering of {@link AMesh}.</p>
	 *
	 * @param mesh  the mesh from which the boundary information is received
	 *
	 * @return      the heap memory {@link IntBuffer} containing the boundary vertex information (1 if the vertex is a boundary vertex and 0 otherwise)
	 */
	public static IntBuffer getBorderVertices(@NotNull final AMesh mesh) {
        Collection<AVertex> vertices = mesh.getVertices();
        IntBuffer vertexBuffer =  MemoryUtil.memAllocInt(vertices.size());

        int index = 0;
        for(AVertex vertex : vertices) {
            assert vertex.getId() == index;
            vertexBuffer.put(index, mesh.isAtBoundary(vertex) ? 1 : 0);
            index++;
        }
        return vertexBuffer;
    }



	/**
	 * Gathers / writes for each half-edge {@link AHalfEdge}
	 * <ol>
	 *     <li>the index of the vertex the half-edge ends</li>
	 *     <li>the index of the vertex the half-edge starts, i.e. the previous ends</li>
	 *     <li>the index of the face of the half-edge or -1 if there is no face, i.e. the half-edge is a boundary edge</li>
	 *     <li>the index of the face of the twin of the half-edge or - if thre is no face, i.e. the half-edge is a boundary edge</li>
	 * </ol>
	 * to the heap memory {@link IntBuffer} in order of the mesh-ordering of {@link AMesh}.
	 *
	 * @param mesh  the mesh from which the boundary information is received
	 * @return      the heap memory {@link IntBuffer} containing (endId, startId, faceId, twinFaceId, ....)
	 * @see <a href="https://www.dcc.uchile.cl/TR/2011/TR_DCC-20110228-002.pdf">Data structure and its purpose</a>
	 */
    public static IntBuffer getEdges(@NotNull final AMesh mesh) {
	    // TODO: maybe remove duplicated edges
    	Collection<AHalfEdge> edges = mesh.getEdges();
        IntBuffer edgeBuffer =  MemoryUtil.memAllocInt(edges.size()*4);
        int index = 0;
        for(AHalfEdge edge : edges) {
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

	/**
	 * <p>Gathers / writes all twin-indices of all half-edges ({@link AHalfEdge}) of a {@link AMesh} to the heap memory {@link IntBuffer}
	 * in order of the mesh-ordering.</p>
	 *
	 * @param mesh  the mesh from which the indices will be received
	 * @return      the heap memory {@link IntBuffer} containing all twin-indices of all vertices
	 */
	public static IntBuffer getTwins(@NotNull final AMesh mesh) {
        Collection<AHalfEdge> edges = mesh.getEdges();
        IntBuffer edgeBuffer =  MemoryUtil.memAllocInt(edges.size());
        int index = 0;
        for(AHalfEdge edge : edges) {
            edgeBuffer.put(index, mesh.getTwin(edge).getId());
            index++;
        }
        return edgeBuffer;
    }

	/**
	 * <p>Gathers / writes the indices of all half-edges {@link AHalfEdge} of all faces ({@link AFace}) of a {@link AMesh}
	 * to the heap memory {@link IntBuffer} padding it by one additional entry which is 0, i.e. the memory looks like
	 * (e1f1, e2f1, e3f1, 0, e1f2, e2f2, e3fn, ..., e1fn, e2fn, e3fn).</p>
	 *
	 * <p>Assumption: All interior faces of the mesh are triangles.</p>
	 *
	 * @param mesh  the mesh from which the indices will be received
	 * @return      the heap memory {@link IntBuffer} containing all edge-indices of all faces
	 */
	public static IntBuffer getTriangles(@NotNull final AMesh mesh) {
        Collection<AFace> faces = mesh.getFaces();
        IntBuffer faceBuffer = MemoryUtil.memAllocInt(faces.size()*4);
        int index = 0;
        for(AFace face : faces) {
            AHalfEdge edge = mesh.getEdge(face);
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
