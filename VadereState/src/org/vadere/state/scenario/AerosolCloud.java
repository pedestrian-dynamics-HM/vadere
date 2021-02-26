package org.vadere.state.scenario;

import org.jetbrains.annotations.NotNull;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.scenario.AttributesAerosolCloud;
import org.vadere.state.types.ScenarioElementType;
import org.vadere.util.geometry.shapes.VShape;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

public class AerosolCloud extends ScenarioElement {

    private AttributesAerosolCloud attributes;

    private final Collection<AerosolCloudListener> aerosolCloudListeners = new LinkedList<>();

    // Constructors
    // TODO: Check constructors -> BK / SS ?
    public AerosolCloud() { this(new AttributesAerosolCloud()); }

    public AerosolCloud(@NotNull AttributesAerosolCloud attributes) { this.attributes = attributes; }

//    public AttributesAerosolCloud(AerosolCloud aerosolCloud) {
//        this(new AttributesAerosolCloud(aerosolCloud.getId(), aerosolCloud.getShape(), aerosolCloud.getAerosolPersistenceTime(), aerosolCloud.getAerosolPersistenceStart(), aerosolCloud.getAerosolCloudDiameter()));
//    }

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
        return ScenarioElementType.AEROSOL_CLOUD;
    }

    @Override
    public Attributes getAttributes() {
        return attributes;
    }

    public double getAerosolCloudRadius() { return attributes.getAerosolCloudRadius(); }
    public double getAerosolPersistenceTime() { return attributes.getAerosolPersistenceTime(); }
    public double getAerosolPersistenceStart() { return attributes.getAerosolPersistenceStart(); }

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

    // Other methods
    @Override
    public ScenarioElement clone() {
        return new AerosolCloud(((AttributesAerosolCloud) attributes.clone()));
    }

    /** Models can register a target listener. */
    public void addListener(AerosolCloudListener listener) {
        aerosolCloudListeners.add(listener);
    }

    public boolean removeListener(AerosolCloudListener listener) {
        return aerosolCloudListeners.remove(listener);
    }

    /** Returns an unmodifiable collection. */
    public Collection<AerosolCloudListener> getAerosolCloudListeners() {
        return Collections.unmodifiableCollection(aerosolCloudListeners);
    }

    // TODO: check BK /SS ?
//    @Override
//    public int hashCode() {
//        final int prime = 31;
//        int result = 1;
//        result = prime * result + ((attributes == null) ? 0 : attributes.hashCode());
//        return result;
//    }
//
//    @Override
//    public boolean equals(Object obj) {
//        if (this == obj) {
//            return true;
//        }
//        if (obj == null) {
//            return false;
//        }
//        if (!(obj instanceof AerosolCloud)) {
//            return false;
//        }
//        AerosolCloud other = (AerosolCloud) obj;
//        if (attributes == null) {
//            if (other.attributes != null) {
//                return false;
//            }
//        } else if (!attributes.equals(other.attributes)) {
//            return false;
//        }
//        return true;
//    }


}
