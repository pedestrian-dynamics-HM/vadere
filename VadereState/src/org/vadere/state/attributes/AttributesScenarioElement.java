package org.vadere.state.attributes;

import com.fasterxml.jackson.annotation.JsonView;
import org.vadere.state.util.Views;

public abstract class AttributesScenarioElement extends Attributes {
    /**
     * This attribute serves as an unique identifier for a scenario element.<br>
     * <b>NOTE:</b> ids are not unique over all scenario elements but are only unique over their types.
     */
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
