package org.vadere.simulator.projects;

import java.util.Collection;

import org.vadere.simulator.projects.dataprocessing.TimeStep;
import org.vadere.simulator.projects.dataprocessing.TimeStepData;
import org.vadere.state.scenario.Topography;

/**
 * A {@link TimeStep} that stores the {@link Topography} of a scenario.
 * 
 * 
 */
public class TimeStepTopography extends TimeStep {
	private Topography topography;

	public TimeStepTopography(Collection<? extends TimeStepData> kinematics,
			double time, int step) {
		super(kinematics, time, step);
	}

	public TimeStepTopography(Topography topography) {
		super(null, 0, 1);

		this.topography = topography;
	}

	public Topography getTopography() {
		return topography;
	}

}
