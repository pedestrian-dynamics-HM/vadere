package org.vadere.util.importSumo;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Polygon;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.Vector2D;
import org.vadere.util.importSumo.fileParsers.roadNetwork.Edges.SumoAgentType;
import org.vadere.util.importSumo.fileParsers.roadNetwork.Edges.SumoEdge;
import org.vadere.util.importSumo.fileParsers.roadNetwork.Edges.SumoEdgeFunction;
import org.vadere.util.importSumo.fileParsers.roadNetwork.Edges.SumoLane;
import org.vadere.util.importSumo.fileParsers.roadNetwork.SumoJunction;
import org.vadere.util.importSumo.processors.fillGaps.FillGapsSumoProcessor;
import org.vadere.util.importSumo.processors.fillGaps.FillGapsSumoProcessorSettings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FillGapsSumoProcessorTest {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

    private static Polygon createRectangle(double minX, double minY, double maxX, double maxY) {
        return GEOMETRY_FACTORY.createPolygon(new Coordinate[]{
                new Coordinate(minX, minY),
                new Coordinate(minX, maxY),
                new Coordinate(maxX, maxY),
                new Coordinate(maxX, minY),
                new Coordinate(minX, minY)
        });
    }

    private static LineString createLine(double x1, double y1, double x2, double y2) {
        return GEOMETRY_FACTORY.createLineString(new Coordinate[]{
                new Coordinate(x1, y1),
                new Coordinate(x2, y2)
        });
    }

    private static SumoLane createLaneFromPolygon(Polygon polygon, String fromJunctionId, String toJunctionId) {
        List<SumoLane> lanes = new ArrayList<>();
        SumoEdge parent = new SumoEdge("E1", SumoEdgeFunction.Default, lanes, fromJunctionId, toJunctionId);
        SumoLane lane = new SumoLane(1, "L1", parent, null, polygon, Set.of(SumoAgentType.Pedestrian));
        lanes.add(lane);
        return lane;
    }

    private static SumoLane createCrosswalkFromLine(LineString lineString, double width) {
        List<SumoLane> lanes = new ArrayList<>();
        SumoEdge parent = new SumoEdge("CWE1", SumoEdgeFunction.Crossing, lanes, null, null);
        SumoLane crosswalk = new SumoLane(2, "CWL1", parent, width, lineString, Set.of(SumoAgentType.Pedestrian));
        lanes.add(crosswalk);
        return crosswalk;
    }

    private static SumoJunction createSumoJunctionFromPolygon(Polygon polygon, String junctionId) {
        return new SumoJunction(10, junctionId, polygon, List.of(), List.of(), List.of(), List.of());
    }

    private static FillGapsSumoProcessor createProcessor(double laneToJunctionMaxSnappingDistance, double crosswalkToEdgeSnappingMaxDistance, double crosswalkToEdgeSnappingMaxAngle) {
        FillGapsSumoProcessorSettings settings = new FillGapsSumoProcessorSettings(true, laneToJunctionMaxSnappingDistance, true, crosswalkToEdgeSnappingMaxDistance, crosswalkToEdgeSnappingMaxAngle);
        FillGapsSumoProcessor processor = new FillGapsSumoProcessor();
        processor.postProcess(Collections.emptyList(), Collections.emptyList(), settings);
        return processor;
    }

    private static void assertSnappingWithinDistanceAndAngle(Coordinate snappedPoint, Coordinate originalPoint, Vector2D normal, double maxSnappingAngle, double maxSnappingDistance) {
        Vector2D snappingDirection = new Vector2D(snappedPoint.x - originalPoint.x, snappedPoint.y - originalPoint.y);
        assertTrue(GeometryUtils.smallestAngleBetweenDegree(normal, snappingDirection) <= maxSnappingAngle + GeometryUtils.DOUBLE_EPS, "Snapping direction should be within max angle to boundary normal");
        double snappingDistance = snappedPoint.distance(originalPoint);
        assertTrue(snappingDistance <= maxSnappingDistance + GeometryUtils.DOUBLE_EPS, "Distance between original and snapped points should be within max snapping distance");
    }

    private static void assertCoordinatesEqual(Coordinate[] original, Coordinate[] snapped, int[] pointIndices) {
        for (int i : pointIndices) {
            assertEquals(original[i].x, snapped[i].x, GeometryUtils.DOUBLE_EPS, "Point " + i + " x-coordinate should remain unchanged");
            assertEquals(original[i].y, snapped[i].y, GeometryUtils.DOUBLE_EPS, "Point " + i + " y-coordinate should remain unchanged");
        }
    }

    @Test
    void testSnapLaneToJunction() {
        FillGapsSumoProcessor processor = createProcessor(1, 0, 0);
        Polygon lanePolygon = GEOMETRY_FACTORY.createPolygon(new Coordinate[]{
                new Coordinate(0.5, 0),
                new Coordinate(1, 2),
                new Coordinate(6, 0),
                new Coordinate(5.5, -2),
                new Coordinate(0.5, 0)
        });
        SumoLane lane = createLaneFromPolygon(lanePolygon, "J_FROM", "J_TO");
        Coordinate[] originalLane = lane.getPolygon().getCoordinates();
        SumoJunction junction = createSumoJunctionFromPolygon(createRectangle(-5, 0, 0, 5), "J_FROM");

        processor.snapLaneToJunction(lane, junction);

        Coordinate[] snappedLane = lane.getPolygon().getCoordinates();
        assertEquals(originalLane.length, snappedLane.length, "Lane should still have 5 points after snapping");
        assertCoordinatesEqual(originalLane, snappedLane, new int[]{2, 3});

        assertEquals(0, snappedLane[0].x, GeometryUtils.DOUBLE_EPS, "First point x-coordinate should be snapped to junction");
        assertEquals(0, snappedLane[0].y, GeometryUtils.DOUBLE_EPS, "First point y-coordinate should be snapped to junction");
        assertEquals(0, snappedLane[1].x, GeometryUtils.DOUBLE_EPS, "Second point x-coordinate should be snapped to junction");
        assertEquals(2, snappedLane[1].y, GeometryUtils.DOUBLE_EPS, "Second point y-coordinate should be snapped to junction");
        assertEquals(0, snappedLane[4].x, GeometryUtils.DOUBLE_EPS, "Last point x-coordinate should be snapped to junction and close the polygon");
        assertEquals(0, snappedLane[4].y, GeometryUtils.DOUBLE_EPS, "Last point y-coordinate should be snapped to junction and close the polygon");
    }

    @Test
    void testNotSnapLaneToJunctionWhenDistanceExceedsMax() {
        FillGapsSumoProcessor processor = createProcessor(0.5, 0, 0);
        Polygon lanePolygon = GEOMETRY_FACTORY.createPolygon(new Coordinate[]{
                new Coordinate(-0.5, 0),
                new Coordinate(1, 2),
                new Coordinate(6, 0),
                new Coordinate(5.5, -2),
                new Coordinate(-0.5, 0)
        });
        SumoLane lane = createLaneFromPolygon(lanePolygon, "J_FROM", "J_TO");
        Coordinate[] originalLane = lane.getPolygon().getCoordinates();
        SumoJunction junction = createSumoJunctionFromPolygon(createRectangle(-5, 0, 0, 5), "J_TO");

        processor.snapLaneToJunction(lane, junction);

        Coordinate[] snappedLane = lane.getPolygon().getCoordinates();
        assertEquals(originalLane.length, snappedLane.length, "Lane should still have 5 points after snapping");
        assertCoordinatesEqual(originalLane, snappedLane, new int[]{0, 1, 2, 3, 4});
    }

    @Test
    void testSnapCrosswalkToWalkwayOutBound() {
        double maxSnappingAngle = 60;
        double maxSnappingDistance = 1.5;
        FillGapsSumoProcessor processor = createProcessor(0, maxSnappingDistance, maxSnappingAngle);

        SumoLane crosswalk = createCrosswalkFromLine(createLine(-3, 0, 0, 0), 2);
        Coordinate[] originalCrosswalk = crosswalk.getPolygon().getCoordinates();
        SumoLane walkway = createLaneFromPolygon(createRectangle(1, -4, 5, 0), null, null);

        processor.snapCrosswalksToWalkways(crosswalk, walkway, true);

        Coordinate[] snappedCrosswalk = crosswalk.getPolygon().getCoordinates();
        assertEquals(originalCrosswalk.length, snappedCrosswalk.length, "Crosswalk should still have 5 points after snapping");
        assertCoordinatesEqual(originalCrosswalk, snappedCrosswalk, new int[]{0, 3, 4});

        Vector2D outBoundNormal = new Vector2D(1, 0);
        assertSnappingWithinDistanceAndAngle(snappedCrosswalk[1], originalCrosswalk[1], outBoundNormal, maxSnappingAngle, maxSnappingDistance);
        assertEquals(1, snappedCrosswalk[1].x, GeometryUtils.DOUBLE_EPS, "Second point should be snapped to walkway's left vertical boundary");

        assertSnappingWithinDistanceAndAngle(snappedCrosswalk[2], originalCrosswalk[2], outBoundNormal, maxSnappingAngle, maxSnappingDistance);
        assertEquals(1, snappedCrosswalk[2].x, GeometryUtils.DOUBLE_EPS, "Third point should be snapped to walkway's left vertical boundary");
    }

    @Test
    void testSnapCrosswalkToWalkwayInBound() {
        double maxSnappingAngle = 50;
        double maxSnappingDistance = 3;
        FillGapsSumoProcessor processor = createProcessor(0, maxSnappingDistance, maxSnappingAngle);

        SumoLane crosswalk = createCrosswalkFromLine(createLine(0, 0.5, -3, 0.5), 1);
        Coordinate[] originalCrosswalk = crosswalk.getPolygon().getCoordinates();
        Polygon walkwayPolygon = GEOMETRY_FACTORY.createPolygon(new Coordinate[]{
                new Coordinate(7, 0),
                new Coordinate(0, 0),
                new Coordinate(3, 3),
                new Coordinate(7, 3),
                new Coordinate(7, 0)
        });
        SumoLane walkway = createLaneFromPolygon(walkwayPolygon, null, null);

        processor.snapCrosswalksToWalkways(crosswalk, walkway, false);

        Coordinate[] snappedCrosswalk = crosswalk.getPolygon().getCoordinates();
        assertEquals(originalCrosswalk.length, snappedCrosswalk.length, "Crosswalk should still have 5 points after snapping");
        assertCoordinatesEqual(originalCrosswalk, snappedCrosswalk, new int[]{0, 1, 2, 4});

        double snappingDistance = snappedCrosswalk[3].distance(originalCrosswalk[3]);
        assertTrue(snappingDistance <= 1, "Snapping point is very close to original point, i.e. the distance is smaller equal to 1");
    }

    @Test
    void testNotSnapCrosswalkToWalkwayOutboundWhenDistanceExceedsMax() {
        double maxSnappingAngle = 50;
        double maxSnappingDistance = 0.9;
        FillGapsSumoProcessor processor = createProcessor(0, maxSnappingDistance, maxSnappingAngle);

        SumoLane crosswalk = createCrosswalkFromLine(createLine(-3, 1, 0, 1), 2);
        Coordinate[] originalCrosswalk = crosswalk.getPolygon().getCoordinates();
        Polygon walkwayPolygon = GEOMETRY_FACTORY.createPolygon(new Coordinate[]{
                new Coordinate(7, 0),
                new Coordinate(1, 0),
                new Coordinate(7, 3),
                new Coordinate(7, 0)
        });
        SumoLane walkway = createLaneFromPolygon(walkwayPolygon, null, null);

        processor.snapCrosswalksToWalkways(crosswalk, walkway, true);

        Coordinate[] snappedCrosswalk = crosswalk.getPolygon().getCoordinates();
        assertEquals(originalCrosswalk.length, snappedCrosswalk.length, "Crosswalk should still have 5 points after snapping");
        assertCoordinatesEqual(originalCrosswalk, snappedCrosswalk, new int[]{0, 1, 2, 3, 4});
    }
}
