package org.vadere.simulator.models.queuing;

import org.vadere.state.scenario.ModelPedestrian;
import org.vadere.state.types.PedestrianAttitudeType;

public class QueueingGamePedestrian extends ModelPedestrian {
	private PedestrianAttitudeType attituteType = PedestrianAttitudeType.COMPETITIVE;

	public PedestrianAttitudeType getAttituteType() {
		return attituteType;
	}

	public void setAttituteType(final PedestrianAttitudeType attituteType) {
		this.attituteType = attituteType;
	}
}
