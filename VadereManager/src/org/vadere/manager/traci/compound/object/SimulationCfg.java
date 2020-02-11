package org.vadere.manager.traci.compound.object;

import org.vadere.manager.TraCIException;
import org.vadere.manager.traci.TraCIDataType;
import org.vadere.manager.traci.compound.CompoundObject;
import org.vadere.manager.traci.compound.CompoundObjectBuilder;

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

	public SimulationCfg(CompoundObject obj) {
		if (obj.size() != 9) {
			throw new TraCIException("Expected at least 9 elements");
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
	}

	public static CompoundObject asCompoundObject(String configName,
												  String experiment,
												  String dateTime,
												  String resultRootDir,
												  String iterationVariables,
												  String repetition,
												  String outputScalarFile,
												  String outputVecFile,
												  long seed) {
		return CompoundObjectBuilder.builder()
				.rest()
				.add(TraCIDataType.STRING, 8)
				.add(TraCIDataType.INTEGER)
				.build(configName,
						experiment,
						dateTime,
						resultRootDir,
						iterationVariables,
						repetition,
						outputScalarFile,
						outputVecFile,
						seed);
	}

	public String outputPath() {
		if (outputVecFile.endsWith(".vec")) {
			return outputVecFile.substring(0, outputVecFile.length() - 4);
		}
		if (outputScalarFile.endsWith(".sca")) {
			return outputScalarFile.substring(0, outputScalarFile.length() - 4);
		}
		return String.format("%s/%s/%s/%s_vars_%s_rep_%s",
				resultRootDir,
				configName,
				experiment,
				dateTime,
				iterationVariables,
				repetition
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

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		SimulationCfg that = (SimulationCfg) o;
		return seed == that.seed &&
				configName.equals(that.configName) &&
				experiment.equals(that.experiment) &&
				dateTime.equals(that.dateTime) &&
				resultRootDir.equals(that.resultRootDir) &&
				iterationVariables.equals(that.iterationVariables) &&
				repetition.equals(that.repetition) &&
				outputScalarFile.equals(that.outputScalarFile) &&
				outputVecFile.equals(that.outputVecFile);
	}

	@Override
	public int hashCode() {
		return Objects.hash(configName, experiment, dateTime, resultRootDir, iterationVariables, repetition, outputScalarFile, outputVecFile, seed);
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
				'}';
	}
}
