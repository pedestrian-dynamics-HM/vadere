package org.vadere.state.attributes.scenario;

import org.vadere.state.attributes.models.infection.AttributesAirTransmissionModel;
import org.vadere.state.scenario.AerosolCloud;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VShape;

/**
 * This class defines the attributes of an {@link AerosolCloud}.
 */
public class AttributesAerosolCloud extends AttributesParticleDispersion {

    private double radius;
    private VPoint center;

    // Constructors
    public AttributesAerosolCloud() {
        super();
        this.radius = new AttributesAirTransmissionModel().getAerosolCloudInitialRadius();
        this.center = new VPoint();
    }

    public AttributesAerosolCloud(int id, double radius, VPoint center, double currentPathogenLoad) {
        super(id, currentPathogenLoad, AerosolCloud.createAerosolCloudShape(center, radius));
        this.radius = radius;
        this.center = center;
    }

    public AttributesAerosolCloud(int id, double radius, VPoint center, double creationTime, double currentPathogenLoad) {
        super(id, creationTime, currentPathogenLoad, AerosolCloud.createAerosolCloudShape(center, radius));
        this.radius = radius;
        this.center = center;
    }

    // Getter
    public double getRadius() {
        return radius;
    }

    public VPoint getCenter() {
        return center;
    }

    public double getArea() {
        return radius * radius * Math.PI;
    }

    public double getVolume() {
        return AttributesAerosolCloud.radiusToVolume(radius);
    }

    /*
     * Assumption: The aerosolCloud is spherical in 3D and circular in 2D
     */
    public static double radiusToVolume(double anyRadius) {
        return 4.0 / 3.0 * Math.pow(anyRadius, 3) * Math.PI;
    }

    public double getPathogenConcentration() {
        return getCurrentPathogenLoad() / getVolume();
    }

    // Setter
    public void setRadius(double radius) {
        this.setShape(AerosolCloud.createAerosolCloudShape(this.center, radius));
    }

    public void setCenter(VPoint center) {
        this.setShape(AerosolCloud.createAerosolCloudShape(center, this.radius));
    }

    public void setShape(VCircle circle) {
        this.setShape((VShape) circle);
        this.radius = circle.getRadius();
        this.center = circle.getCenter();
    }
}