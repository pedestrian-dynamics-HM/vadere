package org.vadere.state.attributes;

import com.fasterxml.jackson.annotation.JsonFilter;

/**
 * Provides attributes for the simulation, like visualizationEnabled and
 * writeSimulationData.
 * 
 */
@JsonFilter("testFilter")
public class AttributesSimulation extends Attributes {

	private double finishTime = 500;
	/** Progress of simulation time between two simulation steps in a row. */
	private double simTimeStepLength = 0.4;
	private double realTimeSimTimeRatio = 0.1;
	private boolean writeSimulationData = true;
	private boolean visualizationEnabled = true;
	private boolean printFPS = false;
	private boolean needsBoundary = false;
	private int digitsPerCoordinate = 2;
	private boolean useRandomSeed = true;
	private long randomSeed = 1;

	// Getter...

	public double getFinishTime() {
		return finishTime;
	}

	public double getSimTimeStepLength() {
		return simTimeStepLength;
	}

	public double getRealTimeSimTimeRatio() {
		return realTimeSimTimeRatio;
	}

	public boolean isWriteSimulationData() {
		return writeSimulationData;
	}

	public boolean isVisualizationEnabled() {
		return visualizationEnabled;
	}

	public boolean isPrintFPS() {
		return printFPS;
	}

	public boolean isNeedsBoundary() {
		return needsBoundary;
	}

	public int getDigitsPerCoordinate() {
		return digitsPerCoordinate;
	}

	public boolean isUseRandomSeed() {
		return useRandomSeed;
	}

	public long getRandomSeed() {
		return randomSeed;
	}

	public void setFinishTime(double finishTime) {
		checkSealed();
		this.finishTime = finishTime;
	}

	public void setSimTimeStepLength(double simTimeStepLength) {
		checkSealed();
		this.simTimeStepLength = simTimeStepLength;
	}

	public void setRealTimeSimTimeRatio(double realTimeSimTimeRatio) {
		checkSealed();
		this.realTimeSimTimeRatio = realTimeSimTimeRatio;
	}

	public void setWriteSimulationData(boolean writeSimulationData) {
		checkSealed();
		this.writeSimulationData = writeSimulationData;
	}

	public void setVisualizationEnabled(boolean visualizationEnabled) {
		checkSealed();
		this.visualizationEnabled = visualizationEnabled;
	}

	public void setPrintFPS(boolean printFPS) {
		checkSealed();
		this.printFPS = printFPS;
	}

	public void setNeedsBoundary(boolean needsBoundary) {
		checkSealed();
		this.needsBoundary = needsBoundary;
	}

	public void setDigitsPerCoordinate(int digitsPerCoordinate) {
		checkSealed();
		this.digitsPerCoordinate = digitsPerCoordinate;
	}

	public void setUseRandomSeed(boolean useRandomSeed) {
		checkSealed();
		this.useRandomSeed = useRandomSeed;
	}

	public void setRandomSeed(long randomSeed) {
		checkSealed();
		this.randomSeed = randomSeed;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		AttributesSimulation that = (AttributesSimulation) o;

		if (digitsPerCoordinate != that.digitsPerCoordinate)
			return false;
		if (needsBoundary != that.needsBoundary)
			return false;
		if (printFPS != that.printFPS)
			return false;
		if (randomSeed != that.randomSeed)
			return false;
		if (Double.compare(that.realTimeSimTimeRatio, realTimeSimTimeRatio) != 0)
			return false;
		if (Double.compare(that.simTimeStepLength, simTimeStepLength) != 0)
			return false;
		if (useRandomSeed != that.useRandomSeed)
			return false;
		if (visualizationEnabled != that.visualizationEnabled)
			return false;
		if (writeSimulationData != that.writeSimulationData)
			return false;
		if (finishTime != that.finishTime)
			return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result;
		long temp;
		temp = Double.doubleToLongBits(simTimeStepLength);
		result = (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(realTimeSimTimeRatio);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(finishTime);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		result = 31 * result + (writeSimulationData ? 1 : 0);
		result = 31 * result + (visualizationEnabled ? 1 : 0);
		result = 31 * result + (printFPS ? 1 : 0);
		result = 31 * result + (needsBoundary ? 1 : 0);
		result = 31 * result + digitsPerCoordinate;
		result = 31 * result + (useRandomSeed ? 1 : 0);
		result = 31 * result + (int) (randomSeed ^ (randomSeed >>> 32));
		return result;
	}
}
