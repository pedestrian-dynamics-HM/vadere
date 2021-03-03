package org.vadere.state.scenario;

import org.jetbrains.annotations.NotNull;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.scenario.AttributesAerosolCloud;
import org.vadere.state.types.ScenarioElementType;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VShape;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

public class AerosolCloud extends ScenarioElement {

    private AttributesAerosolCloud attributes;

    // ToDo: implement AerosolCloudListener (or remove commented code)
    // private final Collection<AerosolCloudListener> aerosolCloudListeners = new LinkedList<>();

    // Constructors
    public AerosolCloud() { this(new AttributesAerosolCloud()); }

    public AerosolCloud(@NotNull AttributesAerosolCloud attributes) {
        this.attributes = attributes;
    }

    public AerosolCloud(AerosolCloud aerosolCloud){
        this(new AttributesAerosolCloud(aerosolCloud.getId(), aerosolCloud.getShape(), aerosolCloud.getCreationTime(),
                aerosolCloud.getPathogenLoad(), aerosolCloud.getLifeTime()));
    }


    // Getter
    @Override
    public VShape getShape() {     // ToDo check of one must use VShape instead -> attributesAerosolCloud
        return attributes.getShape();
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

    public double getLifeTime() { return attributes.getLifeTime(); }
    public double getCreationTime() { return attributes.getCreationTime(); }
    public double getPathogenLoad() { return attributes.getPathogenLoad(); }

    // Setter
    @Override
    public void setShape(VShape newShape) {
        attributes.setShape(newShape);
    }

    @Override
    public void setAttributes(Attributes attributes) {
        this.attributes = (AttributesAerosolCloud) attributes;
    }

    public void setId(int id){
        ((AttributesAerosolCloud)getAttributes()).setId(id);
    }

    public void setCreationTime(double creationTime) { attributes.setCreationTime(creationTime); }
    public void setPathogenLoad(double pathogenLoad) { attributes.setPathogenLoad(pathogenLoad); }


    // Other methods
    public void changeExtent(int dimension, double scalingFactor1D) {
        double scalingFactorInDimension = Math.pow(scalingFactor1D, dimension);
        // setShape(new VShape(getShape().getCentroid(), getShape()."extentInDimension" * scalingFactorInDimension); // increase extent
        setPathogenLoad(getPathogenLoad() / scalingFactorInDimension); // reduce pathogenLoad (density)
    }

    @Override
    public ScenarioElement clone() {
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
            if (other.attributes != null) {
                return false;
            }
        } else if (!attributes.equals(other.attributes)) {
            return false;
        }
        return true;
    }


}
