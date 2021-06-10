package org.vadere.state.attributes.scenario;

import org.vadere.state.attributes.AttributesEmbedShape;


public abstract class AttributesAerosolParticles extends AttributesEmbedShape {

    private int id;
    private double creationTime;
    private double initialPathogenLoad;
    private double currentPathogenLoad;

    public AttributesAerosolParticles() {
        this.id = AttributesEmbedShape.ID_NOT_SET;
        this.creationTime = -1;
        this.initialPathogenLoad = -1;
        this.currentPathogenLoad = -1;
    }

    public AttributesAerosolParticles(double creationTime){
        this();
        this.creationTime = creationTime;
    }

    public AttributesAerosolParticles(int id, double creationTime, double initialPathogenLoad, double currentPathogenLoad) {
        this.id = id;

        this.creationTime = creationTime;
        this.initialPathogenLoad = initialPathogenLoad;
        this.currentPathogenLoad = currentPathogenLoad;
    }

    public int getId() { return id; }

    public double getCreationTime() {
        return creationTime;
    }


    public double getInitialPathogenLoad() {
        return initialPathogenLoad;
    }

    public double getCurrentPathogenLoad() {
        return currentPathogenLoad;
    }

    public void setId(int id) {
        checkSealed();
        this.id = id;
    }

    public void setCreationTime(double creationTime) {
        this.creationTime = creationTime;
    }

    public void setCurrentPathogenLoad(double currentPathogenLoad) {
        this.currentPathogenLoad = currentPathogenLoad;
    }
}
