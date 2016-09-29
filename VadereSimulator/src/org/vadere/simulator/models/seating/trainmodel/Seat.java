package org.vadere.simulator.models.seating.trainmodel;

import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Target;

public class Seat {

	private SeatGroup seatGroup;
	private int seatNumberWithinCompartment;
	private Target associatedTarget;
	private Pedestrian sittingPerson;

	public Seat(SeatGroup seatGroup, Target associatedTarget, int seatNumberWithinCompartment) {
		this.seatGroup = seatGroup;
		this.seatNumberWithinCompartment = seatNumberWithinCompartment;
		this.associatedTarget = associatedTarget;
	}

	public Pedestrian getSittingPerson() {
		return sittingPerson;
	}

	public void setSittingPerson(Pedestrian sittingPerson) {
		this.sittingPerson = sittingPerson;
	}

	public Target getAssociatedTarget() {
		return associatedTarget;
	}
	
	public boolean isOccupied() {
		return sittingPerson != null;
	}
	
	public boolean isAvailable() {
		return !isOccupied();
	}

	public int getSeatNumberWithinCompartment() {
		return seatNumberWithinCompartment;
	}

	public SeatGroup getSeatGroup() {
		return seatGroup;
	}

}
