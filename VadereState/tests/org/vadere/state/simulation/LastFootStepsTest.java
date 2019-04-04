package org.vadere.state.simulation;

import org.junit.Before;
import org.junit.Test;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.ArrayList;

import static org.junit.Assert.*;

public class LastFootStepsTest {

    private static Double ALLOWED_DOUBLE_ERROR_FOR_TIME = 10e-3;

    private int expectedFootStepCapacity;
    private LastFootSteps lastFootSteps;

    @Before
    public void setUp() {
        expectedFootStepCapacity = 10;
        lastFootSteps = new LastFootSteps(expectedFootStepCapacity);
    }


    @Test
    public void getCapacityReturnsCapacityPassedToContructor() {
        int expectedCapacity = 10;
        LastFootSteps lastFootSteps = new LastFootSteps(expectedCapacity);

        int actualCapacity = lastFootSteps.getCapacity();

        assertEquals(expectedCapacity, actualCapacity);
    }

    @Test
    public void getFootStepsReturnsAnEmptyFootStepListByDefault() {
        int expectedListSize = 0;
        assertEquals(expectedListSize, lastFootSteps.getFootSteps().size());
    }

    @Test
    public void getFootStepsReturnsListOfSizeOneIfOneElementWasAdded() {
        FootStep footStep = new FootStep();
        lastFootSteps.getFootSteps().add(footStep);

        int expectedListSize = 1;
        assertEquals(expectedListSize, lastFootSteps.getFootSteps().size());
    }

    @Test
    public void addInsertsAnElementAndSizeIsIncrementedProperly() {
        FootStep footStep = new FootStep();
        lastFootSteps.add(footStep);

        int expectedListSize = 1;
        assertEquals(expectedListSize, lastFootSteps.getFootSteps().size());
    }

    @Test
    public void addInsertsThePassedFootStep() {
        FootStep expectedFootStep = new FootStep(VPoint.ZERO, VPoint.ZERO, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        lastFootSteps.add(expectedFootStep);

        FootStep actualFootStep = lastFootSteps.getFootSteps().get(0);

        assertEquals(expectedFootStep.getEnd(), actualFootStep.getEnd());
        assertEquals(expectedFootStep.getStart(), actualFootStep.getStart());
        assertEquals(expectedFootStep.getEndTime(), actualFootStep.getEndTime(), ALLOWED_DOUBLE_ERROR_FOR_TIME);
        assertEquals(expectedFootStep.getStartTime(), actualFootStep.getStartTime(), ALLOWED_DOUBLE_ERROR_FOR_TIME);
    }

    @Test
    public void addAppendsElements() {
        int totalFootSteps = 2;
        FootStep[] expectedFootSteps = new FootStep[totalFootSteps];

        for (int i = 0; i < totalFootSteps; i++) {
            double startPositionX = i;
            double startTime = i;

            FootStep currentFootStep = new FootStep(new VPoint(startPositionX, 0), new VPoint(startPositionX + 1, 0),
                    startTime, startTime + 1);
            expectedFootSteps[i] = currentFootStep;

            lastFootSteps.add(currentFootStep);
        }

        ArrayList<FootStep> actualFootSteps = lastFootSteps.getFootSteps();

        for (int i = 0; i < actualFootSteps.size(); i++) {
            assertEquals(expectedFootSteps[i].getStartTime(), lastFootSteps.getFootSteps().get(i).getStartTime(),
                    ALLOWED_DOUBLE_ERROR_FOR_TIME);
        }
    }

    @Test
    public void addRemovesOldestElementIfCapacityIsExceeded() {
            int totalFootSteps = expectedFootStepCapacity * 4;

        for (int i = 0; i < totalFootSteps; i++) {
            double startPositionX = i;
            double startTime = i;

            FootStep currentFootStep = new FootStep(new VPoint(startPositionX, 0), new VPoint(startPositionX + 1, 0),
                    startTime, startTime + 1);

            lastFootSteps.add(currentFootStep);

            if (i < expectedFootStepCapacity) {
                assertEquals(currentFootStep.getStartTime(), lastFootSteps.getFootSteps().get(i).getStartTime(), ALLOWED_DOUBLE_ERROR_FOR_TIME);
            } else {
                double expectedStartTimeHead = (startTime - expectedFootStepCapacity) + 1;
                double actualStartTimeHead = lastFootSteps.getFootSteps().get(0).getStartTime();

                assertEquals(expectedStartTimeHead, actualStartTimeHead, ALLOWED_DOUBLE_ERROR_FOR_TIME);

                double expectedStartTimeTail = startTime;
                double actualStartTimeTail = lastFootSteps.getFootSteps().get(expectedFootStepCapacity - 1).getStartTime();

                assertEquals(expectedStartTimeTail, actualStartTimeTail, ALLOWED_DOUBLE_ERROR_FOR_TIME);
            }
        }
    }

    @Test
    public void getAverageSpeedInMeterPerSecond() {
        // TODO: Implement test.
    }

    @Test
    public void getOldestFootStep() {
        // TODO: Implement test.
    }

    @Test
    public void getYoungestFootStep() {
        // TODO: Implement test.
    }
}