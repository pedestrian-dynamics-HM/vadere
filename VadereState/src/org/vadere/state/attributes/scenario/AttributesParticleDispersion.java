package org.vadere.state.attributes.scenario;

import org.vadere.state.attributes.Attributes;
import org.vadere.util.geometry.shapes.VPolygon;
import org.vadere.util.geometry.shapes.VShape;

import java.awt.geom.Path2D;


public abstract class AttributesParticleDispersion extends AttributesVisualElement {

    private int id;
    private double creationTime;
    private double currentPathogenLoad;
    private VShape shape;

    public AttributesParticleDispersion() {
        this.id = Attributes.ID_NOT_SET;
        this.creationTime = -1;
        this.currentPathogenLoad = -1;

        Path2D path = new Path2D.Double();
        path.moveTo(0,0);
        this.shape = new VPolygon(path);
    }

    public AttributesParticleDispersion(int id, double currentPathogenLoad, VShape shape) {
        this();
        this.id = id;
        this.currentPathogenLoad = currentPathogenLoad;
        this.shape = shape;
    }

    public AttributesParticleDispersion(int id, double creationTime, double currentPathogenLoad, VShape shape) {
        this.id = id;
        this.creationTime = creationTime;
        this.currentPathogenLoad = currentPathogenLoad;
        this.shape = shape;
    }

    public int getId() { return id; }

    public double getCreationTime() {
        return creationTime;
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
