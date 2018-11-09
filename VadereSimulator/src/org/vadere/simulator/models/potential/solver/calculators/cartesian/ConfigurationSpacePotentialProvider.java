package org.vadere.simulator.models.potential.solver.calculators.cartesian;

import java.awt.Shape;
import java.util.LinkedList;
import java.util.List;

import org.vadere.util.geometry.Geometry;
import org.vadere.util.geometry.Vector3D;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VPolygon;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.data.cellgrid.CellStateFD;
import org.vadere.simulator.models.potential.solver.calculators.PotentialFieldCalculatorFastMarching3D;
import org.vadere.simulator.models.potential.solver.timecost.TimeCost2D1ConfigurationSpace;

/**
 * Provides the potential values in a configuration space based on 2D geometry
 * and 1D rotation.
 * 
 * This class was part of a test whether rigid objects can be moved through a
 * difficult geometry. Based on Sethian-1999, p180ff. The test was successful.
 * 
 * 
 */
public class ConfigurationSpacePotentialProvider {

	private static final double cellSize = 0.1;
	private static final double cellSizeAngle = Math.PI * 1.0 / 16;
	private PotentialFieldCalculatorFastMarching3D FMsolver;
	private double[][][] potential;
	private CellStateFD[][][] elements;
	private List<Vector3D> targetPointList;
	private int gridsize;
	private double width;
	private double height;
	private int gridsizeAngle;

	public ConfigurationSpacePotentialProvider(Geometry geometry,
			Shape floorshape, List<VShape> targetShapes, VPolygon shape) {
		TimeCost2D1ConfigurationSpace timecostcs = new TimeCost2D1ConfigurationSpace(
				geometry, floorshape, shape, cellSize, cellSizeAngle);

		FMsolver = new PotentialFieldCalculatorFastMarching3D(timecostcs);

		width = floorshape.getBounds2D().getWidth();
		height = floorshape.getBounds2D().getHeight();

		gridsize = (int) Math.ceil(Math.max(width, height) / cellSize);
		gridsizeAngle = (int) Math.ceil(Math.PI * 1.0 / cellSizeAngle);

		elements = new CellStateFD[gridsize][gridsize][gridsizeAngle];
		potential = new double[gridsize][gridsize][gridsizeAngle];
		targetPointList = new LinkedList<Vector3D>();

		for (int r = 0; r < gridsize; r++) {
			for (int c = 0; c < gridsize; c++) {
				// save targets in the elements array
				for (VShape targetShape : targetShapes) {
					if (targetShape.contains(new VPoint(c * cellSize, r
							* cellSize))) {
						for (int iPhi = 0; iPhi < gridsizeAngle; iPhi++) {
							elements[r][c][iPhi] = CellStateFD.TARGET;
							targetPointList.add(new Vector3D(r, c, iPhi));
						}
					} else {
						for (int iPhi = 0; iPhi < gridsizeAngle; iPhi++) {
							elements[r][c][iPhi] = CellStateFD.EMPTY;
						}
					}
				}
			}
		}
	}

	public double[][][] compute() {
		potential = FMsolver.recalculate(potential, elements, targetPointList);
		return potential;
	}

}
