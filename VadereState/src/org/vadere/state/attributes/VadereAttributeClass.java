package org.vadere.state.attributes;

public @interface VadereAttributeClass {
    boolean includeAll() default true;
    boolean noHeader() default false;
}
