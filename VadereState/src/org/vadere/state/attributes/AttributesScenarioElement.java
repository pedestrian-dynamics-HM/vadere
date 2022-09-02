package org.vadere.state.attributes;

import com.fasterxml.jackson.annotation.JsonView;
import org.vadere.state.util.Views;
import org.vadere.util.Attributes;
import org.vadere.util.reflection.VadereAttribute;

public abstract class AttributesScenarioElement extends Attributes {
    @VadereAttribute
    @JsonView(Views.CacheViewExclude.class)
    protected Integer id;

    public  AttributesScenarioElement(){this(-1);}
    public  AttributesScenarioElement(final int id){this.id = id;}

    public int getId() {
        return id;
    }

    public void setId(int id) {
        checkSealed();
        this.id = id;
    }

}
