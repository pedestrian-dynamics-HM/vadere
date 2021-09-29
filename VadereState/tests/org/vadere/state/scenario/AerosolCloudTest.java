package org.vadere.state.scenario;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.vadere.state.attributes.scenario.AttributesAerosolCloud;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VShape;

import java.util.ArrayList;
import java.util.Arrays;

import static org.vadere.state.attributes.Attributes.ID_NOT_SET;


public class AerosolCloudTest {
    public static double ALLOWED_DOUBLE_TOLERANCE = 10e-3;

    protected AerosolCloud aerosolCloud = new AerosolCloud();
    private AerosolCloud aerosolCloudCirc;
    private AerosolCloud aerosolCloudEllip;


    @Before
    public void setUp() {
        // common attributes
        int id = ID_NOT_SET;
        double area = 1;
        double height = 1;
        VPoint center = new VPoint(10, 10);
        double creationTime = 0;
        double halfLife = 60;
        double initialPathogenLoad = 10e4;

        // attributes for circular aerosolCloud
        VPoint vertexCirc1 = new VPoint(center.x, center.y);
        VPoint vertexCirc2 = new VPoint(center.x, center.y);
        ArrayList<VPoint> verticesCirc = new ArrayList<>(Arrays.asList(vertexCirc1, vertexCirc2));
        VShape shapeCirc = AerosolCloud.createTransformedAerosolCloudShape(vertexCirc1, vertexCirc2, area);
        AttributesAerosolCloud attributesCirc = new AttributesAerosolCloud(id, shapeCirc, area, height, center, verticesCirc, creationTime, halfLife, initialPathogenLoad, initialPathogenLoad);
        aerosolCloudCirc = new AerosolCloud(attributesCirc);

        // attributes for elliptical aerosolCloud
        double minSemiAxis = 2 * Math.sqrt(area / Math.PI); // assure that semiAxis is greater than radius
        VPoint vertexEllip1 = new VPoint(center.x, center.y - minSemiAxis);
        VPoint vertexEllip2 = new VPoint(center.x, center.y + minSemiAxis);
        ArrayList<VPoint> verticesEllip = new ArrayList<>(Arrays.asList(vertexEllip1, vertexEllip2));
        VShape shapeEllip = AerosolCloud.createTransformedAerosolCloudShape(vertexEllip1, vertexEllip2, area);
        AttributesAerosolCloud attributesEllip = new AttributesAerosolCloud(id, shapeEllip, area, height, center, verticesEllip, creationTime, halfLife, initialPathogenLoad, initialPathogenLoad);
        aerosolCloudEllip = new AerosolCloud(attributesEllip);
    }


    @Test
    public void testGetId() {
        int expectedId = ID_NOT_SET;
        Assert.assertEquals(expectedId, aerosolCloud.getId());
    }


    @Test
    public void testSetShape() {
        VCircle newShape = new VCircle(new VPoint(10, 10), 2);

        aerosolCloud.setShape(newShape);
        Assert.assertEquals(newShape, aerosolCloud.getShape());
    }


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

        AerosolCloud aerosolCloudOriginal = new AerosolCloud(new AttributesAerosolCloud(id, shape, area, height, center, vertices, creationTime, pathogenLoad, pathogenLoad, lifeTime));
        AerosolCloud aerosolCloudClone = aerosolCloudOriginal.clone();

        Assert.assertEquals(aerosolCloudOriginal.getId(), aerosolCloudClone.getId());
        Assert.assertEquals(aerosolCloudOriginal.getShape(), aerosolCloudClone.getShape());
        Assert.assertEquals(aerosolCloudOriginal.getCreationTime(), aerosolCloudClone.getCreationTime(), ALLOWED_DOUBLE_TOLERANCE);
        Assert.assertEquals(aerosolCloudOriginal.getHalfLife(), aerosolCloudClone.getHalfLife(),ALLOWED_DOUBLE_TOLERANCE);

        // check that original is not affected by setters/changes to the clone
        aerosolCloudClone.setId(2);
        aerosolCloudClone.setShape(newShape);
        aerosolCloudClone.setCreationTime(0);

        Assert.assertEquals(id, aerosolCloudOriginal.getId());
        Assert.assertEquals(shape, aerosolCloudOriginal.getShape());
        Assert.assertEquals(creationTime, aerosolCloudOriginal.getCreationTime(), ALLOWED_DOUBLE_TOLERANCE);
    }

    @Test
    public void throwIfIncreaseShapeChangesCenterOfCircularAerosolCloud() {
        VPoint center = aerosolCloudCirc.getCenter();
        aerosolCloudCirc.increaseShape(0.2);
        Assert.assertEquals(center, aerosolCloudCirc.getCenter());
    }

    @Test
    public void throwIfIncreaseShapeChangesCenterOfEllipticalAerosolCloud() {
        VPoint center = aerosolCloudEllip.getCenter();
        aerosolCloudEllip.increaseShape(0.2);
        Assert.assertEquals(center, aerosolCloudEllip.getCenter());
    }

    @Test
    public void throwIfEllipticalAndCircularAreaNotIncreasedEqually() {
        aerosolCloudEllip.increaseShape(0.2);
        aerosolCloudCirc.increaseShape(0.2);
        Assert.assertEquals(aerosolCloudCirc.getArea(), aerosolCloudEllip.getArea(), ALLOWED_DOUBLE_TOLERANCE);
    }

}