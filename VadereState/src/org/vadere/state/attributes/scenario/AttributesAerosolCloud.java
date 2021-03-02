package org.vadere.state.attributes.scenario;

import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.AttributesEmbedShape;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VShape;


public class AttributesAerosolCloud extends AttributesEmbedShape {

    private int id;
    private VCircle circle;
    private double creationTime;
    private double pathogenLoad;
    final private double lifeTime;

    // Constructors
    // ToDo: check if other constructors are necessary (-> BK)
    public AttributesAerosolCloud(VPoint pos, double creationTime){
        this.id = AttributesEmbedShape.ID_NOT_SET;
        this.circle = new VCircle(pos, 0.75);
        this.creationTime = creationTime;
        this.pathogenLoad = -1;
        this.lifeTime = 60 * 15;
    }

    // Getter
    public int getId() {
        checkSealed();
        return id;
    }


    @Override
    public VCircle getShape() { // ToDo: check if this works instead of public VShape getShape() {return shape;}
        return circle;
    }
    public double getRadius() { return circle.getRadius(); }
    public double getLifeTime() { return lifeTime; }
    public double getCreationTime() { return creationTime; }
    public double getPathogenLoad() { return pathogenLoad; }

    // Setter
    // ToDo: check if this works instead of public void setShape(VShape shape) {this.shape = shape; }; is it necessary to override setShape(VShape shape) from superclass?
    public void setShape(VCircle circle) {
        this.circle = circle;
    }

    public void setId(int id) { this.id = id; }
    public void setCreationTime(double creationTime) { this.creationTime = creationTime; }
    public void setPathogenLoad(double pathogenLoad) { this.pathogenLoad = pathogenLoad; }
    // ToDo: implement setRadius -> modify circle
}