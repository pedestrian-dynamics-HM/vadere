package org.vadere.state.attributes.processor;

/**
 * @author Marion Gödel
 * Evaluates the length of a parade based on the PedestrianPotentialProcessor
 */

public class AttributesParadeLengthProcessor extends AttributesProcessor {
	private int pedestrianPotentialProcessorId;
	private int pedestrianEvacuationTimeProcessor;
	private int pedestrianStartTimeProcessor;
	private int numberOfAgentsAveraged = 10;

    public int getPedestrianPotentialProcessorId() {
        return this.pedestrianPotentialProcessorId;
    }

    public int getPedestrianEvacuationTimeProcessor() {
        return this.pedestrianEvacuationTimeProcessor;
    }

    public int getPedestrianStartTimeProcessor() {
        return this.pedestrianStartTimeProcessor;
    }

    public int getNumberOfAgentsAveraged() {
        return this.numberOfAgentsAveraged;
    }



    public void setPedestrianPotentialProcessorId(int pedestrianPotentialProcessorId) {
        checkSealed();
        this.pedestrianPotentialProcessorId = pedestrianPotentialProcessorId;
    }

    public void setPedestrianEvacuationTimeProcessor(int pedestrianEvacuationTimeProcessor) {
        checkSealed();
        this.pedestrianEvacuationTimeProcessor = pedestrianEvacuationTimeProcessor;
    }

    public void setPedestrianStartTimeProcessor(int pedestrianStartTimeProcessor) {
        checkSealed();
        this.pedestrianStartTimeProcessor = pedestrianStartTimeProcessor;
    }

    public void setNumberOfAgentsAveraged(int numberOfAgentsAveraged) {
        checkSealed();
        this.numberOfAgentsAveraged = numberOfAgentsAveraged;
    }


}
