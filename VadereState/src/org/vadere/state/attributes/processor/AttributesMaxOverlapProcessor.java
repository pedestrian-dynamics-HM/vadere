package org.vadere.state.attributes.processor;

/**
 * @author Marion Gödel
 *
 */

public class AttributesMaxOverlapProcessor extends AttributesProcessor {
    private int pedestrianMaxOverlapProcessorId;

    public int getPedestrianMaxOverlapProcessorId() {
        return this.pedestrianMaxOverlapProcessorId;
    }

    public void getPedestrianMaxOverlapProcessorId(int pedestrianMaxOverlapProcessorId) {
        checkSealed();
        this.pedestrianMaxOverlapProcessorId = pedestrianMaxOverlapProcessorId;
    }
}
