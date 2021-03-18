package org.vadere.state.scenario;

import junit.framework.TestCase;
import org.junit.Test;
import org.vadere.state.attributes.AttributesEmbedShape;
import org.vadere.state.attributes.scenario.AttributesAerosolCloud;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VPoint;


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
        VCircle shape = new VCircle(new VPoint(2, 2), 3);
        VCircle newShape = new VCircle(new VPoint(3, 3), 4);
        double creationTime = 999.9;
        double pathogenLoad = 10e9;
        double lifeTime = 60*60*3;
        boolean hasReachedLifeEnd = false;

        AerosolCloud aerosolCloudOriginal = new AerosolCloud(new AttributesAerosolCloud(id, shape, creationTime, pathogenLoad, lifeTime, hasReachedLifeEnd));
        AerosolCloud aerosolCloudClone = aerosolCloudOriginal.clone();

        assertEquals(aerosolCloudOriginal.getId(), aerosolCloudClone.getId());
        assertEquals(aerosolCloudOriginal.getShape(), aerosolCloudClone.getShape());
        assertEquals(aerosolCloudOriginal.getCreationTime(), aerosolCloudClone.getCreationTime(), ALLOWED_DOUBLE_TOLERANCE);
        assertEquals(aerosolCloudOriginal.getPathogenLoad(), aerosolCloudClone.getPathogenLoad(), ALLOWED_DOUBLE_TOLERANCE);
        assertEquals(aerosolCloudOriginal.getHasReachedLifeEnd(), aerosolCloudClone.getHasReachedLifeEnd());
        assertEquals(aerosolCloudOriginal.getLifeTime(), aerosolCloudClone.getLifeTime(),ALLOWED_DOUBLE_TOLERANCE);

        // check that original is not affected by setters/changes to the clone
        aerosolCloudClone.setId(2);
        aerosolCloudClone.setShape(newShape);
        aerosolCloudClone.setCreationTime(0);
        aerosolCloudClone.setPathogenLoad(10e10);
        aerosolCloudClone.setHasReachedLifeEnd(true);

        assertEquals(id, aerosolCloudOriginal.getId());
        assertEquals(shape, aerosolCloudOriginal.getShape());
        assertEquals(creationTime, aerosolCloudOriginal.getCreationTime(), ALLOWED_DOUBLE_TOLERANCE);
        assertEquals(pathogenLoad, aerosolCloudOriginal.getPathogenLoad(), ALLOWED_DOUBLE_TOLERANCE);
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