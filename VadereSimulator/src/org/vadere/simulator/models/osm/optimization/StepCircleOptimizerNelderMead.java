package org.vadere.simulator.models.osm.optimization;

import org.apache.commons.math.ConvergenceException;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.optimization.GoalType;
import org.apache.commons.math.optimization.MultivariateRealOptimizer;
import org.apache.commons.math.optimization.direct.DirectSearchOptimizer;
import org.apache.commons.math.optimization.direct.NelderMead;
import org.vadere.simulator.models.osm.PedestrianOSM;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.logging.Logger;

import java.awt.*;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;

/**
 * The Class StepCircleOptimizerNelderMead.
 *
 */
public class StepCircleOptimizerNelderMead implements StepCircleOptimizer {

	private static Logger logger = Logger
			.getLogger(StepCircleOptimizerNelderMead.class);

	private final Random random;
	private Map<PedestrianOSM, VPoint> lastSolution;

	public StepCircleOptimizerNelderMead(Random random) {
		this.random = random;
		this.lastSolution = new HashMap<>();
	}

	public static  FileWriter fileWriter;
	public static int artificialDebugStopForReadOnlyNelderMead = 0;
	public static int pointCounter;
	// set break point with condition in NelderMead StepCircleOptimizerNelderMead.artificialDebugStopForReadOnlyNelderMead == 1

	private int pedestrianNrToBeAnalyzed = 10;
	private double TimeToBeAnalyzed = 43.787595405756356;

	@Override
	//public VPoint getNextPosition(PedestrianOSM pedestrian, Shape reachableArea) throws IOException {
	public VPoint getNextPosition(PedestrianOSM pedestrian, Shape reachableArea) {

		if ( (Math.abs(pedestrian.getTimeOfNextStep() - TimeToBeAnalyzed)  < 0.0001) && (pedestrian.getId() == pedestrianNrToBeAnalyzed) )
		{
			System.out.println("Artificial Stop for Debugger");
			artificialDebugStopForReadOnlyNelderMead = 1;

			try {
				fileWriter = new FileWriter("C:\\temp\\log.txt");
				fileWriter.write("Time :");
				fileWriter.write(String.valueOf(pedestrian.getTimeOfNextStep()));
				fileWriter.write("[s], Pedestrian Nr. ");
				fileWriter.write(String.valueOf(pedestrian.getId()));
				fileWriter.write("\r\n");
				fileWriter.write("Point-Nr.; Simplex best x-coor.; Simplex best y-coor.; Simplex 2best x-coor.; Simplex 2best y-coor.;Simplex worst x-coor.; Simplex worst y-coor.\r\n");


			} catch (IOException e) {
				e.printStackTrace();
			}
		}



		double stepSize = ((VCircle) reachableArea).getRadius();
		List<VPoint> positions = StepCircleOptimizerDiscrete.getReachablePositions(pedestrian, (VCircle)reachableArea, random);
		PotentialEvaluationFunction potentialEvaluationFunction = new PotentialEvaluationFunction(pedestrian);
		potentialEvaluationFunction.setStepSize(stepSize);

		double[] position = potentialEvaluationFunction.pointToArray(pedestrian.getPosition());
		double[] newPosition = new double[2];
		double[] minimum = position;
		double[] newMinimum = {0, 0};
		double minimumValue = pedestrian.getPotential(pedestrian.getPosition());
		double newMinimumValue = 0;
		double step = stepSize / 2;
		double threshold = 0.0001;

		logger.info("Time " + pedestrian.getTimeOfNextStep() + " Pedestrian Nr." + pedestrian.getId() );
		NelderMead optimizer = new DebugNelderMead();

		if ( (Math.abs(pedestrian.getTimeOfNextStep() - TimeToBeAnalyzed)  < 0.0001) && (pedestrian.getId() == pedestrianNrToBeAnalyzed) ) {

			// create circle and get potential
			List<VPoint> vpoints = new ArrayList<VPoint>();

			double xStart = position[0] - stepSize;
			double yStart = position[1] - stepSize;
			int numberGridDivision = 10;
			double resolutionGrid = stepSize / numberGridDivision;
			VPoint currentPosition = new VPoint(position[0], position[1]);

			for (int row = 0; row < numberGridDivision; ++row) {
				for (int col = 0; col < numberGridDivision; ++col) {
					VPoint vpoint = new VPoint(xStart + row * resolutionGrid, yStart + col * resolutionGrid);

					if (currentPosition.distance(vpoint) < stepSize) {
						logger.info(vpoint.toString());
						vpoints.add(vpoint);

					}
				}
			}
		}


		try {

			/*if(lastSolution.containsKey(pedestrian)) {
				VPoint optimum = lastSolution.get(pedestrian).add(pedestrian.getPosition());
				if(isLocalMinimum(potentialEvaluationFunction, (VCircle) reachableArea, optimum)) {
					logger.info("quick solution found.");
					return optimum;
				}
			}*/

			//minimum = position;

			pointCounter = 1;

			System.out.println(" ------ Start Initial Run ------ "  );

			double[][] simplex = new double[][] {{0, 0}, {step, step}, {step, -step}};

			optimizer.setStartConfiguration(simplex);
			optimizer.setConvergenceChecker(new NelderMeadConvergenceChecker());
			logger.info("Old Position : (" + position[0] + position[1] + ") with potential: " + minimumValue );

			newMinimum = optimizer.optimize(potentialEvaluationFunction, GoalType.MINIMIZE, position).getPoint();
			//logger.info("["+0+","+0+"],["+step+","+step+"],["+step+","+(-step)+")]");

			logger.info("Number of Evaluations : " + potentialEvaluationFunction.counter);

			newMinimumValue = potentialEvaluationFunction.value(newMinimum);
			logger.info("New Minimum at position : (" + newMinimum[0] + newMinimum[1] + ") with potential " + newMinimumValue );



			int counter = 0;

			if ((minimumValue > newMinimumValue && Math.abs(minimumValue - newMinimumValue) > threshold)) {
				minimumValue = newMinimumValue;
				minimum = newMinimum;
			}

			int bound = positions.size();



			while (counter < bound) {

				pointCounter++;


				//logger.info("  #######  Reachable Position Nr. " + counter + " with (x,y) " + positions.get(counter).toString() + "#########");


				newPosition[0] = positions.get(counter).getX();
				newPosition[1] = positions.get(counter).getY();

				int anotherPoint;
				if (counter == bound - 1) {
					anotherPoint = 0;
				} else {
					anotherPoint = counter + 1;
				}

				double innerDistance = pedestrian.getPosition().distance(
						(positions.get(counter)));
				VPoint innerDirection = pedestrian.getPosition()
						.subtract((positions.get(counter)))
						.scalarMultiply(1.0 / innerDistance);
				double outerDistance = positions.get(anotherPoint).distance(
						(positions.get(counter)));
				VPoint outerDirection = positions.get(anotherPoint)
						.subtract((positions.get(counter)))
						.scalarMultiply(1.0 / outerDistance);

				simplex[1][0] = Math.min(step, innerDistance) * innerDirection.getX();
				simplex[1][1] = Math.min(step, innerDistance) * innerDirection.getY();
				simplex[2][0] = Math.min(step, outerDistance) * outerDirection.getX();
				simplex[2][1] = Math.min(step, outerDistance) * outerDirection.getY();

				if(artificialDebugStopForReadOnlyNelderMead==1) {
					logger.info("Simplex with Vertices (xi,yi): [" + simplex[0][0] + "," + simplex[0][1] + "],[" + simplex[1][0] + "," + simplex[1][1] + "],[" + simplex[2][0] + "," + simplex[2][1] + ")]");
				}

				optimizer.setStartConfiguration(simplex);

				optimizer.setConvergenceChecker(new NelderMeadConvergenceChecker());
				newMinimum = optimizer.optimize(potentialEvaluationFunction,
						GoalType.MINIMIZE, newPosition).getPoint();
				newMinimumValue = potentialEvaluationFunction.value(newMinimum);

				if(artificialDebugStopForReadOnlyNelderMead==1) {
				logger.info("Number of Evaluations : " + potentialEvaluationFunction.counter); }


				if ((minimumValue > newMinimumValue && Math.abs(minimumValue - newMinimumValue) > threshold)) {
					minimumValue = newMinimumValue;
					minimum = newMinimum;
					//logger.info("new min: ["+minimum[0]+","+minimum[1]+"]");
					//idPoint = counter;
				}

				counter++;

			}

		} catch (ConvergenceException | FunctionEvaluationException e) {
			logger.error(e);
		}
		//System.out.println(potentialEvaluationFunction.counter);
		//logger.info("["+(minimum[0]-pedestrian.getPosition().getX())+","+(minimum[1]-pedestrian.getPosition().getY())+"]");
		//lastSolution.put(pedestrian, new VPoint(minimum[0]-pedestrian.getPosition().getX(), minimum[1]-pedestrian.getPosition().getY()));

		//System.out.println(new VPoint(minimum[0], minimum[1]).toString());
		if(artificialDebugStopForReadOnlyNelderMead==1){
		System.out.println(" ------ List of reachable Points ------ "  );
		}


		int i, pointIdOptimalPointFromList = -1;
		for (i = 0; i < positions.size(); ++i )
		{
			if(artificialDebugStopForReadOnlyNelderMead==1) {
				logger.info(" Reachable Point Nr. " + i + " (x,y) : (" + positions.get(i).getX() + +positions.get(i).getY() + ")");
			}

			if (  Math.max( Math.abs(positions.get(i).getX() - minimum[0]), Math.abs(positions.get(i).getY() - minimum[1]) ) < GeometryUtils.DOUBLE_EPS  )
			{
				pointIdOptimalPointFromList = i;

			}
		}

		logger.info("Point Id " + pointIdOptimalPointFromList +"\n");


		if (artificialDebugStopForReadOnlyNelderMead==1)
		{
			try {
				fileWriter.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			artificialDebugStopForReadOnlyNelderMead = 0;
		}



		return new VPoint(minimum[0], minimum[1]);

	}

	public StepCircleOptimizer clone() {
		return new StepCircleOptimizerNelderMead(random);
	}

	private boolean isLocalMinimum(PotentialEvaluationFunction evaluationFunction, VCircle stepDisc, VPoint optimum) throws FunctionEvaluationException {
		double delta = 0.0001;
		double angle = 0.05 * 2 * Math.PI;
		double value = evaluationFunction.getValue(optimum);

		for(double angleDelta = 0; angleDelta <= 2 * Math.PI; angleDelta += angle) {
			VPoint newPoint = optimum.add(new VPoint(delta, 0).rotate(angleDelta));
			if(stepDisc.contains(newPoint) && evaluationFunction.getValue(newPoint) < value) {
				return false;
			}
		}
		return evaluationFunction.getValue(stepDisc.getCenter()) > value;
	}
}
