package org.vadere.simulator.projects;

import org.jetbrains.annotations.NotNull;
import org.vadere.util.logging.Logger;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

public class SimulationResult {

	private static Logger log = Logger.getLogger(SimulationResult.class);

	// tree map such that keys are sorted.
	private TreeMap<String, Object> data;

	private String scenarioName;
	private Duration runTime;
	private int totalOverlaps = -1;
	private double maxOverlap = -1;
	private String state;

	private Instant startTime;

	public SimulationResult(String scenarioName) {
		this.scenarioName = scenarioName;
		this.data = new TreeMap<>();
	}

	public String getScenarioName(){
		return this.scenarioName;
	}

	public TreeMap<String,Object> getData(){
		return this.data;
	}

	public void addData(@NotNull final String header, @NotNull final Object value) {
		assert !data.containsKey(header);
		if(data.containsKey(header)) {
			log.warn("duplicated headers ("+ header +") in simulation results");
		}
		data.put(header, value);
	}

	public void startTime(){
		startTime = Instant.now();
	}

	public void stopTime(){
		runTime = Duration.between(startTime, Instant.now());
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public String[] getAsTableRow(){
		String[] ret = new String[3 + data.size()];
		ret[0] = scenarioName;
		ret[1] = runTime.toString();

		int i = 2;
		for(Map.Entry<String, Object> entry : data.entrySet()) {
			ret[i] = entry.getValue().toString();
			i++;
		}

		ret[i] = state;
		return ret;
	}

	public void addCsvRow(StringBuilder sj, char dl){
		Arrays.stream(getAsTableRow()).forEach(i -> sj.append(i).append(dl));
		sj.setLength(sj.length() -1);
		sj.append("\n");
	}

	public String[] getHeaders(){
		String[] ret = new String[3 + data.size()];
		ret[0] = "Scenario_Name";
		ret[1] = "Runtime";

		int i = 2;
		for(Map.Entry<String, Object> entry : data.entrySet()) {
			ret[i] = entry.getKey();
			i++;
		}

		ret[i] = "State";
		return ret;
	}

	public static void addCsvHeader(SimulationResult simulationResult, StringBuilder sj, char dl){
		sj.append("Scenario_Name").append(dl);
		sj.append("Runtime").append(dl);

		for(Map.Entry<String, Object> entry : simulationResult.data.entrySet()) {
			sj.append(entry.getKey());
		}

		sj.append("State\n");
	}

	public String toJson() {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("SimulationResult{");
		stringBuilder.append("scenarioName='");
		stringBuilder.append("\"");
		stringBuilder.append(scenarioName);
		stringBuilder.append("\"");
		stringBuilder.append(", ");

		for(Map.Entry<String, Object> entry : data.entrySet()) {
			stringBuilder.append(entry.getKey());

			if(entry.getValue().getClass() == String.class) {
				stringBuilder.append("\"");
			}

			stringBuilder.append("=");
			stringBuilder.append(entry.getValue().toString());

			if(entry.getValue().getClass() == String.class) {
				stringBuilder.append("\"");
			}

			stringBuilder.append(", ");
		}

		stringBuilder.append("state='");
		stringBuilder.append("\"");
		stringBuilder.append(state);
		stringBuilder.append("\"");
		stringBuilder.append("}");

		return stringBuilder.toString();
	}

	@Override
	public String toString() {
		return toJson();
	}
}
