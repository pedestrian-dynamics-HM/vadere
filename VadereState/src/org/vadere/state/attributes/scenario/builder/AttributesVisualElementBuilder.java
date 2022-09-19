package org.vadere.state.attributes.scenario.builder;

import org.vadere.state.attributes.scenario.AttributesVisualElement;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;

public final class AttributesVisualElementBuilder {
    private VShape shape = new VRectangle(0,0,1,1);
    private Boolean visible = true;
    private AttributesScenarioElementBuilder scenarioElementBuilder = new AttributesScenarioElementBuilder();

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
