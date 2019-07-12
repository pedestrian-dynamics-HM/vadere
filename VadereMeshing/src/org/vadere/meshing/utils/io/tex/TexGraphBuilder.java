package org.vadere.meshing.utils.io.tex;

import org.vadere.util.visualization.ColorHelper;
import org.vadere.meshing.utils.color.ColorFunctions;
import org.vadere.meshing.mesh.inter.IFace;
import org.vadere.meshing.utils.debug.TriCanvas;
import org.vadere.meshing.mesh.inter.IHalfEdge;
import org.vadere.meshing.mesh.inter.IMesh;
import org.vadere.meshing.mesh.inter.IVertex;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VPolygon;

import java.awt.*;
import java.util.ArrayList;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * Builder class for Tikz Graphics used in tex / latex documents.
 * @author Stefan Schuhbaeck
 */
public class TexGraphBuilder<P extends IPoint, CE, CF, V extends IVertex, E extends IHalfEdge, F extends IFace> {

	private StringBuilder sb;
	private boolean generated;
	private DrawType drawType;
	private ColorFunctions<P, CE, CF, V, E, F> colorFunctions;
	private BiFunction<IMesh<V, E, F>, F, StringBuilder> drawFace;
	private ArrayList<Consumer<StringBuilder>> decorator;
	private IMesh<V, E, F> mesh;
	/**
	 * @param mesh           Mesh to draw
	 * @param scaling        Scaling factor
	 * @param colorFunctions {@link ColorFunctions} object used for color information
	 */
	public TexGraphBuilder(IMesh<V, E, F> mesh, float scaling, ColorFunctions<P, CE, CF, V, E, F> colorFunctions) {
		this.drawType = DrawType.faceFillDraw;
		this.sb = new StringBuilder();
		this.mesh = mesh;
		this.colorFunctions = colorFunctions;
		decorator = new ArrayList<>();
		initDrawFace();
		sb.append("\\begin{tikzpicture}[scale=").append(scaling).append("]\n");
	}

	/**
	 * RGB color syntax for tikz
	 *
	 * @param c color object
	 * @return tikz syntax String (without newline)
	 */
	private static String color(Color c) {
		return "{rgb,255:red," + c.getRed() + ";green," + c.getGreen() + ";blue," + c.getBlue() + "}";
	}

	//statics

	/**
	 * Label for face numbers / ids
	 *
	 * @param color color of the text used to represent the face id.
	 * @param x     x coordinate of the text node
	 * @param y     y coordinate of the text node
	 * @param text  the text used
	 * @return line used for tikzpicture (contains newline character)
	 */
	public static StringBuilder label(String color, String x, String y, String text) {
		StringBuilder sb = new StringBuilder();
		sb.append("\\draw(").append(x).append(",").append(y).append(") ");
		sb.append("node ").append("{\\tiny \\color").append(color).append(" ").append(text).append("};\n");
		return sb;
	}

	public static StringBuilder label(String color, double x, double y, String text) {
		return label(color, Double.toString(x), Double.toString(y), text);
	}

	/**
	 * Create line between point 1 and point 2
	 *
	 * @param color color of line
	 * @param x1    x of point 1
	 * @param y1    y of point 1
	 * @param x2    x of point 2
	 * @param y2    y of point 2
	 * @return line used for tikzpicture (contains newline character)
	 */
	public static StringBuilder line(String color, String x1, String y1, String x2, String y2) {
		StringBuilder sb = new StringBuilder();
		sb.append("\\draw [color=").append(color).append("] (")
				.append(x1).append(",").append(y1).append(") -- (").append(x2).append(",").append(y2).append(");\n");
		return sb;
	}

	public static StringBuilder line(Color c, VPoint a, VPoint b) {
		return line(color(c), Double.toString(a.x), Double.toString(a.y), Double.toString(b.x), Double.toString(b.y));
	}

	/**
	 * Point within tikzpicture
	 *
	 * @param color fill color of point
	 * @param x     x coordinate of point
	 * @param y     y coordinate of point
	 * @param r     radius of point in cm
	 * @return line used for tikzpicture (contains newline character)
	 */
	public static StringBuilder point(String color, String x, String y, String r) {
		StringBuilder sb = new StringBuilder();
		sb.append("\\fill[fill=")
				.append(color)
				.append("] (")
				.append(x)
				.append(",")
				.append(y)
				.append(") circle [radius=").append(r).append("cm];\n");
		return sb;
	}

	public static StringBuilder point(String color, double x, double y, double r) {
		return point(color, Double.toString(x), Double.toString(y), Double.toString(r));
	}

	public static StringBuilder point(Color c, double x, double y, double r) {
		return point(color(c), Double.toString(x), Double.toString(y), Double.toString(r));
	}

	/**
	 * Generate graph. This method should only be called once.
	 */
	public void generateGraph() {
		if (!generated) {
//			drawFaces();
			drawFacesWithId();
			drawElements();
			drawVertex();
			sb.append("\\end{tikzpicture}");
			generated = true;
		}
	}

	//instance

	/**
	 * @return String representation of the Tikz drawing
	 */
	public String returnString() {
		return sb.toString();
	}

	/**
	 * Draw all faces contained within the {@link IMesh}. Color of boarders and filling is defined
	 * by the {@link ColorFunctions} field
	 */
	private void drawFaces() {
		for (F face : mesh.getFaces()) {
			sb.append(drawFace.apply(mesh, face));
		}
	}

	/**
	 * Draw all faces contained within the {@link IMesh}. Color of boarders and filling is defined
	 * by the {@link ColorFunctions} field. Add an ID at the center of a face and a comment line
	 * to the tikz drawing.
	 */
	private void drawFacesWithId() {
		int id = 0;
		for (F face : mesh.getFaces()) {
			sb.append("%id: ").append(Integer.toString(id)).append("\n");
			VPolygon p = mesh.toPolygon(face);
			VPoint x = p.getCentroid();
			sb.append(drawFace.apply(mesh, face));
			Color c = ColorHelper.getContrasColor(colorFunctions.faceFillColor(mesh, face));
			sb.append(label(color(c), x.getX(), x.getY(), Integer.toString(id)));
			id++;
		}
	}

	/**
	 * Draw all additional elements defined besides the standards set in a
	 * {@link TriCanvas} implementation.
	 */
	private void drawElements() {
		decorator.forEach(c -> c.accept(sb));
	}

	public void addElement(Consumer<StringBuilder> c) {
		decorator.add(c);
	}

	/**
	 * Draw circles as {@link IMesh} vertices.
	 */
	private void drawVertex() {
		for (F face : mesh.getFaces()) {
			VPolygon polygon = mesh.toPolygon(face);
			polygon.getPoints().forEach(p -> {
				sb.append(point("red", p.getX(), p.getY(), 0.1));
			});
		}
	}

	/**
	 * Define if the {@link IMesh} should be drawn as a filled, contour plot or both.
	 */
	public void initDrawFace() {
		if (drawFace == null) {
			if (this.drawType == DrawType.faceDraw) {
				drawFace = this::faceDraw;
			} else if (this.drawType == DrawType.faceFill) {
				drawFace = this::faceFill;
			} else {
				drawFace = this::faceFillDraw;
			}
		}
	}

	public void setDrawType(DrawType drawType) {
		this.drawType = drawType;
	}

	public void setDrawFace(BiFunction<IMesh<V, E, F>, F, StringBuilder> drawFace) {
		this.drawFace = drawFace;
	}

	/**
	 * Creates contour plott with no filling
	 */
	private StringBuilder faceDraw(IMesh<V, E, F> mesh, F face) {
		StringBuilder sb = new StringBuilder();
		sb.append("\\draw ").append("[")
				.append("color=").append(color(colorFunctions.faceDrawColor(mesh, face)))
				.append("]");

		sb.append(shape(mesh, face)).append(";\n");
		return sb;
	}

	/**
	 * Creates plot with filling only (no boarders around faces)
	 */
	private StringBuilder faceFill(IMesh<V, E, F> mesh, F face) {
		StringBuilder sb = new StringBuilder();
		sb.append("\\fill ").append("[")
				.append("fill=").append(color(colorFunctions.faceDrawColor(mesh, face)))
				.append("]");

		sb.append(shape(mesh, face)).append(";\n");
		return sb;
	}

	/**
	 * Creates plot with contour and filling (default)
	 */

	private StringBuilder faceFillDraw(IMesh<V, E, F> mesh, F face) {
		StringBuilder sb = new StringBuilder();
		sb.append("\\filldraw ").append("[")
				.append("color=").append(color(colorFunctions.faceDrawColor(mesh, face)))
				.append(",fill=").append(color(colorFunctions.faceFillColor(mesh, face)))
				.append("]");

		sb.append(shape(mesh, face)).append(";\n");
		return sb;
	}

	public StringBuilder shape(IMesh<V, E, F> mesh, F face) {
		StringBuilder sb = new StringBuilder();
		V first = mesh.streamVertices(face).findFirst().get();
		String poly = mesh.streamVertices(face).map(v -> "(" + v.getX() + "," + v.getY() + ")").reduce((s1, s2) -> s1 + "--" + s2).get() + "-- (" + first.getX() + "," + first.getY() + ")";
		sb.append(poly);
		return sb;
	}

	enum DrawType {
		faceFill,
		faceDraw,
		faceFillDraw
	}
}
