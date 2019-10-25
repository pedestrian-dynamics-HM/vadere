package org.vadere.state.attributes.scenario;

import org.vadere.state.attributes.AttributesEmbedShape;
import org.vadere.util.geometry.shapes.VShape;

/**
 * Attributes of "TargetChanger" object, used by "TargetChangerController" during simulation.
 */
public class AttributesTargetChanger extends AttributesEmbedShape {

    // Variables
    private int id = ID_NOT_SET;

    /**
     * Shape and position.
     */
    private VShape shape;
    /**
     * Within this distance, pedestrians have reached this area.
     */
    private double reachDistance = 0.0;
    /**
     * Define if "nextTarget" should be treated as pedestrian.
     */
    private boolean nextTargetIsPedestrian = false;
    /**
     * If "nextTargetIsPedestrian == true", then randomly select a pedestrian which
     * is heading to the given target as new target. Otherwise, use the given target
     * id as "normal" target id.
     */
    private int nextTarget = ID_NOT_SET;
    /**
     * Change target of a given pedestrian only with a certain probability between
     * 0 and 1.
     */
    private double probabilityToChangeTarget = 0.0;

    // Constructors
    public AttributesTargetChanger() {
    }

    public AttributesTargetChanger(final VShape shape) {
        this.shape = shape;
    }

    public AttributesTargetChanger(final VShape shape, final int id) {
        this.shape = shape;
        this.id = id;
    }

    public AttributesTargetChanger(final VShape shape, final int id, double reachDistance, int nextTarget, double probabilityToChangeTarget) {
        this.shape = shape;
        this.id = id;
        this.reachDistance = reachDistance;
        this.nextTarget = nextTarget;

        if (probabilityToChangeTarget < 0.0 || probabilityToChangeTarget > 1.0) {
            throw new IllegalArgumentException("Probability must be in range 0.0 to 1.0!");
        }
        this.probabilityToChangeTarget = probabilityToChangeTarget;
    }

    // Getters
    public int getId() {
        return id;
    }

    @Override
    public VShape getShape() {
        return shape;
    }

    public double getReachDistance() {
        return reachDistance;
    }

    public boolean isNextTargetIsPedestrian() {
        return nextTargetIsPedestrian;
    }

    public int getNextTarget() {
        return nextTarget;
    }

    public double getProbabilityToChangeTarget() {
        return probabilityToChangeTarget;
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

    public void setReachDistance(double reachDistance) {
        checkSealed();
        this.reachDistance = reachDistance;
    }

    public void setNextTargetIsPedestrian(boolean nextTargetIsPedestrian) {
        this.nextTargetIsPedestrian = nextTargetIsPedestrian;
    }

    public void setNextTarget(int nextTarget) {
        this.nextTarget = nextTarget;
    }

    public void setProbabilityToChangeTarget(double probabilityToChangeTarget) {
        if (probabilityToChangeTarget < 0.0 || probabilityToChangeTarget > 1.0) {
            throw new IllegalArgumentException("Probability must be in range 0.0 to 1.0!");
        }
        this.probabilityToChangeTarget = probabilityToChangeTarget;
    }

}
