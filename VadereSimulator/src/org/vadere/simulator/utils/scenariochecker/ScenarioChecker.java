package org.vadere.simulator.utils.scenariochecker;

import org.jetbrains.annotations.NotNull;
import org.vadere.simulator.projects.Scenario;
import org.vadere.simulator.utils.scenariochecker.checks.ScenarioCheckerTest;
import org.vadere.simulator.utils.scenariochecker.checks.dataProcessors.CheckAreasInAreaDensityVoronoiProcessor;
import org.vadere.simulator.utils.scenariochecker.checks.dataProcessors.DataProcessorsLinkedToMeasurementArea;
import org.vadere.simulator.utils.scenariochecker.checks.models.CellularAutomatonSetupCheck;
import org.vadere.simulator.utils.scenariochecker.checks.models.psychology.StimulusCheck;
import org.vadere.simulator.utils.scenariochecker.checks.simulation.GroupSetupCheck;
import org.vadere.simulator.utils.scenariochecker.checks.simulation.SimulationTimeStepLengthCheck;
import org.vadere.simulator.utils.scenariochecker.checks.topography.*;
import org.vadere.state.scenario.Topography;

import java.util.PriorityQueue;

public class ScenarioChecker {
	public static final String CHECKER_ON = "on";
	public static final String CHECKER_OFF = "off";
	public static final String CHECKER_ERROR_ONLY = "error-only";

	private final Topography topography;
	private final Scenario scenario;

	public ScenarioChecker(@NotNull final Topography topography) {
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


	public PriorityQueue<ScenarioCheckerMessage> checkOverlap() {
		return runCheck(new TopographyOverlapCheck());
	}

	public PriorityQueue<ScenarioCheckerMessage> checkObstacleRepulsion() {
		return runCheck(new ObstacleRepulsionCheck());
	}




	private PriorityQueue<ScenarioCheckerMessage> runCheck(ScenarioCheckerTest checkerTest) {
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
		ret.addAll(checkObstacleRepulsion());
		ret.addAll(checkSimulationAttribues());
		ret.addAll(checkSourceSpawnSetting());
		ret.addAll(checkMinSourceRadius());
		ret.addAll(checkObstacleArea());
		ret.addAll(checkTargetNoAbsorber());
		ret.addAll(checkTargetAbsorberNotLast());
		ret.addAll(runCheck(new TopographyOffsetCheck()));
		ret.addAll(runCheck(new DataProcessorsLinkedToMeasurementArea()));
		ret.addAll(runCheck(new CheckAreasInAreaDensityVoronoiProcessor()));
		ret.addAll(runCheck(new GroupSetupCheck()));
		ret.addAll(runCheck(new CellularAutomatonSetupCheck()));
		ret.addAll(runCheck(new StimulusCheck()));
		return ret;
	}

	public PriorityQueue<ScenarioCheckerMessage> checkObstacleArea(){return runCheck(new ObstacleAreaCheck());}

	public PriorityQueue<ScenarioCheckerMessage> checkTargetNoAbsorber(){return runCheck(new ExistingAbsorberCheck());}
	public PriorityQueue<ScenarioCheckerMessage> checkTargetAbsorberNotLast(){return runCheck(new LastIsAbsorberCheck());}
	public PriorityQueue<ScenarioCheckerMessage> checkMinSourceRadius() {
		return runCheck(new SourceMinRadiusCheck());
	}

	public PriorityQueue<ScenarioCheckerMessage> checkSimulationAttribues() {
		PriorityQueue<ScenarioCheckerMessage> ret = new PriorityQueue<>();
		ret.addAll(runCheck(new SimulationTimeStepLengthCheck()));
		return ret;
	}

	public PriorityQueue<ScenarioCheckerMessage> checkSourceSpawnSetting() {
		return runCheck(new SourceSpawnSettingCheck());
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
