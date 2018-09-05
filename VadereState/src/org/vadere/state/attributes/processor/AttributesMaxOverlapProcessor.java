package org.vadere.state.attributes.processor;

/**
 * @author Marion Gödel
 *
 */

public class AttributesMaxOverlapProcessor extends AttributesProcessor {
    private int pedestrianOverlapDistProcessorId;

    public int getPedestrianOverlapDistProcessorId() {
        return this.pedestrianOverlapDistProcessorId;
    }

    public void getPedestrianOverlapDistProcessorId(int pedestrianOverlapDistProcessorId) {
        checkSealed();
        this.pedestrianOverlapDistProcessorId = pedestrianOverlapDistProcessorId;
    }
}
