package org.vadere.state.attributes.processor;

/**
 * @author Marion Gödel
 *
 */

public class AttributesMaxOverlapProcessor extends AttributesProcessor {
    private int pedestrianOverlapProcessorId;

    public int getPedestrianOverlapProcessorId() {
        return this.pedestrianOverlapProcessorId;
    }

    public void setPedestrianOverlapProcessorId(int pedestrianOverlapProcessorId) {
        checkSealed();
        this.pedestrianOverlapProcessorId = pedestrianOverlapProcessorId;
    }
}
