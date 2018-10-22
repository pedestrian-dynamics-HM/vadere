package org.vadere.geometry.tex;

import org.jetbrains.annotations.NotNull;
import org.vadere.geometry.mesh.inter.IFace;
import org.vadere.geometry.mesh.inter.IHalfEdge;
import org.vadere.geometry.mesh.inter.IMesh;
import org.vadere.geometry.mesh.inter.IVertex;
import org.vadere.geometry.shapes.IPoint;
import org.vadere.geometry.shapes.VLine;
import org.vadere.geometry.shapes.VPoint;
import org.vadere.geometry.shapes.VTriangle;

import java.awt.*;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TexGraphGenerator {

	/**
	 * Transforms a {@link IMesh} into a tikz string. The tikz graphic is scaled by the scaling.
	 *
	 * @param mesh      the mesh
	 * @param scaling   the scaling of the tikz graphics
	 * @return a string representing a tikz graphics
	 */
	public static <P extends IPoint, V extends IVertex<P>, E extends IHalfEdge<P>, F extends IFace<P>> String toTikz(
			@NotNull final IMesh<P, V, E, F> mesh, final float scaling){
		StringBuilder builder = new StringBuilder();
		builder.append("\\begin{tikzpicture}[scale="+scaling+"]\n");

		for(VPoint point : mesh.getUniquePoints()) {
			//builder.append("\\draw[fill=black] ("+point.getX()+","+point.getY()+") circle (3pt); \n");
		}

		builder.append("\\draw ");

		for(VLine line : mesh.getLines()) {
			builder.append("("+line.getX1()+","+line.getY1()+") -- ("+line.getX2()+","+line.getY2()+")\n");
		}

		builder.append(";\n");

		builder.append("\\end{tikzpicture}");

		return builder.toString();
	}

	/**
	 * Transforms a {@link IMesh} into a tikz string. The tikz graphic is scaled by the scaling.
	 *
	 * @param mesh  the mesh
	 * @return a string representing a tikz graphics
	 */
	public static <P extends IPoint, V extends IVertex<P>, E extends IHalfEdge<P>, F extends IFace<P>> String toTikz(
			@NotNull final IMesh<P, V, E, F> mesh){
		return toTikz(mesh, 1.0f);
	}

	/**
	 * Transforms a {@link IMesh} into a tikz string. The tikz graphic is scaled by the scaling. Each face
	 * of the mesh is filled by the color defined by the coloring-function.
	 *
	 * @param mesh      the mesh
	 * @param coloring  the coloring function
	 * @param scaling   the scaling of the tikz graphics
	 * @return a string representing a tikz graphics
	 */
	public static <P extends IPoint, V extends IVertex<P>, E extends IHalfEdge<P>, F extends IFace<P>> String toTikz(
			@NotNull final IMesh<P, V, E, F> mesh,
			@NotNull final Function<F, Color> coloring,
			final float scaling) {

		StringBuilder builder = new StringBuilder();
		builder.append("\\begin{tikzpicture}[scale="+scaling+"]\n");

		for(F face : mesh.getFaces()) {
			Color c = coloring.apply(face);
			String tikzColor = "{rgb,255:red,"+c.getRed()+";green,"+c.getGreen()+";blue,"+c.getBlue()+"}";
			V first = mesh.streamVertices(face).findFirst().get();
			String poly = mesh.streamVertices(face).map(v -> "("+v.getX()+","+v.getY()+")").reduce((s1, s2) -> s1 + "--" + s2).get() + "-- ("+first.getX()+","+first.getY()+")";

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
			String poly = points.stream().map(v -> "("+v.getX()+","+v.getY()+")").reduce((s1, s2) -> s1 + "--" + s2).get() + "-- ("+first.getX()+","+first.getY()+")";

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
	 * @return a string representing a tikz graphics
	 */
	public static <P extends IPoint, V extends IVertex<P>, E extends IHalfEdge<P>, F extends IFace<P>> String toTikz(
			@NotNull final IMesh<P, V, E, F> mesh,
			@NotNull final List<F> faces) {
		StringBuilder builder = new StringBuilder();
		builder.append("\\begin{tikzpicture}[scale=1.0]\n");

		builder.append("\\draw[gray, thick] ");

		for(F face : faces) {
			List<VLine> lines = mesh.streamEdges(face).map(e -> mesh.toLine(e)).collect(Collectors.toList());

			for(VLine line : lines) {
				builder.append("("+line.getX1()+","+line.getY1()+") -- ("+line.getX2()+","+line.getY2()+")\n");
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
					builder.append("("+prefIncenter.getX()+","+prefIncenter.getY()+") -- ("+incenter.getX()+","+incenter.getY()+")\n");
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
			builder.append("\\draw[-{Latex[length=3mm]}]("+firstLine.getX1()+","+firstLine.getY1()+") -- ("+firstLine.getX2()+","+firstLine.getY2()+");\n");
			builder.append("\\draw[-{Latex[length=3mm]}]("+lastLine.getX1()+","+lastLine.getY1()+") -- ("+lastLine.getX2()+","+lastLine.getY2()+");\n");
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