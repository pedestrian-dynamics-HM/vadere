package org.vadere.meshing.utils.io;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.inter.IFace;
import org.vadere.meshing.mesh.inter.IHalfEdge;
import org.vadere.meshing.mesh.inter.IMesh;
import org.vadere.meshing.mesh.inter.IVertex;
import org.vadere.util.geometry.shapes.IPoint;

import java.util.HashMap;
import java.util.Map;

public class PolyGenerator {

	public static <P extends IPoint, CE, CF, V extends IVertex<P>, E extends IHalfEdge<CE>, F extends IFace<CF>> String toPoly(
			@NotNull final IMesh<P, CE, CF, V, E, F> mesh) {
		int dimension = 3;
		StringBuilder builder = new StringBuilder();
		builder.append("#node\n");
		builder.append(mesh.getNumberOfVertices() + " " + dimension + "\n");
		Map<P, Integer> map = new HashMap<>();
		int id = 1;
		for(P p : mesh.getPoints()) {
			map.put(p, id);
			builder.append(id + " " + p.getX() + " " + p.getY() + " " + 0.0 + "\n");
			id++;
		}

		builder.append("#FACET 2D\n");
		builder.append(mesh.getNumberOfFaces()+"\n");

		for(F face : mesh.getFaces()) {
			builder.append("1 0\n");
			builder.append(mesh.getPoints(face).size() + " ");
			for(P p : mesh.getPoints(face)) {
				builder.append(map.get(p) + " ");
			}
			builder.delete(builder.length()-1, builder.length());
			builder.append("\n");
		}
		builder.append("0 #holes");
		return builder.toString();
	}
}
