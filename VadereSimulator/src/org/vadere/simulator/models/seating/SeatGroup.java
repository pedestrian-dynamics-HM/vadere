package org.vadere.simulator.models.seating;

public class SeatGroup {

	private TrainModel trainModel;
	private int index;

	public SeatGroup(TrainModel trainModel, int index) {
		this.trainModel = trainModel;
		this.index = index;
	}

	public SeatGroup getNeighborSeatGroup() {
		int neighborIndex;
		if (index % 2 == 0) { // even -> left row
			neighborIndex = index + 1;
		} else {
			neighborIndex = index - 1;
		}
		return trainModel.getSeatGroups().get(neighborIndex);
	}

	public Seat getSeat(int index) {
		return trainModel.getSeats().get(this.index * 4 + index);
	}

	public Seat setSeat(int index, Seat seat) {
		return trainModel.getSeats().set(this.index * 4 + index, seat);
	}

}
