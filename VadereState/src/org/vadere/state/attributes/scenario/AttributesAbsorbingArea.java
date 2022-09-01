package org.vadere.state.attributes.scenario;

import org.vadere.state.attributes.AttributesEmbedShape;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.reflection.VadereAttribute;

/**
 * Attributes of an absorbing area, used by "AbsorbingAreaController" during simulation.
 */
public class AttributesAbsorbingArea extends AttributesVisualElement {
    @VadereAttribute
    protected Double deletionDistance = 0.0;

    // Constructors
    public AttributesAbsorbingArea() {
    }

    public AttributesAbsorbingArea(final VShape shape) {
        this.shape = shape;
    }

    public AttributesAbsorbingArea(final VShape shape, final int id) {
        this.shape = shape;
        this.id = id;
    }

    public AttributesAbsorbingArea(final VShape shape, final int id, double deletionDistance) {
        this.shape = shape;
        this.id = id;
        this.deletionDistance = deletionDistance;
    }

    public double getDeletionDistance() {
        return deletionDistance;
    }

    public void setDeletionDistance(double deletionDistance) {
        checkSealed();
        this.deletionDistance = deletionDistance;
    }

}
