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
    private static final double TOLERANCE = 1e-9;

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

    @Test
    void shouldSnapLaneToJunction() {
        FillGapsSumoProcessor processor = createProcessor(1, 0, 0);
        Polygon lanePolygon = GEOMETRY_FACTORY.createPolygon(new Coordinate[]{
                new Coordinate(0.5, 0),
                new Coordinate(1, 2),
                new Coordinate(6, 0),
                new Coordinate(5.5, -2),
                new Coordinate(0.5, 0)
        });
        SumoLane lane = createLaneFromPolygon(lanePolygon, "J_FROM", "J_TO");
        SumoJunction junction = createSumoJunctionFromPolygon(createRectangle(-5, 0, 0, 5), "J_FROM");

        processor.snapLaneToJunction(lane, junction);

        Coordinate[] snappedLane = lane.getPolygon().getCoordinates();
        assertEquals(5, snappedLane.length, "Lane should still have 5 points after snapping");
        assertEquals(0, snappedLane[0].x, TOLERANCE, "First point x-coordinate should be snapped to junction");
        assertEquals(0, snappedLane[0].y, TOLERANCE, "First point y-coordinate should be snapped to junction");
        assertEquals(0, snappedLane[1].x, TOLERANCE, "Second point x-coordinate should be snapped to junction");
        assertEquals(2, snappedLane[1].y, TOLERANCE, "Second point y-coordinate should be snapped to junction");
        assertEquals(6, snappedLane[2].x, TOLERANCE, "Third point x-coordinate should remain unchanged");
        assertEquals(0, snappedLane[2].y, TOLERANCE, "Third point y-coordinate should remain unchanged");
        assertEquals(5.5, snappedLane[3].x, TOLERANCE, "Fourth point x-coordinate should remain unchanged");
        assertEquals(-2, snappedLane[3].y, TOLERANCE, "Fourth point y-coordinate should remain unchanged");
        assertEquals(0, snappedLane[4].x, TOLERANCE, "Last point x-coordinate should be snapped to junction and close the polygon");
        assertEquals(0, snappedLane[4].y, TOLERANCE, "Last point y-coordinate should be snapped to junction and close the polygon");
    }

    @Test
    void shouldNotSnapLaneToJunction() {
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
        for (int i = 0; i < originalLane.length; i++) {
            assertEquals(originalLane[i].x, snappedLane[i].x, TOLERANCE, "Point " + i + " x-coordinate should remain unchanged");
            assertEquals(originalLane[i].y, snappedLane[i].y, TOLERANCE, "Point " + i + " y-coordinate should remain unchanged");
        }
    }

    @Test
    void shouldSnapCrosswalkToWalkwayOutBound() {
        double maxSnappingAngle = 60;
        double maxSnappingDistance = 1.5;
        FillGapsSumoProcessor processor = createProcessor(0, maxSnappingDistance, maxSnappingAngle);

        SumoLane crosswalk = createCrosswalkFromLine(createLine(-3, 0, 0, 0), 2);
        Coordinate[] originalCrosswalk = crosswalk.getPolygon().getCoordinates();
        SumoLane walkway = createLaneFromPolygon(createRectangle(1, -4, 5, 0), null, null);

        processor.snapCrosswalksToWalkways(crosswalk, walkway, true);

        Coordinate[] snappedCrosswalk = crosswalk.getPolygon().getCoordinates();
        assertEquals(originalCrosswalk.length, snappedCrosswalk.length, "Crosswalk should still have 5 points after snapping");

        assertEquals(originalCrosswalk[0].x, snappedCrosswalk[0].x, TOLERANCE, "First point x-coordinate should remain unchanged");
        assertEquals(originalCrosswalk[0].y, snappedCrosswalk[0].y, TOLERANCE, "First point y-coordinate should remain unchanged");

        Vector2D outBoundNormal = new Vector2D(1, 0);
        Vector2D snappingDirection1 = new Vector2D(snappedCrosswalk[1].x - originalCrosswalk[1].x, snappedCrosswalk[1].y - originalCrosswalk[1].y);
        assertTrue(GeometryUtils.smallestAngleBetweenDegree(outBoundNormal, snappingDirection1) <= maxSnappingAngle + TOLERANCE, "Snapping direction should be within max angle to outbound normal");
        double snappingDistance1 = snappedCrosswalk[1].distance(originalCrosswalk[1]);
        assertTrue(snappingDistance1 <= maxSnappingDistance + TOLERANCE, "Distance between original crosswalk and snapped point should be within max snapping distance");
        assertEquals(1, snappedCrosswalk[1].x, TOLERANCE, "Second point should be snapped to walkway's left vertical boundary");

        Vector2D snappingDirection2 = new Vector2D(snappedCrosswalk[2].x - originalCrosswalk[2].x, snappedCrosswalk[2].y - originalCrosswalk[2].y);
        assertTrue(GeometryUtils.smallestAngleBetweenDegree(outBoundNormal, snappingDirection2) <= maxSnappingAngle + TOLERANCE, "Snapping direction should be within max angle to outbound normal");
        double snappingDistance2 = snappedCrosswalk[2].distance(originalCrosswalk[2]);
        assertTrue(snappingDistance2 <= maxSnappingDistance + TOLERANCE, "Distance between original crosswalk and snapped point should be within max snapping distance");
        assertEquals(1, snappedCrosswalk[2].x, TOLERANCE, "Third point should be snapped to walkway's left vertical boundary");

        assertEquals(originalCrosswalk[3].x, snappedCrosswalk[3].x, TOLERANCE, "Fourth point x-coordinate should remain unchanged");
        assertEquals(originalCrosswalk[3].y, snappedCrosswalk[3].y, TOLERANCE, "Fourth point y-coordinate should remain unchanged");
        assertEquals(originalCrosswalk[4].x, snappedCrosswalk[4].x, TOLERANCE, "Last point x-coordinate should remain unchanged and close the polygon");
        assertEquals(originalCrosswalk[4].y, snappedCrosswalk[4].y, TOLERANCE, "Last point y-coordinate should remain unchanged and close the polygon");
    }

    @Test
    void shouldSnapCrosswalkToWalkwayInBound() {
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
        for (int i : new int[]{0, 1, 2, 4}) {
            assertEquals(originalCrosswalk[i].x, snappedCrosswalk[i].x, TOLERANCE, "Point " + i + " x-coordinate should remain unchanged");
            assertEquals(originalCrosswalk[i].y, snappedCrosswalk[i].y, TOLERANCE, "Point " + i + " y-coordinate should remain unchanged");
        }

        double snappingDistance = snappedCrosswalk[3].distance(originalCrosswalk[3]);
        assertTrue(snappingDistance <= 1, "Snapping point is very close to original point, i.e. the distance is smaller equal to 1");
    }

    @Test
    void shouldNotSnapCrosswalkToWalkwayOutbound() {
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
        for (int i = 0; i < originalCrosswalk.length; i++) {
            assertEquals(originalCrosswalk[i].x, snappedCrosswalk[i].x, TOLERANCE, "Point " + i + " x-coordinate should remain unchanged");
            assertEquals(originalCrosswalk[i].y, snappedCrosswalk[i].y, TOLERANCE, "Point " + i + " y-coordinate should remain unchanged");
        }
    }
}
