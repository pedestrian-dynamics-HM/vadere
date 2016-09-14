package org.vadere.state.attributes.models;

import java.util.List;

import org.apache.commons.math3.util.Pair;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.models.seating.SeatFacingDirection;
import org.vadere.state.attributes.models.seating.SeatRelativePosition;
import org.vadere.state.attributes.models.seating.SeatSide;
import org.vadere.state.scenario.TrainGeometry;

/**
 * Parameters for the seating model.
 *
 */
public class AttributesSeating extends Attributes {

	/** The train geometry class name used to generate the scenario with Traingen. */
	private String trainGeometry = TrainGeometry.class.getName();
	
	/**
	 * Choices with probabilities for the seat group. <code>true</code> is
	 * choosing a seat group with the least number of other pessengers.
	 */
	private List<Pair<Boolean, Double>> seatGroupChoice;

	/** Probabilities for seat indexes 0 to 3. */
	private double[] seatChoice0;

	private List<Pair<SeatRelativePosition, Double>> seatChoice1;

	private List<Pair<SeatSide, Double>> seatChoice2Side;

	private List<Pair<SeatFacingDirection, Double>> seatChoice2FacingDirection;

	public String getTrainGeometry() {
		return trainGeometry;
	}

	public List<Pair<Boolean, Double>> getSeatGroupChoice() {
		return seatGroupChoice;
	}

	public double[] getSeatChoice0() {
		return seatChoice0; // TODO fix names
	}

	public List<Pair<SeatRelativePosition, Double>> getSeatChoice1() {
		return seatChoice1; // TODO fix name
	}

	public List<Pair<SeatSide, Double>> getSeatChoice2Side() {
		return seatChoice2Side; // TODO fix name
	}

	public List<Pair<SeatFacingDirection, Double>> getSeatChoice2FacingDirection() {
		return seatChoice2FacingDirection; // TODO fix name
	}

}
