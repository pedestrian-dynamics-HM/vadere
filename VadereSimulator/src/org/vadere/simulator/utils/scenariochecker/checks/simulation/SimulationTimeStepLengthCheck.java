package org.vadere.simulator.utils.scenariochecker.checks.simulation;

import org.vadere.simulator.projects.Scenario;
import org.vadere.simulator.utils.scenariochecker.ScenarioCheckerMessage;
import org.vadere.simulator.utils.scenariochecker.ScenarioCheckerReason;
import org.vadere.simulator.utils.scenariochecker.checks.AbstractScenarioCheck;
import org.vadere.state.attributes.AttributesSimulation;

import java.util.PriorityQueue;

public class SimulationTimeStepLengthCheck extends AbstractScenarioCheck {

	private final static double LOW_BOUND = 0.01;
	private final static double HIGH_BOUND = 1.0;

	@Override
	public PriorityQueue<ScenarioCheckerMessage> runScenarioCheckerTest(Scenario scenario) {
		PriorityQueue<ScenarioCheckerMessage> ret = new PriorityQueue<>();
		AttributesSimulation simAttr = scenario.getAttributesSimulation();
		double simTimeStep = simAttr.getSimTimeStepLength();

		if(simTimeStep <= LOW_BOUND || simTimeStep >= HIGH_BOUND){

			ret.add(msgBuilder.simulationAttrError()
					.reason(ScenarioCheckerReason.SIM_TIME_STEP_WRONG,
							String.format(" [%3.1f - %3.1f] current value: %3.1f", LOW_BOUND, HIGH_BOUND, simTimeStep))
					.build());
		}
		return ret;
	}
}
