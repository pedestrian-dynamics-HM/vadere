package org.vadere.state.attributes.scenario;

import org.vadere.state.scenario.TargetChangerAlgorithmType;
import org.vadere.util.geometry.shapes.VShape;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Attributes of "TargetChanger" object, used by "TargetChangerController" during simulation.
 */
public class AttributesTargetChanger extends AttributesVisualElement {

    /**
     * Within this distance, pedestrians have reached this area.
     */
    private Double reachDistance = 0.0;
    /**
     * Select TargetChangerAlgorithm {@link TargetChangerAlgorithmType}:
     * <ul>
     *   <li><b>FOLLOW_PERSON:</b> Interpret first item of {@link #nextTarget} as pedestrian id and
     *   use this pedestrian as the new target. In case of groups follow the leader. Fallback
     *   behaviour if neither works: Set target list of pedestrian to {@link #nextTarget}.
     *   Only first element of {@link #probabilityToChangeTarget} will be used.</li>
     *   <li><b>SELECT_LIST:</b> Set the complete list of {@link #nextTarget} as the new target list
     *   of pedestrian. {@link #probabilityToChangeTarget} must have 1 element only.</li>
     *   <li><b>SELECT_ELEMENT:</b> Select *one* element of {@link #nextTarget} as the new target of
     *   the pedestrian.
     *   If {@link #probabilityToChangeTarget} is empty one element of {@link #nextTarget}
     *   will be selected with a uniform distribution. If the length of {@link #probabilityToChangeTarget}
     *   matches with {@link #nextTarget} use the relative select element based on relative probability
     *   given by {@link #probabilityToChangeTarget}. nextTarget = [1, 2, 3]
     *   E.g. If @probabilitiesToChangeTarget = [ 10, 20, 10 ] then the new target list will be
     *   [1] in 25% of the cases, [2] in 50% and [3] in 25%.</li>
     *   <li><b>SORTED_SUB_LIST:</b> The length of {@link #nextTarget} and @link #probabilityToChangeTarget} must
     *   match. For each element in {@link #nextTarget} a bernoulli sample is drawn based on the given
     *   parameters in {@link #probabilityToChangeTarget} with the same index. If the draw is successful
     *   add the element to the new target list and repeat for all elements in {@link #nextTarget}</li>
     *  </ul>
     */
    private TargetChangerAlgorithmType changeAlgorithmType = TargetChangerAlgorithmType.SELECT_LIST;

    /**
     * If "nextTargetIsPedestrian == true", then randomly select a pedestrian which
     * is heading to the given target as new target. Otherwise, use the given target
     * id as "normal" target id.
     */
    private List<Integer> nextTarget = new LinkedList<>();
    /**
     * Change target of a given pedestrian only with a certain probability between
     * 0 and 1.
     */
    private List<Double> probabilityToChangeTarget = new LinkedList<>(Arrays.asList(1.0));



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
        // check if sum of probabilities add up to 1 is moved to specific algorithms.
        this.probabilityToChangeTarget = probabilitiesToChangeTarget;
    }

    public double getReachDistance() {
        return reachDistance;
    }

    public TargetChangerAlgorithmType getChangeAlgorithmType() {
        return changeAlgorithmType;
    }

    public void setChangeAlgorithmType(TargetChangerAlgorithmType changeAlgorithmType) {
        this.changeAlgorithmType = changeAlgorithmType;
    }

    public LinkedList<Integer> getNextTarget() {
        if(!nextTarget.getClass().isAssignableFrom(LinkedList.class)){
            nextTarget = new LinkedList<>(nextTarget);
        }
        return (LinkedList<Integer>) nextTarget;
    }

    public LinkedList<Double> getProbabilitiesToChangeTarget() {
        if(!probabilityToChangeTarget.getClass().isAssignableFrom(LinkedList.class)){
            probabilityToChangeTarget = new LinkedList<>(probabilityToChangeTarget);
        }
        return (LinkedList<Double>) probabilityToChangeTarget;
    }

    // Setters

    public void setReachDistance(double reachDistance) {
        checkSealed();
        this.reachDistance = reachDistance;
    }


    public void setNextTarget(LinkedList<Integer> nextTarget) {
        this.nextTarget = nextTarget;
    }

    public void setProbabilitiesToChangeTarget(LinkedList<Double> probabilitiesToChangeTarget) {

//        for (Double probabilityToChangeTarget : probabilitiesToChangeTarget){
//
//            if (probabilityToChangeTarget < 0.0 || probabilityToChangeTarget > 1.0) {
//                throw new IllegalArgumentException("Probability must be in range 0.0 to 1.0!");
//            }
//        }
        this.probabilityToChangeTarget = probabilitiesToChangeTarget;
    }

}
