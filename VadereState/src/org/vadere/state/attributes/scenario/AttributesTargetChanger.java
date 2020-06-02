package org.vadere.state.attributes.scenario;

import org.vadere.state.attributes.AttributesEmbedShape;
import org.vadere.util.geometry.shapes.VShape;

import java.util.Arrays;
import java.util.LinkedList;

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
    private LinkedList<Integer> nextTarget = new LinkedList<>();
    /**
     * Change target of a given pedestrian only with a certain probability between
     * 0 and 1.
     */
    private LinkedList<Double> probabilityToChangeTarget = new LinkedList<Double>(Arrays.asList(1.0));



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

    public AttributesTargetChanger(final VShape shape, final int id, double reachDistance, LinkedList<Integer> nextTarget, LinkedList<Double> probabilitiesToChangeTarget) {
        this.shape = shape;
        this.id = id;
        this.reachDistance = reachDistance;
        this.nextTarget = nextTarget;
        for (Double probabilityToChangeTarget : probabilitiesToChangeTarget){

            if (probabilityToChangeTarget < 0.0 || probabilityToChangeTarget > 1.0) {
                throw new IllegalArgumentException("Probability must be in range 0.0 to 1.0!");
            }
        }

        this.probabilityToChangeTarget = probabilitiesToChangeTarget;
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

    public LinkedList<Integer> getNextTarget() {
        return nextTarget;
    }

    public LinkedList<Double> getProbabilitiesToChangeTarget() {
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

    public void setNextTarget(LinkedList<Integer> nextTarget) {
        this.nextTarget = nextTarget;
    }

    public void setProbabilitiesToChangeTarget(LinkedList<Double> probabilitiesToChangeTarget) {

        for (Double probabilityToChangeTarget : probabilitiesToChangeTarget){

            if (probabilityToChangeTarget < 0.0 || probabilityToChangeTarget > 1.0) {
                throw new IllegalArgumentException("Probability must be in range 0.0 to 1.0!");
            }
        }
        this.probabilityToChangeTarget = probabilitiesToChangeTarget;
    }

}
