package org.vadere.state.attributes.processor;
/**
 * @author Marion Goedel
 *
 */

public class AttributesFlowProcessor extends AttributesProcessor {
    private int pedestrianLineCrossProcessorId;

    public int getPedestrianLineCrossProcessorId() {
        return this.pedestrianLineCrossProcessorId;
    }

    public void setPedestrianLineCrossProcessorId(int pedestrianLineCrossProcessorId) {
        checkSealed();
        this.pedestrianLineCrossProcessorId = pedestrianLineCrossProcessorId;
    }

}


