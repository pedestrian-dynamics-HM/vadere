package org.vadere.simulator.models.osm.optimization;

import java.awt.Shape;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import org.vadere.simulator.models.osm.PedestrianOSM;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.util.data.SortedList;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.logging.Logger;

/**
 * The Interface StepCircleOptimizer.
 * 
 */
public abstract class StepCircleOptimizer {

	private ArrayList<OptimizationMetric> currentMetricValues;

	protected StepCircleOptimizer(){
		// TODO: read if the metric should be computed from a config file, see issue #243
		boolean computeMetric = true;

		if(computeMetric){
			this.currentMetricValues = new ArrayList<>();
		}else{
			this.currentMetricValues = null;
		}
	}

	/** Returns the reachable position with the minimal potential. */
	public abstract VPoint getNextPosition(PedestrianOSM pedestrian, Shape reachableArea);
	public abstract StepCircleOptimizer clone();


	/** The following functions are to compute the "true" optimal value via brute force. This allows to check the
	 * quality of a optimization algorithm.
	 */

	private OptimizationMetric bruteForceOptimalValue(PedestrianOSM pedestrian){

		var reachableArea = new VCircle( pedestrian.getFreeFlowStepSize() );
		var potentialEvaluationFunction = new PotentialEvaluationFunction(pedestrian);

		var bruteForceMethod = new StepCircleOptimizerDiscrete(0, new Random());

		VPoint optimalPoint = bruteForceMethod.getNextPosition(pedestrian, getReachablePositions(reachableArea),
				reachableArea.getRadius());

		double optimalFuncValue;  // -1 = invalid number
		try{
			optimalFuncValue = potentialEvaluationFunction.getValue(optimalPoint);
		}catch (Exception e) {
			Logger.getLogger(StepCircleOptimizerDiscrete.class).error("Potential evaluation threw error. Setting value " +
					"to invalid (-1)");
			optimalFuncValue = -1;
		}

		return new OptimizationMetric(optimalPoint, optimalFuncValue);
	}

	protected void setBruteForceSolution(PedestrianOSM pedestrian){
		this.currentMetricValues.add(bruteForceOptimalValue(pedestrian));  // adds at the end of the list
	}

	protected void setFoundSolution(VPoint foundSolution, double funcValue){
		// Always add at the last, currently this is not very secure bc. there is no checking if it overwrites other
		// values
		OptimizationMetric metric = this.currentMetricValues.get(this.currentMetricValues.size()-1);
		metric.setFoundPoint(foundSolution);
		metric.setFoundFuncValue(funcValue);
	}

	public ArrayList<OptimizationMetric> getCurrentMetricValues(){
		return this.currentMetricValues;
	}

	public void resetHashMap(){
		this.currentMetricValues = new ArrayList<>();
	}

	private static List<VPoint> getReachablePositions(VCircle reachableArea){
		// TODO: numberPointsOfLargestCircle and numberOfCircles are parameters with a trade off between runtime and
		// precision of brute force solution

		return GeometryUtils.getDiscDiscretizationPoints(
				null,
				false,
				reachableArea,
				20,
				10000,
				0,
				2 * Math.PI);
	}
}
