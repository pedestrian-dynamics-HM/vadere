package org.vadere.simulator.models.seating.trainmodel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.vadere.state.attributes.models.seating.SeatFacingDirection;
import org.vadere.state.attributes.models.seating.SeatRelativePosition;
import org.vadere.state.attributes.models.seating.SeatSide;
import org.vadere.state.attributes.models.seating.model.SeatPosition;
import org.vadere.state.scenario.Target;

public class SeatGroup {

	public static final int SEATS_PER_SEAT_GROUP = 4;

	private List<Seat> seats;
	private int index;
	private Compartment compartment;

	// only for initialization
	private Map<Target, Seat> targetSeatMap;
	private List<List<Target>> longSeatRows;

	public SeatGroup(Compartment compartment, int index, List<List<Target>> longSeatRows, Map<Target, Seat> targetSeatMap) {
		this.index = index;
		this.compartment = compartment;
		this.seats = new ArrayList<>(4);
		
		this.longSeatRows = longSeatRows;
		this.targetSeatMap = targetSeatMap;
		createSeats();
	}

	private void createSeats() {
		final int compartmentIndex = compartment.getIndex();
		
		final int longRowIndex1, longRowIndex2;
		if (isAtLeftSide()) {
			longRowIndex1 = 0;
			longRowIndex2 = 1;
		} else {
			longRowIndex1 = 2;
			longRowIndex2 = 3;
		}

		final int targetIndex1;
		if (compartment.isFirstHalfCompartment())
			targetIndex1 = 0;
		else
			targetIndex1 = (compartmentIndex - 1) * 4 + (index / 2) * 2 + 2;
		
		addNewSeat(longRowIndex1, targetIndex1);
		addNewSeat(longRowIndex2, targetIndex1);
		addNewSeat(longRowIndex1, targetIndex1 + 1);
		addNewSeat(longRowIndex2, targetIndex1 + 1);

	}

	private void addNewSeat(int longRowIndex, int targetIndex) {
		final int number = TrainModel.calculateSeatNumberWithinCompartment(longRowIndex, targetIndex);
		final Target target = longSeatRows.get(longRowIndex).get(targetIndex);

		final Seat newSeat = new Seat(this, target, number);
		seats.add(newSeat);
		targetSeatMap.put(target, newSeat);
		
	}

	public Seat getSeat(int index) {
		return seats.get(index);
	}

	public Seat setSeat(int index, Seat seat) {
		return seats.set(index, seat);
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

		if (isAtLeftSide()) {
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

	public Compartment getCompartment() {
		return compartment;
	}

	public List<Seat> getSeats() {
		return Collections.unmodifiableList(seats);
	}

	public boolean isFull() {
		return getPersonCount() == SEATS_PER_SEAT_GROUP;
	}

	public Seat getSeatByPosition(SeatPosition seatPosition) {
		return seats.get(getSeatIndexByPosition(seatPosition));
	}

	private int getSeatIndexByPosition(SeatPosition seatPosition) {
		final Map<SeatPosition, Integer> seatPositionMapping = new HashMap<>();
		if (isAtLeftSide()) {
			seatPositionMapping.put(SeatPosition.WINDOW_BACKWARD, 0);
			seatPositionMapping.put(SeatPosition.AISLE_BACKWARD,  1);
			seatPositionMapping.put(SeatPosition.WINDOW_FORWARD,  2);
			seatPositionMapping.put(SeatPosition.AISLE_FORWARD,   3);
		} else {
			seatPositionMapping.put(SeatPosition.AISLE_BACKWARD,  0);
			seatPositionMapping.put(SeatPosition.WINDOW_BACKWARD, 1);
			seatPositionMapping.put(SeatPosition.AISLE_FORWARD,   2);
			seatPositionMapping.put(SeatPosition.WINDOW_FORWARD,  3);
		}
		return seatPositionMapping.get(seatPosition);
	}

	public boolean isAtLeftSide() {
		return index % 2 == 0;
	}

}
