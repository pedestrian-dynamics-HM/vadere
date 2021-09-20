package org.vadere.state.attributes.scenario;

import org.vadere.state.attributes.AttributesEmbedShape;
import org.vadere.util.geometry.shapes.VPolygon;
import org.vadere.util.geometry.shapes.VShape;

import java.awt.geom.Path2D;


public abstract class AttributesParticleDispersion extends AttributesEmbedShape {

    private int id;
    private double creationTime;
    private double initialPathogenLoad;
    private double currentPathogenLoad;
    private VShape shape;

    public AttributesParticleDispersion() {
        this.id = AttributesEmbedShape.ID_NOT_SET;
        this.creationTime = -1;
        this.initialPathogenLoad = -1;
        this.currentPathogenLoad = -1;

        Path2D path = new Path2D.Double();
        path.moveTo(0,0);
        this.shape = new VPolygon(path);
    }

    public AttributesParticleDispersion(double creationTime, VShape shape){
        this();
        this.creationTime = creationTime;
        this.shape = shape;
    }

    public AttributesParticleDispersion(int id, VShape shape, double currentPathogenLoad){
        this();
        this.id = id;
        this.shape = shape;
        this.currentPathogenLoad = currentPathogenLoad;
    }

    public AttributesParticleDispersion(int id, double creationTime, double initialPathogenLoad, double currentPathogenLoad, VShape shape) {
        this.id = id;

        this.creationTime = creationTime;
        this.initialPathogenLoad = initialPathogenLoad;
        this.currentPathogenLoad = currentPathogenLoad;
        this.shape = shape;
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

    public VShape getShape() {
        return shape;
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

    public void setShape(VShape shape) {
        this.shape = shape;
    }
}
