package org.vadere.meshing.utils.io.poly;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vadere.meshing.mesh.inter.IFace;
import org.vadere.meshing.mesh.inter.IHalfEdge;
import org.vadere.meshing.mesh.inter.IMesh;
import org.vadere.meshing.mesh.inter.IVertex;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VPolygon;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The {@link MeshPSLGWriter} transforms a {@link IMesh} into a ascii-PSLG {@link String} and writes into a file.
 *
 * @param <V> the type of the vertices
 * @param <E> the type of the half-edges
 * @param <F> the type of the faces
 */
public class MeshPSLGWriter<V extends IVertex, E extends IHalfEdge, F extends IFace> {

	private final static String SEPARATOR = " ";
	private final static int DIMENSION = 2;

	/**
	 * Transforms a {@link IMesh} into a PSLG-{@link String}. Note that in the PSLG-format faces aren't saved explicit.
	 *
	 * @param mesh  the mesh
	 *
	 * @return a PSLG-{@link String}
	 */
	public String toPSLG(@NotNull final IMesh<V, E, F> mesh) {
		return toPSLG(mesh, 0, null);
	}
	/**
	 * Transforms a {@link IMesh} into a PSLG-{@link String}. Note that in the PSLG-format faces aren't saved explicit.
	 *
	 * @param mesh          the mesh
	 * @param nAttributes   number of vertex attributes
	 * @param attrNameFunc  a function attributeIndex -> attributeName
	 *
	 * @return a PSLG-{@link String}
	 */
	public String toPSLG(
			@NotNull final IMesh<V, E, F> mesh,
			int nAttributes,
			@Nullable final Function<Integer, String> attrNameFunc){
		assert nAttributes <= 0 || attrNameFunc != null;
		int boundaryMarker = 0; // no boundary marker
		StringBuilder builder = new StringBuilder();
		builder.append("#node\n");
		builder.append(mesh.getNumberOfVertices() + SEPARATOR + DIMENSION + SEPARATOR + nAttributes + SEPARATOR + boundaryMarker + "\n");

		Map<V, Integer> map = new HashMap<>();
		int id = 1;
		for(V v : mesh.getVertices()) {
			map.put(v, id);
			builder.append(String.format(Locale.US, "%d" + SEPARATOR +"%f" + SEPARATOR + "%f", id, v.getX(), v.getY()));

			for(int j = 1; j <= nAttributes; j++) {
				builder.append(String.format(Locale.US, SEPARATOR + "%f", mesh.getDoubleData(v, attrNameFunc.apply(j))));
			}
			builder.append("\n");
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
}
