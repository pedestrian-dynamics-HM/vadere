package org.vadere.state.simulation;

import org.junit.jupiter.api.Test;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;

import java.util.LinkedList;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class VTrajectoryTest {

    @Test
    void cut_betweenThreeSteps_returnsMiddleStep() {
        // arrange
        VRectangle rectangle = new VRectangle(1, 0, 1, 2);

        FootStep firstStep = new FootStep(
                new VPoint(0, 0),
                new VPoint(1, 1),
                0,
                1
        );
        FootStep secondStep = new FootStep(
                new VPoint(1, 1),
                new VPoint(2, 2),
                1,
                2
        );
        FootStep thirdStep = new FootStep(
                new VPoint(2, 2),
                new VPoint(4, 0),
                2,
                4
        );

        VTrajectory trajectory = new VTrajectory();
        trajectory.add(firstStep).add(secondStep).add(thirdStep);

        // act
        VTrajectory cut = trajectory.cut(rectangle);

        // assert
        assertEquals(1, cut.size());
        LinkedList<FootStep> steps = cut.getFootSteps();

        FootStep middleStep = steps.get(0);
        assertEquals(1, middleStep.getStart().x);
        assertEquals(1, middleStep.getStart().y);
        assertEquals(1, middleStep.getStartTime(), 1e-6f);
        assertEquals(2, middleStep.getEndTime(), 1e-6f);
    }

    @Test
    void cut_halfFirstAndLastStep_returnsFirstHalfThenMiddleThenLastHalf() {
        // arrange
        VRectangle rectangle = new VRectangle(0.5, 0, 2, 2);

        FootStep firstStep = new FootStep(
                new VPoint(0, 3),
                new VPoint(1, 1),
                0,
                1
        );
        FootStep secondStep = new FootStep(
                new VPoint(1, 1),
                new VPoint(2, 2),
                1,
                2
        );
        FootStep thirdStep = new FootStep(
                new VPoint(2, 2),
                new VPoint(3, 0),
                2,
                3
        );

        VTrajectory trajectory = new VTrajectory();
        trajectory.add(firstStep).add(secondStep).add(thirdStep);

        // act
        VTrajectory cut = trajectory.cut(rectangle);

        // assert
        assertEquals(3, cut.size());
        LinkedList<FootStep> steps = cut.getFootSteps();

        FootStep firstStepOfCut = steps.get(0);
        assertEquals(0.5, firstStepOfCut.getStart().x);
        assertEquals(2, firstStepOfCut.getStart().y);
        assertEquals(1, firstStepOfCut.getEnd().x);
        assertEquals(1, firstStepOfCut.getEnd().y);
        assertEquals(0.5, firstStepOfCut.getStartTime(), 1e-6f);
        assertEquals(1, firstStepOfCut.getEndTime(), 1e-6f);

        FootStep middleStepOfCut = steps.get(1);
        assertEquals(secondStep, middleStepOfCut);

        FootStep lastStepOfCut = steps.get(2);
        assertEquals(2, lastStepOfCut.getStart().x);
        assertEquals(2, lastStepOfCut.getStart().y);
        assertEquals(2.5, lastStepOfCut.getEnd().x);
        assertEquals(1, lastStepOfCut.getEnd().y);
        assertEquals(2, lastStepOfCut.getStartTime(), 1e-6f);
        assertEquals(2.5, lastStepOfCut.getEndTime(), 1e-6f);
    }

    @Test
    void cut_singleStepDiagonal_returnsMiddlePiece() {
        // arrange
        VRectangle rectangle = new VRectangle(1, 1, 1, 1);

        FootStep step = new FootStep(
                new VPoint(0, 0),
                new VPoint(3, 3),
                0,
                3
        );

        VTrajectory trajectory = new VTrajectory();
        trajectory.add(step);

        // act
        VTrajectory cut = trajectory.cut(rectangle);

        // assert
        assertEquals(1, cut.size());
        LinkedList<FootStep> steps = cut.getFootSteps();

        FootStep middlePiece = steps.get(0);
        assertEquals(1, middlePiece.getStart().x);
        assertEquals(1, middlePiece.getStart().y);
        assertEquals(2, middlePiece.getEnd().x);
        assertEquals(2, middlePiece.getEnd().y);
        assertEquals(1, middlePiece.getStartTime(), 1e-6f);
        assertEquals(2, middlePiece.getEndTime(), 1e-6f);
    }

    @Test
    void cut_insideAndOutside_returnsInsidePiece() {
        // arrange
        VRectangle rectangle = new VRectangle(1, 1, 1, 1);

        FootStep outside1 = new FootStep(
                new VPoint(0, 0),
                new VPoint(0.5, .5),
                0,
                3
        );

        FootStep inside = new FootStep(
                new VPoint(1.2, 1.2),
                new VPoint(1.5, 1.5),
                3,
                4
        );

        FootStep outside2 = new FootStep(
                new VPoint(2, 2),
                new VPoint(3, 3),
                4,
                5
        );

        VTrajectory trajectory = new VTrajectory();
        trajectory.add(outside1);
        trajectory.add(inside);
        trajectory.add(outside2);

        // act
        VTrajectory cut = trajectory.cut(rectangle);

        // assert
        assertEquals(1, cut.size());
        LinkedList<FootStep> steps = cut.getFootSteps();

        FootStep insideCutStep = steps.get(0);
        assertEquals(inside, insideCutStep);
    }

    @Test
    void cut_startsInsideGoesOutside_returnsInsidePiece() {
        // arrange
        VRectangle rectangle = new VRectangle(1, 1, 1, 1);

        FootStep startsInside = new FootStep(
                new VPoint(1, 1.5),
                new VPoint(3, 1.5),
                3,
                4
        );

        VTrajectory trajectory = new VTrajectory();
        trajectory.add(startsInside);

        // act
        VTrajectory cut = trajectory.cut(rectangle);

        // assert
        assertEquals(1, cut.size());
        LinkedList<FootStep> steps = cut.getFootSteps();

        FootStep insideCutStep = steps.get(0);
        assertEquals(1, insideCutStep.getStart().x);
        assertEquals(1.5, insideCutStep.getStart().y);
        assertEquals(2, insideCutStep.getEnd().x);
        assertEquals(1.5, insideCutStep.getEnd().y);
        assertEquals(3, insideCutStep.getStartTime(), 1e-6f);
        assertEquals(3.5, insideCutStep.getEndTime(), 1e-6f);
    }

    @Test
    void cut_touchesRectangle_returnsTouchingPart() {
        // arrange
        VRectangle rectangle = new VRectangle(1, 1, 1, 1);

        FootStep startsInside = new FootStep(
                new VPoint(0, 1),
                new VPoint(3, 1),
                0,
                3
        );

        VTrajectory trajectory = new VTrajectory();
        trajectory.add(startsInside);

        // act
        VTrajectory cut = trajectory.cut(rectangle);

        // assert
        assertEquals(1, cut.size());
        LinkedList<FootStep> steps = cut.getFootSteps();
        FootStep touchingStep = steps.get(0);
        assertEquals(1, touchingStep.getStart().x);
        assertEquals(1, touchingStep.getStart().y);
        assertEquals(2, touchingStep.getEnd().x);
        assertEquals(1, touchingStep.getEnd().y);
        assertEquals(1, touchingStep.getStartTime(), 1e-6f);
        assertEquals(2, touchingStep.getEndTime(), 1e-6f);
    }

    @Test
    void cut_isExactlyOnBounds_returnsTouchingPart() {
        // arrange
        VRectangle rectangle = new VRectangle(1, 1, 1, 1);

        FootStep startsInside = new FootStep(
                new VPoint(1, 1),
                new VPoint(2, 1),
                0,
                3
        );

        VTrajectory trajectory = new VTrajectory();
        trajectory.add(startsInside);

        // act
        VTrajectory cut = trajectory.cut(rectangle);

        // assert
        assertEquals(1, cut.size());
        LinkedList<FootStep> steps = cut.getFootSteps();
        FootStep touchingStep = steps.get(0);
        assertEquals(1, touchingStep.getStart().x);
        assertEquals(1, touchingStep.getStart().y);
        assertEquals(2, touchingStep.getEnd().x);
        assertEquals(1, touchingStep.getEnd().y);
        assertEquals(0, touchingStep.getStartTime(), 1e-6f);
        assertEquals(3, touchingStep.getEndTime(), 1e-6f);
    }

    @Test
    void cut_multipleTimes_returnsLastTrajectory() {
        // arrange
        VRectangle rectangle = new VRectangle(1, 1, 2, 4);

        FootStep firstIntersectionEnter = new FootStep( // will be cut at 1=3, y=2 then discarded as only last intersection is kept
                new VPoint(0, 2),
                new VPoint(2, 2),
                0,
                1
        );
        FootStep firstIntersectionExit = new FootStep( // will be cut at x=3, y=2 then discarded as only last intersection is kept
                new VPoint(2, 2),
                new VPoint(4, 2),
                1,
                2
        );
        FootStep outsideStep = new FootStep(
                new VPoint(4, 2),
                new VPoint(4, 3),
                2,
                3
        );
        FootStep secondIntersectionEnter = new FootStep( // will be cut at x=3, y=3
                new VPoint(4, 3),
                new VPoint(2, 3),
                3,
                4
        );
        FootStep secondIntersectionExit = new FootStep( // will be cut at x=1, y=3
                new VPoint(2, 3),
                new VPoint(0, 3),
                4,
                5
        );

        VTrajectory trajectory = new VTrajectory();
        trajectory.add(firstIntersectionEnter)
                .add(firstIntersectionExit)
                .add(outsideStep)
                .add(secondIntersectionEnter)
                .add(secondIntersectionExit);

        // act
        VTrajectory cut = trajectory.cut(rectangle);

        // assert
        assertEquals(2, cut.size());
        LinkedList<FootStep> steps = cut.getFootSteps();
        FootStep cutSecondIntersectionEnter = steps.get(0);
        assertEquals(3, cutSecondIntersectionEnter.getStart().x);
        assertEquals(3, cutSecondIntersectionEnter.getStart().y);
        assertEquals(2, cutSecondIntersectionEnter.getEnd().x);
        assertEquals(3, cutSecondIntersectionEnter.getEnd().y);
        assertEquals(3.5, cutSecondIntersectionEnter.getStartTime(), 1e-6f);
        assertEquals(4, cutSecondIntersectionEnter.getEndTime(), 1e-6f);

        FootStep cutSecondIntersectionExit = steps.get(1);
        assertEquals(2, cutSecondIntersectionExit.getStart().x);
        assertEquals(3, cutSecondIntersectionExit.getStart().y);
        assertEquals(1, cutSecondIntersectionExit.getEnd().x);
        assertEquals(3, cutSecondIntersectionExit.getEnd().y);
        assertEquals(4, cutSecondIntersectionExit.getStartTime(), 1e-6);
        assertEquals(4.5, cutSecondIntersectionExit.getEndTime(), 1e-6f);
    }
}
