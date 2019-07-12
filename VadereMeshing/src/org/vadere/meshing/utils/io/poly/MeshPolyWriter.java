package org.vadere.meshing.utils.io.poly;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vadere.meshing.mesh.inter.IFace;
import org.vadere.meshing.mesh.inter.IHalfEdge;
import org.vadere.meshing.mesh.inter.IMesh;
import org.vadere.meshing.mesh.inter.IVertex;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VPolygon;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * The {@link MeshPolyWriter} transforms a {@link IMesh} into a ascii-poly {@link String} and writes into a file.
 * It is the counterpart to {@link MeshPolyReader}.
 *
 * @param <V> the type of the vertices
 * @param <E> the type of the half-edges
 * @param <F> the type of the faces
 */
public class MeshPolyWriter<V extends IVertex, E extends IHalfEdge, F extends IFace> {

	private final static String SEPARATOR = " ";
	private final static int DIMENSION = 2;

	public MeshPolyWriter() {}

	/**
	 * Transforms a {@link IMesh} into a Poly-{@link String}.
	 *
	 * @param mesh the mesh
	 *
	 * @return a PSLG-{@link String}
	 */
	public String to2DPoly(@NotNull final IMesh<V, E, F> mesh) {
		return to2DPoly(mesh, 0, null, v -> false);
	}

	/**
	 * Transforms a {@link IMesh} into a Poly-{@link String}.
	 *
	 * @param mesh              the mesh
	 * @param nAttributes       number of vertex attributes
	 * @param attrNameFunc      a function attributeIndex -> attributeName
	 * @param targetPredicate   mark a specific vertex to be a target vertex
	 *
	 * @return a PSLG-{@link String}
	 */
	public String to2DPoly(
			@NotNull final IMesh<V, E, F> mesh,
			int nAttributes,
			@Nullable final Function<Integer, String> attrNameFunc,
			@NotNull Predicate<V> targetPredicate) {
		int dimension = 2;
		int boundaryMarker = 1;
		int targetMarker = 2;
		StringBuilder builder = new StringBuilder();
		builder.append("#nVertices dimension boundaryMarker targetMarker nAttributes\n");
		builder.append(mesh.getNumberOfVertices() + SEPARATOR + dimension + SEPARATOR + boundaryMarker + SEPARATOR + targetMarker + SEPARATOR + nAttributes + "\n");

		Map<V, Integer> map = new HashMap<>();
		int id = 1;
		for(V v : mesh.getVertices()) {
			int boundary = mesh.isAtBoundary(v) ? 1 : 0;
			int target = targetPredicate.test(v) ? targetMarker : 0;
			map.put(v, id);
			builder.append(String.format(Locale.US, "%d" + SEPARATOR + "%d" + SEPARATOR + "%d" + SEPARATOR + "%f" + SEPARATOR + "%f", id, boundary, target, v.getX(), v.getY()));
			for (int j = 1; j <= nAttributes; j++) {
				builder.append(String.format(Locale.US, SEPARATOR + "%f", mesh.getDoubleData(v, attrNameFunc.apply(j))));
			}
			builder.append("\n");
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
