package org.vadere.state.attributes.scenario;

import org.vadere.state.scenario.Droplets;
import org.vadere.util.geometry.shapes.VShape;

/**
 * This class defines the attributes of a {@link Droplets}.
 */
public class AttributesDroplets extends AttributesParticleDispersion {

    private VShape shape;
    private double lifeTime;

    // Constructors
    public AttributesDroplets() {
        super();
        // ToDo this.shape = some circularSector ...
    }

    public AttributesDroplets(VShape shape, double lifeTime) {
        super();
        this.shape = shape;
        this.lifeTime = lifeTime;
    }

    public AttributesDroplets(int id, VShape shape, double creationTime, double lifeTime, double initialPathogenLoad) {
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
