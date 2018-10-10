package org.vadere.simulator.util;

import org.jetbrains.annotations.NotNull;
import org.vadere.simulator.projects.Scenario;
import org.vadere.simulator.util.checks.ScenarioCheckerTest;
import org.vadere.simulator.util.checks.simulation.SimulationTimeStepLengthCheck;
import org.vadere.simulator.util.checks.topography.PedestrianSpeedSetupCheck;
import org.vadere.simulator.util.checks.topography.StairTreadSanityCheck;
import org.vadere.simulator.util.checks.topography.TopographyOverlapCheck;
import org.vadere.simulator.util.checks.topography.UniqueSourceIdCheck;
import org.vadere.simulator.util.checks.topography.UnusedTargetsCheck;
import org.vadere.simulator.util.checks.topography.ValidTargetsInSourceCheck;
import org.vadere.state.scenario.Topography;

import java.util.PriorityQueue;

public class ScenarioChecker {
	private final Topography topography;
	private final Scenario scenario;

	public ScenarioChecker(@NotNull final Topography topography){
		Scenario s = new Scenario("");
		s.setTopography(topography);
		this.scenario = s;
		this.topography = topography;
	}

	public ScenarioChecker(@NotNull final Scenario scenario) {
		this.scenario = scenario;
		this.topography = scenario.getTopography();
	}

	public Topography getTopography() {
		return topography;
	}

	public Scenario getScenario() {
		return scenario;
	}


	public PriorityQueue<ScenarioCheckerMessage> checkOverlap(){
		return runCheck(new TopographyOverlapCheck());
	}


	private PriorityQueue<ScenarioCheckerMessage> runCheck(ScenarioCheckerTest checkerTest){
		return checkerTest.runScenarioCheckerTest(scenario);
	}

	public PriorityQueue<ScenarioCheckerMessage> checkBuildingStep() {
		PriorityQueue<ScenarioCheckerMessage> ret = new PriorityQueue<>();
		ret.addAll(checkValidTargetsInSource());
		ret.addAll(checkUniqueSourceId());
		ret.addAll(checkUnusedTargets());
		ret.addAll(checkStairTreadSanity());
		ret.addAll(checkPedestrianSpeedSetup());
		ret.addAll(checkOverlap());
		ret.addAll(checkSimulationAttribues());
		return ret;
	}

	public PriorityQueue<ScenarioCheckerMessage> checkSimulationAttribues(){
		PriorityQueue<ScenarioCheckerMessage> ret = new PriorityQueue<>();
		ret.addAll(runCheck(new SimulationTimeStepLengthCheck()));
		return ret;
	}

	public PriorityQueue<ScenarioCheckerMessage> checkPedestrianSpeedSetup() {
		return runCheck(new PedestrianSpeedSetupCheck());
	}

	public PriorityQueue<ScenarioCheckerMessage> checkStairTreadSanity() {
		return runCheck(new StairTreadSanityCheck());
	}


	public PriorityQueue<ScenarioCheckerMessage> checkUnusedTargets() {
		return runCheck(new UnusedTargetsCheck());
	}


	public PriorityQueue<ScenarioCheckerMessage> checkValidTargetsInSource() {
		return runCheck(new ValidTargetsInSourceCheck());
	}

	public PriorityQueue<ScenarioCheckerMessage> checkUniqueSourceId() {
		return runCheck(new UniqueSourceIdCheck());
	}

}
