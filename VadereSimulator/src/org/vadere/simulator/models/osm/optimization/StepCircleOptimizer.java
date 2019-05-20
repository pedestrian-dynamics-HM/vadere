package org.vadere.simulator.models.osm.optimization;

import java.awt.Shape;

import org.vadere.simulator.models.osm.PedestrianOSM;
import org.vadere.util.geometry.shapes.VPoint;

/**
 * The Interface StepCircleOptimizer.
 * 
 */
public abstract class StepCircleOptimizer {

	private OptimizationMetric currentMetricValues;

	protected StepCircleOptimizer(){
		// TODO: read if the metric should be computed

		boolean computeMetric = true;

		if(computeMetric){
			this.currentMetricValues = new OptimizationMetric();
		}else{
			this.currentMetricValues = null;
		}
	}

	/** Returns the reachable position with the minimal potential. */
	abstract VPoint getNextPosition(PedestrianOSM pedestrian, Shape reachableArea);

	//abstract StepCircleOptimizer clone();
}
