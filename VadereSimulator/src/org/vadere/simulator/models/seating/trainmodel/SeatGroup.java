package org.vadere.simulator.models.seating.trainmodel;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.vadere.state.attributes.models.seating.SeatFacingDirection;
import org.vadere.state.attributes.models.seating.SeatRelativePosition;
import org.vadere.state.attributes.models.seating.SeatSide;

public class SeatGroup {

	private TrainModel trainModel;
	private List<Seat> seats;
	private int index;

	public SeatGroup(TrainModel trainModel, List<Seat> seats, int index) {
		this.trainModel = trainModel;
		this.seats = seats;
		this.index = index;
	}

	public SeatGroup getNeighborSeatGroup() {
		int neighborIndex;
		if (isInLeftRow()) {
			neighborIndex = index + 1;
		} else {
			neighborIndex = index - 1;
		}
		return trainModel.getSeatGroups().get(neighborIndex);
	}

	private boolean isInLeftRow() {
		return index % 2 == 0; // even -> left row
	}

	public Seat getSeat(int index) {
		return seats.get(calculateOverallIndex(index));
	}

	public Seat setSeat(int index, Seat seat) {
		return seats.set(calculateOverallIndex(index), seat);
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

	public Seat getTheOccupiedSeat() {
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

	public Seat seatRelativeTo(Seat theOccupiedSeat, SeatRelativePosition relativePosition) {
		for (int i = 0; i < 4; i++) {
			if (getSeat(i) == theOccupiedSeat) {
				switch (relativePosition) {
				case NEXT:
					return getSeatNextTo(i);
				case ACROSS:
					return getSeatAccrossFrom(i);
				case DIAGONAL:
					return getSeatDiagonallyOppositeTo(i);
				}
			}
		}
		throw new IllegalArgumentException("Supplied occupied seat is not part of this seat group.");
	}

	private Seat getSeatDiagonallyOppositeTo(int i) {
		return getSeat(3 - i);
	}

	private Seat getSeatAccrossFrom(int i) {
		return getSeat((i + 2) % 4);
	}

	private Seat getSeatNextTo(int i) {
		final int[] diagonalOpposites = { 1, 0, 3, 2 };
		return getSeat(diagonalOpposites[i]);
	}

	public Seat availableSeatAtSide(SeatSide side) {
		int[] indexes;

		if (isInLeftRow()) {
			if (side == SeatSide.WINDOW) {
				indexes = new int[] {0, 2};
			} else {
				indexes = new int[] {1, 3};
			}
		} else {
			if (side == SeatSide.WINDOW) {
				indexes = new int[] {1, 3};
			} else {
				indexes = new int[] {0, 2};
			}
		}

		for (int i : indexes) {
			if (isSeatAvailable(i)) {
				return getSeat(i);
			}
		}
		throw new IllegalStateException("This method must only be called when there is a seat available at side " + side);
	}

	public Seat availableSeatAtFacingDirection(SeatFacingDirection facingDirection) {
		int[] indexes;

		if (facingDirection == SeatFacingDirection.FORWARD) {
			indexes = new int[] {2, 3};
		} else {
			indexes = new int[] {0, 1};
		}

		for (int i : indexes) {
			if (isSeatAvailable(i)) {
				return getSeat(i);
			}
		}
		throw new IllegalStateException("This method must only be called when there is a seat available with facing direction" + facingDirection);
	}

}
