package org.vadere.simulator.models.seating;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
		return trainModel.getSeats().get(calculateOverallIndex(index));
	}

	public Seat setSeat(int index, Seat seat) {
		return trainModel.getSeats().set(calculateOverallIndex(index), seat);
	}

	public int getIndex() {
		return index;
	}
	
	public int getPersonCount() {
		return (int) getOccupiedSeats().count();
	}

	public Seat getTheAvailableSeat() {
		checkPersonCount(3);
		return getAvailableSeats().findAny().get();
	}

	public Object getTheOccupiedSeat() {
		checkPersonCount(1);
		return getOccupiedSeats().findAny().get();
	}

	public List<Seat> getTheTwoAvailableSeats() {
		checkPersonCount(2);
		return getAvailableSeats().collect(Collectors.toList());
	}

	private Stream<Seat> getAvailableSeats() {
		return getSeatsAsStream().filter(Seat::isAvailable);
	}
	
	private Stream<Seat> getOccupiedSeats() {
		return getSeatsAsStream().filter(Seat::isOccupied);
	}

	private Stream<Seat> getSeatsAsStream() {
		List<Seat> list = new ArrayList<>(4);
		for (int i = 0; i < 4; i++) {
			list.add(getSeat(i));
		}
		return list.stream();
	}

	private int calculateOverallIndex(int index) {
		return this.index * 4 + index;
	}

	private void checkPersonCount(int count) {
		if (getPersonCount() != count) {
			throw new IllegalStateException(
					"This method must only be called on seat groups with exactly " + count + " pessengers.");
		}
	}

	public boolean onlySideChoice() {
		return isSeatAvailable(0) && isSeatAvailable(1)
				|| isSeatAvailable(2) && isSeatAvailable(3);
	}

	public boolean onlyFacingDirectionChoice() {
		return isSeatAvailable(0) && isSeatAvailable(2)
				|| isSeatAvailable(1) && isSeatAvailable(3);
	}

	private boolean isSeatAvailable(int index) {
		return getSeat(index).getSittingPerson() == null;
	}

}
