package org.vadere.state.attributes.processor;

/**
 * @author Mario Teixeira Parente
 *
 */

public class AttributesPedestrianWaitingTimeProcessor extends AttributesProcessor {
    private int waitingAreaId = -1;

    public int getWaitingAreaId() {
        return waitingAreaId;
    }

    public void setWaitingAreaId(int waitingAreaId) {
        checkSealed();
        this.waitingAreaId = waitingAreaId;
    }
}
