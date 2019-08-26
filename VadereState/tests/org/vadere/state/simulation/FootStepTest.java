package org.vadere.state.simulation;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import org.vadere.util.geometry.shapes.VPoint;

public class FootStepTest {

    private FootStep footStepHorizontal;
    private FootStep footStepVertical;
    private FootStep footStepDiagonal;


    @Before
    public void setup(){
        footStepHorizontal = new FootStep(new VPoint(1,1), new VPoint(2,1), 0,1);
        footStepVertical = new FootStep(new VPoint(1,1), new VPoint(1,2), 0,1);
        footStepDiagonal = new FootStep(new VPoint(1,1), new VPoint(2,2), 0,1);
    }


    @Test
    public void interpolationTestStart(){

        // Give start point of FootStep

        VPoint actual, expected;

        actual = FootStep.interpolateFootStep(footStepHorizontal, 0);
        expected = new VPoint(1 ,1.);

        assertEquals(actual, expected);

        actual = FootStep.interpolateFootStep(footStepVertical, 0);
        expected = new VPoint(1 ,1.);

        assertEquals(actual, expected);

        actual = FootStep.interpolateFootStep(footStepDiagonal, 0);
        expected = new VPoint(1 ,1.);

        assertEquals(actual, expected);
    }


    @Test
    public void interpolationTestMid(){
        // Get point in the middle of direction from start

        VPoint actual, expected;

        actual = FootStep.interpolateFootStep(footStepHorizontal, 0.5);
        expected = new VPoint(1.5 ,1.);

        assertEquals(actual, expected);

        actual = FootStep.interpolateFootStep(footStepVertical, 0.5);
        expected = new VPoint(1. ,1.5);

        assertEquals(actual, expected);

        actual = FootStep.interpolateFootStep(footStepDiagonal, 0.5);
        expected = new VPoint(1.5 ,1.5);

        assertEquals(actual, expected);
    }


    @Test
    public void interpolationTestEnd(){
        // Get last point of FootStep

        VPoint actual, expected;

        actual = FootStep.interpolateFootStep(footStepHorizontal, 1);
        expected = new VPoint(2. ,1.); // Give same start point

        assertEquals(actual, expected);

        actual = FootStep.interpolateFootStep(footStepVertical, 1);
        expected = new VPoint(1. ,2.); // Give same start point

        assertEquals(actual, expected);

        actual = FootStep.interpolateFootStep(footStepDiagonal, 1);
        expected = new VPoint(2. ,2.); // Give same start point

        assertEquals(actual, expected);
    }


    @Test
    public void interpolationTestTinyFootStep(){
        FootStep footStep = new FootStep(new VPoint(0,0), new VPoint(0.0001,0), 0, 0+1E-15);
        VPoint actual = FootStep.interpolateFootStep(footStep, 1E-16);
        VPoint expected = footStep.getStart(); // Return start, when footstep duration is too small

        assertEquals(actual, expected);
    }


    @Test(expected = IllegalArgumentException.class)
    public void interpolationTestFail01(){
        FootStep.interpolateFootStep(footStepHorizontal, 2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void interpolationTestFail02(){
        FootStep.interpolateFootStep(footStepHorizontal, -1);
    }


}
