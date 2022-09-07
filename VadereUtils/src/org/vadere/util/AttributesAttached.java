package org.vadere.util;

public abstract class AttributesAttached<T extends Attributes> {
    protected T attributes;
    public T getAttributes(){return attributes;}

    public void setAttributes(T attributes){this.attributes = attributes;}
}
