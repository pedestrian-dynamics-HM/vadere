package org.vadere.state.attributes.processor;

/**
 * @author Marion Gödel
 *
 */

public class AttributesNumberOverlapsProcessor extends AttributesProcessor {
    private int pedestrianOverlapProcessorId;

    public int getPedestrianOverlapProcessorId() {
        return this.pedestrianOverlapProcessorId;
    }

    public void setPedestrianOverlapProcessorId(int pedestrianOverlapProcessorId) {
        checkSealed();
        this.pedestrianOverlapProcessorId = pedestrianOverlapProcessorId;
    }
}
