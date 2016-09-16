package org.vadere.simulator.models.seating.trainmodel;

import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Target;

public class Seat {

	private Target associatedTarget;
	private Pedestrian sittingPerson;

	public Seat(Target associatedTarget) {
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
}
