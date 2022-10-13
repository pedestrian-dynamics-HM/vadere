package org.vadere.state.attributes.distributions;

import java.util.ArrayList;
import java.util.List;

/**
 * This is the parameter structure used with an empirical distribution.
 * @author Lukas Gradl (lgradl@hm.edu), Ludwig Jaeck
 */

public class AttributesLinearInterpolationDistribution extends AttributesDistribution {

	Double spawnFrequency = 0.0;
	List<Double> xValues = new ArrayList<>();
	List<Double> yValues = new ArrayList<>();


	public double getSpawnFrequency() {
		return spawnFrequency;
	}

	public void setSpawnFrequency(double spawnFrequency) {
		this.spawnFrequency = spawnFrequency;
	}


	public List<Double> getxValues() {
		return xValues;
	}

	public void setxValues(List<Double> xValues) {
		this.xValues = xValues;
	}


	public List<Double> getyValues() {
		return yValues;
	}

	public void setyValues(List<Double> yValues) {
		this.yValues = yValues;
	}


}
