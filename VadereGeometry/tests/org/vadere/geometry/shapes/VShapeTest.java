package org.vadere.geometry.shapes;

import org.junit.Test;

import static org.junit.Assert.*;

public class VShapeTest {

    @Test
    public void testIntersectShapesReturnsTrueWithOverlappingShapes(){
        VShape a = new VRectangle(0,0,1,1);
        VShape b = new VRectangle(0,0,1,1);

        assertTrue(a.intersects(b));
    }

    @Test
    public void testIntersectShapesReturnsFalseWithNonOverlappingShapes(){
        VShape a = new VRectangle(0,0,1,1);
        VShape b = new VRectangle(1.1,0,1,1);

        assertFalse(a.intersects(b));
    }

    @Test
    public void testIntersectShapesReturnsTrueWithOverlappingCircles(){
        VShape a = new VRectangle(0,0,1,1);
        VShape b = new VCircle(new VPoint(-1, -1), 5.0);

        assertTrue(a.intersects(b));
    }
}