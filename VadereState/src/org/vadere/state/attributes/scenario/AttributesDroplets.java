package org.vadere.state.attributes.scenario;

import org.vadere.state.scenario.Droplets;
import org.vadere.util.geometry.shapes.VShape;

/**
 * This class defines the attributes of a {@link Droplets}.
 */
public class AttributesDroplets extends AttributesParticleDispersion {

    //private VShape shape;

    // Constructors
    public AttributesDroplets() {
        super();
        // ToDo this.shape = some circularSector ...
    }


    public AttributesDroplets(int id, VShape shape, double creationTime, double initialPathogenLoad) {
        super(id, creationTime, initialPathogenLoad, initialPathogenLoad, shape);
    }
}
