package org.vadere.meshing.mesh.triangulation.improver.distmesh.deprecated;


import org.vadere.meshing.mesh.triangulation.edgeLengthFunctions.IEdgeLengthFunction;
import org.vadere.meshing.mesh.triangulation.improver.distmesh.Parameters;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;

import java.util.Random;
import java.util.function.Function;

/**
 * @author Matthias Laubinger
 */
@Deprecated
public class DensityEdgeLenFunction implements IEdgeLengthFunction {

	private final Function<IPoint, Double> densityFunc;
	private double maxDensity = Double.NaN;
	private VRectangle regionBoundingBox;

	public DensityEdgeLenFunction(final Function<IPoint, Double> densityFunc, final VRectangle regionBoundingBox){
		this.densityFunc = densityFunc;
		this.regionBoundingBox = regionBoundingBox;
	}

	@Override
	public Double apply(final IPoint iPoint) {
		if(maxDensity == Double.NaN) {
			maxDensity = calculateMaxDensity();
		}
		return 1 / (Parameters.MINIMUM + (densityFunc.apply(iPoint) / maxDensity) * Parameters.DENSITYWEIGHT);
	}

	/**
	 * Compute the an approximation of the maximal density based on samples.
	 * @return an approximation of the maximal density
	 */
	private double calculateMaxDensity()
	{
		double maxDensity = 0;
		double[][] means = new double[Parameters.SAMPLEDIVISION][Parameters.SAMPLEDIVISION];
		Random random = new Random();
		for (int i = 0; i < Parameters.SAMPLENUMBER; i++)
		{
			for (int j = 0; j < Parameters.NPOINTS; j++) {
				double x = random.nextInt((int) (regionBoundingBox.getMaxX() - regionBoundingBox.getMinX()) + 1);
				double y = random.nextInt((int) (regionBoundingBox.getMaxY() - regionBoundingBox.getMinY()) + 1);
				int xi = (int)Math.floor(x/(regionBoundingBox.getMaxX()-regionBoundingBox.getMinX())*(Parameters.SAMPLEDIVISION-1));
				int yi = (int)Math.floor(y/(regionBoundingBox.getMaxY()-regionBoundingBox.getMinY())*(Parameters.SAMPLEDIVISION-1));
				means[yi][xi] = (means[yi][xi] + densityFunc.apply(new VPoint(x, y)))/2;
				if(maxDensity < means[yi][xi])
					maxDensity = means[yi][xi];
			}
		}
		return maxDensity;
	}
}
