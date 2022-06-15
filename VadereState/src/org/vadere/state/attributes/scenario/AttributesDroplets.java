package org.vadere.state.attributes.scenario;

import org.vadere.state.attributes.models.infection.AttributesAirTransmissionModelDroplets;
import org.vadere.state.scenario.Droplets;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.Vector2D;

/**
 * This class defines the attributes of a {@link Droplets}.
 */
public class AttributesDroplets extends AttributesParticleDispersion {

    private VPoint origin;
    private Vector2D direction;

    // Constructors
    public AttributesDroplets() {
        super();
        this.setShape(new VPoint(), new Vector2D(), new AttributesAirTransmissionModelDroplets().getDistanceOfSpread(), new AttributesAirTransmissionModelDroplets().getAngleOfSpreadInDeg());
    }

    public AttributesDroplets(int id, double creationTime, double pathogenLoad, VPoint origin, Vector2D direction, double distanceOfSpread, double angleOfSpreadInDeg) {
        super(id, creationTime, pathogenLoad, Droplets.createTransformedDropletsShape(origin,
                direction,
                distanceOfSpread,
                angleOfSpreadInDeg));
        this.origin = origin;
        this.direction = direction;
    }

    public VPoint getOrigin() {
        return origin;
    }

    public Vector2D getDirection() {
        return direction;
    }

    public void setShape(VPoint origin, Vector2D direction, double distanceOfSpread, double angleOfSpreadInDeg) {
        this.setShape(Droplets.createTransformedDropletsShape(origin, direction, distanceOfSpread, angleOfSpreadInDeg));
        this.origin = origin;
        this.direction = direction;
    }
}
