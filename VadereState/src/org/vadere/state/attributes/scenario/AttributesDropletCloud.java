package org.vadere.state.attributes.scenario;

import org.vadere.state.scenario.DropletCloud;
import org.vadere.util.geometry.shapes.VShape;

import java.util.ArrayList;

/**
 * This class defines the attributes of a {@link DropletCloud}.
 */
public class AttributesDropletCloud extends AttributesAerosolParticles {

    private VShape shape;
    private double lifeTime;

    // Constructors
    public AttributesDropletCloud() {
        super();
        // ToDo this.shape = some circularSector ...
    }

    public AttributesDropletCloud(VShape shape, double lifeTime) {
        super();
        this.shape = shape;
        this.lifeTime = lifeTime;
    }

    public AttributesDropletCloud(int id, VShape shape, double creationTime, double lifeTime, double initialPathogenLoad) {
        super(id, creationTime, initialPathogenLoad, initialPathogenLoad);
        this.shape = shape;
        this.lifeTime = lifeTime;
    }

    // Getter
    public VShape getShape() {
        return shape;
    }

    public double getLifeTime() { return lifeTime; }

    // Setter
    public void setShape(VShape shape) {
        this.shape = shape;
    }
}
