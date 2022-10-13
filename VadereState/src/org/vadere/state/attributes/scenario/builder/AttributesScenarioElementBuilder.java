package org.vadere.state.attributes.scenario.builder;

import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.AttributesScenarioElement;

public final class AttributesScenarioElementBuilder {
    private Integer id = Attributes.ID_NOT_SET;

    public AttributesScenarioElementBuilder setId(int id) {
        this.id = id;
        return this;
    }

    public AttributesScenarioElement build(AttributesScenarioElement element){
        element.setId(this.id);
        return element;
    }
}
