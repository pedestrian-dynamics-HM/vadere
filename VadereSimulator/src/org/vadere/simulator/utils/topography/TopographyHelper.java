package org.vadere.simulator.utils.topography;

import org.jetbrains.annotations.NotNull;
import org.vadere.simulator.models.bhm.PedestrianBHM;
import org.vadere.simulator.models.osm.PedestrianOSM;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.psychology.cognition.SelfCategory;
import org.vadere.state.psychology.perception.types.Threat;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.ScenarioElement;
import org.vadere.state.scenario.Target;
import org.vadere.state.scenario.Topography;
import org.vadere.state.simulation.FootstepHistory;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.Vector2D;

import java.util.List;
import java.util.stream.Collectors;

/**
 * A summary static methods which help
 * <ul>
 *     <li>to define the cognitive state of an agent or</li>
 *     <li>to maneuver and agent through the {@link org.vadere.state.scenario.Topography}</li>
 * </ul>
 *
 * TODO Complete unit tests in "TopographyHelperTest" for all these methods.
 */
public class TopographyHelper {

    public static Target findClosestTargetToSource(Topography topography, ScenarioElement source, Threat threat) {
        VPoint sourceCentroid = source.getShape().getCentroid();

        List<Target> sortedTargets = topography.getTargets().stream()
                .filter(target -> target.getId() != threat.getOriginAsTargetId())
                .sorted((target1, target2) -> Double.compare(
                        sourceCentroid.distance(target1.getShape().getCentroid()),
                        sourceCentroid.distance(target2.getShape().getCentroid())))
                .collect(Collectors.toList());

        Target closestTarget = (sortedTargets.isEmpty()) ? null : sortedTargets.get(0);

        return closestTarget;
    }

    public static List<Pedestrian> getNeighborsWithSelfCategory(Pedestrian pedestrian, SelfCategory expectedSelfCategory, Topography topography) {
        VPoint positionOfPedestrian = pedestrian.getPosition();

        List<Pedestrian> closestPedestrians = topography.getSpatialMap(Pedestrian.class)
                .getObjects(positionOfPedestrian, pedestrian.getAttributes().getSearchRadius());

        // Filter out "me" and pedestrians with unexpected "selfCategory".
        closestPedestrians = closestPedestrians.stream()
                .filter(candidate -> pedestrian.getId() != candidate.getId())
                .filter(candidate -> candidate.getSelfCategory() == expectedSelfCategory)
                .collect(Collectors.toList());

        // Sort by distance away from "me".
        closestPedestrians = closestPedestrians.stream()
                .sorted((pedestrian1, pedestrian2) ->
                        Double.compare(
                                positionOfPedestrian.distance(pedestrian1.getPosition()),
                                positionOfPedestrian.distance(pedestrian2.getPosition())
                        ))
                .collect(Collectors.toList());

        return closestPedestrians;
    }

    public static Pedestrian getNeighborCloserToTargetCentroid(Pedestrian pedestrian, Topography topography) {
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

    @NotNull
    public static List<Pedestrian> getNeighborsCloserToTarget(PedestrianOSM pedestrian, Topography topography) {
        VPoint positionOfPedestrian = pedestrian.getPosition();

        List<Pedestrian> closestPedestrians = topography.getSpatialMap(Pedestrian.class)
                .getObjects(positionOfPedestrian, pedestrian.getAttributes().getSearchRadius());

        // Filter out "me" and pedestrians which are further away from target than "me".
        closestPedestrians = closestPedestrians.stream()
                .filter(candidate -> pedestrian.getId() != candidate.getId())
                .filter(candidate -> pedestrian.getTargetPotential(candidate.getPosition()) < pedestrian.getTargetPotential(pedestrian.getPosition()))
                .collect(Collectors.toList());

        // Sort by distance away from "me".
        closestPedestrians = closestPedestrians.stream()
                .sorted((pedestrian1, pedestrian2) ->
                        Double.compare(
                                positionOfPedestrian.distance(pedestrian1.getPosition()),
                                positionOfPedestrian.distance(pedestrian2.getPosition())
                        ))
                .collect(Collectors.toList());

        return closestPedestrians;
    }

    public static boolean otherPedestrianIsCloserToTarget(Topography topography, PedestrianBHM me, Pedestrian  other) {
        boolean otherIsCloserToTarget = false;

        if (me.isPotentialFieldInUse()) { // Use geodetic distance
            double myDistanceToTarget = me.getPotentialFieldTarget().getPotential(me.getPosition(), me);
            double otherDistanceToTarget = me.getPotentialFieldTarget().getPotential(other.getPosition(), other);

            otherIsCloserToTarget = otherDistanceToTarget < myDistanceToTarget;
        } else { // Use Euclidean distance
            Target myTarget = topography.getTarget(me.getNextTargetId());

            if (myTarget != null) {
                double myDistanceToTarget = myTarget.getShape().distanceToCenter(me.getPosition());
                double otherDistanceToTarget = myTarget.getShape().distanceToCenter(other.getPosition());

                otherIsCloserToTarget = otherDistanceToTarget < myDistanceToTarget;
            }
        }

        return otherIsCloserToTarget;
    }

    public static boolean pedestrianIsBlockedByObstacle(Pedestrian pedestrian, Topography topography) {
        boolean isBlocked = false;

        int requiredFootSteps = 2;
        double requiredSpeedToBeBlocked = 0.05;
        double requiredDistanceToObstacle = 1.0;

        FootstepHistory footstepHistory = pedestrian.getFootstepHistory();

        if (footstepHistory.size() >= requiredFootSteps) {
            if (footstepHistory.getAverageSpeedInMeterPerSecond() <= requiredSpeedToBeBlocked) {
                // Watch out: This is probably a very expensive call but Gerta suggests to include it to get a realistic behavior!
                if (topography.distanceToObstacle(pedestrian.getPosition()) <= requiredDistanceToObstacle) {
                    isBlocked = true;
                }
            }
        }

        return  isBlocked;
    }

    public static boolean walkingDirectionDiffers(Pedestrian pedestrian1, Pedestrian pedestrian2, Topography topography) {
        boolean directionIsDifferent = false;

        double angleInRadian = calculateAngleBetweenWalkingDirections(pedestrian1, pedestrian2, topography);
        if (angleInRadian == -1 || Math.toDegrees(angleInRadian) > pedestrian1.getAttributes().getWalkingDirectionSameIfAngleLessOrEqual()) {
            directionIsDifferent = true;
        }

        return  directionIsDifferent;
    }

    public static double calculateAngleBetweenWalkingDirections(Pedestrian pedestrian1, Pedestrian pedestrian2, Topography topography) {
        double angleInRadian = -1;

        switch (pedestrian1.getAttributes().getWalkingDirectionCalculation()) {
            case BY_GRADIENT:
                angleInRadian = calculateAngleBetweenTargetGradients((PedestrianOSM)pedestrian1, (PedestrianOSM)pedestrian2);
                break;
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

    public static double calculateAngleBetweenTargetGradients(PedestrianOSM pedestrian1, PedestrianOSM pedestrian2) {
        double angleInRadian = -1;

        Vector2D targetGradientPedestrian1 = pedestrian1.getTargetGradient(pedestrian1.getPosition());
        Vector2D targetGradientPedestrian2 = pedestrian2.getTargetGradient(pedestrian2.getPosition());

        double dotProduct = targetGradientPedestrian1.dotProduct(targetGradientPedestrian2);
        double multipliedMagnitudes = targetGradientPedestrian1.distanceToOrigin() * targetGradientPedestrian2.distanceToOrigin();

        angleInRadian = Math.acos(dotProduct / multipliedMagnitudes);

        return angleInRadian;
    }

    /**
     * Calculate the angle3D between the two vectors v1 and v2 where
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
     *     a : angle3D between the two vectors
     * </pre>
     *
     * This is required to decide if pedestrian1 and pedestrian2 can be swapped because they have different walking
     * directions.
     *
     * @return An angle3D between 0 and <i>pi</i> radian or -1 if at least one of the given pedestrians has no target.
     */
    public static double calculateAngleBetweenTargets(Pedestrian pedestrian1, Pedestrian pedestrian2, Topography topography) {
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

    public static VPoint calculateVectorPedestrianToTarget(Pedestrian pedestrian, Target target) {
        VPoint vectorPedestrianToTarget = null;

        if (pedestrian.getAttributes().getWalkingDirectionCalculation() == AttributesAgent.WalkingDirectionCalculation.BY_TARGET_CENTER) {
            vectorPedestrianToTarget = target.getShape().getCentroid().subtract(pedestrian.getPosition());
        } else if (pedestrian.getAttributes().getWalkingDirectionCalculation() == AttributesAgent.WalkingDirectionCalculation.BY_TARGET_CLOSEST_POINT) {
            VPoint closestTargetPoint = target.getShape().closestPoint(pedestrian.getPosition());
            vectorPedestrianToTarget = closestTargetPoint.subtract(pedestrian.getPosition());
        } else {
            throw new IllegalArgumentException(String.format("Unsupported angle3D calculation type: \"%s\"", pedestrian.getAttributes().getWalkingDirectionCalculation()));
        }

        return vectorPedestrianToTarget;
    }

}
