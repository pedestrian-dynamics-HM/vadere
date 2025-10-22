package org.vadere.util.importSumo.processors.fillGaps;

import org.locationtech.jts.geom.*;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.util.GeometryFixer;
import org.locationtech.jts.operation.distance.DistanceOp;
import org.jetbrains.annotations.Nullable;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.Vector2D;
import org.vadere.util.importSumo.fileParsers.roadNetwork.Edges.SumoConnection;
import org.vadere.util.importSumo.fileParsers.roadNetwork.Edges.SumoEdge;
import org.vadere.util.importSumo.fileParsers.roadNetwork.Edges.SumoEdgeFunction;
import org.vadere.util.importSumo.fileParsers.roadNetwork.Edges.SumoLane;
import org.vadere.util.importSumo.fileParsers.roadNetwork.SumoJunction;
import org.vadere.util.logging.Logger;
import org.vadere.util.math.MathUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FillGapsSumoProcessor {
    private static final Logger logger = Logger.getLogger(FillGapsSumoProcessor.class);
    private final GeometryFactory geometryFactory = new GeometryFactory();
    private FillGapsSumoProcessorSettings settings;

    public void postProcess(List<SumoEdge> allEdges, List<SumoJunction> allJunction, FillGapsSumoProcessorSettings settings) {
        this.settings = settings;

        List<SumoLane> allLanes = allEdges.stream()
                .flatMap(osmEdge -> osmEdge.getLanesFromLeftToRight().stream())
                .toList();

        Map<String, SumoJunction> junctionMap = allJunction.stream().collect(Collectors.toMap(SumoJunction::getSumoId, e -> e));

        if(settings.isEnableLaneToJunctionSnapping()){
            snapLaneToJunction(allLanes, junctionMap);
        }
        if(settings.isEnableCrosswalkToWalkwaysSnapping()){
            snapCrosswalksToWalkways(allEdges);
        }
    }

    private void snapLaneToJunction(List<SumoLane> lanes, Map<String, SumoJunction> junctionMap) {
        for (SumoLane lane : lanes) {
            SumoEdge parent = lane.getParent();
            String fromJunctionId = parent.getFromJunctionId();
            if (fromJunctionId != null && junctionMap.containsKey(fromJunctionId)) {
                snapLaneToJunction(lane, junctionMap.get(fromJunctionId));
            }

            String toJunctionId = parent.getToJunctionId();
            if (toJunctionId != null && junctionMap.containsKey(toJunctionId)) {
                snapLaneToJunction(lane, junctionMap.get(toJunctionId));
            }
        }
    }

    /**
     * Snaps a lane polygon to the nearest junction polygon by adjusting its boundary point.
     * The snap occurs only if:
     *   - The closest point on the junction is within the distance threshold.
     *   - The adjustment moves the lane boundary outward, not inward.
     */
    public void snapLaneToJunction(SumoLane lane, SumoJunction osmJunction) {
        Coordinate[] laneCoordinates = lane.getPolygon().getCoordinates();
        List<Vector2D> normals = calculateNormals(laneCoordinates);

        boolean changed = false;

        for (int coordinateIndex = 0; coordinateIndex < laneCoordinates.length - 1; coordinateIndex++) {
            Coordinate laneCoordinate = laneCoordinates[coordinateIndex];
            List<LineString> junctionLineStrings = toLineRings(osmJunction);

            for (LineString junctionLineString : junctionLineStrings) {
                DistanceOp distanceOp = new DistanceOp(junctionLineString, geometryFactory.createPoint(laneCoordinate));

                if (distanceOp.distance() > settings.getLaneToJunctionMaxSnappingDistance()) {
                    continue;
                }
                Coordinate nearestCoordinateOnJunction = distanceOp.nearestPoints()[0];

                if (!isMovingCoordinateOutwards(laneCoordinate, coordinateIndex, nearestCoordinateOnJunction, normals)) {
                    continue;
                }

                laneCoordinates[coordinateIndex] = nearestCoordinateOnJunction;
                if (coordinateIndex == 0) {
                    // close polygon again (startpoint == endpoint)
                    laneCoordinates[laneCoordinates.length - 1] = nearestCoordinateOnJunction;
                }
                changed = true;
            }
        }

        if (changed) {
            try{
                Polygon polygon = geometryFactory.createPolygon(laneCoordinates);
                lane.setPolygon(polygon);
            }catch(Exception e){
                logger.error("Error while creating snap polygon between {} and {}", lane.getTypedSumoId(), osmJunction.getTypedSumoId(), e);
            }
        }
    }

    private void snapCrosswalksToWalkways(List<SumoEdge> allEdges) {
        for (SumoEdge osmEdge : allEdges) {
            if (osmEdge.getFunction() != SumoEdgeFunction.Crossing) {
                continue;
            }
            SumoEdge crosswalkEdge = osmEdge;

            for (SumoConnection crosswalkOutBoundConnection : crosswalkEdge.getOutBoundConnection()) {
                SumoLane targetWalkway = crosswalkOutBoundConnection.getTargetLane();
                SumoLane crosswalkToMerge = crosswalkOutBoundConnection.getSourceLane();
                snapCrosswalksToWalkways(crosswalkToMerge, targetWalkway, true);
            }

            for (SumoConnection crosswalkInBoundConnection : crosswalkEdge.getInBoundConnection()) {
                SumoLane targetWalkway = crosswalkInBoundConnection.getSourceLane();
                SumoLane crosswalkLaneToMerge = crosswalkInBoundConnection.getTargetLane();
                snapCrosswalksToWalkways(crosswalkLaneToMerge, targetWalkway, false);
            }
        }
    }

    /**
     * Attempts to snap both points of a crosswalk's end edge (when `outBound` is true)
     * or its starting edge to a walkway lane.
     * The snapping logic prioritizes positions based on angle and snap direction.
     */
    public void snapCrosswalksToWalkways(SumoLane crosswalk, SumoLane walkway, boolean outBound) {
        Coordinate[] crosswalkCoordinates = crosswalk.getPolygon().getCoordinates();
        List<Vector2D> allCrosswalkNormals = calculateNormals(crosswalkCoordinates);

        int crosswalkEdgeFromIndex;
        int crosswalkEdgeToIndex;
        Vector2D[] crosswalkNormals;
        if (outBound) {
            int crosswalkLineStringPoints = crosswalk.getLineString().getNumPoints();

            // Based on ImportSumoGeometryUtils#expandLineToWidth:
            // - To get to the of the walkway polygon, we can skip the first `crosswalkLineStringPoints`
            //   amount of coordinates that form the left side of the polygon.
            // - The walkway polygon’s closing edge runs from the last point of the left side
            //   to the first point of the right side.
            crosswalkEdgeFromIndex = crosswalkLineStringPoints - 1;
            crosswalkEdgeToIndex = crosswalkLineStringPoints;

            crosswalkNormals = new Vector2D[]{allCrosswalkNormals.get(crosswalkLineStringPoints - 1)};
        } else {

            // Based on ImportSumoGeometryUtils#expandLineToWidth:
            // The starting edge of the crosswalk polygon is defined by its last two coordinates.
            // The final coordinate connects the right side back to the left side.
            crosswalkEdgeFromIndex = crosswalkCoordinates.length - 1;
            crosswalkEdgeToIndex = crosswalkCoordinates.length - 2;

            crosswalkNormals = new Vector2D[]{allCrosswalkNormals.get(crosswalkCoordinates.length - 2)};
        }

        LineString walkWayLineString = walkway.getPolygon().getExteriorRing();
        boolean updated = false;
        updated |= tryMoveCrosswalkCoordinateToWalkWay(walkWayLineString, crosswalkCoordinates, crosswalkEdgeFromIndex, crosswalkNormals);
        updated |= tryMoveCrosswalkCoordinateToWalkWay(walkWayLineString, crosswalkCoordinates, crosswalkEdgeToIndex, crosswalkNormals);

        if (updated) {
            if (!outBound) {
                // ensure polygon closes correctly (first point = last point)
                crosswalkCoordinates[0] = crosswalkCoordinates[crosswalkCoordinates.length - 1];
            }

            crosswalk.setPolygon(geometryFactory.createPolygon(crosswalkCoordinates));
        }
    }

    private boolean tryMoveCrosswalkCoordinateToWalkWay(LineString walkWayLineString, Coordinate[] crosswalkCoordinates, int coordinateIndex, Vector2D... crosswalkNormals) {
        Coordinate crosswalkCoordinate = crosswalkCoordinates[coordinateIndex];

        Coordinate snappingPoint = findSnappingPoint(
                crosswalkCoordinate,
                walkWayLineString,
                settings.getCrosswalkToEdgeSnappingMaxDistance(),
                settings.getCrosswalkToEdgeSnappingMaxAngle(),
                crosswalkNormals);
        if (snappingPoint == null) {
            return false;
        }

        crosswalkCoordinates[coordinateIndex] = snappingPoint;
        return true;
    }

    /**
     * Finds a snapping point on the target linestring from a crosswalkCoordinate. For this a circle is cast from the crosswalkCoordinate.
     * If the circle's area intersects with the line, one or more PotentialSnapPoint is calculated. If multiple PotentialSnapPoint are found
     * the point with the highest score (based on distance and snapping angle) is selected.
     */
    @Nullable
    private Coordinate findSnappingPoint(Coordinate crosswalkCoordinate, LineString to, double maxSnapDistance, double maxSnapAngle, Vector2D... crosswalkNormals) {
        if(!to.isValid()){
            Geometry fix = GeometryFixer.fix(to);
            if(fix.isValid() && fix instanceof LineString fixedTo) {
                to = fixedTo;
            }else{
                return null;
            }
        }

        Point crosswalkPoint = geometryFactory.createPoint(crosswalkCoordinate);
        Geometry circularAreaAroundCrosswalkPoint = crosswalkPoint.buffer(maxSnapDistance);
        Geometry intersection = circularAreaAroundCrosswalkPoint.intersection(to);
        if (intersection.isEmpty()) {
            return null;
        }

        PotentialSnapPoint bestMatch = null;
        if (intersection instanceof LineString lineString) {
            bestMatch = GetSnapPoint(crosswalkCoordinate, lineString, maxSnapDistance, maxSnapAngle, crosswalkNormals);
        }
        if (intersection instanceof MultiLineString multiLineString) {
            double highestScore = 0;

            for (int i = 0; i < multiLineString.getNumGeometries(); i++) {
                LineString seg = (LineString) multiLineString.getGeometryN(i);

                PotentialSnapPoint potentialSnapPoint = GetSnapPoint(crosswalkCoordinate, seg, maxSnapDistance, maxSnapAngle, crosswalkNormals);
                if (potentialSnapPoint != null && potentialSnapPoint.score > highestScore) {
                    bestMatch = potentialSnapPoint;
                    highestScore = potentialSnapPoint.score;
                }
            }
        }

        return bestMatch != null ? bestMatch.point : null;
    }

    @Nullable
    private PotentialSnapPoint GetSnapPoint(Coordinate crosswalkCoordinate, LineString potentialSnapPointsLine, double maxSnapDistance, double maxSnapAngle,  Vector2D... crosswalkNormals) {
        double highestScore = 0;
        PotentialSnapPoint bestMatch = null;

        for (Vector2D crosswalkNormal : crosswalkNormals) {
            PotentialSnapPoint potentialSnapPoint = GetSnapPoint(crosswalkCoordinate, potentialSnapPointsLine, maxSnapDistance, maxSnapAngle, crosswalkNormal);
            if (potentialSnapPoint != null && potentialSnapPoint.score > highestScore) {
                highestScore = potentialSnapPoint.score;
                bestMatch = potentialSnapPoint;
            }
        }

        return bestMatch;
    }

    @Nullable
    private PotentialSnapPoint GetSnapPoint(Coordinate crosswalkCoordinate, LineString potentialSnapPointsLine, double maxSnapDistance, double maxSnapAngle, Vector2D crosswalkNormal) {
        Coordinate[] potentialSnapPointsLineCoordinates = potentialSnapPointsLine.getCoordinates();

        double highestScore = 0;
        Coordinate lowestPoint = null;

        for (int coordinateIndex = 0; coordinateIndex < potentialSnapPointsLineCoordinates.length - 1; coordinateIndex++) {
            Vector2D snapToSegmentOnLine = new Vector2D(
                    potentialSnapPointsLineCoordinates[coordinateIndex + 1].x - potentialSnapPointsLineCoordinates[coordinateIndex].x,
                    potentialSnapPointsLineCoordinates[coordinateIndex + 1].y - potentialSnapPointsLineCoordinates[coordinateIndex].y);
            Vector2D snapToLineSegmentNormalized = snapToSegmentOnLine.normalize();
            Vector2D snapToLineSegmentNormal = new Vector2D(snapToLineSegmentNormalized.y, -snapToLineSegmentNormalized.x);
            double toSegmentLength = snapToSegmentOnLine.getLength();

            for (double length = 0; length < toSegmentLength; length += 0.5) {
                Coordinate candidateSnapPoint = new Coordinate(
                        potentialSnapPointsLineCoordinates[coordinateIndex].x + snapToLineSegmentNormalized.x * length,
                        potentialSnapPointsLineCoordinates[coordinateIndex].y + snapToLineSegmentNormalized.y * length);

                Double score = CalculateSnapPointScore(crosswalkCoordinate, candidateSnapPoint, snapToLineSegmentNormal, maxSnapDistance, maxSnapAngle, crosswalkNormal);
                if(score == null) continue;

                if (score > highestScore) {
                    lowestPoint = candidateSnapPoint;
                    highestScore = score;
                }
            }

        }

        return lowestPoint != null ? new PotentialSnapPoint(lowestPoint, highestScore) : null;
    }

    private Double CalculateSnapPointScore(
            Coordinate crosswalkCoordinate, Coordinate candidateSnapPoint, Vector2D snapToLineSegmentNormal,  double maxSnapDistance, double maxSnapAngle, Vector2D crosswalkNormal){
        Vector2D crosswalkToSnapPointVector = new Vector2D(
                candidateSnapPoint.x - crosswalkCoordinate.x,
                candidateSnapPoint.y - crosswalkCoordinate.y);

        double distanceToSnapPoint = crosswalkToSnapPointVector.getLength();
        if (distanceToSnapPoint > maxSnapDistance) {
            return null;
        }

        double snappingAngle = GeometryUtils.smallestAngleBetweenDegree(crosswalkNormal, crosswalkToSnapPointVector);
        boolean isVeryCloseSnap = distanceToSnapPoint <= 1;
        boolean isAngleTooLarge = snappingAngle > maxSnapAngle;
        if (isAngleTooLarge && !isVeryCloseSnap) {
            return null;
        }

        double angleBetweenSnapAndSegment = GeometryUtils.smallestAngleBetweenDegree(crosswalkNormal, snapToLineSegmentNormal);
        if(angleBetweenSnapAndSegment > 90){
            return null;
        }

        double snappingAlignmentScore = 1 - MathUtil.clamp(angleBetweenSnapAndSegment / 60, 0, 1);
        double distanceRatio = MathUtil.clamp(distanceToSnapPoint / maxSnapDistance, 0, 1);
        double distanceScore = 1 - distanceRatio * distanceRatio;
        double angleScore = isVeryCloseSnap ? 1 : 1 - MathUtil.clamp(snappingAngle / maxSnapAngle, 0, 1);

        return (2 * distanceScore + (angleScore * snappingAlignmentScore)) / 3;
    }

    private record PotentialSnapPoint(Coordinate point, double score) {
    }

    /**
     * Determines if the coordinate is being moved away from one of the polygon's connecting edges
     * based on the direction of movement relative to the normals of the edges connected to the coordinate.
     */
    private static boolean isMovingCoordinateOutwards(
            Coordinate coordinateToMove, int coordinateToMoveIndex, Coordinate targetCoordinate, List<Vector2D> normals) {

        Vector2D coordinateMoveDirection = new Vector2D(targetCoordinate.x - coordinateToMove.x, targetCoordinate.y - coordinateToMove.y);

        boolean coordinateToMoveIsFirstOrLast = coordinateToMoveIndex == 0 || coordinateToMoveIndex == normals.size();
        if (coordinateToMoveIsFirstOrLast) {
            if (isInSameDirection(normals.get(0), coordinateMoveDirection)) {
                return true;
            }
            if (isInSameDirection(normals.get(normals.size() - 1), coordinateMoveDirection)) {
                return true;
            }
            return false;
        }

        if (isInSameDirection(normals.get(coordinateToMoveIndex), coordinateMoveDirection)) {
            return true;
        }
        if (isInSameDirection(normals.get(coordinateToMoveIndex - 1), coordinateMoveDirection)) {
            return true;
        }

        return false;
    }

    private static boolean isInSameDirection(Vector2D normal, Vector2D coordinateMoveDirection) {
        double degree = GeometryUtils.smallestAngleBetweenDegree(normal, coordinateMoveDirection);
        return degree < 45;
    }

    private List<Vector2D> calculateNormals(Coordinate[] coordinates) {
        List<Vector2D> normals = new ArrayList<>();
        boolean isClockwise = isClockwise(coordinates);

        for (int i = 0; i < coordinates.length - 1; i++) {
            Coordinate p1 = coordinates[i];
            Coordinate p2 = coordinates[i + 1];

            Vector2D edge = new Vector2D(p2.x - p1.x, p2.y - p1.y);
            Vector2D normal = new Vector2D(-edge.y, edge.x); // rotated by 90°

            if (!isClockwise) {
                normal = normal.multiply(-1);
            }
            normal = normal.normalize();

            normals.add(normal);
        }

        return normals;
    }

    // https://element84.com/software-engineering/web-development/determining-the-winding-of-a-polygon-given-as-a-set-of-ordered-points/
    public static boolean isClockwise(Coordinate[] coordinates) {
        double sum = 0.0;

        for (int i = 0; i < coordinates.length - 1; i++) {
            Coordinate c1 = coordinates[i];
            Coordinate c2 = coordinates[i + 1];
            sum += (c2.x - c1.x) * (c2.y + c1.y);
        }

        return sum > 0; // Positive sum means counter-clockwise, negative means clockwise
    }

    private List<LineString> toLineRings(SumoJunction junction) {
        ArrayList<LineString> lineStrings = new ArrayList<>();

        if (junction.getPolygon() != null) {
            lineStrings.add(junction.getPolygon().getExteriorRing());
            return lineStrings;
        }

        for (SumoEdge walkingArea : junction.getWalkingAreas()) {
            LineString lineString = walkingArea.getMergedPolygon().getExteriorRing();
            lineStrings.add(lineString);
        }

        return lineStrings;
    }
}
