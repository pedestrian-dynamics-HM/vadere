package org.vadere.state.attributes.scenario;

import com.fasterxml.jackson.annotation.JsonView;
import org.vadere.state.attributes.AttributesScenarioElement;
import org.vadere.state.util.Views;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.reflection.VadereAttribute;

public class AttributesVisualElement extends AttributesScenarioElement {
    //@VadereAttribute
    @JsonView(Views.CacheViewExclude.class)
    protected VShape shape;
    @VadereAttribute
    @JsonView(Views.CacheViewExclude.class)
    protected Boolean visible;


    public void setShape(VShape shape){
        checkSealed();
        this.shape = shape;
    };

    public VShape getShape(){return this.shape;};

    public void setVisible(boolean visible){
        checkSealed();
        this.visible = visible;
    }

    public Boolean getVisible(){
        return this.visible;
    }

}
