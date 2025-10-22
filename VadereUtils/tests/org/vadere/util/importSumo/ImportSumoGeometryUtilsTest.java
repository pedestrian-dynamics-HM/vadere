package org.vadere.util.importSumo;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.*;
import org.vadere.util.importSumo.processors.inverseSpace.SumoInvertSettings;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImportSumoGeometryUtilsTest {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

    private static Polygon createRectangle(double minX, double minY, double maxX, double maxY) {
        Coordinate[] cs = new Coordinate[]{
                new Coordinate(minX, minY),
                new Coordinate(minX, maxY),
                new Coordinate(maxX, maxY),
                new Coordinate(maxX, minY),
                new Coordinate(minX, minY)
        };
        return GEOMETRY_FACTORY.createPolygon(cs);
    }

    private static Polygon createRectangleRing(double innerMinX, double innerMinY, double innerMaxX, double innerMaxY,
                                               double outerMinX, double outerMinY, double outerMaxX, double outerMaxY) {
        LinearRing inner = GEOMETRY_FACTORY.createLinearRing(new Coordinate[]{
                new Coordinate(innerMinX, innerMinY),
                new Coordinate(innerMinX, innerMaxY),
                new Coordinate(innerMaxX, innerMaxY),
                new Coordinate(innerMaxX, innerMinY),
                new Coordinate(innerMinX, innerMinY)
        });
        LinearRing outer = GEOMETRY_FACTORY.createLinearRing(new Coordinate[]{
                new Coordinate(outerMinX, outerMinY),
                new Coordinate(outerMinX, outerMaxY),
                new Coordinate(outerMaxX, outerMaxY),
                new Coordinate(outerMaxX, outerMinY),
                new Coordinate(outerMinX, outerMinY)
        });
        return GEOMETRY_FACTORY.createPolygon(outer, new LinearRing[]{inner});
    }

    @Test
    void shouldCalculateInverseOfRectangle() {
        Envelope bounds = createRectangle(0, 0, 10, 10).getEnvelopeInternal();
        Polygon obstacle = createRectangle(3, 0, 5, 10);
        SumoInvertSettings invertGroupSettings = new SumoInvertSettings(2.0, 2, 1.0, 1.0);

        List<Polygon> result = ImportSumoGeometryUtils.calculateInverse(obstacle, bounds, invertGroupSettings);

        Polygon leftInverse = createRectangle(0, 0, 3, 10);
        Polygon rightInverse = createRectangle(5, 0, 10, 10);
        Geometry expectedInverse = leftInverse.union(rightInverse);
        Geometry resultCombined = GEOMETRY_FACTORY.buildGeometry(result).union();

        assertTrue(resultCombined.symDifference(expectedInverse).isEmpty(), "Calculated inverse must equal the free space");
        assertEquals(80.0, resultCombined.getArea(), 1e-9, "Inverse area should match free space area");
        for (Polygon polygon : result) {
            assertEquals(0, polygon.getNumInteriorRing(), "Calculated polygons must not contain holes");
        }
    }

    @Test
    void shouldCalculateInverseOfRectangles() {
        Polygon obstacle1 = createRectangle(-1, -1, 0, 1);
        Polygon obstacle2 = createRectangle(1, -1, 3, 1);
        SumoInvertSettings invertGroupSettings = new SumoInvertSettings(2.0);

        List<Polygon> result = ImportSumoGeometryUtils.calculateInverse(List.of(obstacle1, obstacle2), invertGroupSettings);

        Geometry expectedInverse = createRectangle(0, -1, 1, 1);
        Geometry resultCombined = GEOMETRY_FACTORY.buildGeometry(result).union();

        assertTrue(resultCombined.symDifference(expectedInverse).isEmpty(), "Calculated inverse must equal the free space");
        assertEquals(2.0, resultCombined.getArea(), 1e-9, "Inverse area should match free space area");
        for (Polygon polygon : result) {
            assertEquals(0, polygon.getNumInteriorRing(), "Calculated polygons must not contain holes");
        }
    }

    @Test
    void shouldCalculateInverseOfRectanglesWithoutHoles() {
        Envelope bounds = createRectangle(-2, -2, 4, 2).getEnvelopeInternal();
        Polygon obstacle1 = createRectangle(-1, -1, 0, 1);
        Polygon obstacle2 = createRectangle(1, -2, 2, 2);
        SumoInvertSettings invertGroupSettings = new SumoInvertSettings(2.0);

        List<Polygon> result = ImportSumoGeometryUtils.calculateInverse(List.of(obstacle1, obstacle2), bounds, invertGroupSettings);

        Polygon rightInverse = createRectangle(2, -2, 4, 2);
        Polygon leftInverse = createRectangleRing(-1, -1, 0, 1, -2, -2, 1, 2);
        Geometry expectedInverse = rightInverse.union(leftInverse);
        Geometry resultCombined = GEOMETRY_FACTORY.buildGeometry(result).union();

        assertTrue(resultCombined.symDifference(expectedInverse).isEmpty(), "Calculated inverse must equal the free space");
        assertEquals(18.0, resultCombined.getArea(), 1e-9, "Inverse area should match free space area");
        assertEquals(3, result.size(), "Number of polygons in merged inverses");
        for (Polygon polygon : result) {
            assertEquals(0, polygon.getNumInteriorRing(), "Calculated polygons must not contain holes");
        }
    }

    @Test
    void shouldCalculateInverseOfRingWithoutHoles() {
        Envelope bounds = createRectangle(0, 0, 15, 6).getEnvelopeInternal();
        Polygon obstacle = createRectangleRing(2, 2, 13, 4, 1, 1, 14, 5);
        SumoInvertSettings invertGroupSettings = new SumoInvertSettings(1.5);

        List<Polygon> result = ImportSumoGeometryUtils.calculateInverse(obstacle, bounds, invertGroupSettings);

        Polygon innerInverse = createRectangle(2, 2, 13, 4);
        Polygon outerInverse = createRectangleRing(1, 1, 14, 5, 0, 0, 15, 6);
        Geometry expectedInverse = innerInverse.union(outerInverse);
        Geometry resultCombined = GEOMETRY_FACTORY.buildGeometry(result).union();

        assertTrue(resultCombined.symDifference(expectedInverse).isEmpty(), "Calculated inverse must equal the free space");
        assertEquals(60.0, resultCombined.getArea(), 1e-9, "Inverse area should match free space area");
        assertEquals(3, result.size(), "Number of polygons in merged inverses");
        for (Polygon polygon : result) {
            assertEquals(0, polygon.getNumInteriorRing(), "Calculated polygons must not contain holes");
        }
    }

    @Test
    void shouldCalculateInverseAndFilterByMinPolygonSize() {
        Envelope bounds = createRectangle(0, 0, 15, 6).getEnvelopeInternal();
        Polygon obstacle = createRectangle(1, 1, 14, 5);
        SumoInvertSettings invertGroupSettings = new SumoInvertSettings(1.5, null, null, 45.0);

        List<Polygon> result = ImportSumoGeometryUtils.calculateInverse(obstacle, bounds, invertGroupSettings);

        assertTrue(result.isEmpty(), "Small obstacles should be filtered out by minPolygonSize");
    }

    @Test
    void shouldCalculateInverseAndFilterByMinPolygonDiameter() {
        Envelope bounds = createRectangle(0, 0, 9, 9).getEnvelopeInternal();
        Polygon obstacle = createRectangle(2, 0, 7, 9);
        SumoInvertSettings invertGroupSettings = new SumoInvertSettings(3.0, null, 3.0, null);

        List<Polygon> result = ImportSumoGeometryUtils.calculateInverse(obstacle, bounds, invertGroupSettings);

        assertTrue(result.isEmpty(), "Small obstacles should be filtered out by minResultPolygonDiameter");
    }

    @Test
    void shouldDiscardInverseWithHole() {
        Envelope bounds = createRectangle(0, 0, 3, 3).getEnvelopeInternal();
        Polygon obstacle = createRectangle(0.5, 0.5, 2, 2);
        SumoInvertSettings invertGroupSettings = new SumoInvertSettings(3);

        List<Polygon> result = ImportSumoGeometryUtils.calculateInverse(obstacle, bounds, invertGroupSettings);

        assertTrue(result.isEmpty(), "Inverse with hole must be discarded");
    }

    @Test
    void shouldCalculateEmptyInverse() {
        Polygon obstacle = createRectangle(0, 0, 15, 6);
        SumoInvertSettings invertGroupSettings = new SumoInvertSettings(1.5);

        List<Polygon> result = ImportSumoGeometryUtils.calculateInverse(obstacle, invertGroupSettings);

        assertTrue(result.isEmpty(), "The inverse of a full area must be empty");
    }

    @Test
    void shouldReturnNullWhenExceptionThrown() {
        Envelope bounds = createRectangle(0, 0, 1, 1).getEnvelopeInternal();
        Polygon obstacle = new Polygon(createRectangle(0, 0, 0.7, 0.4).getExteriorRing(), null, GEOMETRY_FACTORY) {
            @Override
            public Geometry buffer(double distance) {
                throw new RuntimeException("Error from 'getDifference'");
            }
        };
        SumoInvertSettings invertGroupSettings = new SumoInvertSettings(1);

        List<Polygon> result = ImportSumoGeometryUtils.calculateInverse(obstacle, bounds, invertGroupSettings);

        assertTrue(result.isEmpty(), "'difference == null' path should not produce any inverses");
    }

    @Test
    void shouldContainHoles() {
        Polygon obstacle1 = createRectangle(0, 0, 1, 1);
        Polygon obstacle2 = createRectangle(2, 0, 3, 1);

        Geometry obstacles = obstacle1.union(obstacle2);

        assertTrue(ImportSumoGeometryUtils.hasHoles(obstacles));
    }

    @Test
    void shouldTranslateLineToTheRight() {
        LineString line = GEOMETRY_FACTORY.createLineString(new Coordinate[]{
                new Coordinate(0, 0),
                new Coordinate(5, 0),
                new Coordinate(5, 10)});

        LineString shifted = ImportSumoGeometryUtils.translateLineToTheRight(line, 1.0);

        LineString expected = GEOMETRY_FACTORY.createLineString(new Coordinate[]{
                new Coordinate(1, 0),
                new Coordinate(5, 1),
                new Coordinate(5, 11)});

        assertTrue(shifted.equalsExact(expected, 1e-9));
    }

    @Test
    void shouldExpandStraightLineToWidth() {
        LineString line = GEOMETRY_FACTORY.createLineString(new Coordinate[]{new Coordinate(0, 0), new Coordinate(5, 0)});

        Polygon result = ImportSumoGeometryUtils.expandLineToWidth(line, 1.0);

        Polygon expected = createRectangle(0, -0.5, 5, 0.5);

        assertTrue(result.symDifference(expected).isEmpty(), "Expanded line must match expected polygon");
        assertEquals(5.0, result.getArea(), 1e-9, "Expanded line area must match expected area");
    }

    @Test
    void shouldExpandLineToWidth() {
        LineString line = GEOMETRY_FACTORY.createLineString(new Coordinate[]{
                new Coordinate(0, 0),
                new Coordinate(5, 0),
                new Coordinate(5, 10)});

        Polygon result = ImportSumoGeometryUtils.expandLineToWidth(line, 1.0);

        assertTrue(result.covers(line), "Polygon should fully cover the original line");

        Envelope envelope = result.getEnvelopeInternal();
        assertEquals(0.0, envelope.getMinX(), 1e-9);
        assertEquals(5.5, envelope.getMaxX(), 1e-9);
        assertEquals(-0.5, envelope.getMinY(), 1e-9);
        assertEquals(10.0, envelope.getMaxY(), 1e-9);
    }
}