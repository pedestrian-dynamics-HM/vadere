package org.vadere.state.attributes;

import java.util.Objects;
import java.util.Random;

/**
 * Provides attributes for the simulation, like visualizationEnabled and
 * writeSimulationData.
 * 
 */
public class AttributesSimulation extends Attributes {

	private double finishTime = 500;
	/** Progress of simulation time between two simulation steps in a row. */
	private double simTimeStepLength = 0.4;
	private double realTimeSimTimeRatio = 0.1;
	private boolean writeSimulationData = true;
	private boolean visualizationEnabled = true;
	private boolean printFPS = false;
	private int digitsPerCoordinate = 2;
	private boolean useFixedSeed = true;
	private long fixedSeed = new Random().nextLong();
	private long simulationSeed;

	private boolean runInOnlineMode = false;

	/**
	 * The uri at which TranslateIt or Translate sends their data,
	 * has only an effect if runInOnlineMode == true.
	 */
	private String onlineModeSubscriber = "tcp://localhost:5010";

	/**
	 * The uri at which VADERE sends its simulation results,
	 * has only an effect if runInOnlineMode == true.
	 */
	private String onlineModePublisher = "tcp://localhost:5011";

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


	public int getDigitsPerCoordinate() {
		return digitsPerCoordinate;
	}

	public boolean isUseFixedSeed() {
		return useFixedSeed;
	}

	public long getFixedSeed() {
		return fixedSeed;
	}

	public long getSimulationSeed() {
		return simulationSeed;
	}

	public String getOnlineModePublisher() {
		return onlineModePublisher;
	}

	public String getOnlineModeSubscriber() {
		return onlineModeSubscriber;
	}

	public boolean isRunInOnlineMode() {
		return runInOnlineMode;
	}

	public void setOnlineModePublisher(String onlineModePublisher) {
		checkSealed();
		this.onlineModePublisher = onlineModePublisher;
	}

	public void setOnlineModeSubscriber(String onlineModeSubscriber) {
		checkSealed();
		this.onlineModeSubscriber = onlineModeSubscriber;
	}

	public void setRunInOnlineMode(boolean runInOnlineMode) {
		checkSealed();
		this.runInOnlineMode = runInOnlineMode;
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


	public void setDigitsPerCoordinate(int digitsPerCoordinate) {
		checkSealed();
		this.digitsPerCoordinate = digitsPerCoordinate;
	}

	public void setUseFixedSeed(boolean useFixedSeed) {
		checkSealed();
		this.useFixedSeed = useFixedSeed;
	}

	public void setFixedSeed(long fixedSeed) {
		checkSealed();
		this.fixedSeed = fixedSeed;
	}

	public void setSimulationSeed(long simulationSeed) {
		checkSealed();
		this.simulationSeed = simulationSeed;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		AttributesSimulation that = (AttributesSimulation) o;
		return Double.compare(that.finishTime, finishTime) == 0 &&
				Double.compare(that.simTimeStepLength, simTimeStepLength) == 0 &&
				Double.compare(that.realTimeSimTimeRatio, realTimeSimTimeRatio) == 0 &&
				writeSimulationData == that.writeSimulationData &&
				visualizationEnabled == that.visualizationEnabled &&
				printFPS == that.printFPS &&
				digitsPerCoordinate == that.digitsPerCoordinate &&
				useFixedSeed == that.useFixedSeed &&
				fixedSeed == that.fixedSeed &&
				simulationSeed == that.simulationSeed;
	}

	@Override
	public int hashCode() {

		return Objects.hash(finishTime, simTimeStepLength, realTimeSimTimeRatio, writeSimulationData, visualizationEnabled, printFPS, digitsPerCoordinate, useFixedSeed, fixedSeed, simulationSeed);
	}
}
