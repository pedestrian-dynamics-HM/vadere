package org.vadere.state.simulation;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.ArrayList;

import static org.junit.Assert.*;

public class LastFootStepsTest {

    enum Direction { HORIZONTAL, VERTICAL, DIAGONAL};

    private static Double ALLOWED_DOUBLE_ERROR = 10e-3;

    private int expectedFootStepCapacity;
    private LastFootSteps lastFootSteps;

    // Helper method
    private void addFootStepsToLastFootSteps(int totalFootSteps, double stepLength, Direction direction) {
        lastFootSteps.getFootSteps().clear();

        for (int i = 0; i < totalFootSteps; i++) {
            FootStep currentFootStep;

            if (direction == Direction.HORIZONTAL) {
                int y = 0;
                currentFootStep = new FootStep(new VPoint(i, y), new VPoint(i + stepLength, y),
                        i, i + 1);
            } else if (direction == Direction.VERTICAL) {
                int x = 0;
                currentFootStep = new FootStep(new VPoint(x, i), new VPoint(x, i + stepLength),
                        i, i + 1);
            } else if (direction == Direction.DIAGONAL) {
                currentFootStep = new FootStep(new VPoint(i, i), new VPoint(i + stepLength, i + stepLength),
                        i, i + 1);
            } else {
                throw new UnsupportedOperationException();
            }

            lastFootSteps.add(currentFootStep);
        }
    }

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
        assertEquals(expectedFootStep.getEndTime(), actualFootStep.getEndTime(), ALLOWED_DOUBLE_ERROR);
        assertEquals(expectedFootStep.getStartTime(), actualFootStep.getStartTime(), ALLOWED_DOUBLE_ERROR);
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
                    ALLOWED_DOUBLE_ERROR);
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
                assertEquals(currentFootStep.getStartTime(), lastFootSteps.getFootSteps().get(i).getStartTime(), ALLOWED_DOUBLE_ERROR);
            } else {
                double expectedStartTimeHead = (startTime - expectedFootStepCapacity) + 1;
                double actualStartTimeHead = lastFootSteps.getFootSteps().get(0).getStartTime();

                assertEquals(expectedStartTimeHead, actualStartTimeHead, ALLOWED_DOUBLE_ERROR);

                double expectedStartTimeTail = startTime;
                double actualStartTimeTail = lastFootSteps.getFootSteps().get(expectedFootStepCapacity - 1).getStartTime();

                assertEquals(expectedStartTimeTail, actualStartTimeTail, ALLOWED_DOUBLE_ERROR);
            }
        }
    }

    @Test
    public void getAverageSpeedInMeterPerSecondReturnsDoubleNaNIfFootStepListIsEmpty() {
        int totalFootSteps = 0;
        double stepLength = 1;
        addFootStepsToLastFootSteps(totalFootSteps, stepLength, Direction.HORIZONTAL);

        double expectedSpeedMeterPerSecond = Double.NaN;
        double actualSpeedMeterPerSecond = lastFootSteps.getAverageSpeedInMeterPerSecond();

        assertEquals(expectedSpeedMeterPerSecond, actualSpeedMeterPerSecond, ALLOWED_DOUBLE_ERROR);
    }

    @Test
    public void getAverageSpeedInMeterPerSecondCalculatesSpeedProperlyIfOnlyOneHorizontalFootStepExists() {
        int totalFootSteps = 1;
        double stepLength = 1;
        addFootStepsToLastFootSteps(totalFootSteps, stepLength, Direction.HORIZONTAL);

        double expectedSpeedMeterPerSecond = 1;
        double actualSpeedMeterPerSecond = lastFootSteps.getAverageSpeedInMeterPerSecond();

        assertEquals(expectedSpeedMeterPerSecond, actualSpeedMeterPerSecond, ALLOWED_DOUBLE_ERROR);
    }

    @Test
    public void getAverageSpeedInMeterPerSecondCalculatesSpeedProperlyIfOnlyOneVerticalFootStepExists() {
        int totalFootSteps = 1;
        double stepLength = 1;
        addFootStepsToLastFootSteps(totalFootSteps, stepLength, Direction.VERTICAL);

        double expectedSpeedMeterPerSecond = 1;
        double actualSpeedMeterPerSecond = lastFootSteps.getAverageSpeedInMeterPerSecond();

        assertEquals(expectedSpeedMeterPerSecond, actualSpeedMeterPerSecond, ALLOWED_DOUBLE_ERROR);
    }

    @Test
    public void getAverageSpeedInMeterPerSecondCalculatesSpeedProperlyIfOnlyOneDiagonalFootStepExists() {
        int totalFootSteps = 1;
        double stepLength = 1;
        addFootStepsToLastFootSteps(totalFootSteps, stepLength, Direction.DIAGONAL);

        double expectedSpeedMeterPerSecond = Math.sqrt(2);
        double actualSpeedMeterPerSecond = lastFootSteps.getAverageSpeedInMeterPerSecond();

        assertEquals(expectedSpeedMeterPerSecond, actualSpeedMeterPerSecond, ALLOWED_DOUBLE_ERROR);
    }

    @Test
    public void getAverageSpeedInMeterPerSecondCalculatesSpeedProperlyIf20HorizontalFootStepExists() {
        int totalFootSteps = 20;
        double stepLength = 1;
        addFootStepsToLastFootSteps(totalFootSteps, stepLength, Direction.HORIZONTAL);

        double expectedSpeedMeterPerSecond = 1;
        double actualSpeedMeterPerSecond = lastFootSteps.getAverageSpeedInMeterPerSecond();

        assertEquals(expectedSpeedMeterPerSecond, actualSpeedMeterPerSecond, ALLOWED_DOUBLE_ERROR);
    }

    @Test
    public void getAverageSpeedInMeterPerSecondCalculatesSpeedProperlyIf20VerticalFootStepExists() {
        int totalFootSteps = 20;
        double stepLength = 1;
        addFootStepsToLastFootSteps(totalFootSteps, stepLength, Direction.VERTICAL);

        double expectedSpeedMeterPerSecond = 1;
        double actualSpeedMeterPerSecond = lastFootSteps.getAverageSpeedInMeterPerSecond();

        assertEquals(expectedSpeedMeterPerSecond, actualSpeedMeterPerSecond, ALLOWED_DOUBLE_ERROR);
    }

    @Test
    public void getAverageSpeedInMeterPerSecondCalculatesSpeedProperlyIf20DiagonalFootStepExists() {
        int totalFootSteps = 20;
        double stepLength = 1;
        addFootStepsToLastFootSteps(totalFootSteps, stepLength, Direction.DIAGONAL);

        double expectedSpeedMeterPerSecond = Math.sqrt(2);
        double actualSpeedMeterPerSecond = lastFootSteps.getAverageSpeedInMeterPerSecond();

        assertEquals(expectedSpeedMeterPerSecond, actualSpeedMeterPerSecond, ALLOWED_DOUBLE_ERROR);
    }

    @Test
    public void getAverageSpeedInMeterPerSecondCalculatesSpeedProperlyThreeUnequalFootStepsExists() {
        int totalFootSteps = 3;

        int time = 0;
        FootStep horizontalFootStepHalfMeter = new FootStep(new VPoint(0, 0), new VPoint(0.5, 0),
                time, time + 1);
        time += 1;

        FootStep verticalFootStepTwoMeter = new FootStep(new VPoint(0.5, 0), new VPoint(0.5, 2),
                time, time + 1);
        time += 1;

        FootStep diagonalFootStepOneMeter = new FootStep(new VPoint(0.5, 2), new VPoint(1.5, 3),
                time, time + 1);
        time += 1;

        lastFootSteps.add(horizontalFootStepHalfMeter);
        lastFootSteps.add(verticalFootStepTwoMeter);
        lastFootSteps.add(diagonalFootStepOneMeter);

        double expectedSpeedMeterPerSecond = (0.5 + 2 + Math.sqrt(2)) / time;
        double actualSpeedMeterPerSecond = lastFootSteps.getAverageSpeedInMeterPerSecond();

        assertEquals(expectedSpeedMeterPerSecond, actualSpeedMeterPerSecond, ALLOWED_DOUBLE_ERROR);
    }

    @Test
    public void getOldestFootStepReturnsNullIfNoFootStepsExist() {
        assertNull(lastFootSteps.getOldestFootStep());
    }

    @Test
    public void getOldestFootStepReturnsOldestFootStep() {
        double expectedXWithinCapacity = -1;
        FootStep oldestFootStep = new FootStep(new VPoint(expectedXWithinCapacity, 0), new VPoint(0, 0),
                0, 1);
        lastFootSteps.add(oldestFootStep);

        for (int i = 0; i < expectedFootStepCapacity * 2; i++) {
            double expectedXOutsideCapacity = -2;
            FootStep currentFootStep = new FootStep(new VPoint(expectedXOutsideCapacity, 0), new VPoint(2, 0),
                    i, i + 1);
            lastFootSteps.add(currentFootStep);

            double expectedValue = (i < expectedFootStepCapacity - 1) ? expectedXWithinCapacity : expectedXOutsideCapacity;
            double actualX = lastFootSteps.getOldestFootStep().getStart().x;

            assertEquals(expectedValue, actualX, ALLOWED_DOUBLE_ERROR);
        }
    }

    @Test
    public void getYoungestFootStepReturnsNullIfNoFootStepsExist() {
        assertNull(lastFootSteps.getYoungestFootStep());
    }

    @Test
    public void getYoungestFootStepReturnsYoungestFootStep() {
        for (int i = 0; i < expectedFootStepCapacity * 2; i++) {
            FootStep currentFootStep = new FootStep(new VPoint(i, 0), new VPoint(i + 1, 0),
                    i, i + 1);
            lastFootSteps.add(currentFootStep);

            double expectedValue = currentFootStep.getStart().x;
            double actualX = lastFootSteps.getYoungestFootStep().getStart().x;

            assertEquals(expectedValue, actualX, ALLOWED_DOUBLE_ERROR);
        }
    }
}