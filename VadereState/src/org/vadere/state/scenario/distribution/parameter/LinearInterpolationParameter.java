package org.vadere.state.scenario.distribution.parameter;

/**
 * @author Lukas Gradl (lgradl@hm.edu)
 */

public class LinearInterpolationParameter {

	double spawnFrequency;
	double[] xValues;
	double[] yValues;


	public double getSpawnFrequency() {
		return spawnFrequency;
	}

	public void setSpawnFrequency(double spawnFrequency) {
		this.spawnFrequency = spawnFrequency;
	}


	public double[] getxValues() {
		return xValues;
	}

	public void setxValues(double[] xValues) {
		this.xValues = xValues;
	}


	public double[] getyValues() {
		return yValues;
	}

	public void setyValues(double[] yValues) {
		this.yValues = yValues;
	}


}
