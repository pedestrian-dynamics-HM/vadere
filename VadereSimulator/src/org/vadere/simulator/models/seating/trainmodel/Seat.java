package org.vadere.simulator.models.seating.trainmodel;

import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Target;

public class Seat {

	private Target associatedTarget;
	private Pedestrian sittingPerson;

	// TODO is this used?
	private Integer sittingPersonId;

	public Seat(Target associatedTarget) {
		this.associatedTarget = associatedTarget;
	}

	public Pedestrian getSittingPerson() {
		return sittingPerson;
	}

	public void setSittingPerson(Pedestrian sittingPerson) {
		this.sittingPerson = sittingPerson;
	}

	public void setSittingPersonId(Integer sittingPersonId) {
		this.sittingPersonId = sittingPersonId;
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
}
