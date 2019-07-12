package org.vadere.simulator.utils;

import org.jetbrains.annotations.NotNull;
import org.vadere.state.scenario.Obstacle;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.ScenarioElement;
import org.vadere.state.scenario.Source;
import org.vadere.state.scenario.Target;
import org.vadere.state.scenario.Topography;
import org.vadere.meshing.mesh.inter.IFace;
import org.vadere.meshing.mesh.inter.IHalfEdge;
import org.vadere.meshing.mesh.inter.IMesh;
import org.vadere.meshing.mesh.inter.IVertex;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VShape;

import java.awt.Color;
import java.util.List;
import java.util.function.Function;

/**
 * @author Benedikt Zoennchen
 */
public class TexGraphGenerator extends org.vadere.meshing.utils.io.tex.TexGraphGenerator {

	/**
	 * Transforms a {@link IMesh} into a tikz string. The tikz graphic is scaled by the scaling. Each face
	 * of the mesh is filled by the color defined by the coloring-function.
	 *
	 * @param mesh      the mesh
	 * @param coloring  the coloring function
	 * @param scaling   the scaling of the tikz graphics
	 * @return a string representing a tikz graphics
	 */
	public static <V extends IVertex, E extends IHalfEdge, F extends IFace> String toTikz(
			@NotNull final IMesh<V, E, F> mesh,
			@NotNull final Function<F, Color> coloring,
			final float scaling,
			@NotNull final Topography topography) {

		StringBuilder builder = new StringBuilder();
		builder.append("\\begin{tikzpicture}[scale="+scaling+"]\n");

		Color obsColor = Color.BLACK;
		Color targetColor = Color.BLACK;
		Color sourceColor = Color.BLACK;
		Color pedestrianColor = Color.BLACK;

		String cObstacle = "{"+obsColor.getRed()+","+obsColor.getGreen()+","+obsColor.getBlue()+"}";
		String cTarget = "{"+targetColor.getRed()+","+targetColor.getGreen()+","+targetColor.getBlue()+"}";
		String cSource = "{"+sourceColor.getRed()+","+sourceColor.getGreen()+","+sourceColor.getBlue()+"}";
		String cPedestrian = "{"+pedestrianColor.getRed()+","+pedestrianColor.getGreen()+","+pedestrianColor.getBlue()+"}";

		builder.append("\\xdefinecolor{cObstacle}{rgb}" + cObstacle + "\n");
		builder.append("\\xdefinecolor{cTarget}{rgb}" + cTarget + "\n");
		builder.append("\\xdefinecolor{cSource}{rgb}" + cSource + "\n");
		builder.append("\\xdefinecolor{cPedestrian}{rgb}" + cPedestrian + "\n");

		// obstacles
		for (Obstacle obstacle : topography.getObstacles()) {
			builder.append(toTikz(obstacle, "cObstacle"));
		}

		// targets
		for (Target target : topography.getTargets()) {
			builder.append(toTikz(target, "cTarget"));
		}

		// sources
		for (Source source : topography.getSources()) {
			builder.append(toTikz(source, "cSource"));
		}

		// pedestrian
		for (Pedestrian pedestrian : topography.getElements(Pedestrian.class)) {
			builder.append("\\filldraw[color=black,fill=cPedestrian](" + pedestrian.getPosition().getX() + "," + pedestrian.getPosition().getY() + ") circle (" + pedestrian.getRadius() + ")");
		}

		for(F face : mesh.getFaces()) {
			Color c = coloring.apply(face);
			String tikzColor = "{rgb,255:red,"+c.getRed()+";green,"+c.getGreen()+";blue,"+c.getBlue()+"}";
			V first = mesh.streamVertices(face).findFirst().get();
			String poly = mesh.streamVertices(face).map(v -> "("+v.getX()+","+v.getY()+")").reduce((s1, s2) -> s1 + "--" + s2).get() + "-- ("+first.getX()+","+first.getY()+")";

			//builder.append("\\fill[fill="+tikzColor+"]" + poly + ";\n");
			builder.append("\\filldraw[color=black,fill="+tikzColor+"]" + poly + ";\n");
		}

		/*for(F face : mesh.getFaces()) {
			String poly = mesh.streamVertices(face).map(v -> "("+v.getX()+","+v.getY()+")").reduce((s1, s2) -> s1 + "--" + s2).get();
			builder.append("\\draw[black,thick]" + poly + ";\n");
		}*/

		builder.append("\\end{tikzpicture}");
		return builder.toString();
	}

	private static String toTikz(@NotNull final ScenarioElement element, @NotNull final String tikzColor) {
		StringBuilder builder = new StringBuilder();
		VShape shape = element.getShape();
		builder.append("\\filldraw[color=black,fill="+tikzColor+"]");

		List<VPoint> path = shape.getPath();

		for(VPoint point : path) {
			builder.append("("+point.getX()+","+point.getY()+") -- ");
		}

		builder.append("("+path.get(path.size()-1).getX()+","+path.get(path.size()-1).getY()+");\n");

		return builder.toString();
	}
}
