package org.vadere.meshing.utils.io.poly;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.impl.PSLG;
import org.vadere.meshing.mesh.inter.IFace;
import org.vadere.meshing.mesh.inter.IHalfEdge;
import org.vadere.meshing.mesh.inter.IMesh;
import org.vadere.meshing.mesh.inter.IVertex;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VPolygon;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PolyGenerator {

	private final static String SEPARATOR = " ";
	private final static String SPLITTER = "\\s+";

	/**
	 * Transforms a mesh into a String which can be displayed by the gmesh vis-tool.
	 * @param mesh the mesh
	 * @param <V> the type of the vertices
	 * @param <E> the type of the half-edges
	 * @param <F> the type of the faces
	 *
	 * @return a string which can be saved into a .poly file to display the mesh via the gmesh vis-tool
	 */
	public static <V extends IVertex, E extends IHalfEdge, F extends IFace> String to3DPoly(
			@NotNull final IMesh<V, E, F> mesh) {
		int dimension = 3;
		StringBuilder builder = new StringBuilder();
		builder.append("#node\n");
		builder.append(mesh.getNumberOfVertices() + SEPARATOR + dimension + "\n");
		Map<V, Integer> map = new HashMap<>();
		int id = 1;
		for(V v : mesh.getVertices()) {
			map.put(v, id);
			builder.append(id + SEPARATOR + v.getX() + SEPARATOR + v.getY() + SEPARATOR + 0.0 + "\n");
			id++;
		}

		builder.append("#FACET 2D\n");
		builder.append(mesh.getNumberOfFaces()+"\n");

		for(F face : mesh.getFaces()) {
			builder.append("1 0\n");
			builder.append(mesh.getPoints(face).size() + SEPARATOR);
			for(V v : mesh.getVertices(face)) {
				builder.append(map.get(v) + SEPARATOR);
			}
			builder.delete(builder.length()-SEPARATOR.length(), builder.length());
			builder.append("\n");
		}
		builder.append("0 #holes");
		return builder.toString();
	}

	private static String readLine(@NotNull final BufferedReader reader) throws IOException {
		String st;
		while ((st = reader.readLine()) != null) {
			st = st.strip();
			if(!st.startsWith("#") || st.strip().equals("")) {
				break;
			}
		}
		return st.strip();
	}

	/**
	 * Reads a PSLG (.poly) from an {@link InputStream} and converts the file into a {@link IMesh}.
	 *
	 * @param inputStream   the input stream
	 * @param meshSupplier  mesh suppliert to construct an empty mesh
	 *
	 * @param <V> the type of the vertices
	 * @param <E> the type of the half-edges
	 * @param <F> the type of the faces
	 *
	 * @return a {@link Math} containing all segments and holes of the PSLG
	 *
	 * @throws IOException
	 */
	public static <V extends IVertex, E extends IHalfEdge, F extends IFace> IMesh<V, E, F> toMesh(
			@NotNull final InputStream inputStream,
			@NotNull final Supplier<IMesh<V, E, F>> meshSupplier) throws IOException {
		var mesh = meshSupplier.get();

		Map<Integer, V> vertices = new HashMap<>();
		Map<Pair<Integer, Integer>, E> edges = new HashMap<>();

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
			// TODO: attributes ? boundaryMark?
			if(nAttributes == 1) {

			}
		}

		// border
		// this is always 1
		String nBorderString = readLine(reader);
		assert Integer.parseInt(nBorderString.strip()) == 1;
		String borderVertices = readLine(reader);
		toFace(borderVertices, vertices, edges, mesh, mesh.getBorder());

		// triangles
		Integer nTriangles = Integer.parseInt(readLine(reader).strip());
		for(int i = 0; i < nTriangles; i++) {
			F face = mesh.createFace();
			toFace(readLine(reader), vertices, edges, mesh, face);
		}

		// holes
		Integer nHoles = Integer.parseInt(readLine(reader).strip());
		for(int i = 0; i < nHoles; i++) {
			F face = mesh.createFace(true);
			toFace(readLine(reader), vertices, edges, mesh, face);
		}

		assert mesh.isValid();
		return mesh;
	}

	private static <V extends IVertex, E extends IHalfEdge, F extends IFace> void toFace(
			@NotNull final String line,
			@NotNull final Map<Integer, V> vertices,
			@NotNull final Map<Pair<Integer, Integer>, E> edges,
			@NotNull final IMesh<V, E, F> mesh,
			@NotNull final F face) {
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
	 * Transforms a boundary and a list of holes (together these two elements may define a topography) into a string representing a PSLG.
	 *
	 * Assumption: There is no hole intersecting or containing any other hole and the segment-bounding polygon containes all holes
	 *
	 * @param boundary  the segment-bounding polygon
	 * @param holes     holes i.e. polygon contained in the segment-bounded polygon.
	 * @return a {@link String} representation of the PSLG
	 */
	public static String toPSLG(@NotNull final VPolygon boundary, @NotNull final Collection<VPolygon> holes) {
		return PolyGenerator.toPSLG(boundary, holes,true);
	}

	public static String toPSLG(@NotNull final VPolygon boundary, @NotNull final Collection<VPolygon> cHoles, final boolean addComments) {
		StringBuilder builder = new StringBuilder();
		Set<VPoint> vertices = new HashSet<>();
		List<VPolygon> holes = cHoles.stream().collect(Collectors.toList());
		List<VLine> lines = Stream.concat(boundary.getLinePath().stream(), holes.stream().flatMap(hole -> hole.getLinePath().stream()))
				.distinct()
				.collect(Collectors.toList());

		int dimension = 2;
		int nAttributes = 0;
		int boundaryMarker = 0; // no boundary marker

		// filter duplicated points
		for(VLine line : lines) {
			vertices.add(new VPoint(line.x1, line.y1));
			vertices.add(new VPoint(line.x2, line.y2));
		}

		List<VPoint> iVertices = vertices.stream().collect(Collectors.toList());
		Map<VPoint, Integer> indexMap = new HashMap<>();

		if(addComments) {
			builder.append("# nVertices dimension nAttributes boundaryMarker\n");
		}
		builder.append(iVertices.size() + SEPARATOR + dimension + SEPARATOR + nAttributes + SEPARATOR + boundaryMarker);

		if(addComments) {
			builder.append("\n# vertexId x y");
		}
		for(int index = 1; index <= iVertices.size(); index++) {
			VPoint vertex = iVertices.get(index-1);
			builder.append("\n" + String.format(Locale.US, "%d" + SEPARATOR +"%f" + SEPARATOR + "%f", index, vertex.x, vertex.y));
			indexMap.put(vertex, index);
		}

		if(addComments) {
			builder.append("\n#\n# nSegments boundaryMarker");
		}
		builder.append("\n" + lines.size() + SEPARATOR + boundaryMarker);
		if(addComments) {
			builder.append("\n# lineId vertexId1 vertexId2");
		}
		for(int index = 1; index <= lines.size(); index++) {
			VLine line = lines.get(index-1);

			Integer from = indexMap.get(line.getVPoint1());
			assert from != null;
			if(from == null) {
				throw new IllegalArgumentException("could not find index for point " + line.getVPoint1());
			}

			Integer to = indexMap.get(line.getVPoint2());
			assert to != null;
			if(to == null) {
				throw new IllegalArgumentException("could not find index for point " + line.getVPoint2());
			}
			builder.append("\n" + index + SEPARATOR + from + SEPARATOR + to);
		}

		if(addComments) {
			builder.append("\n#\n# nHoles");
		}
		builder.append("\n"+holes.size());
		if(addComments) {
			builder.append("\n# vertexId x y (of a vertex which lies inside the hole)");
		}
		for(int index = 1; index <= holes.size(); index++) {
			VPoint interiorPoint = GeometryUtils.getInteriorPoint(holes.get(index-1));
			builder.append("\n" + String.format(Locale.US, "%d" + SEPARATOR +"%f" + SEPARATOR + "%f", index, interiorPoint.x, interiorPoint.y));
		}

		return builder.toString();
	}

	/**
	 * Reads a PSLG from an {@link InputStream} and transforms the PSLG into a list of {@link VPolygon} polygons and {@link VLine} lines.
	 * The first element of the returning list of polygons is the segment-bounding polygon.
	 *
	 * @param inputStream       the input stream
	 *
	 * @return a list of {@link VPolygon} polygons and {@link VLine} lines representing the PSLG
	 *
	 * @throws IOException
	 */
	public static PSLG toPSLGtoVShapes(@NotNull final InputStream inputStream) throws IOException {
		// (1) read input file
		Map<Integer, VPoint> vertices = new HashMap<>();
		Map<Integer, VPoint> holes = new HashMap<>();
		Map<Integer, LinkedList<Integer>> segments = new HashMap<>();
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
			String st = PolyGenerator.readLine(br);

			String[] split = st.split(SPLITTER);
			if(split.length < 1) {
				throw new IOException("wrong file format.");
			}

			int numberOfVertices = 0;
			int dimension = 2;
			int numberOfAttributes = 0;
			int boundaryMarker = 0;

			if(split.length > 1) {
				numberOfVertices = Integer.parseInt(split[0].strip());
			}
			if(split.length > 2) {
				dimension = Integer.parseInt(split[1].strip());
			}
			if(split.length > 3) {
				numberOfAttributes = Integer.parseInt(split[2].strip());
			}
			if(split.length > 4) {
				boundaryMarker = Integer.parseInt(split[3].strip());
			}

			for(int i = 1; i <= numberOfVertices; i++) {
				st = PolyGenerator.readLine(br);
				if(st == null) {
					throw new IOException("wrong file format: missing vertices");
				}
				split = st.strip().split(SPLITTER);
				if(split.length < 2) {
					throw new IOException("wrong file format: missing vertex coordinate");
				}

				int id = i;
				if(split.length > 2) {
					id = Integer.parseInt(split[0].strip());
				}

				double x = Double.parseDouble(split[1].strip());
				double y = Double.parseDouble(split[2].strip());
				vertices.put(id, new VPoint(x, y));
			}

			st = PolyGenerator.readLine(br);
			split = st.strip().split(SPLITTER);
			int numberOfSegments = 0;
			boundaryMarker = 0;

			if(split.length > 1) {
				numberOfSegments = Integer.parseInt(split[0].strip());
			}
			if(split.length > 2) {
				boundaryMarker = Integer.parseInt(split[1].strip());
			}

			for(int i = 1; i <= numberOfSegments; i++) {
				st = PolyGenerator.readLine(br);
				if(st == null) {
					throw new IOException("wrong file format: missing vertices");
				}
				split = st.strip().split(SPLITTER);
				if(split.length < 2) {
					throw new IOException("wrong file format: missing vertex coordinate");
				}

				int id = i;
				if(split.length > 2) {
					id = Integer.parseInt(split[0].strip());
				}

				int from = Integer.parseInt(split[1].strip());
				int to = Integer.parseInt(split[2].strip());
				if(!segments.containsKey(from)) {
					segments.put(from, new LinkedList<>());
				}

				segments.get(from).add(to);
			}

			st = PolyGenerator.readLine(br);
			int numberOfHoles = 0;
			if(st != null) {
				st = st.strip();
				numberOfHoles = Integer.parseInt(st);

				for(int i = 1; i <= numberOfHoles; i++) {
					st = PolyGenerator.readLine(br);
					split = st.split(SPLITTER);
					if(split.length < 2) {
						throw new IOException("wrong file format: missing vertex coordinate");
					}

					int id = i;
					if(split.length > 2) {
						id = Integer.parseInt(split[0].strip());
					}

					double x = Double.parseDouble(split[1].strip());
					double y = Double.parseDouble(split[2].strip());
					holes.put(id, new VPoint(x,y));
				}
			}

		} catch (NumberFormatException e) {
			throw new IOException("wrong file format: " + e.getMessage());
		}

		// (2) transform to VShapes and VLines
		List<VPolygon> polygons = new ArrayList<>();
		List<VLine> lines = new ArrayList<>();

		while (!segments.isEmpty()) {
			List<Integer> polyIndices = new ArrayList<>();
			var entry = segments.entrySet().iterator().next();
			int start = entry.getKey();
			int from = start;

			polyIndices.add(from);
			do {
				int to = segments.get(from).poll();
				if(segments.get(from).isEmpty()) {
					segments.remove(from);
				}
				polyIndices.add(to);
				from = to;

			} while (from != start && segments.containsKey(from));

			// defines a polygon
			if(polyIndices.size() > 3 && polyIndices.get(0) == polyIndices.get(polyIndices.size()-1)) {
				polyIndices.remove(polyIndices.size()-1);
				List<VPoint> pointList = polyIndices.stream().map(index -> vertices.get(index)).collect(Collectors.toList());
				VPolygon polygon = GeometryUtils.toPolygon(pointList);

				// is the segment-bound
				if(holes.values().stream().allMatch(p -> polygon.contains(p))) {
					polygons.add(0, polygon);
				}
				else {
					polygons.add(polygon);
				}
			}
			else {
				// TODO: duplicated code
				for(int i = 0; i < polyIndices.size(); i++) {
					int vertexId1 = polyIndices.get(i);
					int vertexId2 = polyIndices.get((i+1) % polyIndices.size());
					VPoint vertex1 = vertices.get(vertexId1);
					VPoint vertex2 = vertices.get(vertexId2);
					VLine line = new VLine(vertex1, vertex2);
					lines.add(line);
				}
			}
		}

		if(polygons.size() == 2) {
			if(polygons.get(1).contains(polygons.get(0).getPath().get(0))) {
				VPolygon tmp = polygons.get(0);
				polygons.set(0, polygons.get(1));
				polygons.set(1, tmp);
			}
		}

		if(!polygons.isEmpty()) {
			return new PSLG(polygons.get(0), polygons.subList(1, polygons.size()), lines, Collections.EMPTY_SET);
		}
		else {
			throw new IOException("invalid .poly format.");
		}
	}

	/**
	 * Transforms a {@link IMesh} into a PSLG-{@link String}.
	 *
	 * @param mesh  the mesh
	 *
	 * @param <V> the type of the vertices
	 * @param <E> the type of the half-edges
	 * @param <F> the type of the faces
	 *
	 * @return a PSLG-{@link String}
	 */
	public static <V extends IVertex, E extends IHalfEdge, F extends IFace> String toPSLG(
			@NotNull final IMesh<V, E, F> mesh) {
		return toPSLG(mesh, v -> 0.0);
	}

	/**
	 * Transforms a {@link IMesh} into a PSLG-{@link String}.
	 *
	 * @param mesh  the mesh
	 * @param eval  gives the values which should be added to the vertices of the PSLG
	 *
	 * @param <V> the type of the vertices
	 * @param <E> the type of the half-edges
	 * @param <F> the type of the faces
	 *
	 * @return a PSLG-{@link String}
	 */
	public static <V extends IVertex, E extends IHalfEdge, F extends IFace> String toPSLG(
			@NotNull final IMesh<V, E, F> mesh,
			@NotNull final Function<V, Double> eval) {
		int dimension = 2;
		int nAttributes = 0;
		int boundaryMarker = 0; // no boundary marker
		StringBuilder builder = new StringBuilder();
		builder.append("#node\n");
		builder.append(mesh.getNumberOfVertices() + SEPARATOR + dimension + SEPARATOR + nAttributes + SEPARATOR + boundaryMarker + "\n");
		Map<V, Integer> map = new HashMap<>();
		int id = 1;
		for(V v : mesh.getVertices()) {
			map.put(v, id);
			builder.append(String.format(Locale.US, "%d" + SEPARATOR +"%f" + SEPARATOR + "%f" + SEPARATOR + "%f\n", id, v.getX(), v.getY(), eval.apply(v)));
			id++;
		}

		List<VLine> lines = mesh.getLines().stream().collect(Collectors.toList());
		builder.append("\n" + lines.size() + SEPARATOR + boundaryMarker);
		for(int index = 1; index <= lines.size(); index++) {
			VLine line = lines.get(index-1);

			Integer from = map.get(line.getVPoint1());
			assert from != null;
			if(from == null) {
				throw new IllegalArgumentException("could not find index for point " + line.getVPoint1());
			}

			Integer to = map.get(line.getVPoint2());
			assert to != null;
			if(to == null) {
				throw new IllegalArgumentException("could not find index for point " + line.getVPoint2());
			}
			builder.append("\n" + index + SEPARATOR + from + SEPARATOR + to);
		}
		builder.append("#holes\n");
		List<F> holes = mesh.getHoles();
		builder.append(holes.size()+"\n");

		builder.append("#interior points for each hole\n");
		id = 1;
		for(F hole : holes) {
			VPolygon polygon = mesh.toPolygon(hole);
			VPoint p = GeometryUtils.getInteriorPoint(polygon);
			builder.append(String.format(Locale.US, "%d" + SEPARATOR +"%f" + SEPARATOR + "%f\n", id, p.getX(), p.getY()));
		}
		return builder.toString();
	}

	public static <V extends IVertex, E extends IHalfEdge, F extends IFace> String to2DPoly(
			@NotNull final IMesh<V, E, F> mesh) {
		return to2DPoly(mesh, v -> 0.0, v -> false);
	}

	public static <V extends IVertex, E extends IHalfEdge, F extends IFace> String to2DPoly(
			@NotNull final IMesh<V, E, F> mesh, @NotNull Function<V, Double> vertexFunc, @NotNull Predicate<V> targetPred) {
		int dimension = 2;
		int nAttributes = 1;
		int boundaryMarker = 1;
		int targetMarker = 2;
		StringBuilder builder = new StringBuilder();
		builder.append("#nVertices dimension boundaryMarker targetMarker nAttributes\n");
		builder.append(mesh.getNumberOfVertices() + SEPARATOR + dimension + SEPARATOR + boundaryMarker + SEPARATOR + targetMarker + SEPARATOR + nAttributes + "\n");
		Map<V, Integer> map = new HashMap<>();
		int id = 1;
		for(V v : mesh.getVertices()) {
			int boundary = mesh.isAtBoundary(v) ? 1 : 0;
			int target = targetPred.test(v) ? targetMarker : 0;
			map.put(v, id);
			builder.append(String.format(Locale.US, "%d" + SEPARATOR + "%d" + SEPARATOR + "%d" + SEPARATOR +"%f" + SEPARATOR + "%f" + SEPARATOR + "%f\n", id, boundary, target, v.getX(), v.getY(), vertexFunc.apply(v)));
			id++;
		}

		// 1 boundary
		builder.append("# nBorders\n");
		builder.append(1+"\n");
		builder.append(mesh.getPoints(mesh.getBorder()).size() + SEPARATOR);
		for(V v : mesh.getVertices(mesh.getBorder())) {
			builder.append(map.get(v) + SEPARATOR);
		}
		builder.delete(builder.length()-SEPARATOR.length(), builder.length());
		builder.append("\n");

		builder.append("# nTriangels\n");
		builder.append(mesh.getNumberOfFaces()+"\n");

		builder.append("# nVertices vertexIds\n");
		for(F face : mesh.getFaces()) {
			//builder.append("1 0\n");
			builder.append(mesh.getPoints(face).size() + SEPARATOR);
			for(V v : mesh.getVertices(face)) {
				builder.append(map.get(v) + SEPARATOR);
			}
			builder.delete(builder.length()-SEPARATOR.length(), builder.length());
			builder.append("\n");
		}
		builder.append("# nHoles\n");
		List<F> holes = mesh.getHoles();
		builder.append(holes.size()+"\n");

		//
		for(F hole : holes) {
			int size = mesh.getPoints(hole).size();
			builder.append(size + SEPARATOR);
			for(V V : mesh.getVertices(hole)) {
				builder.append(map.get(V) + SEPARATOR);
			}
			builder.delete(builder.length()-SEPARATOR.length(), builder.length());
			builder.append("\n");
		}
		builder.append("# interior points for each hole\n");
		id = 1;
		for(F hole : holes) {
			VPolygon polygon = mesh.toPolygon(hole);
			VPoint p = GeometryUtils.getInteriorPoint(polygon);
			builder.append(String.format(Locale.US, "%d" + SEPARATOR +"%f" + SEPARATOR + "%f\n", id, p.getX(), p.getY()));
		}
		return builder.toString();
	}
}
