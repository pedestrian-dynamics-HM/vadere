package org.vadere.state.attributes.processor;

/**
 * @author Mario Teixeira Parente
 * @author Marion Gödel
 */

public class AttributesEvacuationTimeProcessor extends AttributesProcessor {
    private int pedestrianStartTimeProcessorId;
    private int pedestrianEndTimeProcessorId;

    public int getPedestrianStartTimeProcessorId() {
        return this.pedestrianStartTimeProcessorId;
    }

    public int getPedestrianEndTimeProcessorId() {
        return this.pedestrianEndTimeProcessorId;
    }

    public void setPedestrianStartTimeProcessorId(int pedestrianStartTimeProcessorId) {
        checkSealed();
        this.pedestrianStartTimeProcessorId = pedestrianStartTimeProcessorId;
    }

    public void setPedestrianEndTimeProcessorId(int pedestrianEndTimeProcessorId) {
        checkSealed();
        this.pedestrianEndTimeProcessorId = pedestrianEndTimeProcessorId;
    }
}
