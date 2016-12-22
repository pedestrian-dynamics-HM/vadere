package org.vadere.simulator.projects.dataprocessing.store;

import org.vadere.state.attributes.processor.AttributesProcessor;

public class DataProcessorStore {
    private String type;
    private int id;
    private String attributesType;
    private AttributesProcessor attributes;

    public DataProcessorStore() {
        this.type = "";
        this.id = 0;
        this.attributesType = "";
    }

    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getAttributesType() {
        return this.attributesType;
    }

    public void setAttributesType(String attributesType) {
        this.attributesType = attributesType;
    }

    public AttributesProcessor getAttributes() {
        return this.attributes;
    }

    public void setAttributes(AttributesProcessor attributes) {
        this.attributes = attributes;
    }
}
