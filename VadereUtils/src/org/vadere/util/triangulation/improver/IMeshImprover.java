package org.vadere.util.triangulation.improver;

import org.vadere.util.geometry.mesh.inter.*;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VTriangle;

import java.util.Collection;

/**
 * @author Benedikt Zoennchen
 */
public interface IMeshImprover<P extends IPoint, V extends IVertex<P>, E extends IHalfEdge<P>, F extends IFace<P>> {

    /**
     * returns a collection of triangles i.e. all the faces of the current mesh.
     *
     * @return a collection of triangles i.e. all the faces of the current mesh
     */
    Collection<VTriangle> getTriangles();

    /**
     * returns the current mesh (reference) i.e. with the current state of improvement.
     *
     * @return  the current mesh (reference)
     */
    IMesh<P, V, E, F> getMesh();

    /**
     * improves the current triangulation / mesh.
     *
     */
    void improve();

    /**
     * returns the current triangulation / mesh.
     *
     * @return
     */
    ITriangulation<P, V, E, F> getTriangulation();

	/**
	 *
	 * @return
	 */
	default double getQuality() {
		Collection<F> faces = getMesh().getFaces();
		return faces.stream().map(face -> faceToQuality(face)).reduce((d1, d2) -> d1 + d2).get() / faces.size();
	}

	default double getMinQuality() {
		Collection<F> faces = getMesh().getFaces();
		return faces.stream().map(face -> faceToQuality(face)).reduce((d1, d2) -> Math.min(d1, d2)).get();
	}

	default double faceToQuality(final F face) {
		VLine[] lines = getMesh().toTriangle(face).getLines();
		double a = lines[0].length();
		double b = lines[1].length();
		double c = lines[2].length();
		double part = 0.0;
		if(a != 0.0 && b != 0.0 && c != 0.0) {
			part = ((b + c - a) * (c + a - b) * (a + b - c)) / (a * b * c);
		}
		else {
			throw new IllegalArgumentException(face + " is not a feasible triangle!");
		}
		return part;
	}

}
