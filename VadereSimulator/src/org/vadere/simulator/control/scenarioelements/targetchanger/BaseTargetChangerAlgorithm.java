package org.vadere.simulator.control.scenarioelements.targetchanger;

import org.vadere.state.scenario.TargetChanger;
import org.vadere.state.scenario.Topography;

/**
 * Abstract TargetChangerAlgorithm holding the corresponding {@link TargetChanger}
 * and {@link Topography} to access during simulation update. This class is triggered
 * by a {@link org.vadere.simulator.control.scenarioelements.TargetChangerController}.
 */
public abstract class BaseTargetChangerAlgorithm implements TargetChangerAlgorithm {

	protected TargetChanger targetChanger;
	protected Topography topography;

	public BaseTargetChangerAlgorithm(TargetChanger targetChanger, Topography topography) {
		this.targetChanger = targetChanger;
		this.topography = topography;
	}

	protected void checkProbabilityIsNormalized(){
		for (Double probabilityToChangeTarget : targetChanger.getAttributes().getProbabilitiesToChangeTarget()){

			if (probabilityToChangeTarget < 0.0 || probabilityToChangeTarget > 1.0) {
				throw new IllegalArgumentException("Probability must be in range 0.0 to 1.0!");
			}
		}
	}

	@Override
	public TargetChanger getTargetChanger() {
		return targetChanger;
	}
}
