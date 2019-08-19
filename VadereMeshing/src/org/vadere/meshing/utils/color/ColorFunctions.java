package org.vadere.meshing.utils.color;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.inter.IFace;
import org.vadere.meshing.mesh.inter.IHalfEdge;
import org.vadere.meshing.mesh.inter.IMesh;
import org.vadere.meshing.mesh.inter.IVertex;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VLine;

import java.awt.*;
import java.util.HashMap;
import java.util.function.BiFunction;

/**
 * <p>Provides color values for mesh drawings depending on some functions.
 * default implementations for faceDrawColor (boarder lines) and faceFillColor (filling)
 * are present.</p>
 *
 * @param <P> the type of the points (containers)
 * @param <CE> the type of container of the half-edges
 * @param <CF> the type of the container of the faces
 * @param <V> the type of the vertices
 * @param <E> the type of the half-edges
 * @param <F> the type of the faces
 */
public class ColorFunctions
		<P extends IPoint, CE, CF, V extends IVertex, E extends IHalfEdge, F extends IFace> {

	private BiFunction<IMesh<V, E, F>, F, Color> faceFillColorFunc;
	private BiFunction<IMesh<V, E, F>, F, Color> faceDrawColorFunc;
	private HashMap<F, Color> faceFillColorMap;
	private HashMap<F, Color> faceDrawColorMap;


	/**
	 * <p>The default constructor.</p>
	 */
	public ColorFunctions() {
		faceFillColorFunc = (mesh, face) -> ColorFunctions.qualityToGrayScale(mesh, face);
		faceDrawColorFunc = (m, f) -> Color.BLACK;
		faceFillColorMap = new HashMap<>();
		faceDrawColorMap = new HashMap<>();
	}

	/**
	 * <p>Computes the triangle quality of this face / triangle.</p>
	 *
	 * <p>Assumption: The face is a valid triangle.</p>
	 *
	 * @param mesh Mesh used for coloring
	 * @param face face / triangle of which the quality will be computed
	 *
	 * @param <P> the type of the points (containers)
	 * @param <CE> the type of container of the half-edges
	 * @param <CF> the type of the container of the faces
	 * @param <V> the type of the vertices
	 * @param <E> the type of the half-edges
	 * @param <F> the type of the faces
	 *
	 * @return the quality in (0;1) of the triangle
	 */
	public static <P extends IPoint, CE, CF, V extends IVertex, E extends IHalfEdge, F extends IFace> double faceToQuality(final IMesh<V, E, F> mesh, final F face) {
		VLine[] lines = mesh.toTriangle(face).getLines();
		double a = lines[0].length();
		double b = lines[1].length();
		double c = lines[2].length();
		double part = 0.0;
		if (a != 0.0 && b != 0.0 && c != 0.0) {
			part = ((b + c - a) * (c + a - b) * (a + b - c)) / (a * b * c);
		} else {
			throw new IllegalArgumentException(face + " is not a feasible triangle!");
		}
		return part;
	}

	/**
	 *
	 * @param mesh Mesh used for coloring
	 * @param face face / triangle of which the color will be computed
	 *
	 * @param <P> the type of the points (containers)
	 * @param <CE> the type of container of the half-edges
	 * @param <CF> the type of the container of the faces
	 * @param <V> the type of the vertices
	 * @param <E> the type of the half-edges
	 * @param <F> the type of the faces
	 *
	 * @return gray scale color object
	 */
	public static <P extends IPoint, CE, CF, V extends IVertex, E extends IHalfEdge, F extends IFace> Color qualityToGrayScale(final IMesh<V, E, F> mesh, final F face) {
		if(!mesh.isBoundary(face)) {
			float quality = (float) faceToQuality(mesh, face);
			if(quality <= 1 && quality >= 0) {
				return new Color(quality, quality, quality);
			}
			else {
				return Color.RED;
			}
		}
		else {
			return Color.WHITE;
		}
	}

	/**
	 * <p>Select face FillColor based on {@link #faceFillColorFunc} set in this object. If the color is
	 * overwritten for the specified face by {@link #faceFillColorMap} then use this color.</p>
	 *
	 * @param mesh Mesh used for coloring
	 * @param face Face to color
	 * @return color object
	 */
	public Color faceFillColor(final IMesh<V, E, F> mesh, final F face) {
		if (faceFillColorMap.containsKey(face)) {
			return faceFillColorMap.get(face);
		}
		return this.faceFillColorFunc.apply(mesh, face);
	}

	/**
	 * <p>Select face DrawColor based on {@link #faceDrawColorFunc} set in this object. If the color is
	 * overwritten for the specified face by {@link #faceDrawColorMap} then use this color.</p>
	 *
	 * @param mesh Mesh used for coloring
	 * @param face Face to color
	 * @return Color object
	 */
	public Color faceDrawColor(final IMesh<V, E, F> mesh, final F face) {
		if (faceDrawColorMap.containsKey(face)) {
			return faceDrawColorMap.get(face);
		}

		return this.faceDrawColorFunc.apply(mesh, face);
	}

	/**
	 * <p>Overwrite color returned by {@link #faceFillColorFunc} for specified face with specified color.</p>
	 *
	 * @param face  Face which color will be overwritten
	 * @param color New color for face
	 */
	public void overwriteFillColor(@NotNull final F face, @NotNull final Color color) {
		faceFillColorMap.put(face, color);
	}

	/**
	 * <p>Set a new faceFillColorFunc for this object.</p>
	 *
	 * @param faceFillColorFunc BiFunction which specifies which color a face interior will get.
	 */
	public void setFaceFillColorFunc(@NotNull final BiFunction<IMesh<V, E, F>, F, Color> faceFillColorFunc) {
		this.faceFillColorFunc = faceFillColorFunc;
	}

	/**
	 * <p>Set a new faceFillColorFunc for this object.</p>
	 *
	 * @param faceDrawColorFunc BiFunction which specifies which color the face contour will get.
	 */
	public void setFaceDrawColorFunc(@NotNull final BiFunction<IMesh<V, E, F>, F, Color> faceDrawColorFunc) {
		this.faceDrawColorFunc = faceDrawColorFunc;
	}

	/**
	 * <p>Overwrite color returned by {@link #faceDrawColorFunc} for specified face with specified color.</p>
	 *
	 * @param face  Face which color will be overwritten
	 * @param color New color for face
	 */
	public void overwriteDrawColor(@NotNull final F face, @NotNull final Color color) {
		faceDrawColorMap.put(face, color);
	}

	/**
	 * <p>delete previously overwritten face/color pairs.</p>
	 */
	public void clear() {
		faceDrawColorMap.clear();
		faceFillColorMap.clear();
	}

}
