package org.vadere.simulator.models.seating;

import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Target;

public class Seat {

	Target associatedTarget;
	Pedestrian sittingPerson;
	Integer sittingPersonId;

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
}
