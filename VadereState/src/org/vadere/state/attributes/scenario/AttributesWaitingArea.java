package org.vadere.state.attributes.scenario;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.vadere.state.attributes.AttributesWaiter;
import org.vadere.state.attributes.distributions.AttributesDistribution;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.reflection.VadereAttribute;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class AttributesWaitingArea extends AttributesVisualElement {
    @VadereAttribute
    private AttributesWaiter waiter = new AttributesWaiter();

    AttributesWaitingArea(){
        super();
    }

    AttributesWaitingArea(int id, VShape shape){
        super();
        this.id = id;
        this.shape = shape;
    }

    public AttributesDistribution getDistribution() {
        return this.waiter.getDistribution();
    }

    public AttributesWaitingArea setDistribution(AttributesDistribution distribution) {
        this.waiter.setDistribution(distribution);
        return this;
    }
}
