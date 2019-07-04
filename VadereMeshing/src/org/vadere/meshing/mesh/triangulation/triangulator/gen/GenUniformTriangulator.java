package org.vadere.meshing.mesh.triangulation.triangulator.gen;

import org.vadere.meshing.mesh.inter.IFace;
import org.vadere.meshing.mesh.inter.IHalfEdge;
import org.vadere.meshing.mesh.inter.IMesh;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.meshing.mesh.inter.IVertex;
import org.vadere.meshing.mesh.triangulation.triangulator.inter.ITriangulator;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VRectangle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * This triangulator triangulates a whole bounding box by a uniform triangulation of a specific side length.
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
public class GenUniformTriangulator<V extends IVertex, E extends IHalfEdge, F extends IFace> implements ITriangulator<V, E, F> {

	private double left;
	private double top;
	private double width;
	private double height;
	private double minTriangleSideLength;
	private VRectangle bound;
	private final IIncrementalTriangulation<V, E, F> triangulation;

	/**
	 * The default constructor.
	 *
	 * @param bound                 the bounding box
	 * @param minTriangleSideLength the minimal side length
	 * @param triangulation         the triangulation i.e. the algorithm inserting points
	 */
	public GenUniformTriangulator(final VRectangle bound,
	                              final double minTriangleSideLength,
	                              final IIncrementalTriangulation<V, E, F> triangulation) {
		this.bound = bound;
		this.triangulation = triangulation;
		this.left = bound.getMinX();
		this.top = bound.getMinY();
		this.width = bound.getWidth();
		this.height = bound.getHeight();
		this.minTriangleSideLength = minTriangleSideLength;
	}

    @Override
    public IIncrementalTriangulation<V, E, F> generate() {
        return generate(true);
    }

	@Override
	public IIncrementalTriangulation<V, E, F> generate(boolean finalize) {

		triangulation.init();

		List<IPoint> pointList = new ArrayList<>(generatePointSet());
		//Collections.shuffle(pointList);

		for(IPoint point : pointList) {
			if(bound.contains(point)) {
				triangulation.insert(point);
			}
		}

		if(finalize) {
			triangulation.finish();
		}
		return triangulation;
	}

	@Override
	public IIncrementalTriangulation<V, E, F> getTriangulation() {
		return triangulation;
	}

	@Override
	public IMesh<V, E, F> getMesh() {
        return triangulation.getMesh();
	}

	private Collection<IPoint> generatePointSet() {
		// height of a triangle with 60 deg everywhere
		List<IPoint> pointSet = new ArrayList<>();
		double s = minTriangleSideLength;
		double h = minTriangleSideLength * Math.sqrt(3) / 2.0;
		// create stencil with four triangle which can be used to triangulate
		// the whole rectangle seamlessly
		IPoint add1 = getMesh().createPoint(-s / 2, h);
		IPoint add2 = getMesh().createPoint(s / 2, h);
		IPoint add3 = getMesh().createPoint(s, 0);
		IPoint add4 = getMesh().createPoint(0, 2 * h);
		IPoint add5 = getMesh().createPoint(s, 2 * h);

		for (int row = 0; row < (int) Math.ceil(height / h) + 1; row += 2) {
			for (int col = 0; col < (int) Math.ceil(width
					/ minTriangleSideLength); col++) {
				IPoint p1 = getMesh().createPoint(left + col * minTriangleSideLength, top + row * h);

				IPoint p2 = getMesh().createPoint(p1.getX() + add1.getX(), p1.getY() + add1.getY());
				IPoint p3 = getMesh().createPoint(p1.getX() + add2.getX(), p1.getY() + add2.getY());
				IPoint p4 = getMesh().createPoint(p1.getX() + add3.getX(), p1.getY() + add3.getY());
				IPoint p5 = getMesh().createPoint(p1.getX() + add4.getX(), p1.getY() + add4.getY());
				IPoint p6 = getMesh().createPoint(p1.getX() + add5.getX(), p1.getY() + add5.getY());

				pointSet.add(p1);
				pointSet.add(p2);
				pointSet.add(p3);
				pointSet.add(p4);
				pointSet.add(p5);
				pointSet.add(p6);
			}
		}
		return pointSet;
	}

}
