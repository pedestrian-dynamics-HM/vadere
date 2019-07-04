package org.vadere.meshing.utils.io.poly;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vadere.meshing.mesh.inter.IFace;
import org.vadere.meshing.mesh.inter.IHalfEdge;
import org.vadere.meshing.mesh.inter.IMesh;
import org.vadere.meshing.mesh.inter.IVertex;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * The {@link MeshPolyReader} reads a ascii-poly file and transforms it into a {@link IMesh}.
 * It is the counterpart to {@link MeshPolyWriter}.
 *
 * @param <V> the type of the vertices
 * @param <E> the type of the half-edges
 * @param <F> the type of the faces
 */
public class MeshPolyReader<V extends IVertex, E extends IHalfEdge, F extends IFace> {

	private final static String SPLITTER = "\\s+";
	private final static String COMMENT_MARKER = "#";

	/**
	 * a Functions id -> vertex which gives for a specific vertex id the correct vertex
	 */
	private final Map<Integer, V> vertices;

	/**
	 * a Function (startVertexId, endVertexId) -> halfEdge which gives for a specific pair of ids the correct half-edge
	 */
	private final Map<Pair<Integer, Integer>, E> edges;

	/**
	 * the mesh for which the face will be added
	 */
	private IMesh<V, E, F> mesh;

	private final Supplier<IMesh<V, E, F>> meshSupplier;

	public MeshPolyReader(@NotNull final Supplier<IMesh<V, E, F>> meshSupplier) {
		this.meshSupplier = meshSupplier;
		this.edges = new HashMap<>();
		this.vertices = new HashMap<>();
	}

	/**
	 * Adds a face to the mesh by parsing the String <tt>line</tt> which should represent a face.
	 *
	 * @param line a String that represents the face
	 * @param face the face object which was already be added
	 */
	private void addFace(@NotNull final String line, @NotNull final F face) {

		String[] split = line.split(SPLITTER);
		int nVertices = Integer.parseInt(split[0].strip());
		assert nVertices == split.length-1;
		List<Integer> vertexIds = new ArrayList<>(nVertices);
		for(int i = 0; i < nVertices; i++) {
			vertexIds.add(Integer.parseInt(split[i+1].strip()));
		}

		List<E> ccwEdges = new ArrayList<>(nVertices);
		for(int i = 0; i < nVertices; i++) {
			int i1 = vertexIds.get(i);
			int i2 = vertexIds.get((i+1) % nVertices);
			V v1 = vertices.get(i1);
			V v2 = vertices.get(i2);
			E edge = mesh.createEdge(v2);
			mesh.setFace(edge, face);
			mesh.setEdge(face, edge);

			if(mesh.getEdge(v2) == null || !mesh.isBoundary(mesh.getEdge(v2))) {
				mesh.setEdge(v2, edge);
			}

			edges.put(Pair.of(i1, i2), edge);

			if(edges.containsKey(Pair.of(i2, i1))) {
				E twin = edges.get(Pair.of(i2, i1));
				mesh.setTwin(edge, twin);
			}

			ccwEdges.add(edge);
		}

		for(int i = 0; i < nVertices; i++) {
			E edge = ccwEdges.get(i);
			E next = ccwEdges.get((i+1)%nVertices);
			mesh.setNext(edge, next);
		}
	}

	/**
	 * Reads a PSLG (.poly) from an {@link InputStream} and converts the file into a {@link IMesh}.
	 *
	 * @param inputStream   the input stream
	 *
	 * @return a {@link Math} containing all segments and holes of the PSLG
	 *
	 * @throws IOException
	 */
	public IMesh<V, E, F> readMesh(@NotNull final InputStream inputStream) throws IOException {
		return toMesh(inputStream, null);
	}


	public IMesh<V, E, F> readMesh(@NotNull final InputStream inputStream, @NotNull final Function<Integer, String> attrNameFunc) throws IOException {
		return toMesh(inputStream, attrNameFunc);
	}

	/**
	 * Reads a PSLG (.poly) from an {@link InputStream} and converts the file into a {@link IMesh}.
	 *
	 * @param inputStream   the input stream
	 * @return a {@link Math} containing all segments and holes of the PSLG
	 *
	 * @throws IOException
	 */
	private IMesh<V, E, F> toMesh(
			@NotNull final InputStream inputStream,
			@Nullable final Function<Integer, String> attrNameFunc) throws IOException {
		mesh = meshSupplier.get();
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

		String line = readLine(reader);
		String[] split = line.split(SPLITTER);
		int nVertex = Integer.parseInt(split[0].strip());
		int dimension = Integer.parseInt(split[1].strip());
		int boundaryMarker = Integer.parseInt(split[2].strip());
		int targetMarker = Integer.parseInt(split[3].strip());
		int nAttributes = Integer.parseInt(split[4].strip());

		if(nAttributes > 1) {
			throw new IOException("number of attributes > 1, is not jet supported.");
		}

		for(int i = 0; i < nVertex; i++) {
			String vertexLine = readLine(reader);
			split = vertexLine.split(SPLITTER);
			int id = Integer.parseInt(split[0].strip());
			int boundaryMark = Integer.parseInt(split[1].strip());
			int targetMark = Integer.parseInt(split[2].strip());
			double x = Double.parseDouble(split[3].strip());
			double y = Double.parseDouble(split[4].strip());

			V vertex = mesh.insertVertex(x, y);
			vertices.put(id, vertex);
			// TODO: ? boundaryMark?
			if(attrNameFunc != null) {
				for(int j = 1; j <= nAttributes; j++) {
					mesh.setDoubleData(vertex, attrNameFunc.apply(j), Double.parseDouble(split[4+j]));
				}
			}
		}

		// border
		// this is always 1
		String nBorderString = readLine(reader);
		assert Integer.parseInt(nBorderString.strip()) == 1;
		String borderVertices = readLine(reader);
		addFace(borderVertices, mesh.getBorder());

		// triangles
		Integer nTriangles = Integer.parseInt(readLine(reader).strip());
		for(int i = 0; i < nTriangles; i++) {
			F face = mesh.createFace();
			addFace(readLine(reader), face);
		}

		// holes
		Integer nHoles = Integer.parseInt(readLine(reader).strip());
		for(int i = 0; i < nHoles; i++) {
			F face = mesh.createFace(true);
			addFace(readLine(reader), face);
		}

		edges.clear();
		vertices.clear();

		assert mesh.isValid();
		return mesh;
	}

	private static String readLine(@NotNull final BufferedReader reader) throws IOException {
		String st;
		while ((st = reader.readLine()) != null) {
			st = st.strip();
			if(!st.startsWith(COMMENT_MARKER) || st.strip().equals("")) {
				break;
			}
		}
		return st.strip();
	}
}
