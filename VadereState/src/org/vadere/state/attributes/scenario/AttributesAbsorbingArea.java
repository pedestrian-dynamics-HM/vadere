package org.vadere.state.attributes.scenario;

import org.vadere.state.attributes.AttributesEmbedShape;
import org.vadere.util.geometry.shapes.VShape;

/**
 * Attributes of an absorbing area, used by "AbsorbingAreaController" during simulation.
 */
public class AttributesAbsorbingArea extends AttributesEmbedShape {

    // Variables
    private int id = ID_NOT_SET;

    /**
     * Shape and position.
     */
    private VShape shape;
    /**
     * Within this distance, pedestrians have reached the absorbing area.
     */
    private double deletionDistance = 0.0;

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

    // Getters
    public int getId() {
        return id;
    }

    @Override
    public VShape getShape() {
        return shape;
    }

    public double getDeletionDistance() {
        return deletionDistance;
    }

    // Setters
    public void setId(int id) {
        checkSealed();
        this.id = id;
    }

    @Override
    public void setShape(VShape shape) {
        this.shape = shape;
    }

    public void setDeletionDistance(double deletionDistance) {
        checkSealed();
        this.deletionDistance = deletionDistance;
    }

}
