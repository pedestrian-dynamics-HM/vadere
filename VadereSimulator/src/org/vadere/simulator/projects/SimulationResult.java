package org.vadere.simulator.projects;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;

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
		runTime = Duration.between(startTime, Instant.now());
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


	public String[] getAsTableRow(){
		String[] ret = new String[4];
		ret[0] = scenarioName;
		ret[1] = runTime.toString();
		ret[2] = Integer.toString(totalOverlaps);
		ret[3] = state;
		return ret;
	}

	public void addCsvRow(StringBuilder sj, char dl){
		Arrays.stream(getAsTableRow()).forEach(i -> sj.append(i).append(dl));
		sj.setLength(sj.length() -1);
		sj.append("\n");
	}

	public static void addCsvHeader(StringBuilder sj, char dl){
		sj.append("Scenario_Name").append(dl);
		sj.append("Runtime").append(dl);
		sj.append("Overlaps").append(dl);
		sj.append("State\n");

	}

	@Override
	public String toString() {
		return "SimulationResult{" +
				"scenarioName='" + scenarioName + '\'' +
				", runTime=" + runTime +
				", totalOverlaps=" + totalOverlaps +
				", state='" + state + '\'' +
				'}';
	}
}
