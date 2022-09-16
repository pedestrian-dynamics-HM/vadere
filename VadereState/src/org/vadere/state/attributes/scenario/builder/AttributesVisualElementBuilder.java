package org.vadere.state.attributes.scenario.builder;

import org.vadere.state.attributes.scenario.AttributesVisualElement;
import org.vadere.util.geometry.shapes.VShape;

public final class AttributesVisualElementBuilder {
    private VShape shape;
    private Boolean visible;
    private AttributesScenarioElementBuilder scenarioElementBuilder;

    public AttributesVisualElementBuilder setScenarioElementBuilder(AttributesScenarioElementBuilder scenarioElementBuilder) {
        this.scenarioElementBuilder = scenarioElementBuilder;
        return this;
    }


    public AttributesVisualElementBuilder setShape(VShape shape){
        this.shape = shape;
        return this;
    }

    public AttributesVisualElementBuilder setVisible(Boolean visible){
        this.visible = visible;
        return this;
    }

    public static AttributesVisualElementBuilder anVisualElement() {
        try {
            return new AttributesVisualElementBuilder();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    public AttributesVisualElement build(AttributesVisualElement element){
        element = (AttributesVisualElement) this.scenarioElementBuilder.build(element);
        element.setShape(this.shape);
        element.setVisible(this.visible);
        return  element;
    }
}
