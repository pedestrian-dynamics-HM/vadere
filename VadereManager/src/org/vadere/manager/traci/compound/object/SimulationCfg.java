package org.vadere.manager.traci.compound.object;

import org.vadere.state.traci.TraCIException;
import org.vadere.state.traci.TraCIDataType;
import org.vadere.state.traci.CompoundObject;
import org.vadere.state.traci.CompoundObjectBuilder;

import java.util.Objects;

/**
 * Configuration options received from TraCI client.
 *
 * Used to seed Random object and redirect output to a client defined location.
 */
public class SimulationCfg {
	private String configName;
	private String experiment;
	private String dateTime;
	private String resultRootDir;
	private String iterationVariables;
	private String repetition;
	private String outputScalarFile;
	private String outputVecFile;
	private long seed;
	private boolean useVadereSeed;

	public SimulationCfg(CompoundObject obj) {
		if (obj.size() != 10) {
			throw new TraCIException("Expected at least 10 elements");
		}
		configName = (String) obj.getData(0, TraCIDataType.STRING);
		experiment = (String) obj.getData(1, TraCIDataType.STRING);
		dateTime = (String) obj.getData(2, TraCIDataType.STRING);
		resultRootDir = (String) obj.getData(3, TraCIDataType.STRING);
		iterationVariables = (String) obj.getData(4, TraCIDataType.STRING);
		repetition = (String) obj.getData(5, TraCIDataType.STRING);
		outputScalarFile = (String) obj.getData(6, TraCIDataType.STRING);
		outputVecFile = (String) obj.getData(7, TraCIDataType.STRING);
		seed = Long.valueOf((int) obj.getData(8, TraCIDataType.INTEGER));
		int useVadereSeedVal = (Integer) obj.getData(9, TraCIDataType.U_BYTE);
		if (useVadereSeedVal == 1){
			useVadereSeed = true;
		} else {
			useVadereSeed = false;
		}
	}

	public static CompoundObject asCompoundObject(String configName,
												  String experiment,
												  String dateTime,
												  String resultRootDir,
												  String iterationVariables,
												  String repetition,
												  String outputScalarFile,
												  String outputVecFile,
												  long seed,
												  boolean useVadereSeed) {
		return CompoundObjectBuilder.builder()
				.rest()
				.add(TraCIDataType.STRING, 8)
				.add(TraCIDataType.INTEGER)
				.add(TraCIDataType.U_BYTE)
				.build(configName,
						experiment,
						dateTime,
						resultRootDir,
						iterationVariables,
						repetition,
						outputScalarFile,
						outputVecFile,
						seed,
						useVadereSeed);
	}

	public CompoundObject getCompoundObject(){
		return CompoundObjectBuilder.builder()
				.rest()
				.add(TraCIDataType.STRING, 8)
				.add(TraCIDataType.INTEGER)
				.add(TraCIDataType.U_BYTE)
				.build(configName,
						experiment,
						dateTime,
						resultRootDir,
						iterationVariables,
						repetition,
						outputScalarFile,
						outputVecFile,
						seed,
						useVadereSeed);
	}

	public String outputPath() {
		return String.format("%s/%s_%s/vadere.d",
				resultRootDir,
				configName,
				experiment
		);
	}

	public String getConfigName() {
		return configName;
	}

	public void setConfigName(String configName) {
		this.configName = configName;
	}

	public String getExperiment() {
		return experiment;
	}

	public void setExperiment(String experiment) {
		this.experiment = experiment;
	}

	public String getDateTime() {
		return dateTime;
	}

	public void setDateTime(String dateTime) {
		this.dateTime = dateTime;
	}

	public String getResultRootDir() {
		return resultRootDir;
	}

	public void setResultRootDir(String resultRootDir) {
		this.resultRootDir = resultRootDir;
	}

	public String getIterationVariables() {
		return iterationVariables;
	}

	public void setIterationVariables(String iterationVariables) {
		this.iterationVariables = iterationVariables;
	}

	public String getRepetition() {
		return repetition;
	}

	public void setRepetition(String repetition) {
		this.repetition = repetition;
	}

	public String getOutputScalarFile() {
		return outputScalarFile;
	}

	public void setOutputScalarFile(String outputScalarFile) {
		this.outputScalarFile = outputScalarFile;
	}

	public String getOutputVecFile() {
		return outputVecFile;
	}

	public void setOutputVecFile(String outputVecFile) {
		this.outputVecFile = outputVecFile;
	}

	public long getSeed() {
		return seed;
	}

	public void setSeed(long seed) {
		this.seed = seed;
	}

	public boolean isUseVadereSeed() {
		return useVadereSeed;
	}

	public void setUseVadereSeed(boolean useVadereSeed) {
		this.useVadereSeed = useVadereSeed;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		SimulationCfg that = (SimulationCfg) o;
		return seed == that.seed &&
				useVadereSeed == that.useVadereSeed &&
				Objects.equals(configName, that.configName) &&
				Objects.equals(experiment, that.experiment) &&
				Objects.equals(dateTime, that.dateTime) &&
				Objects.equals(resultRootDir, that.resultRootDir) &&
				Objects.equals(iterationVariables, that.iterationVariables) &&
				Objects.equals(repetition, that.repetition) &&
				Objects.equals(outputScalarFile, that.outputScalarFile) &&
				Objects.equals(outputVecFile, that.outputVecFile);
	}

	@Override
	public int hashCode() {
		return Objects.hash(configName, experiment, dateTime, resultRootDir, iterationVariables, repetition, outputScalarFile, outputVecFile, seed, useVadereSeed);
	}

	@Override
	public String toString() {
		return "SimulationCfg{" +
				"configName='" + configName + '\'' +
				", experiment='" + experiment + '\'' +
				", dateTime='" + dateTime + '\'' +
				", resultRootDir='" + resultRootDir + '\'' +
				", iterationVariables='" + iterationVariables + '\'' +
				", repetition='" + repetition + '\'' +
				", outputScalarFile='" + outputScalarFile + '\'' +
				", outputVecFile='" + outputVecFile + '\'' +
				", seed=" + seed +
				", useVadereSeed=" + useVadereSeed +
				'}';
	}
}
