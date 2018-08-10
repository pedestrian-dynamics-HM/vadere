package org.vadere.simulator.projects;

import org.lwjgl.system.CallbackI;
import org.vadere.simulator.projects.dataprocessing.datakey.DataKey;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;

public class SimulationResult {


	private String scenarioName;
	private Duration runTime;
	private int totalOverlaps;
	private String state;

	private Instant startTime;

	public SimulationResult(String scenarioName) {
		this.scenarioName = scenarioName;
	}


	public void startTime(){
		startTime = Instant.now();
	}
	public void stopTime(){
		runTime = Duration.between(Instant.now(), startTime);
	}


	public String getScenarioName() {
		return scenarioName;
	}

	public void setScenarioName(String scenarioName) {
		this.scenarioName = scenarioName;
	}

	public Duration getRunTime() {
		return runTime;
	}

	public String getRunTimeAsString(){
		return runTime.toString();
	}


	public int getTotalOverlaps() {
		return totalOverlaps;
	}

	public void setTotalOverlaps(int totalOverlaps) {
		this.totalOverlaps = totalOverlaps;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}
}
