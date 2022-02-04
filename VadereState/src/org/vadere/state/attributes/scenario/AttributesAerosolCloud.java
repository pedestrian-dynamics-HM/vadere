package org.vadere.state.attributes.scenario;

import org.vadere.state.scenario.AerosolCloud;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VShape;

/**
 * This class defines the attributes of an {@link AerosolCloud}.
 */
public class AttributesAerosolCloud extends AttributesParticleDispersion {

    private double radius;
    private VPoint center;
    private double halfLife;

    // Constructors
    public AttributesAerosolCloud() {
        super();
        this.radius = 1.5;
        this.center = new VPoint(0, 0);
        this.halfLife = 600;
    }

    public AttributesAerosolCloud(VShape shape, double creationTime){
        super(creationTime, shape);
    }

    public AttributesAerosolCloud(int id, double radius, VPoint center, double currentPathogenLoad){
        super(id, AerosolCloud.createAerosolCloudShape(center, radius), currentPathogenLoad);
    }

    public AttributesAerosolCloud(int id, double radius, VPoint center, double creationTime, double halfLife, double initialPathogenLoad, double currentPathogenLoad) {
        super(id, creationTime, initialPathogenLoad, currentPathogenLoad, AerosolCloud.createAerosolCloudShape(center, radius));
        this.radius = radius;
        this.center = center;
        this.halfLife = halfLife;
    }

    // Getter
    public double getRadius() {
        return radius;
    }

    public VPoint getCenter() {
        return center;
    }

    public double getHalfLife() { return halfLife; }

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
        this.radius = radius;
    }

    public void setCenter(VPoint center) {
        this.center = center;
    }
}