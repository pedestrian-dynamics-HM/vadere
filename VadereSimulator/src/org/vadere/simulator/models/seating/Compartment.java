package org.vadere.simulator.models.seating;

import java.util.ArrayList;
import java.util.List;

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

}
