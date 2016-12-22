package org.vadere.state.attributes.processor;

/**
 * @author Mario Teixeira Parente
 *
 */

public class AttributesPedestrianFlowProcessor extends AttributesProcessor {
    private int pedestrianVelocityProcessorId;
    private int pedestrianDensityProcessorId;

    public int getPedestrianVelocityProcessorId() {
        return this.pedestrianVelocityProcessorId;
    }

    public int getPedestrianDensityProcessorId() {
        return this.pedestrianDensityProcessorId;
    }
}
