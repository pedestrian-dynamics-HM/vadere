package org.vadere.simulator.models.seating.trainmodel;

import java.util.ArrayList;
import java.util.List;

import org.vadere.state.scenario.Target;

/**
 * The first and the last compartments are half-compartments.
 */
public class Compartment {

	private TrainModel trainModel;
	private int index;
	private List<SeatGroup> seatGroups;

	public Compartment(TrainModel trainModel, int index) {
		this.trainModel = trainModel;
		this.index = index;

		seatGroups = new ArrayList<>(4);
		if (index == 0) {
			addFirstHalfCompartment();
		} else if (index == trainModel.getNumberOfEntranceAreas()) {
			addLastHalfCompartment();
		} else {
			addNormalCompartment();
		}
	}

	private void addNormalCompartment() {
		final int startSeatGroupIndex = index * 4 - 2;
		seatGroups.add(trainModel.getSeatGroup(startSeatGroupIndex));
		seatGroups.add(trainModel.getSeatGroup(startSeatGroupIndex + 1));
		seatGroups.add(trainModel.getSeatGroup(startSeatGroupIndex + 2));
		seatGroups.add(trainModel.getSeatGroup(startSeatGroupIndex + 3));
	}
	
	private void addFirstHalfCompartment() {
		seatGroups.add(null);
		seatGroups.add(null);
		seatGroups.add(trainModel.getSeatGroup(0));
		seatGroups.add(trainModel.getSeatGroup(1));
	}

	private void addLastHalfCompartment() {
		final int n = trainModel.getSeatGroups().size();
		seatGroups.add(trainModel.getSeatGroup(n - 2));
		seatGroups.add(trainModel.getSeatGroup(n - 1));
		seatGroups.add(null);
		seatGroups.add(null);
	}

	/**
	 * For half-compartments, the first and the last seat groups respectively are set to null.
	 */
	public List<SeatGroup> getSeatGroups() {
		return seatGroups;
	}

	public Target getInterimTargetCloserTo(int entranceAreaIndex) {
		// entrance areas:    0   1   2   3
		// compartments:    0   1   2   3   4
		// interim targets:0 1 2 3 4 5 6 7 8 9
		final int interimTargetStartIndex = index * 2;
		if (index <= entranceAreaIndex) {
			// use interim target with higher number
			return trainModel.getInterimDestinations().get(interimTargetStartIndex + 1);
		} else {
			// use interim target with smaller number
			return trainModel.getInterimDestinations().get(interimTargetStartIndex);
		}
	}

}
