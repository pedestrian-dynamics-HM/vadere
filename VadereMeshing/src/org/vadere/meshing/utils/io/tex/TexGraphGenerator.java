package org.vadere.meshing.utils.io.tex;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vadere.meshing.mesh.inter.IFace;
import org.vadere.meshing.mesh.inter.IHalfEdge;
import org.vadere.meshing.mesh.inter.IMesh;
import org.vadere.meshing.mesh.inter.IVertex;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VPolygon;
import org.vadere.util.geometry.shapes.VTriangle;

import java.awt.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * @author Benedikt Zoennchen
 */
public class TexGraphGenerator {

	public static final String STANDALONE_HEADER ="\\documentclass{standalone}\n" +
			"\\usepackage{tikz}\n" +
			"\\begin{document}";

	public static final String STANDALONE_FOOTER = "\\end{document}\n";
	private static final Color DEFAULT_VERTEX_COLOR = Color.BLACK;
	private static final Color DEFAULT_EDGE_COLOR = Color.BLACK;
	private static final Color DEFAULT_FACE_COLOR = Color.WHITE;


	public static <V extends IVertex, E extends IHalfEdge, F extends IFace> String toTikz(
			@NotNull final IMesh<V, E, F> mesh, final float scaling){
		return toTikz(mesh, scaling, false);
	}


	public static <V extends IVertex, E extends IHalfEdge, F extends IFace> String toTikz(
			@NotNull final IMesh<V, E, F> mesh, boolean standalone){
		return toTikz(mesh, f -> DEFAULT_FACE_COLOR, null, null, 1.0f, false);
				//toTikz(mesh, 1.0f, standalone);
	}

	public static String toTikz(@NotNull final Collection<VLine> lines) {
		return toTikz(lines, Collections.EMPTY_LIST, true, 1.0);
	}

	public static String toTikz(
			@NotNull final Collection<VLine> lines,
			@NotNull final Collection<VPolygon> polygons,
			boolean drawVertices,
			final double scaling) {
		StringBuilder builder = new StringBuilder();

		builder.append("\\documentclass{standalone}\n");
		builder.append("\\usepackage{tikz}\n\n");

		builder.append("% Color Definitions\n");
		String colorTextPattern = "\\definecolor{%s}{RGB}{%d,%d,%d}\n";
		builder.append(String.format(Locale.US, colorTextPattern, "vertexColor1", DEFAULT_EDGE_COLOR.getRed(), DEFAULT_EDGE_COLOR.getGreen(), DEFAULT_EDGE_COLOR.getBlue()));
		builder.append(String.format(Locale.US, colorTextPattern, "vertexColor1Fill", DEFAULT_EDGE_COLOR.getRed(), DEFAULT_EDGE_COLOR.getGreen(), DEFAULT_EDGE_COLOR.getBlue()));
		builder.append(String.format(Locale.US, colorTextPattern, "faceColor1", DEFAULT_EDGE_COLOR.getRed(), DEFAULT_EDGE_COLOR.getGreen(), DEFAULT_EDGE_COLOR.getBlue()));
		builder.append(String.format(Locale.US, colorTextPattern, "faceColor1Fill", DEFAULT_FACE_COLOR.getRed(), DEFAULT_FACE_COLOR.getGreen(), DEFAULT_FACE_COLOR.getBlue()));
		builder.append(String.format(Locale.US, colorTextPattern, "edgeColor1", DEFAULT_EDGE_COLOR.getRed(), DEFAULT_EDGE_COLOR.getGreen(), DEFAULT_EDGE_COLOR.getBlue()));

		builder.append("\\pgfmathsetmacro{\\circleSize}{1.5pt}");
		builder.append("\\begin{document}\n");
		builder.append("% Change scaling to [x=1mm,y=1mm] if TeX reports \"Dimension too large\".\n");
		builder.append("\\begin{tikzpicture}\n");
		builder.append("[x=1cm,y=1cm]\n");

		for(VLine line : lines) {
			String colorName = "edgeColor1";
			builder.append("\\draw[color="+colorName+"]("+toString(line.getX1())+","+toString(line.getY1())+") -- ("+toString(line.getX2())+","+toString(line.getY2())+");\n");
		}

		for(VPolygon polygon : polygons) {
			String colorName = "faceColor1";
			//String tikzColor = "{rgb,255:red,"+c.getRed()+";green,"+c.getGreen()+";blue,"+c.getBlue()+"}";
			List<VPoint> points = polygon.getPoints();
			VPoint first = points.get(0);
			String poly = points.stream().map(v -> "("+toString(v.getX())+","+toString(v.getY())+")").reduce((s1, s2) -> s1 + "--" + s2).get() + "-- ("+toString(first.getX())+","+toString(first.getY())+")";

			//builder.append("\\fill[fill="+tikzColor+"]" + poly + ";\n");
			builder.append("\\filldraw[color="+colorName+",fill="+colorName+"Fill]" + poly + ";\n");
		}

		if(drawVertices) {
			String colorName = "vertexColor1";
			for(VPolygon polygon : polygons) {
				for (VPoint point : polygon.getPath()) {
					//String tikzColor = "{rgb,255:red,"+c.getRed()+";green,"+c.getGreen()+";blue,"+c.getBlue()+"}";
					builder.append("\\draw[color=" + colorName + ", fill=" + colorName + "Fill](" + toString(point.getX()) + "," + toString(point.getY()) + ") circle (\\circleSize);\n");
				}
			}

			for(VLine linne : lines) {
				builder.append("\\draw[color=" + colorName + ", fill=" + colorName + "Fill](" + toString(linne.getP1().getX()) + "," + toString(linne.getP1().getY()) + ") circle (\\circleSize);\n");
				builder.append("\\draw[color=" + colorName + ", fill=" + colorName + "Fill](" + toString(linne.getP2().getX()) + "," + toString(linne.getP2().getY()) + ") circle (\\circleSize);\n");
			}
		}


		builder.append("\\end{tikzpicture}\n");
		builder.append("\\end{document}\n");
		return builder.toString();
	}


	/**
	 * Transforms a {@link IMesh} into a tikz string. The tikz graphic is scaled by the scaling.
	 *
	 * @param mesh      the mesh
	 * @param scaling   the scaling of the tikz graphics
	 *
	 * @param <V> the type of the vertices
	 * @param <E> the type of the half-edges
	 * @param <F> the type of the faces
	 *
	 * @return a string representing a tikz graphic
	 */
	public static <V extends IVertex, E extends IHalfEdge, F extends IFace> String toTikz(
			@NotNull final IMesh<V, E, F> mesh, final float scaling, final boolean standalone){
		return toTikz(mesh, f -> DEFAULT_FACE_COLOR, null, null, 1.0f, true);
	}

	private static String toString(final double z) {
		return String.format(Locale.ENGLISH, "%.4f", z);
	}

	/**
	 * Transforms a {@link IMesh} into a tikz string. The tikz graphic is scaled by the scaling.
	 *
	 * @param mesh      the mesh
	 *
	 * @param <V> the type of the vertices
	 * @param <E> the type of the half-edges
	 * @param <F> the type of the faces
	 *
	 * @return a string representing a tikz graphics
	 */
	public static <V extends IVertex, E extends IHalfEdge, F extends IFace> String toTikz(
			@NotNull final IMesh<V, E, F> mesh){
		return toTikz(mesh, 1.0f);
	}

	/**
	 * Transforms a {@link IMesh} into a tikz string. The tikz graphic is scaled by the scaling. Each face
	 * of the mesh is filled by the color defined by the coloring-function.
	 *
	 * @param mesh      the mesh
	 * @param coloring  the coloring function
	 * @param scaling   the scaling of the tikz graphics
	 *
	 * @param <V> the type of the vertices
	 * @param <E> the type of the half-edges
	 * @param <F> the type of the faces
	 *
	 * @return a string representing a tikz graphics
	 */
	public static <V extends IVertex, E extends IHalfEdge, F extends IFace> String toTikz(
			@NotNull final IMesh<V, E, F> mesh,
			@NotNull final Function<F, Color> coloring,
			@Nullable final Function<E, Color> edgeColorFunction,
			final float scaling,
			final boolean drawVertices) {
		return toTikz(mesh, coloring, edgeColorFunction, null, scaling, drawVertices);
	}


	private static <V extends IVertex, E extends IHalfEdge, F extends IFace> Map<Color, String> buildFaceColorMap(
			@NotNull final IMesh<V, E, F> mesh,
			@NotNull final Function<F, Color> coloring) {

		Map<Color, String> bidiMap = new HashMap<>();

		int counter = 1;
		for(F face : mesh.getFaces()) {
			Color c = coloring.apply(face);
			if(!bidiMap.containsKey(c)) {
				bidiMap.put(c, "faceColor"+counter);
				counter++;
			}
		}
		return bidiMap;
	}

	private static <V extends IVertex, E extends IHalfEdge, F extends IFace> Map<Color, String> buildEdgeColorMap(
			@NotNull final IMesh<V, E, F> mesh,
			@Nullable final Function<E, Color> coloring) {

		Map<Color, String> bidiMap = new HashMap<>();

		if(coloring == null) {
			bidiMap.put(DEFAULT_EDGE_COLOR, "edgeColor"+1);
		} else {
			int counter = 1;
			for(E edge : mesh.getEdges()) {
				Color c = coloring.apply(edge);
				if(!bidiMap.containsKey(c)) {
					bidiMap.put(c, "edgeColor"+counter);
					counter++;
				}
			}
		}

		return bidiMap;
	}

	private static <V extends IVertex, E extends IHalfEdge, F extends IFace> Map<Color, String> buildVertexColorMap(
			@NotNull final IMesh<V, E, F> mesh,
			@Nullable final Function<V, Color> coloring) {

		Map<Color, String> bidiMap = new HashMap<>();

		if(coloring == null) {
			bidiMap.put(DEFAULT_VERTEX_COLOR, "vertexColor"+1);
		} else {
			int counter = 1;
			for(V vertex : mesh.getVertices()) {
				Color c = coloring.apply(vertex);
				if(!bidiMap.containsKey(c)) {
					bidiMap.put(c, "vertexColor"+counter);
					counter++;
				}
			}
		}

		return bidiMap;
	}

	private static void prolog(@NotNull final StringBuilder builder,
                             @NotNull final Map<Color, String> faceColorBidiMap,
                             @NotNull final Map<Color, String> edgeColorBidiMap,
                             @NotNull final Map<Color, String> vertexColorBidiMap) {
		builder.append("\\documentclass{standalone}\n");
		builder.append("\\usepackage{tikz}\n\n");
		colorDefinitions(builder, faceColorBidiMap, edgeColorBidiMap, vertexColorBidiMap);
		builder.append("\\pgfmathsetmacro{\\circleSize}{0.01pt}");
		builder.append("\\begin{document}\n");
		builder.append("% Change scaling to [x=1mm,y=1mm] if TeX reports \"Dimension too large\".\n");
		builder.append("\\begin{tikzpicture}\n");
		builder.append("[x=10cm,y=10cm]\n");
		//generateTikzStyles() +
	}

	private static void ending(@NotNull final StringBuilder builder) {
		builder.append("\\end{tikzpicture}\n");
		builder.append("\\end{document}\n");
	}


	private static void colorDefinitions(
			@NotNull final StringBuilder builder,
	                              @NotNull final Map<Color, String> faceColorBidiMap,
	                              @NotNull final Map<Color, String> edgeColorBidiMap,
	                              @NotNull final Map<Color, String> vertexColorBidiMap) {

		builder.append("% Color Definitions\n");
		String colorTextPattern = "\\definecolor{%s}{RGB}{%d,%d,%d}\n";

		for(Object object : faceColorBidiMap.keySet()) {
			Color c = (Color)object;
			String name = faceColorBidiMap.get(c);
			builder.append(String.format(Locale.US, colorTextPattern, name, c.getRed(), c.getGreen(), c.getBlue()));
			builder.append(String.format(Locale.US, colorTextPattern, name+"Fill", c.getRed(), c.getGreen(), c.getBlue()));
		}

		for(Object object : edgeColorBidiMap.keySet()) {
			Color c = (Color)object;
			String name = edgeColorBidiMap.get(c);
			builder.append(String.format(Locale.US, colorTextPattern, name, c.getRed(), c.getGreen(), c.getBlue()));
		}

		for(Object object : vertexColorBidiMap.keySet()) {
			Color c = (Color)object;
			String name = vertexColorBidiMap.get(c);
			builder.append(String.format(Locale.US, colorTextPattern, name, c.getRed(), c.getGreen(), c.getBlue()));
			builder.append(String.format(Locale.US, colorTextPattern, name+"Fill", c.getRed(), c.getGreen(), c.getBlue()));
		}

		builder.append("\n");
	}

	/**
	 * Transforms a {@link IMesh} into a tikz string. The tikz graphic is scaled by the scaling. Each face
	 * of the mesh is filled by the color defined by the coloring-function.
	 *
	 * @param mesh      the mesh
	 * @param coloring  the coloring function
	 * @param scaling   the scaling of the tikz graphics
	 *
	 * @param <V> the type of the vertices
	 * @param <E> the type of the half-edges
	 * @param <F> the type of the faces
	 *
	 * @return a string representing a tikz graphics
	 */
	public static <V extends IVertex, E extends IHalfEdge, F extends IFace> String toTikz(
			@NotNull final IMesh<V, E, F> mesh,
			@NotNull final Function<F, Color> coloring,
			@Nullable final Function<E, Color> edgeColorFunction,
			@Nullable final Function<V, Color> vertexColorFunction,
			final float scaling,
			final boolean drawVertices) {

		StringBuilder builder = new StringBuilder();
		// key = color, value = name (String)
		Map<Color, String> faceColorBidiMap = buildFaceColorMap(mesh, coloring);
		Map<Color, String> edgeColorBidiMap = buildEdgeColorMap(mesh, edgeColorFunction);
		Map<Color, String> vertexColorBidiMap = buildVertexColorMap(mesh, vertexColorFunction);

		prolog(builder, faceColorBidiMap, edgeColorBidiMap, vertexColorBidiMap);

		for(F face : mesh.getFaces()) {
			Color c = coloring.apply(face);
			String colorName = faceColorBidiMap.get(c);
			//String tikzColor = "{rgb,255:red,"+c.getRed()+";green,"+c.getGreen()+";blue,"+c.getBlue()+"}";
			V first = mesh.streamVertices(face).findFirst().get();
			String poly = mesh.streamVertices(face).map(v -> "("+toString(v.getX())+","+toString(v.getY())+")").reduce((s1, s2) -> s1 + "--" + s2).get() + "-- ("+toString(first.getX())+","+toString(first.getY())+")";

			//builder.append("\\fill[fill="+tikzColor+"]" + poly + ";\n");
			if(edgeColorFunction != null) {
				builder.append("\\filldraw[fill="+colorName+"]" + poly + ";\n");
			}
			else {
				builder.append("\\filldraw[color="+colorName+",fill="+colorName+"Fill]" + poly + ";\n");
			}
		}


		for (E edge : mesh.getEdges()) {
			Color c = edgeColorFunction != null ? edgeColorFunction.apply(edge) : DEFAULT_EDGE_COLOR;
			String colorName = edgeColorBidiMap.get(c);
			VLine line = mesh.toLine(edge);
			//String tikzColor = "{rgb,255:red,"+c.getRed()+";green,"+c.getGreen()+";blue,"+c.getBlue()+"}";
			builder.append("\\draw[color="+colorName+"]("+toString(line.getX1())+","+toString(line.getY1())+") -- ("+toString(line.getX2())+","+toString(line.getY2())+");\n");
		}


		if(drawVertices) {
			for(V vertex : mesh.getVertices()) {
				Color c = vertexColorFunction != null ? vertexColorFunction.apply(vertex) : DEFAULT_VERTEX_COLOR;
				String colorName = vertexColorBidiMap.get(c);
				//String tikzColor = "{rgb,255:red,"+c.getRed()+";green,"+c.getGreen()+";blue,"+c.getBlue()+"}";
				builder.append("\\draw[color="+colorName+", fill="+colorName+"Fill]("+toString(vertex.getX())+","+toString(vertex.getY())+") circle (\\circleSize);\n");
			}
		}

		/*for(F face : mesh.getFaces()) {
			String poly = mesh.streamVertices(face).map(v -> "("+v.getX()+","+v.getY()+")").reduce((s1, s2) -> s1 + "--" + s2).get();
			builder.append("\\draw[black,thick]" + poly + ";\n");
		}*/

		ending(builder);
		return builder.toString();
	}

	/**
	 * Transforms a {@link IMesh} into a tikz string. The tikz graphic is scaled by the scaling. Each face
	 * of the mesh is filled by the color defined by the coloring-function.
	 *
	 * @param mesh      the mesh
	 * @param coloring  the coloring function
	 * @param scaling   the scaling of the tikz graphics
	 *
	 * @param <P> the type of the points (containers)
	 * @param <CE> the type of container of the half-edges
	 * @param <CF> the type of the container of the faces
	 * @param <V> the type of the vertices
	 * @param <E> the type of the half-edges
	 * @param <F> the type of the faces
	 *
	 * @return a string representing a tikz graphics
	 */
	public static <P extends IPoint, CE, CF, V extends IVertex, E extends IHalfEdge, F extends IFace> String toTikz(
			@NotNull final IMesh<V, E, F> mesh,
			@NotNull final Function<F, Color> coloring,
			final float scaling) {
		return toTikz(mesh, coloring, null, scaling, false);
	}

	/**
	 * Transforms a {@link IMesh} into a tikz string. The tikz graphic is scaled by the scaling. Each face
	 * of the mesh is filled by the color defined by the coloring-function.
	 *
	 * @param mesh      the mesh
	 * @param coloring  the coloring function
	 * @param scaling   the scaling of the tikz graphics
	 * @param line      a line which will be additionally be drawn
	 *
	 * @param <V> the type of the vertices
	 * @param <E> the type of the half-edges
	 * @param <F> the type of the faces
	 *
	 * @return a string representing a tikz graphics
	 */
	public static <V extends IVertex, E extends IHalfEdge, F extends IFace> String toTikz(
			@NotNull final IMesh<V, E, F> mesh,
			@NotNull final Function<F, Color> coloring,
			final float scaling,
			final VLine line) {

		StringBuilder builder = new StringBuilder();
		builder.append("\\begin{tikzpicture}[scale="+scaling+"]\n");

		for(F face : mesh.getFaces()) {
			Color c = coloring.apply(face);
			String tikzColor = "{rgb,255:red,"+c.getRed()+";green,"+c.getGreen()+";blue,"+c.getBlue()+"}";
			V first = mesh.streamVertices(face).findFirst().get();
			String poly = mesh.streamVertices(face).map(v -> "("+toString(v.getX())+","+toString(v.getY())+")").reduce((s1, s2) -> s1 + "--" + s2).get() + "-- ("+toString(first.getX())+","+toString(first.getY())+")";

			//builder.append("\\fill[fill="+tikzColor+"]" + poly + ";\n");
			builder.append("\\filldraw[color=gray,fill="+tikzColor+"]" + poly + ";\n");
		}

		/*for(F face : mesh.getFaces()) {
			String poly = mesh.streamVertices(face).map(v -> "("+v.getX()+","+v.getY()+")").reduce((s1, s2) -> s1 + "--" + s2).get();
			builder.append("\\draw[black,thick]" + poly + ";\n");
		}*/

		builder.append("\\draw ("+toString(line.getX1()) + "," + toString(line.getY1()) + ") -- ("+ toString(line.getX2()) +"," + toString(line.getY2())+");");
		builder.append("\\end{tikzpicture}");
		return builder.toString();
	}


	public static String toTikz(
			@NotNull final Collection<VTriangle> faces,
			@NotNull final Function<VTriangle, Color> coloring,
			@Nullable final Function<VLine, Color> edgeColorFunction,
			@Nullable final Function<VPoint, Color> vertexColoring,
			final float scaling,
			boolean drawVertices) {

		List<VLine> edges = faces.stream().flatMap(tri -> Arrays.stream(tri.getLines())).collect(Collectors.toList());
		List<VPoint> points = faces.stream().flatMap(tri -> tri.getPoints().stream()).distinct().collect(Collectors.toList());
		StringBuilder builder = new StringBuilder();

		Map<Color, String> faceColorMap = new HashMap<>();
		int counter = 1;
		for(VTriangle face : faces) {
			Color c = coloring.apply(face);
			if(!faceColorMap.containsKey(c)) {
				faceColorMap.put(c, "faceColor"+counter);
				counter++;
			}
		}

		Map<Color, String> edgeColorMap = new HashMap<>();
		if(coloring == null) {
			edgeColorMap.put(DEFAULT_EDGE_COLOR, "edgeColor"+1);
		} else {
			counter = 1;
			for(VLine edge : edges) {
				Color c = edgeColorFunction.apply(edge);
				if(!edgeColorMap.containsKey(c)) {
					edgeColorMap.put(c, "edgeColor"+counter);
					counter++;
				}
			}
		}

		Map<Color, String> vertexColorMap = new HashMap<>();
		if(coloring == null) {
			vertexColorMap.put(DEFAULT_VERTEX_COLOR, "vertexColor"+1);
		} else {
			counter = 1;
			for(VPoint vertex : points) {
				Color c = vertexColoring.apply(vertex);
				if(!vertexColorMap.containsKey(c)) {
					vertexColorMap.put(c, "vertexColor"+counter);
					counter++;
				}
			}
		}

		prolog(builder, faceColorMap, edgeColorMap, vertexColorMap);

		for(VTriangle face : faces) {
			Color c = coloring.apply(face);
			String colorName = faceColorMap.get(c);
			//String tikzColor = "{rgb,255:red,"+c.getRed()+";green,"+c.getGreen()+";blue,"+c.getBlue()+"}";
			VPoint first = face.p1;
			String poly = face.streamPoints().map(v -> "("+toString(v.getX())+","+toString(v.getY())+")").reduce((s1, s2) -> s1 + "--" + s2).get() + "-- ("+toString(first.getX())+","+toString(first.getY())+")";

			//builder.append("\\fill[fill="+tikzColor+"]" + poly + ";\n");
			if(edgeColorFunction != null) {
				builder.append("\\filldraw[fill="+colorName+"]" + poly + ";\n");
			}
			else {
				builder.append("\\filldraw[color="+colorName+",fill="+colorName+"Fill]" + poly + ";\n");
			}
		}


		for (VLine edge : edges) {
			Color c = edgeColorFunction != null ? edgeColorFunction.apply(edge) : DEFAULT_EDGE_COLOR;
			String colorName = edgeColorMap.get(c);
			VLine line = edge;
			//String tikzColor = "{rgb,255:red,"+c.getRed()+";green,"+c.getGreen()+";blue,"+c.getBlue()+"}";
			builder.append("\\draw[color="+colorName+"]("+toString(line.getX1())+","+toString(line.getY1())+") -- ("+toString(line.getX2())+","+toString(line.getY2())+");\n");
		}


		if(drawVertices) {
			for(VPoint vertex : points) {
				Color c = vertexColoring != null ? vertexColoring.apply(vertex) : DEFAULT_VERTEX_COLOR;
				String colorName = vertexColorMap.get(c);
				//String tikzColor = "{rgb,255:red,"+c.getRed()+";green,"+c.getGreen()+";blue,"+c.getBlue()+"}";
				builder.append("\\draw[color="+colorName+", fill="+colorName+"Fill]("+toString(vertex.getX())+","+toString(vertex.getY())+") circle (\\circleSize);\n");
			}
		}

		ending(builder);
		return builder.toString();
	}

	public static <V extends IVertex, E extends IHalfEdge, F extends IFace> String toTikz(
			@NotNull final IMesh<V, E, F> mesh,
			@NotNull final List<F> faces) {
			return toTikz(mesh, DEFAULT_FACE_COLOR, faces, Color.BLACK);
	}

	/**
	 * Helper method to draw the space filling curve.
	 *
	 * @param mesh  the mesh which used to access components of each face
	 * @param faces the list of faces
	 *
	 * @param <V> the type of the vertices
	 * @param <E> the type of the half-edges
	 * @param <F> the type of the faces
	 *
	 * @return a string representing a tikz graphics
	 */
	public static <V extends IVertex, E extends IHalfEdge, F extends IFace> String toTikz(
			@NotNull final IMesh<V, E, F> mesh,
			Color faceColor,
			@NotNull final List<F> faces,
			Color curveColor) {
		StringBuilder builder = new StringBuilder();

		builder.append("\\documentclass{standalone}\n");
		builder.append("\\usepackage{tikz}\n\n");

		builder.append("% Color Definitions\n");
		String colorTextPattern = "\\definecolor{%s}{RGB}{%d,%d,%d}\n";
		builder.append(String.format(Locale.US, colorTextPattern, "face1", faceColor.getRed(), faceColor.getGreen(), faceColor.getBlue()));
		builder.append(String.format(Locale.US, colorTextPattern, "face1Fill", faceColor.getRed(), faceColor.getGreen(), faceColor.getBlue()));

		builder.append(String.format(Locale.US, colorTextPattern, "edge1", DEFAULT_EDGE_COLOR.getRed(), DEFAULT_EDGE_COLOR.getGreen(), DEFAULT_EDGE_COLOR.getBlue()));
		builder.append(String.format(Locale.US, colorTextPattern, "edge1Fill", DEFAULT_EDGE_COLOR.getRed(), DEFAULT_EDGE_COLOR.getGreen(), DEFAULT_EDGE_COLOR.getBlue()));

		builder.append(String.format(Locale.US, colorTextPattern, "curveColor", curveColor.getRed(), curveColor.getGreen(), curveColor.getBlue()));

		builder.append("\\pgfmathsetmacro{\\circleSize}{1.5pt}");
		builder.append("\\begin{document}\n");
		builder.append("% Change scaling to [x=1mm,y=1mm] if TeX reports \"Dimension too large\".\n");
		builder.append("\\begin{tikzpicture}\n");
		builder.append("[x=1cm,y=1cm]\n");


		builder.append("\\draw[curveColor, thick] ");

		for(F face : faces) {
			List<VLine> lines = mesh.streamEdges(face).map(e -> mesh.toLine(e)).collect(Collectors.toList());

			for(VLine line : lines) {
				builder.append("("+toString(line.getX1())+","+toString(line.getY1())+") -- ("+toString(line.getX2())+","+toString(line.getY2())+")\n");
			}
		}

		builder.append(";\n");
		builder.append("\n");

		builder.append("\\draw[curveColor, thick] ");
		VPoint prefIncenter = null;
		VLine firstLine = null;
		VLine lastLine = null;
		for(F face : faces) {
			List<E> edges = mesh.getEdges(face);

			// is triangle
			if(edges.size() == 3) {
				VTriangle triangle = mesh.toTriangle(face);
				VPoint incenter = triangle.getIncenter();

				if(prefIncenter != null) {
					builder.append("("+toString(prefIncenter.getX())+","+toString(prefIncenter.getY())+") -- ("+toString(incenter.getX())+","+toString(incenter.getY())+")\n");
					if(firstLine == null) {
						firstLine = new VLine(prefIncenter, incenter);
					}

					lastLine = new VLine(prefIncenter, incenter);
				}

				prefIncenter = incenter;
			}
		}

		builder.append(";\n");
		if(firstLine != null && lastLine != null) {
			builder.append("\\draw[-{Latex[length=3mm]}, curveColor]("+toString(firstLine.getX1())+","+toString(firstLine.getY1())+") -- ("+toString(firstLine.getX2())+","+toString(firstLine.getY2())+");\n");
			builder.append("\\draw[-{Latex[length=3mm]}, curveColor]("+toString(lastLine.getX1())+","+toString(lastLine.getY1())+") -- ("+toString(lastLine.getX2())+","+toString(lastLine.getY2())+");\n");
		}
		builder.append("\\end{tikzpicture}");
		return builder.toString();
	}

}

/*

%% vertices
\draw[fill=black] (0,0) circle (3pt);
\draw[fill=black] (4,0) circle (3pt);
\draw[fill=black] (2,1) circle (3pt);
\draw[fill=black] (2,3) circle (3pt);
%% vertex labels
\node at (-0.5,0) {1};
\node at (4.5,0) {2};
\node at (2.5,1.2) {3};
\node at (2,3.3) {4};

\begin{tikzpicture}[thick,scale=0.8]
    % The following path utilizes several useful tricks and features:
    % 1) The foreach statement is put inside a path, so all the edges
    %    will in fact be a the same path.
    % 2) The node construct is used to draw the nodes. Nodes are special
    %    in the way that they are drawn *after* the path is drawn. This
    %    is very useful in this case because the nodes will be drawn on
    %    top of the path and therefore hide all edge joins.
    % 3) Simple arithmetics can be used when specifying coordinates.
    \draw \foreach \x in {0,36,...,324}
    {
        (\x:2) node {}  -- (\x+108:2)
        (\x-10:3) node {} -- (\x+5:4)
        (\x-10:3) -- (\x+36:2)
        (\x-10:3) --(\x+170:3)
        (\x+5:4) node {} -- (\x+41:4)
    };
\end{tikzpicture}
 */