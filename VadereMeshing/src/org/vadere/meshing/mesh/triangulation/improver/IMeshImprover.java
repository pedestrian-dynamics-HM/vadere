package org.vadere.meshing.mesh.triangulation.improver;

import org.vadere.meshing.mesh.inter.IFace;
import org.vadere.meshing.mesh.inter.IHalfEdge;
import org.vadere.meshing.mesh.inter.IMesh;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.meshing.mesh.inter.IVertex;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VTriangle;

import java.util.Collection;

/**
 *
 * @author Benedikt Zoennchen
 *
 * @param <P> the type of the points (containers)
 * @param <CE> the type of container of the half-edges
 * @param <CF> the type of the container of the faces
 * @param <V> the type of the vertices
 * @param <E> the type of the half-edges
 * @param <F> the type of the faces
 */
public interface IMeshImprover<P extends IPoint, CE, CF, V extends IVertex<P>, E extends IHalfEdge<CE>, F extends IFace<CF>> {

    /**
     * Returns a collection of triangles i.e. all the faces of the current mesh.
     *
     * @return a collection of triangles i.e. all the faces of the current mesh
     */
    Collection<VTriangle> getTriangles();

    /**
     * Returns the current mesh (reference) i.e. with the current state of improvement.
     *
     * @return  the current mesh (reference)
     */
    IMesh<P, CE, CF, V, E, F> getMesh();

    /**
     * improves the current triangulation / mesh.
     *
     */
    void improve();

    /**
     * Returns the current triangulation / mesh.
     *
     * @return the current triangulation / mesh
     */
    IIncrementalTriangulation<P, CE, CF, V, E, F> getTriangulation();

	/**
	 * Returns the current triangulation / mesh.
	 *
	 * @return the overall quality of the triangulation.
	 */
	default double getQuality() {
		Collection<F> faces = getMesh().getFaces();
		return faces.stream().map(face -> faceToQuality(face)).reduce((d1, d2) -> d1 + d2).get() / faces.size();
	}

	/**
	 * Returns the quality of the triangle with the lowest / worst quality in O(n),
	 * where n is the number of faces.
	 *
	 * @return the quality of the triangle with the lowest quality
	 */
	default double getMinQuality() {
		Collection<F> faces = getMesh().getFaces();
		return faces.stream().map(face -> faceToQuality(face)).reduce((d1, d2) -> Math.min(d1, d2)).get();
	}

	/**
	 * Returns the quality of a face / triangle.
	 *
	 * @param face the face which has to be a valid triangle
	 * @return the quality of a face / triangle
	 */
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
