package org.vadere.state.attributes.scenario.builder;

import org.vadere.state.attributes.AttributesScenarioElement;
import org.vadere.util.Attributes;

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
