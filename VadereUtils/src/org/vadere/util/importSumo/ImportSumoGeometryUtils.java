package org.vadere.util.importSumo;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.locationtech.jts.algorithm.MinimumDiameter;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.index.quadtree.Quadtree;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.Vector2D;
import org.vadere.util.importSumo.processors.inverseSpace.SumoInvertSettings;
import org.vadere.util.io.CollectionUtils;
import org.vadere.util.logging.Logger;
import org.vadere.util.math.InterpolationUtil;
import org.vadere.util.math.MathUtil;

import javax.annotation.Nullable;
import java.util.*;
import java.util.List;

public class ImportSumoGeometryUtils {
    private static final GeometryFactory geometryFactory = new GeometryFactory();
    private static final Logger logger = Logger.getLogger(ImportSumoGeometryUtils.class);

    @NotNull
    public static List<Polygon> calculateInverse(List<Polygon> polygons,  SumoInvertSettings invertGroupSettings) {
        Envelope bounds = getBounds(polygons);
        return calculateInverse(polygons, bounds, invertGroupSettings);
    }

    @NotNull
    public static List<Polygon> calculateInverse(Polygon polygon, SumoInvertSettings invertGroupSettings) {
        Envelope bounds = polygon.getEnvelopeInternal();
        return calculateInverse(polygon, bounds, invertGroupSettings);
    }

    @NotNull
    public static List<Polygon> calculateInverse(Polygon polygon, Envelope bounds, SumoInvertSettings invertGroupSettings) {
        ArrayList<Polygon> polygonsList = new ArrayList<>(1);
        polygonsList.add(polygon);
        return calculateInverse(polygonsList, bounds, invertGroupSettings);
    }

    @NotNull
    public static List<Polygon> calculateInverse(List<Polygon> polygons, Envelope bounds, SumoInvertSettings invertGroupSettings) {
        Map<CoordinateXY, List<Polygon>> inverseSquares = calculateInverseSquares(polygons, bounds, invertGroupSettings.getCellSize(), invertGroupSettings.getMaxCellsToCombinePerAxis(), invertGroupSettings.getMinResultPolygonDiameter());
        List<Polygon> merged = mergePolygonsPreventingHoles(inverseSquares);

        Double minPolygonSize = invertGroupSettings.getMinPolygonSize();
        if(minPolygonSize != null){
            for (int i = merged.size() - 1; i >= 0; i--) {
                Polygon polygon = merged.get(i);
                if (polygon.getArea() < minPolygonSize) {
                    merged.remove(i);
                }
            }
        }

        return merged;
    }

    @NotNull
    private static Map<CoordinateXY, List<Polygon>> calculateInverseSquares(List<Polygon> allPolygons, Envelope bounds, double cellSize, @Nullable Integer maxCellsToCombinePerAxis, Double minimumResultPolygonDiameter) {
        Quadtree quadtree = new Quadtree();

        int rowCount = (int) Math.ceil((bounds.getMaxY() - bounds.getMinY()) / cellSize);
        int colCount = (int) Math.ceil((bounds.getMaxX() - bounds.getMinX()) / cellSize);

        double cellMinY = bounds.getMinY();

        for (int row = 0; row < rowCount; row++) {
            double cellMinX = bounds.getMinX();
            double cellMaxY = cellMinY + cellSize;
            for(int col = 0; col < colCount; col++) {
                double cellMaxX = cellMinX + cellSize;
                Polygon square = geometryFactory.createPolygon(new Coordinate[]{
                        new Coordinate(cellMinX, cellMinY),
                        new Coordinate(cellMinX, cellMaxY),
                        new Coordinate(cellMaxX, cellMaxY),
                        new Coordinate(cellMaxX, cellMinY),
                        new Coordinate(cellMinX, cellMinY),
                });

                quadtree.insert(square.getEnvelopeInternal(), square);

                cellMinX+= cellSize;
            }

            cellMinY+= cellSize;
        }

        for (Polygon polygon : allPolygons) {
            List queried = quadtree.query(polygon.getEnvelopeInternal());
            for (Object o : queried) {
                Polygon item = (Polygon)o;
                quadtree.remove(item.getEnvelopeInternal(), item);

                Geometry difference = getDifference(polygon, item);
                if(difference == null){
                    break;
                }

                for (int i = 0; i < difference.getNumGeometries(); i++) {
                    Geometry geometryN = difference.getGeometryN(i);
                    if(!(geometryN instanceof Polygon differencePolygon)){
                        continue;
                    }

                    if(differencePolygon.isEmpty() || differencePolygon.getNumInteriorRing() > 0){
                        continue;
                    }
                    MinimumDiameter md = new MinimumDiameter(differencePolygon);
                    if(minimumResultPolygonDiameter != null && md.getLength() < minimumResultPolygonDiameter){
                        continue;
                    }

                    Envelope envelopeInternal = differencePolygon.getEnvelopeInternal();

                    quadtree.insert(envelopeInternal, differencePolygon);
                }
            }
        }

        List all = quadtree.queryAll();
        return calculateCombineCellBuckets(cellSize, maxCellsToCombinePerAxis, all);
    }

    @Nullable
    private static Geometry getDifference(Polygon polygon, Polygon item) {
        try{
            return item.difference(polygon.buffer(0));
        }catch(Exception e){
           logger.error("Failed to calculate a difference for inversion");
           return null;
        }
    }

    @NotNull
    private static Map<CoordinateXY, List<Polygon>> calculateCombineCellBuckets(double cellSize, @Nullable Integer maxCellsToCombinePerAxis, List all) {
        Map<CoordinateXY, List<Polygon>> result = new Hashtable<>(all.size());

        if(maxCellsToCombinePerAxis == null){
                CoordinateXY coordinateXY = new CoordinateXY(0,0);
            for (Object o : all) {
                Polygon polygon = (Polygon) o;
                CollectionUtils.addToValueList(result, coordinateXY, polygon);
            }
            return result;
        }

        double bucketAreaSize = cellSize * maxCellsToCombinePerAxis;
        for (Object o : all) {
            Polygon polygon = (Polygon) o;

            Envelope env = polygon.getEnvelopeInternal();

            int x = (int) (env.getMinX() / bucketAreaSize);
            int y = (int) (env.getMinY() / bucketAreaSize);

            CoordinateXY coordinateXY = new CoordinateXY(x, y);
            CollectionUtils.addToValueList(result, coordinateXY, polygon);
        }

        return result;
    }

    private static List<Polygon> mergePolygonsPreventingHoles(Map<CoordinateXY, List<Polygon>> polygonBuckets) {
        Set<Polygon> allPolygons = new HashSet<>();

        for (Map.Entry<CoordinateXY, List<Polygon>> bucket : polygonBuckets.entrySet()) {
            Map<Coordinate, List<Polygon>> coordinateToPolygons = new Hashtable<>();

             for (Polygon polygon : bucket.getValue()) {

                boolean foundMerge;

                 do{
                     foundMerge = false;

                     for (Coordinate coordinate : polygon.getCoordinates()) {
                         List<Polygon> touchingPolygons = coordinateToPolygons.get(coordinate);
                         boolean foundTouchingPolygon = touchingPolygons != null;
                         if (!foundTouchingPolygon) {
                             continue;
                         }

                         for (Polygon polygonWithSameCoordinate : new ArrayList<>(touchingPolygons)) {
                             Geometry union = polygonWithSameCoordinate.union(polygon);
                             boolean hasHoles = hasHoles(union);
                             if (hasHoles) {
                                 continue;
                             }

                             for (Coordinate polygonCoordinate : polygonWithSameCoordinate.getCoordinates()) {
                                 CollectionUtils.removeFromValue(coordinateToPolygons, polygonCoordinate, polygonWithSameCoordinate);
                             }
                             allPolygons.remove(polygonWithSameCoordinate);

                             polygon = (Polygon) union;
                             foundMerge = true;
                             break;
                         }
                     }
                 } while (foundMerge);

                // no other polygon to merge found
                for (Coordinate coordinate : polygon.getCoordinates()) {
                    CollectionUtils.addToValueList(coordinateToPolygons, coordinate, polygon);
                }
                allPolygons.add(polygon);
            }
        }

        return new ArrayList<>(allPolygons);
    }

    public static Envelope getBounds(List<Polygon> polygons) {
        Envelope bounds = new Envelope();
        for (Polygon poly : polygons) {
            bounds.expandToInclude(poly.getEnvelopeInternal());
        }
        return bounds;
    }

    public static boolean hasHoles(Geometry union) {
        if(union.getNumGeometries() > 1){
            return true;
        }

        return ((Polygon) union.getGeometryN(0)).getNumInteriorRing() > 0;
    }

    public static LineString translateLineToTheRight(LineString line, double offsetDistance) {
        Coordinate[] lineCoords = line.getCoordinates();
        Coordinate[] shiftedCoords = new Coordinate[lineCoords.length];

        for (int i = 0; i < lineCoords.length; i++) {
            Coordinate current = lineCoords[i];
            Coordinate prev;
            Coordinate next;
            if(i < lineCoords.length - 1){
                prev = lineCoords[i];
                next = lineCoords[i + 1];
            }else{
                prev = lineCoords[i-1];
                next = lineCoords[i];
            }

            Vector2D normal = new Vector2D(next.x - prev.x, next.y - prev.y).normalize();

            // Shift the point (right) in the normal direction
            shiftedCoords[i] = new Coordinate(
                    current.x + offsetDistance * normal.getX(),
                    current.y + offsetDistance * normal.getY()
            );
        }

        return geometryFactory.createLineString(shiftedCoords);
    }

    public static Polygon expandLineToWidth(LineString line, double width) {
        double triangelDiagonal = Math.sqrt(width * width + width * width);
        Coordinate[] lineCoords = line.getCoordinates();

        List<Coordinate> leftCoords = new ArrayList<>();
        List<Coordinate> rightCoords = new ArrayList<>();

        for (int i = 0; i < lineCoords.length; i++) {
            Coordinate current = lineCoords[i];
            Pair<Vector2D, Double> orthogonalVectorAndAngle = calculateOrthogonalVectorAndAngleDegree(lineCoords, i);
            Vector2D orthogonalVector = orthogonalVectorAndAngle.getLeft();

            // Interpolate the width between the base width and the triangle's diagonal,
            // At 0° -> use base width, at 90° or higher -> use full diagonal.
            double angleDegree = orthogonalVectorAndAngle.getRight();
            double percentage = MathUtil.clamp(angleDegree, 0, 90) / 90.0f;
            double interpolatedWidth = InterpolationUtil.lerp(width, triangelDiagonal, percentage);
            double halfWidth = interpolatedWidth / 2;

            // Offset the points by halfWidth (left and right)
            leftCoords.add(new Coordinate(current.x + orthogonalVector.x * halfWidth, current.y + orthogonalVector.y * halfWidth));
            rightCoords.add(new Coordinate(current.x - orthogonalVector.x * halfWidth, current.y - orthogonalVector.y * halfWidth));
        }

        // Create the polygon by first adding left and then right points
        List<Coordinate> polygonCoords = new ArrayList<>();
        for (int i = 0; i < leftCoords.size(); i++) {
            polygonCoords.add(leftCoords.get(i));
        }
        for (int i = rightCoords.size() - 1; i >= 0; i--) {
            polygonCoords.add(rightCoords.get(i));
        }

        polygonCoords.add(polygonCoords.get(0)); // close the polygon

        Coordinate[] polygonArray = new Coordinate[polygonCoords.size()];
        polygonArray = polygonCoords.toArray(polygonArray);
        return geometryFactory.createPolygon(polygonArray);
    }

    /**
     * Calculates an orthogonal vector at a given coordinate of a line
     * and the angle (in degrees) between the segments before and after this point.
     */
    private static Pair<Vector2D, Double> calculateOrthogonalVectorAndAngleDegree(Coordinate[] lineCoords, int coordinateIndex) {
        Coordinate current = lineCoords[coordinateIndex];

        Coordinate prev;
        Coordinate next;
        boolean isStartOrEndCoordinate = coordinateIndex == 0 || coordinateIndex == lineCoords.length - 1;
        if(isStartOrEndCoordinate){
            if(coordinateIndex == 0){
                prev = lineCoords[coordinateIndex];
                next = lineCoords[coordinateIndex + 1];
            }else{
                prev = lineCoords[coordinateIndex - 1];
                next = lineCoords[coordinateIndex];
            }
            return Pair.of(new Vector2D(-(next.y - prev.y), next.x - prev.x).normalize(), 0.0);
        }

        prev = lineCoords[coordinateIndex - 1];
        next = lineCoords[coordinateIndex + 1];

        Vector2D prevToCurrentOrthogonal = new Vector2D(-(current.y - prev.y), current.x - prev.x).normalize();
        Vector2D currentToNextOrthogonal = new Vector2D(-(next.y - current.y), next.x - current.x).normalize();
        Vector2D averageOrthogonal = prevToCurrentOrthogonal.add(currentToNextOrthogonal).divide(2);

        double angle = GeometryUtils.smallestAngleBetweenDegree(prevToCurrentOrthogonal, currentToNextOrthogonal);

        return Pair.of(averageOrthogonal, angle);
    }
}
