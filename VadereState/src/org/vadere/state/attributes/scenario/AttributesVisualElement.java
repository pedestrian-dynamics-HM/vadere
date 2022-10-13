package org.vadere.state.attributes.scenario;

import com.fasterxml.jackson.annotation.JsonView;
import org.vadere.state.attributes.AttributesScenarioElement;
import org.vadere.state.util.Views;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.reflection.VadereAttribute;

public class AttributesVisualElement extends AttributesScenarioElement {
    /**
     * This attribute stores the shape of a scenario element.<br>
     * Possible types are: <i>Rectangle,Polygon,Circle</i>
     */
    protected VShape shape;
    /**
     * This attribute stores the visibility state of a scenario element. It is only used for the
     * topography editor and won't influence a simulation result.
     */
    @JsonView(Views.CacheViewExclude.class)
    protected Boolean visible;

    public AttributesVisualElement() {
        super();
        this.shape = new VRectangle(0, 0, 1, 1);
        this.visible = true;
    }

    public VShape getShape() {
        return this.shape;
    }

    public void setShape(VShape shape) {
        checkSealed();
        this.shape = shape;
    }

    public void setVisible(boolean visible) {
        checkSealed();
        this.visible = visible;
    }

    public Boolean isVisible(){
        return this.visible;
    }

}
