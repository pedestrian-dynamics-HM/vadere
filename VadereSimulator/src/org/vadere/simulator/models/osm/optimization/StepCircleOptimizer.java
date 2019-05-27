package org.vadere.simulator.models.osm.optimization;

import java.awt.Shape;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.commons.math.FunctionEvaluationException;
import org.vadere.simulator.models.osm.PedestrianOSM;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.logging.Logger;

/**
 * Abstract Base Class for StepCircleOptimizer.
 *
 * The abstract functions need to be implemented by every StepCircleOptimizer.
 * The additional functions only serve to compute a true solution (obtained via computationally expensive brute force).
 * This allows to compute a metric that measures the quality of the respective concrete subclass of StepCircleOptimizer.
 *
 * Currently, only the StepCircleOptimizerNelderMead uses the metric functionality. To compute the brute force
 * solution only the "computeAndAddBruteForceSolutionMetric" function has to be called (NOTE: depending on the setting
 * in "getReachablePositions" this can be very expensive.
 */
public abstract class StepCircleOptimizer {

	private boolean computeMetric;
    private ArrayList<OptimizationMetric> currentMetricValues;

	protected StepCircleOptimizer(){
		// TODO: read if the metric should be computed from a config file, see issue #243
		this.computeMetric = true;

		if(this.computeMetric){
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

	protected class SolutionPair {
		/* Inner data class to store point and function value. */
		public final VPoint point;
		public final double funcValue;
		public SolutionPair(VPoint point, double funcValue){
			this.point = point;
			this.funcValue = funcValue;
		}
	}

	protected boolean getIsComputeMetric(){
		return computeMetric;
	}

	private SolutionPair bruteForceOptimalValue(final PedestrianOSM pedestrian){
        //TODO move this function (rename!) to StepCircleOptimizerDiscrete
		var reachableArea = new VCircle(pedestrian.getPosition(), pedestrian.getFreeFlowStepSize());
		var potentialEvaluationFunction = new PotentialEvaluationFunction(pedestrian);
		potentialEvaluationFunction.setStepSize(reachableArea.getRadius());

		var bruteForceMethod = new StepCircleOptimizerDiscrete(0, new Random());

		//VPoint optimalPoint = bruteForceMethod.getNextPosition(pedestrian, getReachablePositions2(pedestrian, reachableArea, pedestrian.getPosition()),
		VPoint optimalPoint = bruteForceMethod.getNextPosition(pedestrian, getReachablePositions(reachableArea),
				reachableArea.getRadius(), true);

		double optimalFuncValue;  // -1 = invalid number
		try{
			optimalFuncValue = potentialEvaluationFunction.getValue(optimalPoint);
		}catch (Exception e) {
			Logger.getLogger(StepCircleOptimizerDiscrete.class).error("Potential evaluation for computing the brute " +
					"force solution threw error. Setting value to invalid (-1).");
			optimalFuncValue = -1;
		}

		System.out.println("BF: " + optimalPoint.toString() + "minimum value " + optimalFuncValue);

        return new SolutionPair(optimalPoint, optimalFuncValue);
	}

	protected void computeAndAddBruteForceSolutionMetric(final PedestrianOSM pedestrian,
														 final SolutionPair foundSolution){

        var bruteForceSolution = bruteForceOptimalValue(pedestrian);

        // TODO: maybe time of nextStep is not the actual correct one, possibly adapt
        var optimizationMetric = new OptimizationMetric(pedestrian.getId(), pedestrian.getTimeOfNextStep(),
                bruteForceSolution.point, bruteForceSolution.funcValue, foundSolution.point, foundSolution.funcValue);

        currentMetricValues.add(optimizationMetric);
    }

	public ArrayList<OptimizationMetric> getCurrentMetricValues(){
		return this.currentMetricValues;
	}

	public void clearMetricValues(){
		this.currentMetricValues = new ArrayList<>();
	}

	private static List<VPoint> getReachablePositions(VCircle reachableArea){
		// NOTE: numberPointsOfLargestCircle and numberOfCircles are parameters with a trade off between runtime and
		// precision of brute force solution

        //TODO move this function (rename!) to StepCircleOptimizerDiscrete

		return GeometryUtils.getDiscDiscretizationPoints(
				null,
				false,
				reachableArea,
				1000,
				10000,
				0,
				2.0 * Math.PI);
	}

	private static List<VPoint> getReachablePositions2(PedestrianOSM pedestrianOSM, VCircle reachableArea, VPoint centerPoint) {

		final int nrLines = 2000;  // both have to be larger than 2
		final int nrPointsPerLine = 2000;

		final double intervalXDirection = 2 * reachableArea.getRadius() / (nrPointsPerLine - 1);
		final double intervalYDirection = 2 * reachableArea.getRadius() / (nrLines - 1);

		var potentialEvaluationFunction = new PotentialEvaluationFunction(pedestrianOSM);
		potentialEvaluationFunction.setStepSize(reachableArea.getRadius());

		VPoint refPoint = new VPoint(centerPoint.x - reachableArea.getRadius(), centerPoint.y + reachableArea.getRadius());
		//VPoint endLine = new VPoint(position.x + radius, position.y + radius);

		VPoint currentBestPoint = pedestrianOSM.getPosition();
		double currentBest = Double.MAX_VALUE;


		List<VPoint> returnList = new ArrayList<>();
		try{
			for (int i = 0; i < nrLines; ++i) {
				for (int j = 0; j < nrPointsPerLine; ++j) {
					VPoint currentPoint = new VPoint(refPoint.x + intervalXDirection * j, refPoint.y);
					if(currentPoint.distance(centerPoint) <= reachableArea.getRadius()){
						returnList.add(currentPoint.clone());
						double currentEval = potentialEvaluationFunction.getPotential(currentPoint);
						if(currentEval <= currentBest){
							currentBest = currentEval;
							currentBestPoint = currentPoint.clone();
						}
					}
				}
				refPoint.y -= intervalYDirection;
			}
		}catch(FunctionEvaluationException e){
			e.printStackTrace();
		}
		System.out.println("BF2:" + currentBestPoint.toString() + " value = " + currentBest);
		return returnList;
	}


}
