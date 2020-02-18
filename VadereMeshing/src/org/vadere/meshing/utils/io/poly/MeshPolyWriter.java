package org.vadere.meshing.utils.io.poly;

import it.unimi.dsi.fastutil.io.FastBufferedOutputStream;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vadere.meshing.mesh.inter.IFace;
import org.vadere.meshing.mesh.inter.IHalfEdge;
import org.vadere.meshing.mesh.inter.IMesh;
import org.vadere.meshing.mesh.inter.IVertex;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VPolygon;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
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
		StringBuilder builder = new StringBuilder();
		to2DPoly(mesh, nAttributes, attrNameFunc, targetPredicate, new Appender(builder));
		return builder.toString();
	}

	/**
	 * Transforms a {@link IMesh} into a Poly-{@link String} and writes it into the <tt>write</tt>.
	 *
	 * @param mesh              the mesh
	 * @param nAttributes       number of vertex attributes
	 * @param attrNameFunc      a function attributeIndex -> attributeName
	 * @param targetPredicate   mark a specific vertex to be a target vertex
	 * @param writer            the print writer where the string will be written into.
	 *
	 * @return a PSLG-{@link String}
	 */
	public void to2DPoly(
			@NotNull final IMesh<V, E, F> mesh,
			int nAttributes,
			@Nullable final Function<Integer, String> attrNameFunc,
			@NotNull Predicate<V> targetPredicate,
			@NotNull final PrintWriter writer) {
		StringBuilder builder = new StringBuilder();
		to2DPoly(mesh, nAttributes, attrNameFunc, targetPredicate, new Appender(writer));
	}

	private void to2DPoly(
			@NotNull final IMesh<V, E, F> mesh,
			int nAttributes,
			@Nullable final Function<Integer, String> attrNameFunc,
			@NotNull Predicate<V> targetPredicate,
			@NotNull final Appender appender) {
		int dimension = 2;
		int boundaryMarker = 1;
		int targetMarker = 2;
		appender.append("#nVertices dimension boundaryMarker targetMarker nAttributes\n");
		appender.append(mesh.getNumberOfVertices() + SEPARATOR + dimension + SEPARATOR + boundaryMarker + SEPARATOR + targetMarker + SEPARATOR + nAttributes + "\n");

		Map<V, Integer> map = new HashMap<>();
		int id = 1;
		for(V v : mesh.getVertices()) {
			int boundary = mesh.isAtBoundary(v) ? 1 : 0;
			int target = targetPredicate.test(v) ? targetMarker : 0;
			map.put(v, id);
			appender.append(String.format(Locale.US, "%d" + SEPARATOR + "%d" + SEPARATOR + "%d" + SEPARATOR + "%f" + SEPARATOR + "%f", id, boundary, target, v.getX(), v.getY()));
			for (int j = 1; j <= nAttributes; j++) {
				appender.append(String.format(Locale.US, SEPARATOR + "%f", mesh.getDoubleData(v, attrNameFunc.apply(j))));
			}
			appender.append("\n");
			id++;
		}

		// 1 boundary
		appender.append("# nBorders\n");
		appender.append(1+"\n");
		appender.append(mesh.getPoints(mesh.getBorder()).size() + "");
		for(V v : mesh.getVertices(mesh.getBorder())) {
			appender.append(SEPARATOR + map.get(v).toString());
		}
		appender.append("\n");

		appender.append("# nTriangels\n");
		appender.append(mesh.getNumberOfFaces()+"\n");

		appender.append("# nVertices vertexIds\n");
		for(F face : mesh.getFaces()) {
			//builder.append("1 0\n");
			appender.append(mesh.getPoints(face).size() + "");
			for(V v : mesh.getVertices(face)) {
				appender.append(SEPARATOR + map.get(v));
			}
			appender.append("\n");
		}
		appender.append("# nHoles\n");
		List<F> holes = mesh.getHoles();
		appender.append(holes.size()+"\n");

		//
		for(F hole : holes) {
			int size = mesh.getPoints(hole).size();
			appender.append(size + "");
			for(V V : mesh.getVertices(hole)) {
				appender.append(SEPARATOR + map.get(V));
			}
			appender.append("\n");
		}
		appender.append("# interior points for each hole\n");
		id = 1;
		for(F hole : holes) {
			VPolygon polygon = mesh.toPolygon(hole);
			VPoint p = GeometryUtils.getInteriorPoint(polygon);
			appender.append(String.format(Locale.US, "%d" + SEPARATOR +"%f" + SEPARATOR + "%f\n", id, p.getX(), p.getY()));
		}
	}

	public void to2DPoly(
			@NotNull final IMesh<V, E, F> mesh,
			int nAttributes,
			@Nullable final Function<Integer, String> attrNameFunc,
			@NotNull Predicate<V> targetPredicate,
			@NotNull final OutputStream outputStream) {
		PrintWriter stream = new PrintWriter(
				new FastBufferedOutputStream(outputStream));
	}

	private static class Appender {

		private final StringBuilder builder;
		private final PrintWriter writer;

		private Appender(@NotNull final StringBuilder builder) {
			this.builder = builder;
			this.writer = null;
		}

		private Appender(@NotNull final PrintWriter writer) {
			this.writer = writer;
			this.builder = null;
		}

		private void append(@NotNull final String txt) {
			if(writer != null) {
				writer.append(txt);
			} else {
				builder.append(txt);
			}
		}
	}
}
