package org.vadere.state.attributes;

import org.vadere.util.reflection.VadereAttribute;


@VadereAttributeClass(noHeader = true)
public class AttributesEnabled extends Attributes {
    @VadereAttribute
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
