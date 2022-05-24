package org.vadere.state.psychology.perception.types;

import org.junit.Test;
import org.vadere.util.geometry.shapes.ShapeType;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VRectangle;

import java.util.LinkedList;

import static org.junit.Assert.*;

public class StimulusTest {

    public static double ALLOWED_DOUBLE_TOLERANCE = 10e-3;

    @Test
    public void testThreatClone() {
        double expectedTime = 1;
        int expectedOriginAsTargetId = 2;

        Threat threatOriginal = new Threat(expectedTime, expectedOriginAsTargetId);

        Threat threatClone = threatOriginal.clone();
        threatClone.setTime(3);

        assertEquals(expectedTime, threatOriginal.getTime(), ALLOWED_DOUBLE_TOLERANCE);
        assertEquals(threatOriginal.getLoudness(), threatClone.getLoudness(), ALLOWED_DOUBLE_TOLERANCE);
    }

    @Test
    public void testChangeTargetClone() {
        double expectedTime = 1;
        LinkedList<Integer> expectedTargetIdList = new LinkedList<>();
        expectedTargetIdList.add(1);
        expectedTargetIdList.add(2);

        ChangeTarget changeTargetOriginal = new ChangeTarget(expectedTime, expectedTargetIdList);

        ChangeTarget changeTargetClone = changeTargetOriginal.clone();
        LinkedList<Integer> newTargetIdList = new LinkedList<>();
        newTargetIdList.add(3);
        changeTargetClone.setNewTargetIds(newTargetIdList);

        assertEquals(2, changeTargetOriginal.getNewTargetIds().size());
        assertEquals(1, changeTargetClone.getNewTargetIds().size());
        assertEquals(changeTargetOriginal.getTime(), changeTargetClone.getTime(), ALLOWED_DOUBLE_TOLERANCE);
    }

    @Test
    public void testElapsedClone() {
        double expectedTime = 1;
        ElapsedTime elapsedTimeOriginal = new ElapsedTime(expectedTime);

        ElapsedTime elapsedTimeClone =  elapsedTimeOriginal.clone();
        elapsedTimeClone.setTime(-1);

        assertEquals(expectedTime, elapsedTimeOriginal.getTime(), ALLOWED_DOUBLE_TOLERANCE);
        assertEquals(-1, elapsedTimeClone.getTime(), ALLOWED_DOUBLE_TOLERANCE);
    }

    @Test
    public void testWaitClone() {
        double expectedTime = 1;

        Wait waitOriginal = new Wait(expectedTime);

        Wait waitClone = waitOriginal.clone();
        waitClone.setTime(-1);

        assertEquals(expectedTime, waitOriginal.getTime(), ALLOWED_DOUBLE_TOLERANCE);
        assertEquals(-1, waitClone.getTime(), ALLOWED_DOUBLE_TOLERANCE);
    }

    @Test
    public void testWaitInAreaClone() {
        double expectedTime = 1;
        VRectangle rectangleOriginal = new VRectangle(0, 0, 2, 2);

        WaitInArea waitInAreaOriginal = new WaitInArea(expectedTime, rectangleOriginal);

        WaitInArea waitInAreaClone = waitInAreaOriginal.clone();
        VCircle circleNew = new VCircle(0, 0, 1);
        waitInAreaClone.setArea(circleNew);

        assertEquals(ShapeType.RECTANGLE, waitInAreaOriginal.getArea().getType());
        assertEquals(ShapeType.CIRCLE, waitInAreaClone.getArea().getType());
    }

}