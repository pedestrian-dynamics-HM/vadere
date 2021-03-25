package org.vadere.state.attributes.scenario;

import org.vadere.state.attributes.AttributesEmbedShape;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VShape;


public class AttributesAerosolCloud extends AttributesEmbedShape {

    private int id;
    private VShape shape;
    private double creationTime;
    private double pathogenDensity; // assumption: same density along z-axis
    final private double lifeTime;
    private boolean hasReachedLifeEnd;

    // Constructors
    public AttributesAerosolCloud() {
        this.id = AttributesEmbedShape.ID_NOT_SET;
        this.shape = new VCircle(0.75);
        this.creationTime = -1;
        this.pathogenDensity = -1;
        this.lifeTime = 60 * 15;
        this.hasReachedLifeEnd = false;
    }

    public AttributesAerosolCloud(VShape shape, double creationTime){
        this();
        this.shape = shape;
        this.creationTime = creationTime;
    }

    public AttributesAerosolCloud(int id, VShape shape, double creationTime, double pathogenDensity, double lifeTime, boolean hasReachedLifeEnd) {
        this.id = id;
        this.shape = shape;
        this.creationTime = creationTime;
        this.pathogenDensity = pathogenDensity;
        this.lifeTime = lifeTime;
        this.hasReachedLifeEnd = hasReachedLifeEnd;
    }

    // Getter
    public int getId() { return id; }

    @Override
    public VShape getShape() { // ToDo: check if this works instead of public VShape getShape() {return shape;}
        return shape;
    }

    public double getLifeTime() { return lifeTime; }

    public double getCreationTime() { return creationTime; }

    public double getPathogenDensity() { return pathogenDensity; }

    public boolean getHasReachedLifeEnd() { return hasReachedLifeEnd; }

    // Setter
    public void setId(int id) {
        checkSealed();
        this.id = id;
    }

    @Override
    public void setShape(VShape shape) {
        this.shape = shape;
    }

    public void setCreationTime(double creationTime) { this.creationTime = creationTime; }

    public void setPathogenDensity(double pathogenDensity) { this.pathogenDensity = pathogenDensity; }

    public void setHasReachedLifeEnd(boolean hasReachedLifeEnd) { this.hasReachedLifeEnd = hasReachedLifeEnd; }
}