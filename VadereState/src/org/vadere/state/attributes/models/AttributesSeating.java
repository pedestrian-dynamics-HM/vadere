package org.vadere.state.attributes.models;

import java.util.List;

import org.apache.commons.math3.util.Pair;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.scenario.TrainGeometry;

/**
 * Parameters for the seating model.
 *
 */
public class AttributesSeating extends Attributes {

	/** The train geometry used to generate the scenario with Traingen. */
	private String trainGeometryClassName = TrainGeometry.class.getName();
	
	/**
	 * Choices with probabilities for the seat group. <code>true</code> is
	 * choosing a seat group with the least number of other pessengers.
	 */
	private List<Pair<Boolean, Double>> seatGroupChoice;

	public String getTrainGeometryClassName() {
		return trainGeometryClassName;
	}

	public List<Pair<Boolean, Double>> getSeatGroupChoice() {
		return seatGroupChoice;
	}

}
