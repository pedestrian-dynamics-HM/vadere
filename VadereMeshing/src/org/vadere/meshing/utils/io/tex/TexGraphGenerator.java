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
import org.vadere.util.geometry.shapes.VTriangle;

import java.awt.*;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
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

	public static <V extends IVertex, E extends IHalfEdge, F extends IFace> String toTikz(
			@NotNull final IMesh<V, E, F> mesh, final float scaling){
		return toTikz(mesh, scaling, false);
	}


	public static <V extends IVertex, E extends IHalfEdge, F extends IFace> String toTikz(
			@NotNull final IMesh<V, E, F> mesh, boolean standalone){
		return toTikz(mesh, 1.0f, standalone);
	}

	public static String toTikz(@NotNull final Collection<VLine> lines) {
		return toTikz(lines, 1.0);
	}

	public static String toTikz(@NotNull final Collection<VLine> lines, final double scaling) {
		StringBuilder builder = new StringBuilder();
		builder.append("\\begin{tikzpicture}[scale="+scaling+"]\n");
		for(VLine line : lines) {
			builder.append("\\draw ("+toString(line.getX1()) + "," + toString(line.getY1()) + ") -- ("+ toString(line.getX2()) +"," + toString(line.getY2())+");\n");
		}
		builder.append("\\end{tikzpicture}");
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
		StringBuilder builder = new StringBuilder();
		if(standalone) {
			builder.append("\\documentclass[usenames,dvipsnames]{standalone}\n");
			builder.append("\\usepackage{tikz}\n");
			builder.append("\\begin{document}\n");
		}
		builder.append("\\begin{tikzpicture}[scale="+scaling+"]\n");

		for(VPoint point : mesh.getUniquePoints()) {
			//builder.append("\\draw[fill=black] ("+point.getX()+","+point.getY()+") circle (3pt); \n");
		}

		builder.append("\\draw ");

		for(VLine line : mesh.getLines()) {
			builder.append("("+toString(line.getX1())+","+toString(line.getY1())+") -- ("+toString(line.getX2())+","+toString(line.getY2())+")\n");
		}

		builder.append(";\n");

		builder.append("\\end{tikzpicture}");

		if(standalone) {
			builder.append("\\end{document}");
		}

		return builder.toString();
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
		builder.append("\\begin{tikzpicture}[scale="+scaling+"]\n");

		for(F face : mesh.getFaces()) {
			Color c = coloring.apply(face);
			String tikzColor = "{rgb,255:red,"+c.getRed()+";green,"+c.getGreen()+";blue,"+c.getBlue()+"}";
			V first = mesh.streamVertices(face).findFirst().get();
			String poly = mesh.streamVertices(face).map(v -> "("+toString(v.getX())+","+toString(v.getY())+")").reduce((s1, s2) -> s1 + "--" + s2).get() + "-- ("+toString(first.getX())+","+toString(first.getY())+")";

			//builder.append("\\fill[fill="+tikzColor+"]" + poly + ";\n");
			if(edgeColorFunction != null) {
				builder.append("\\filldraw[fill="+tikzColor+"]" + poly + ";\n");
			}
			else {
				builder.append("\\filldraw[color=gray,fill="+tikzColor+"]" + poly + ";\n");
			}
		}

		if(edgeColorFunction != null) {
			for (E edge : mesh.getEdges()) {
				Color c = edgeColorFunction.apply(edge);
				VLine line = mesh.toLine(edge);
				String tikzColor = "{rgb,255:red,"+c.getRed()+";green,"+c.getGreen()+";blue,"+c.getBlue()+"}";
				builder.append("\\draw[color="+tikzColor+"]("+toString(line.getX1())+","+toString(line.getY1())+") -- ("+toString(line.getX2())+","+toString(line.getY2())+");\n");
			}
		}
		double pt = 1.5;
		if(drawVertices) {
			for(V vertex : mesh.getVertices()) {
				Color c = vertexColorFunction != null ? vertexColorFunction.apply(vertex) : Color.BLACK;
				String tikzColor = "{rgb,255:red,"+c.getRed()+";green,"+c.getGreen()+";blue,"+c.getBlue()+"}";
				builder.append("\\draw[color="+tikzColor+", fill="+tikzColor+"]("+toString(vertex.getX())+","+toString(vertex.getY())+") circle ("+pt+"pt);\n");
			}
		}

		/*for(F face : mesh.getFaces()) {
			String poly = mesh.streamVertices(face).map(v -> "("+v.getX()+","+v.getY()+")").reduce((s1, s2) -> s1 + "--" + s2).get();
			builder.append("\\draw[black,thick]" + poly + ";\n");
		}*/

		builder.append("\\end{tikzpicture}");
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
			final float scaling) {

		StringBuilder builder = new StringBuilder();
		builder.append("\\begin{tikzpicture}[scale="+scaling+"]\n");

		for(VTriangle face : faces) {
			Color c = coloring.apply(face);
			String tikzColor = "{rgb,255:red,"+c.getRed()+";green,"+c.getGreen()+";blue,"+c.getBlue()+"}";
			List<VPoint> points = face.getPoints();
			VPoint first = points.get(0);
			String poly = points.stream().map(v -> "("+toString(v.getX())+","+toString(v.getY())+")").reduce((s1, s2) -> s1 + "--" + s2).get() + "-- ("+toString(first.getX())+","+toString(first.getY())+")";

			//builder.append("\\fill[fill="+tikzColor+"]" + poly + ";\n");
			builder.append("\\filldraw[color=gray,fill="+tikzColor+"]" + poly + ";\n");
		}

		/*for(F face : mesh.getFaces()) {
			String poly = mesh.streamVertices(face).map(v -> "("+v.getX()+","+v.getY()+")").reduce((s1, s2) -> s1 + "--" + s2).get();
			builder.append("\\draw[black,thick]" + poly + ";\n");
		}*/

		builder.append("\\end{tikzpicture}");
		return builder.toString();
	}

	/**
	 * Transforms a list of faces into a tikz string.
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
			@NotNull final List<F> faces) {
		StringBuilder builder = new StringBuilder();
		builder.append("\\begin{tikzpicture}[scale=1.0]\n");

		builder.append("\\draw[gray, thick] ");

		for(F face : faces) {
			List<VLine> lines = mesh.streamEdges(face).map(e -> mesh.toLine(e)).collect(Collectors.toList());

			for(VLine line : lines) {
				builder.append("("+toString(line.getX1())+","+toString(line.getY1())+") -- ("+toString(line.getX2())+","+toString(line.getY2())+")\n");
			}
		}

		builder.append(";\n");
		builder.append("\n");

		builder.append("\\draw[black, thick] ");
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
			builder.append("\\draw[-{Latex[length=3mm]}]("+toString(firstLine.getX1())+","+toString(firstLine.getY1())+") -- ("+toString(firstLine.getX2())+","+toString(firstLine.getY2())+");\n");
			builder.append("\\draw[-{Latex[length=3mm]}]("+toString(lastLine.getX1())+","+toString(lastLine.getY1())+") -- ("+toString(lastLine.getX2())+","+toString(lastLine.getY2())+");\n");
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