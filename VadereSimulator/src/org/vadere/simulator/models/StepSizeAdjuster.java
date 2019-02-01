package org.vadere.simulator.models;

import org.vadere.state.scenario.Pedestrian;

public interface StepSizeAdjuster {
	double getAdjustedStepSize(Pedestrian ped, double originalStepSize);
}
