package org.vadere.simulator.models.osm.updateScheme;

import org.vadere.simulator.models.osm.PedestrianOSM;
import org.vadere.simulator.models.osm.updateScheme.UpdateSchemeOSM.CallMethod;

public class ParallelWorkerOSM implements Runnable {

	private final PedestrianOSM ped;
	private final CallMethod callMethod;
	private final double timeStepInSec;

	public ParallelWorkerOSM(CallMethod callMethod, PedestrianOSM ped,
			double timeStepInSec) {
		this.ped = ped;
		this.callMethod = callMethod;
		this.timeStepInSec = timeStepInSec;
	}

	@Override
	public void run() {
		//ped.update(timeStepInSec, -1, callMethod);
	}

}
