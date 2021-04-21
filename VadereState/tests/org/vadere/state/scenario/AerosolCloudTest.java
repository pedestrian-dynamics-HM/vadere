package org.vadere.state.scenario;

import junit.framework.TestCase;
import org.junit.Test;
import org.vadere.state.attributes.AttributesEmbedShape;
import org.vadere.state.attributes.scenario.AttributesAerosolCloud;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.ArrayList;
import java.util.Arrays;


public class AerosolCloudTest extends TestCase {
    public static double ALLOWED_DOUBLE_TOLERANCE = 10e-3;

    protected AerosolCloud aerosolCloud = new AerosolCloud();

    @Test
    public void testGetShape() {
        VCircle expectedShape = new VCircle(0.75);
        assertEquals(expectedShape, aerosolCloud.getShape());
    }

    @Test
    public void testGetId() {
        int expectedId = AttributesEmbedShape.ID_NOT_SET;
        assertEquals(expectedId, aerosolCloud.getId());
    }

//    @Test
//    public void testGetType() {
//    }
//
//    @Test
//    public void testGetAttributes() {
//    }
//
//    @Test
//    public void testGetLifeTime() {
//    }
//
//    @Test
//    public void testGetCreationTime() {
//    }
//
//    @Test
//    public void testGetPathogenLoad() {
//    }
//
//    @Test
//    public void testGetHasReachedLifeEnd() {
//    }

    @Test
    public void testSetShape() {
        VCircle newShape = new VCircle(new VPoint(10, 10), 2);

        aerosolCloud.setShape(newShape);
        assertEquals(newShape, aerosolCloud.getShape());
    }

//    @Test
//    public void testSetAttributes() {
//    }
//
//    @Test
//    public void testSetId() {
//    }
//
//    @Test
//    public void testSetCreationTime() {
//    }
//
//    @Test
//    public void testSetPathogenLoad() {
//    }
//
//    @Test
//    public void testSetHasReachedLifeEnd() {
//    }

    @Test
    public void testTestClone() {
        int id = 1;
        double radius = 3.0;
        VPoint center = new VPoint(2, 2);
        VCircle shape = new VCircle(center, radius);
        double area = radius * radius * Math.PI;
        double height = 1.0;
        ArrayList<VPoint> vertices = new ArrayList<>(Arrays.asList(new VPoint(0, 0), new VPoint(0, 0)));

        VCircle newShape = new VCircle(new VPoint(3, 3), 4);
        double creationTime = 999.9;
        double pathogenLoad = 10e9;
        double lifeTime = 60*60*3;
        boolean hasReachedLifeEnd = false;

        AerosolCloud aerosolCloudOriginal = new AerosolCloud(new AttributesAerosolCloud(id, shape, area, height, center, vertices, creationTime, pathogenLoad, pathogenLoad, lifeTime, hasReachedLifeEnd));
        AerosolCloud aerosolCloudClone = aerosolCloudOriginal.clone();

        assertEquals(aerosolCloudOriginal.getId(), aerosolCloudClone.getId());
        assertEquals(aerosolCloudOriginal.getShape(), aerosolCloudClone.getShape());
        assertEquals(aerosolCloudOriginal.getCreationTime(), aerosolCloudClone.getCreationTime(), ALLOWED_DOUBLE_TOLERANCE);
        assertEquals(aerosolCloudOriginal.getHasReachedLifeEnd(), aerosolCloudClone.getHasReachedLifeEnd());
        assertEquals(aerosolCloudOriginal.getHalfLife(), aerosolCloudClone.getHalfLife(),ALLOWED_DOUBLE_TOLERANCE);

        // check that original is not affected by setters/changes to the clone
        aerosolCloudClone.setId(2);
        aerosolCloudClone.setShape(newShape);
        aerosolCloudClone.setCreationTime(0);
        aerosolCloudClone.setHasReachedLifeEnd(true);

        assertEquals(id, aerosolCloudOriginal.getId());
        assertEquals(shape, aerosolCloudOriginal.getShape());
        assertEquals(creationTime, aerosolCloudOriginal.getCreationTime(), ALLOWED_DOUBLE_TOLERANCE);
        assertEquals(hasReachedLifeEnd, aerosolCloudOriginal.getHasReachedLifeEnd());
    }

//    @Test
//    public void testTestHashCode() {
//    }
//
//    @Test
//    public void testTestEquals() {
//    }

}