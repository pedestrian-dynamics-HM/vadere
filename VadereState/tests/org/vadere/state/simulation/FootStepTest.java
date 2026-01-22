package org.vadere.state.simulation;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static  org.junit.jupiter.api.Assertions.*;
import static org.vadere.util.test.TestUtils.assertMissing;
import static org.vadere.util.test.TestUtils.assertPresent;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;

import java.util.Optional;

public class FootStepTest {

    private FootStep footStepHorizontal;
    private FootStep footStepVertical;
    private FootStep footStepDiagonal;


    @BeforeEach
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


    @Test
    public void interpolationTestFail01(){
        Assertions.assertThrows(IllegalArgumentException.class, ()->{
            FootStep.interpolateFootStep(footStepHorizontal, 2);
        });
    }

    @Test
    public void interpolationTestFail02(){
        Assertions.assertThrows(IllegalArgumentException.class, ()->{
            FootStep.interpolateFootStep(footStepHorizontal, -1);
        });
    }

    @ParameterizedTest
    @CsvSource({
            // startX, startY, endX, endY, expectedEnterX, expectedEnterY, expectedExitX, expectedExitY
            // cutting bottom left edge diagonally
            "2,0, 0,2, 1,1, 1,1", // ↖
            "0,2, 2,0, 1,1, 1,1", // ↘

            // cutting bottom right edge diagonally
            "3,0, 5,2, 4,1 ,4,1", // ↗
            "5,2, 3,0, 4,1 ,4,1", // ↙

            // cutting top left edge diagonally
            "0,3, 2,5, 1,4 ,1,4", // ↗
            "2,5, 0,3, 1,4 ,1,4", // ↙

            // cutting top right edge diagonally
            "3,5, 5,3, 4,4 ,4,4", // ↖
            "5,3, 3,5, 4,4 ,4,4", // ↘

            // Entering from left (→)
            "0,2, 6,2, 1,2, 4,2",
            // Entering from right (←)
            "6,2, 0,2, 4,2, 1,2",
            // Entering from bottom (↑)
            "2,0, 2,6, 2,1, 2,4",
            // Entering from top (↓)
            "2,6, 2,0, 2,4, 2,1"

    })
    void computeClipping_lineCrossingRect_returnsBothBoundaryPoint(
            float startX,
            float startY,
            float endX,
            float endY,
            float expectedEnterX,
            float expectedEnterY,
            float expectedExitX,
            float expectedExitY
    ) {
        // arrange
        VRectangle rectangle = new VRectangle(1, 1, 3, 3);
        FootStep footStep = new FootStep(
                new VPoint(startX, startY),
                new VPoint(endX, endY),
                0, 5
        );

        // act
        Optional<FootStep.LineRectClippingResult> intersection = footStep.computeClipping(rectangle);

        // assert
        FootStep.LineRectClippingResult clippingResult = assertPresent(intersection);
        FootStep.IntersectionPointAndTime enter = assertPresent(clippingResult.entryPoint());
        assertEquals(expectedEnterX, enter.point().x, 1e-6f);
        assertEquals(expectedEnterY, enter.point().y, 1e-6f);

        FootStep.IntersectionPointAndTime exit = assertPresent(clippingResult.exitPoint());
        assertEquals(expectedExitX, exit.point().x, 1e-6f);
        assertEquals(expectedExitY, exit.point().y, 1e-6f);
    }

    @ParameterizedTest
    @CsvSource({
            // startX, startY, endX, endY, expectedX, expectedY
            // Enter from left (→)
            "0,2, 2,2, 1,2",
            // Touch from left (→)
            "0,2, 1,2, 1,2",

            // Enter from right (←)
            "6,2, 2,2, 4,2",
            // Touch from right (←)
            "5,2, 4,2, 4,2",

            // Enter from top (↓)
            "2,6, 2,2, 2,4",
            // Touch from right (↓)
            "2,6, 2,4, 2,4",

            // Enter from bottom (↑)
            "2,0, 2,2, 2,1",
            // Touch from bottom (↑)
            "2,0, 2,1, 2,1",

            // Enter from top-left (↘)
            "-1,5, 2,2, 1,3",
            // Touch from top-left (↘)
            "-1,5, 1,3, 1,3",

            // Enter from top-right (↙)
            "5,5, 2,2, 4,4",
            // Touch from top-right (↙)
            "5,5, 4,4, 4,4",

            // Enter from bottom-left (↗)
            "0,0, 2,2, 1,1",
            // Touch from bottom-left (↗)
            "0,0, 1,1, 1,1",

            // Enter from bottom-right (↖)
            "5,-1, 2,2, 3,1",
            // Touch from bottom-right (↖)
            "5,-1, 4,1, 4,1"
    })
    void computeClipping_enteringRect_returnsOnlyBoundaryPoint(
            float startX,
            float startY,
            float endX,
            float endY,
            float expectedX,
            float expectedY
    ) {
        // arrange
        VRectangle rectangle = new VRectangle(1, 1, 3, 3);

        FootStep footStep = new FootStep(
                new VPoint(startX, startY),
                new VPoint(endX, endY),
                0,
                6
        );

        // act
        Optional<FootStep.LineRectClippingResult> optionalClippingResult =
                footStep.computeClipping(rectangle);

        // assert
        FootStep.LineRectClippingResult clippingResult = assertPresent(optionalClippingResult);
        assertMissing(clippingResult.exitPoint());
        FootStep.IntersectionPointAndTime enter = assertPresent(clippingResult.entryPoint());
        assertEquals(expectedX, enter.point().x, 1e-6f);
        assertEquals(expectedY, enter.point().y, 1e-6f);
    }

    @ParameterizedTest
    @CsvSource({
            // startX, startY, endX, endY, expectedX, expectedY
            // Exit right (→)
            "1,1,3,3,  2,2, 6,2, 4,2",
            "1,0,1,2,  2,2, 4,0, 2,2",
            // Starts on right edge going outward (→)
            "1,1,3,3,  4,2, 6,2, 4,2",

            // Exit left (←)
            "1,1,3,3,  2,2, 0,2, 1,2",
            // Starts on left edge going outward (←)
            "1,1,3,3,  1,2, 0,2, 1,2",

            // Exit top (↑)
            "1,1,3,3,  2,2, 2,6, 2,4",
            // Starts on top edge going outward (↑)
            "1,1,3,3,  2,4 ,2,6, 2,4",

            // Exit bottom (↓)
            "1,1,3,3,  2,2, 2,0, 2,1",
            // Starts on bottom edge going outward (↓)
            "1,1,3,3,  2,1, 2,0, 2,1",

            // Exit top-right (↗)
            "1,1,3,3,  2,2, 5,5, 4,4",
            // Starts on top-right edge going outward (↗)
            "1,1,3,3,  4,4, 5,5, 4,4",

            // Exit bottom-right (↘)
            "1,1,3,3,  2,2, 5,-1, 3,1",
            // Starts on bottom-right edge going outward (↘)
            "1,1,3,3,  4,1, 5,-1, 4,1",

            // Exit top-left (↖)
            "1,1,3,3,  2,2, -1,5, 1,3",
            // Starts on top-left edge going outward (↖)
            "1,1,3,3,  1,4, -1,5, 1,4",

            // Exit bottom-left (↙)
            "1,1,3,3,  2,2, -1,-1, 1,1",
            // Starts on bottom-left edge going outward (↙)
            "1,1,3,3,  1,1, -1,-1, 1,1"
    })
    void computeClipping_lineExitingRect_returnsOnlyBoundaryPoint(
            float rectangleStartX, float rectangleStartY, float rectWidth, float rectHeight,
            float startX, float startY,
            float endX, float endY,
            float expectedX, float expectedY
    ) {
        // arrange
        VRectangle rectangle = new VRectangle(rectangleStartX, rectangleStartY, rectWidth, rectHeight);

        FootStep footStep = new FootStep(
                new VPoint(startX, startY),
                new VPoint(endX, endY),
                0,
                6
        );

        // act
        Optional<FootStep.LineRectClippingResult> optionalClippingResult =
                footStep.computeClipping(rectangle);

        // assert
        FootStep.LineRectClippingResult clippingResult = assertPresent(optionalClippingResult);
        assertMissing(clippingResult.entryPoint());
        FootStep.IntersectionPointAndTime exit = assertPresent(clippingResult.exitPoint());
        assertEquals(expectedX, exit.point().x, 1e-6f);
        assertEquals(expectedY, exit.point().y, 1e-6f);
    }

    @ParameterizedTest
    @CsvSource({
            // startX, startY, endX, endY
            "2,2, 3,3",
            "2,2, 4,2",
            "3,3, 2,2",
            "1,1, 3,3",
            "3,3, 1,1,",

            // touching corner from the inside
            "1,2, 4,2",
            "4,2, 1,2",
            "2,1, 2,4",
            "2,4, 2,1",

            // loop corners
            "1,1, 1,4",
            "1,4, 1,1",
            "1,4, 4,4",
            "4,4, 1,4",
            "4,4, 4,1",
            "4,1, 4,4",
            "4,1, 1,1",
            "1,1, 4,4",

            // zero length line inside
            "2,2, 2,2",

            // zero length line on edge
            "1,2, 1,2",
            "2,1, 2,1",
            "2,4, 2,4",
            "4,2, 4,2",

            // zero length line on corner
            "1,1, 1,1",
            "1,4, 1,4",
            "4,4, 4,4",
            "4,1, 4,1"
    })
    void computeClipping_lineCompletelyInsideRect_returnsEmptyStartAndEnd(
            float startX,
            float startY,
            float endX,
            float endY
    ) {
        // arrange
        VRectangle rectangle = new VRectangle(1, 1, 3, 3);

        FootStep footStep = new FootStep(
                new VPoint(startX, startY),
                new VPoint(endX, endY),
                0,
                6
        );

        // act
        Optional<FootStep.LineRectClippingResult> optionalClippingResult =
                footStep.computeClipping(rectangle);

        // assert
        FootStep.LineRectClippingResult clippingResult = assertPresent(optionalClippingResult);
        assertMissing(clippingResult.entryPoint());
        assertMissing(clippingResult.exitPoint());
        assertTrue(clippingResult.isCompletelyInside());

        assertEquals(clippingResult.clippingStart().point(), footStep.getStart());
        assertEquals(clippingResult.clippingStart().time(), footStep.getStartTime());
        assertEquals(clippingResult.clippingEnd().point(), footStep.getEnd());
        assertEquals(clippingResult.clippingEnd().time(), footStep.getEndTime());
    }

    @ParameterizedTest
    @CsvSource({
            // startX, startY, endX, endY, startT, endT, expectedTime
            // Enter from left (→) hits x = 1
            "0,2, 2,2, 0, 4, 2",
            // Enter from right (←) hits x = 4
            "6,2, 2,2, 0, 4, 2",
            // Enter from top (↓) hits y = 4
            "2,6, 2,2, 0, 4, 2",
            // Enter from bottom (↑) hits y = 1
            "2,0, 2,2, 0, 10, 5"
    })
    void computeClipping_enteringRect_returnsCorrectTime(
            float startX,
            float startY,
            float endX,
            float endY,
            double startT,
            double endT,
            double expectedTime
    ) {
        // arrange
        VRectangle rectangle = new VRectangle(1, 1, 3, 3);

        FootStep footStep = new FootStep(
                new VPoint(startX, startY),
                new VPoint(endX, endY),
                startT,
                endT
        );

        // act
        Optional<FootStep.LineRectClippingResult> optionalClippingResult = footStep.computeClipping(rectangle);

        // assert
        FootStep.LineRectClippingResult clippingResult = assertPresent(optionalClippingResult);
        assertMissing(clippingResult.exitPoint());
        FootStep.IntersectionPointAndTime enterPointAndTime = assertPresent(clippingResult.entryPoint());
        assertEquals(expectedTime, enterPointAndTime.time(), 1e-6);
    }

    @ParameterizedTest
    @CsvSource({
            // startX, startY, endX, endY, startT, endT, expectedTime
            // Exit right (→) hits x = 4
            "2,2, 6,2, 0, 4, 2",
            // Exit left (←) hits x = 1
            "2,2, 0,2, 0, 2, 1",
            // Exit top (↑) hits y = 4
            "2,2, 2,6, 0, 4, 2",
            // Exit bottom (↓) hits y = 1
            "2,2, 2,0, 0, 2, 1"
    })
    void computeClipping_exitingRect_returnsCorrectTime(
            float startX,
            float startY,
            float endX,
            float endY,
            double startT,
            double endT,
            double expectedTime
    ) {
        // arrange
        VRectangle rectangle = new VRectangle(1, 1, 3, 3);

        FootStep footStep = new FootStep(
                new VPoint(startX, startY),
                new VPoint(endX, endY),
                startT,
                endT
        );

        // act
        Optional<FootStep.LineRectClippingResult> optionalClippingResult = footStep.computeClipping(rectangle);

        // assert
        FootStep.LineRectClippingResult clippingResult = assertPresent(optionalClippingResult);
        assertMissing(clippingResult.entryPoint());
        FootStep.IntersectionPointAndTime exitPointAndTime = assertPresent(clippingResult.exitPoint());
        assertEquals(expectedTime, exitPointAndTime.time(), 1e-6);
    }

    @ParameterizedTest
    @CsvSource({
            // Completely to the left of the rectangle
            "0,0, 0,2",
            "0,2, 0,0",
            // Completely to the right of the rectangle
            "5,0, 5,2",
            "5,2, 5,0",
            // Completely above the rectangle
            "0,5, 7,7",
            "7,7, 0,5",
            // Completely below the rectangle
            "0,0, 2,0",
            "2,0, 0,0",
            // diagonal below left
            "-1,-1, 0,0",
            "0,0,  -1,-1",
            // diagonal below right
            "6,-1, 5,0",
            "5,0,  6,-1",
            // diagonal top left
            "0,6, 10,5.5", // cuts the rect's x-axis twice but not the y-axis
            "10,5.5, 0,6",
            "0,5, -1,6",
            // diagonal top right
            "5,5, 6,6",
            "6,6, 5,5"
    })
    void computeClipping_outsideRect_returnsEmpty(
            float startX,
            float startY,
            float endX,
            float endY
    ) {
        // arrange
        VRectangle rectangle = new VRectangle(1, 1, 3, 3);

        FootStep footStep = new FootStep(
                new VPoint(startX, startY),
                new VPoint(endX, endY),
                0,
                6
        );

        // act
        Optional<FootStep.LineRectClippingResult> intersection = footStep.computeClipping(rectangle);

        // assert
        assertMissing(intersection);
    }
}
