package org.vadere.simulator.models.osm.optimization;

import java.awt.Shape;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.apache.commons.math.util.MathUtils;
import org.vadere.simulator.models.osm.PedestrianOSM;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VPoint;

/**
 * The Class StepCircleOptimizerEvolStrat.
 * 
 */
public class StepCircleOptimizerEvolStrat extends StepCircleOptimizer {

	private final Random random;
	private final double startrandom;
	private final double threshold;

	/**
	 * Instantiates a new evolution strategy
	 */
	public StepCircleOptimizerEvolStrat() {

		// this.potentialEvaluationFunction = new HimmelblauFunction( pedestrian
		// );
		this.threshold = 100.0 * MathUtils.EPSILON;
		this.random = new Random();
		startrandom = random.nextGaussian();
	}

	@Override
	public VPoint getNextPosition(PedestrianOSM ped, Shape reachableArea) {

		double stepSize = ((VCircle) reachableArea).getRadius();
		List<VPoint> positions = StepCircleOptimizerDiscrete.getReachablePositions(ped, (VCircle)reachableArea, random);
		int numberOfParents = positions.size();
		int numberOfChildren = numberOfParents * 7;

		PotentialEvaluationFunction potentialEvaluationFunction = new PotentialEvaluationFunction(
				ped);
		potentialEvaluationFunction.setStepSize(stepSize);

		List<EvolStratIndividual> parents = new ArrayList<EvolStratIndividual>();

		for (int i = 0; i < numberOfParents; i++) {
			EvolStratIndividual indiv = new EvolStratIndividual(
					positions.get(i));
			try {
				indiv.setFitness(potentialEvaluationFunction.getPotential(indiv
						.getPosition()));
			} catch (Exception e) {
				System.out.println(e);
			}
			parents.add(indiv);
		}

		int index;
		List<EvolStratIndividual> children;
		EvolStratIndividual child;
		boolean converged = false;
		double meanFitness, fitness;
		int iter = 0;

		while (!converged) {
			iter++;
			children = new ArrayList<EvolStratIndividual>();

			for (int i = 0; i < numberOfChildren; i++) {
				index = random.nextInt(numberOfParents);

				child = mutate(parents.get(index), potentialEvaluationFunction);

				children.add(child);
			}
			Collections.sort(children);

			parents = children.subList(0, numberOfParents);

			meanFitness = 0;
			fitness = 0;
			for (int i = 0; i < numberOfParents; i++) {
				meanFitness += parents.get(i).getFitness();
			}
			meanFitness /= numberOfParents;
			for (int i = 0; i < numberOfParents; i++) {
				fitness += Math.pow(parents.get(i).getFitness() - meanFitness,
						2);
			}
			fitness /= numberOfParents;
			if (threshold > fitness || iter > 1000) {
				converged = true;
			}
		}

		return parents.get(0).getPosition();
	}

	/**
	 * Mutate the individual.
	 */
	private EvolStratIndividual mutate(EvolStratIndividual indiv,
			PotentialEvaluationFunction potentialEvaluationFunction) {
		EvolStratIndividual newIndiv = new EvolStratIndividual(indiv);
		double x = indiv.getPosition().x + random.nextGaussian()
				* indiv.getSigma().x * random.nextInt(2);
		double y = indiv.getPosition().y + random.nextGaussian()
				* indiv.getSigma().y * random.nextInt(2);
		VPoint mutation = new VPoint(x, y);
		newIndiv.setPosition(mutation);
		try {
			newIndiv.setFitness(potentialEvaluationFunction
					.getPotential(mutation));
		} catch (Exception e) {
			newIndiv.setFitness(100000);
		}
		x = indiv.getSigma().x
				* Math.exp(0.1 * startrandom + 0.2 * random.nextGaussian());
		y = indiv.getSigma().y
				* Math.exp(0.1 * startrandom + 0.2 * random.nextGaussian());
		VPoint mutationsigma = new VPoint(x, y);
		newIndiv.setSigma(mutationsigma);
		return newIndiv;
	}

	public StepCircleOptimizer clone() {
		return new StepCircleOptimizerEvolStrat();
	}

}
