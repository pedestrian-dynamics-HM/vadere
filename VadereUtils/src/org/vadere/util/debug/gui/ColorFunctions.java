package org.vadere.util.debug.gui;

import org.vadere.util.geometry.mesh.inter.IFace;
import org.vadere.util.geometry.mesh.inter.IHalfEdge;
import org.vadere.util.geometry.mesh.inter.IMesh;
import org.vadere.util.geometry.mesh.inter.IVertex;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VLine;

import java.awt.*;
import java.util.HashMap;
import java.util.function.BiFunction;

/**
 * Provides color values for mesh drawings depending on some functions.
 * default implementations for faceDrawColor (boarder lines) and faceFillColor (filling)
 * are present.
 *
 * @param <P> P extends IPoint
 * @param <V> V extends IVertex<P>
 * @param <E> E extends IHalfEdge<P>
 * @param <F> F extends IFace<P>
 */
public class ColorFunctions
		<P extends IPoint, V extends IVertex<P>, E extends IHalfEdge<P>, F extends IFace<P>> {

	private BiFunction<IMesh<P, V, E, F>, F, Color> faceFillColorFunc;
	private BiFunction<IMesh<P, V, E, F>, F, Color> faceDrawColorFunc;
	private HashMap<F, Color> faceFillColorMap;
	private HashMap<F, Color> faceDrawColorMap;


	public ColorFunctions() {
		faceFillColorFunc = (mesh, face) -> ColorFunctions.qualityToGrayScale(mesh, face);
		faceDrawColorFunc = (m, f) -> Color.BLACK;
		faceFillColorMap = new HashMap<>();
		faceDrawColorMap = new HashMap<>();
	}

	/**
	 * Create gray scale color codes for 'goodness' of a triangle from black (bad) to white (good)
	 *
	 * @param mesh Mesh used for coloring
	 * @param face Face to color
	 * @return double value used for Color (only one needed for gray scale)
	 */
	public static <P extends IPoint, V extends IVertex<P>, E extends IHalfEdge<P>, F extends IFace<P>> double faceToQuality(final IMesh<P, V, E, F> mesh, final F face) {
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
	 * Create gray scale color codes for 'goodness' of a triangle from black (bad) to white (good)
	 *
	 * @param mesh Mesh used for coloring
	 * @param face Face to color
	 * @return Gray scale color object
	 */
	public static <P extends IPoint, V extends IVertex<P>, E extends IHalfEdge<P>, F extends IFace<P>> Color qualityToGrayScale(final IMesh<P, V, E, F> mesh, final F face) {
		float quality = (float) faceToQuality(mesh, face);
		if(quality <= 1 && quality >= 0) {
			return new Color(quality, quality, quality);
		}
		else {
			return Color.RED;
		}
	}

	/**
	 * Select face FillColor based on {@link #faceFillColorFunc} set in this object. If the color is
	 * overwritten for the specified face by {@link #faceFillColorMap} then use this color.
	 *
	 * @param mesh Mesh used for coloring
	 * @param face Face to color
	 * @return Color object
	 */
	public Color faceFillColor(final IMesh<P, V, E, F> mesh, final F face) {
		if (faceFillColorMap.containsKey(face)) {
			return faceFillColorMap.get(face);
		}
		return this.faceFillColorFunc.apply(mesh, face);
	}

	/**
	 * Select face DrawColor based on {@link #faceDrawColorFunc} set in this object. If the color is
	 * overwritten for the specified face by {@link #faceDrawColorMap} then use this color.
	 *
	 * @param mesh Mesh used for coloring
	 * @param face Face to color
	 * @return Color object
	 */
	public Color faceDrawColor(final IMesh<P, V, E, F> mesh, final F face) {
		if (faceDrawColorMap.containsKey(face)) {
			return faceDrawColorMap.get(face);
		}

		return this.faceDrawColorFunc.apply(mesh, face);
	}

	/**
	 * Overwrite color returned by {@link #faceFillColorFunc} for specified face with specified color
	 *
	 * @param face  Face which color will be overwritten
	 * @param color New color for face
	 */
	public void overwriteFillColor(F face, Color color) {
		faceFillColorMap.put(face, color);
	}

	/**
	 * Set a new faceFillColorFunc for this object.
	 *
	 * @param faceFillColorFunc BiFunction which specifies which color a face interior will get.
	 */
	public void setFaceFillColorFunc(BiFunction<IMesh<P, V, E, F>, F, Color> faceFillColorFunc) {
		this.faceFillColorFunc = faceFillColorFunc;
	}

	/**
	 * Set a new faceFillColorFunc for this object.
	 *
	 * @param faceDrawColorFunc BiFunction which specifies which color the face contour will get.
	 */
	public void setFaceDrawColorFunc(BiFunction<IMesh<P, V, E, F>, F, Color> faceDrawColorFunc) {
		this.faceDrawColorFunc = faceDrawColorFunc;
	}

	/**
	 * Overwrite color returned by {@link #faceDrawColorFunc} for specified face with specified color
	 *
	 * @param face  Face which color will be overwritten
	 * @param color New color for face
	 */
	public void overwriteDrawColor(F face, Color color) {
		faceDrawColorMap.put(face, color);
	}

	/**
	 * delete previously overwritten face/color pairs.
	 */
	public void clear() {
		faceDrawColorMap.clear();
		faceFillColorMap.clear();
	}

}
