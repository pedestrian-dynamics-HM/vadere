package org.vadere.state.attributes;

import com.fasterxml.jackson.annotation.JsonView;
import org.vadere.state.util.Views;
import org.vadere.util.reflection.VadereAttribute;



public class AttributesEnabled extends Attributes {
    /**
     * This attribute stores the active state of its component
     */
    @JsonView(Views.CacheViewExclude.class)
    protected Boolean enabled = false;

    public  AttributesEnabled(){
        super();
    }
    public AttributesEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        checkSealed();
        this.enabled = enabled;
    }
}
