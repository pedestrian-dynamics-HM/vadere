package org.vadere.util.math.optimization.pso;

/**
 * @author Benedikt Zoennchen
 */
public class AttributesPSO {
	final int numberOfInformedParticles = 3;
	final int swarmSize = 30;
	final int minIteration = 4;
	final int maxNoUpdate = 5;
	final int maxIteration = 15;
	final int problemDimension = 2;
	final double c1 = 2.0;
	final double c2 = 2.0;
	final double wUpperBound = 1.0;
	final double wLowerBound = 0.0;
}
