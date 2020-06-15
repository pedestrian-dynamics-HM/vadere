package org.vadere.simulator.control.psychology.cognition;

import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.psychology.cognition.SelfCategory;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Target;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The {@link CounterflowCognitionModel} enables a pedestrian to evade if
 * counter-flowing agents are detected. The locomotion layer must implemenet
 * the specific evasion behavior (e.g. tangentially).
 *
 * TODO: Maybe, combine this behavior also with {@link SelfCategory#COOPERATIVE} to avoid
 *   jams.
 */
public class CounterflowCognitionModel implements ICognitionModel {

    private Topography topography;

    @Override
    public void initialize(Topography topography) {
        this.topography = topography;
    }

    @Override
    public void update(Collection<Pedestrian> pedestrians) {
        for (Pedestrian pedestrian : pedestrians) {
            pedestrian.setSelfCategory(SelfCategory.TARGET_ORIENTED);

            if (pedestrian.hasNextTarget()) {
                Pedestrian closestPedestrian = getClosestPedestrianWhichIsCloserToTarget(pedestrian, topography);

                if (closestPedestrian != null) {
                    if (walkingDirectionDiffers(pedestrian, closestPedestrian)) {
                        pedestrian.setSelfCategory(SelfCategory.EVADE);
                    } else if (closestPedestrian.getSelfCategory() == SelfCategory.EVADE) {
                        pedestrian.setSelfCategory(SelfCategory.EVADE); // Imitate behavior
                    }
                }
            }
        }
    }

    private Pedestrian getClosestPedestrianWhichIsCloserToTarget(Pedestrian pedestrian, Topography topography) {
        VPoint positionOfPedestrian = pedestrian.getPosition();
        List<Integer> targetList = pedestrian.getTargets();

        Pedestrian closestPedestrian = null;

        if (targetList.size() > 0) {
            Target target = topography.getTarget(targetList.get(0));
            VPoint targetCentroid = target.getShape().getCentroid();

            List<Pedestrian> closestPedestrians = topography.getSpatialMap(Pedestrian.class)
                    .getObjects(positionOfPedestrian, pedestrian.getAttributes().getSearchRadius());

            // Filter out "me" and pedestrians which are further away from target than "me".
            closestPedestrians = closestPedestrians.stream()
                    .filter(closestPed -> pedestrian.getId() != closestPed.getId())
                    .filter(closestPed -> closestPed.getPosition().distance(targetCentroid) < pedestrian.getPosition().distance(targetCentroid))
                    .collect(Collectors.toList());

            // Sort by distance away from "me".
            closestPedestrians = closestPedestrians.stream()
                    .sorted((pedestrian1, pedestrian2) ->
                            Double.compare(
                                    positionOfPedestrian.distance(pedestrian1.getPosition()),
                                    positionOfPedestrian.distance(pedestrian2.getPosition())
                            ))
                    .collect(Collectors.toList());

            if (closestPedestrians.size() > 0) {
                closestPedestrian = closestPedestrians.get(0);
            }
        }

        return closestPedestrian;
    }

    private boolean walkingDirectionDiffers(Pedestrian pedestrian1, Pedestrian pedestrian2) {
        boolean directionIsDifferent = false;

        double angleInRadian = calculateAngleBetweenWalkingDirections(pedestrian1, pedestrian2, topography);
        if (angleInRadian == -1 || Math.toDegrees(angleInRadian) > pedestrian1.getAttributes().getWalkingDirectionSameIfAngleLessOrEqual()) {
            directionIsDifferent = true;
        }

        return  directionIsDifferent;
    }

    private double calculateAngleBetweenWalkingDirections(Pedestrian pedestrian1, Pedestrian pedestrian2, Topography topography) {
        double angleInRadian = -1;

        switch (pedestrian1.getAttributes().getWalkingDirectionCalculation()) {
            case BY_TARGET_CENTER:
            case BY_TARGET_CLOSEST_POINT:
                angleInRadian = calculateAngleBetweenTargets(pedestrian1, pedestrian2, topography);
                break;
            default:
                throw new IllegalArgumentException(String.format("Unsupported calculation type: \"%s\"",
                        pedestrian1.getAttributes().getWalkingDirectionCalculation()));
        }

        return angleInRadian;
    }

    /**
     * Calculate the angle between the two vectors v1 and v2 where
     * v1 = (TargetPedestrian1 - pedestrian1) and v2 = (TargetPedestrian2 - pedestrian2):
     *
     * <pre>
     *     T2 o   o T1
     *        ^   ^
     *         \a/
     *          x
     *         / \
     *     P1 o   o P2
     *
     *     T1: target of pedestrian 1
     *     T2: target of pedestrian 2
     *     P1: pedestrian 1
     *     P2: pedestrian 2
     *     a : angle between the two vectors
     * </pre>
     *
     * This is required to decide if pedestrian1 and pedestrian2 can be swapped because they have different walking
     * directions.
     *
     * @return An angle between 0 and <i>pi</i> radian or -1 if at least one of the given pedestrians has no target.
     */
    private double calculateAngleBetweenTargets(Pedestrian pedestrian1, Pedestrian pedestrian2, Topography topography) {
        double angleInRadian = -1;

        if (pedestrian1.hasNextTarget() && pedestrian2.hasNextTarget()) {
            Target targetPed1 = topography.getTarget(pedestrian1.getNextTargetId());
            Target targetPed2 = topography.getTarget(pedestrian2.getNextTargetId());

            VPoint targetVectorPed1 = calculateVectorPedestrianToTarget(pedestrian1, targetPed1);
            VPoint targetVectorPed2 = calculateVectorPedestrianToTarget(pedestrian2, targetPed2);

            double dotProduct = targetVectorPed1.dotProduct(targetVectorPed2);
            double multipliedMagnitudes = targetVectorPed1.distanceToOrigin() * targetVectorPed2.distanceToOrigin();

            angleInRadian = Math.acos(dotProduct / multipliedMagnitudes);
        }

        return angleInRadian;
    }

    private VPoint calculateVectorPedestrianToTarget(Pedestrian pedestrian, Target target) {
        VPoint vectorPedestrianToTarget = null;

        if (pedestrian.getAttributes().getWalkingDirectionCalculation() == AttributesAgent.WalkingDirectionCalculation.BY_TARGET_CENTER) {
            vectorPedestrianToTarget = target.getShape().getCentroid().subtract(pedestrian.getPosition());
        } else if (pedestrian.getAttributes().getWalkingDirectionCalculation() == AttributesAgent.WalkingDirectionCalculation.BY_TARGET_CLOSEST_POINT) {
            VPoint closestTargetPoint = target.getShape().closestPoint(pedestrian.getPosition());
            vectorPedestrianToTarget = closestTargetPoint.subtract(pedestrian.getPosition());
        } else {
            throw new IllegalArgumentException(String.format("Unsupported angle calculation type: \"%s\"", pedestrian.getAttributes().getWalkingDirectionCalculation()));
        }

        return vectorPedestrianToTarget;
    }
}
