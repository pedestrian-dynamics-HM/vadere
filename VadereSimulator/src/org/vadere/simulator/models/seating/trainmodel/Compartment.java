package org.vadere.simulator.models.seating.trainmodel;

import java.util.ArrayList;
import java.util.List;

import org.vadere.state.scenario.Target;

public class Compartment {

	private TrainModel trainModel;
	private int index;

	public Compartment(TrainModel trainModel, int index) {
		this.trainModel = trainModel;
		this.index = index;
	}
	
	public List<SeatGroup> getSeatGroups() {
		List<SeatGroup> result = new ArrayList<>(4);
		final int startSeatGroupIndex = index * 4;
		result.add(trainModel.getSeatGroups().get(startSeatGroupIndex));
		result.add(trainModel.getSeatGroups().get(startSeatGroupIndex + 1));
		result.add(trainModel.getSeatGroups().get(startSeatGroupIndex + 2));
		result.add(trainModel.getSeatGroups().get(startSeatGroupIndex + 3));
		return result;
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
