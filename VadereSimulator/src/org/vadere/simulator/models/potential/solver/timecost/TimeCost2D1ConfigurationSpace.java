package org.vadere.simulator.models.potential.solver.timecost;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.nio.file.Paths;

import org.vadere.util.geometry.Geometry;
import org.vadere.util.geometry.Vector3D;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VPolygon;
import org.vadere.util.io.GeometryPrinter;
import org.vadere.util.io.IOUtils;

/**
 * Computes a time cost potential field based on obstacles and a rigid body that
 * can be rotated around its center. This creates a 3D time cost field (2 for
 * space, 1 for angle3D). see Sethian: Level Set Methods and Fast Marching
 * Methods, p. 287ff
 * 
 */
public class TimeCost2D1ConfigurationSpace implements ITimeCostFunction3D {

	private double[][][] costField;
	private int gridsize;
	private double cellSize;
	private int gridsizeAngle;

	/**
	 * Computes a time cost potential field based on obstacles and a rigid body
	 * that can be rotated around its center.
	 * 
	 * @param geometry
	 *        geometry with obstacles
	 * @param floorshape
	 *        the shape/dimensions of the current floor
	 * @param rigidBodyShape
	 *        shape of the rigid body
	 * @param cellSize
	 *        size of one space cell
	 * @param cellSizeAngle
	 *        size of one rotation cell
	 */
	public TimeCost2D1ConfigurationSpace(Geometry geometry, Shape floorshape,
			VPolygon rigidBodyShape, double cellSize, double cellSizeAngle) {

		floorshape.getBounds2D().getMinX();
		floorshape.getBounds2D().getMinY();
		double width = floorshape.getBounds2D().getWidth();
		double height = floorshape.getBounds2D().getHeight();

		this.cellSize = cellSize;
		// compute the grid size based on the given cell size
		gridsize = (int) Math.ceil(Math.max(width, height) / cellSize);
		gridsizeAngle = (int) Math.ceil(1.0 * Math.PI / cellSizeAngle);

		costField = new double[gridsize][gridsize][gridsizeAngle];

		// generate cost field
		generateCostField(geometry, rigidBodyShape);

		String str = GeometryPrinter.grid2string(costField);
		try {
			IOUtils.printDataFile(Paths.get("output", "test_FM_3D_cost"), str);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Generate the cost field for the configuration space. The given shape is
	 * rotated 360 degrees.
	 * 
	 * @param scenario
	 * @param shape
	 */
	private void generateCostField(Geometry geometry, VPolygon shape) {

		// compute time cost
		for (int r = 0; r < gridsize; r++) {
			for (int c = 0; c < gridsize; c++) {
				for (int iPhi = 0; iPhi < gridsizeAngle; iPhi++) {
					VPoint current = new VPoint(c * cellSize, r * cellSize);
					VPolygon cshape = new VPolygon(shape);
					cshape.transform(AffineTransform.getTranslateInstance(
							current.x, current.y));
					cshape.transform(AffineTransform.getRotateInstance(iPhi
							/ ((double) gridsizeAngle) * Math.PI * 1.0));

					if (geometry.intersects(cshape, false, true)) {
						costField[r][c][iPhi] = 0;
					} else {
						costField[r][c][iPhi] = 1;
					}
				}
			}
		}
	}

	/**
	 * @see calculators.geometry.timecost.ITimeCostFunction3D#costAt(org.vadere.util.geometry.Vector3D)
	 */
	@Override
	public double costAt(Vector3D p) {
		return costField[(int) p.x][(int) p.y][(int) p.z];
	}

}
