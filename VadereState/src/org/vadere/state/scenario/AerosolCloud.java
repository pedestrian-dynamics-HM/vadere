package org.vadere.state.scenario;

import org.jetbrains.annotations.NotNull;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.scenario.AttributesAerosolCloud;
import org.vadere.state.types.ScenarioElementType;
import org.vadere.util.geometry.shapes.*;

/**
 * AerosolCloud represents one medium of transmission how pathogen can spread among
 * pedestrians.
 */
public class AerosolCloud extends ParticleDispersion {

    private AttributesAerosolCloud attributes;

    // ToDo: implement AerosolCloudListener (or remove commented code)
    // private final Collection<AerosolCloudListener> aerosolCloudListeners = new LinkedList<>();


    // Constructors
    public AerosolCloud() {
        this(new AttributesAerosolCloud());
    }

    public AerosolCloud(@NotNull AttributesAerosolCloud attributes) {
        this.attributes = attributes;
    }

    // Getter
    @Override
    public VShape getShape() {
        return attributes.getShape();
    }

    public double getArea() {
        return attributes.getArea();
    }

    public double getVolume() {
        return attributes.getVolume();
    }

    public static double radiusToVolume(double radius) {
        return AttributesAerosolCloud.radiusToVolume(radius);
    }

    public double getPathogenConcentration() {
        return attributes.getPathogenConcentration();
    }

    @Override
    public int getId() {
        return attributes.getId();
    }

    @Override
    public ScenarioElementType getType() {
        return ScenarioElementType.AEROSOL_CLOUD;
    }

    @Override
    public Attributes getAttributes() {
        return attributes;
    }

    public double getRadius() {
        return attributes.getRadius();
    }

    public VPoint getCenter() {
        return attributes.getCenter();
    }

    public double getCreationTime() {
        return attributes.getCreationTime();
    }

    public double getCurrentPathogenLoad() {
        return attributes.getCurrentPathogenLoad();
    }

    // Setter
    @Override
    public void setShape(VShape newShape) {
        attributes.setShape(newShape);
    }

    @Override
    public void setAttributes(Attributes attributes) {
        this.attributes = (AttributesAerosolCloud) attributes;
    }

    public void setId(int id) {
        ((AttributesAerosolCloud) getAttributes()).setId(id);
    }

    public void setCreationTime(double creationTime) {
        attributes.setCreationTime(creationTime);
    }

    public void setCurrentPathogenLoad(double currentPathogenLoad) {
        attributes.setCurrentPathogenLoad(currentPathogenLoad);
    }

    // Other methods
    @Override
    public AerosolCloud clone() {
        return new AerosolCloud(((AttributesAerosolCloud) attributes.clone()));
    }

    // ToDo: implement AerosolCloudListener (or remove commented code)
//    /** Models can register a target listener. */
//    public void addListener(AerosolCloudListener listener) {
//        aerosolCloudListeners.add(listener);
//    }
//
//    public boolean removeListener(AerosolCloudListener listener) {
//        return aerosolCloudListeners.remove(listener);
//    }
//
//    /** Returns an unmodifiable collection. */
//    public Collection<AerosolCloudListener> getAerosolCloudListeners() {
//        return Collections.unmodifiableCollection(aerosolCloudListeners);
//    }


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
        if (!(obj instanceof AerosolCloud)) {
            return false;
        }
        AerosolCloud other = (AerosolCloud) obj;
        if (attributes == null) {
            return other.attributes == null;
        } else return attributes.equals(other.attributes);
    }

    /*
     * This method increases the shape of a circular aerosolCloud about deltaRadius;
     */
    public void increaseShape(double deltaRadius) {
        if (deltaRadius > 0.0) {

            double newRadius = attributes.getRadius() + deltaRadius;
            VPoint center = attributes.getCenter();

            // define new cross-sectional area
            attributes.setRadius(newRadius);

            // define shape
            VCircle newShape = createAerosolCloudShape(center, newRadius);
            attributes.setShape(newShape);
        }
    }

    /*
     * The 2D representation of the spherical aerosol clouds is a circle (cross-sectional area of the cloud at the
     * agents' heads)
     */
    public static VCircle createAerosolCloudShape(VPoint center, double radius) {
        return new VCircle(center, radius);
    }
}
