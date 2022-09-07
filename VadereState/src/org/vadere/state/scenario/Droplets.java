package org.vadere.state.scenario;

import org.jetbrains.annotations.NotNull;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.scenario.AttributesDroplets;
import org.vadere.state.types.ScenarioElementType;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VPolygon;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.geometry.shapes.Vector2D;

import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;

public class Droplets extends ParticleDispersion {

    private AttributesDroplets attributes;
    final static int numberOfCircularSections = 10; // should not be lower; lower values cause edges when approximating a circular segment if angleOfSpreadInDeg in AttributesAirTransmissionModelDroplets is large

    // Constructors
    public Droplets() {
        this(new AttributesDroplets());
    }

    public Droplets(@NotNull AttributesDroplets attributes) {
        this.attributes = attributes;
    }

    // Getter
    @Override
    public VShape getShape() {
        return attributes.getShape();
    }

    @Override
    public int getId() {
        return attributes.getId();
    }

    @Override
    public ScenarioElementType getType() {
        return ScenarioElementType.DROPLETS;
    }

    @Override
    public Attributes getAttributes() {
        return attributes;
    }

    public VPoint getOrigin() {
        return attributes.getOrigin();
    }

    public Vector2D getDirection() {
        return attributes.getDirection();
    }

    public double getCreationTime() { return attributes.getCreationTime(); }

       // Setter
    @Override
    public void setShape(VShape newShape) {
        attributes.setShape(newShape);
    }

    @Override
    public void setAttributes(Attributes attributes) {
        this.attributes = (AttributesDroplets) attributes;
    }

    public void setId(int id) {
        ((AttributesDroplets) getAttributes()).setId(id);
    }

    // Other methods
    @Override
    public Droplets clone() {
        return new Droplets(((AttributesDroplets) attributes.clone()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((attributes == null) ? 0 : attributes.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Droplets)) {
            return false;
        }
        Droplets other = (Droplets) obj;
        if (attributes == null) {
            return other.attributes == null;
        } else return attributes.equals(other.attributes);
    }

    public static VShape createTransformedDropletsShape(VPoint origin, Vector2D direction, double radius, double centralAngleInDeg) {
        VPolygon shape;

        double centralAngleInRad = Math.toRadians(centralAngleInDeg);

        Path2D path = new Path2D.Double();
        path.moveTo(0, 0); // define stating point
        for (double angle = 0.0; angle <= centralAngleInRad; angle += centralAngleInRad / numberOfCircularSections) {
            path.lineTo(Math.cos(angle) * radius, Math.sin(angle) * radius);
        }
        path.closePath();
        VShape polygon = new VPolygon(path);
        double theta = direction.angleToZero(); // get orientation of shape

        AffineTransform transform = new AffineTransform();
        transform.translate(origin.x, origin.y);
        transform.rotate(theta - centralAngleInRad / 2.0);

        shape = new VPolygon(transform.createTransformedShape(polygon));

        return shape;
    }
}

