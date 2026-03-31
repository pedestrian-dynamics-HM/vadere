package org.vadere.simulator.dataprocessing.processors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.projects.dataprocessing.datakey.PedestrianIdKey;
import org.vadere.simulator.projects.dataprocessing.processor.PedestrianCrossingTimeProcessor;
import org.vadere.simulator.utils.TimeSpanSeconds;
import org.vadere.state.attributes.scenario.AttributesMeasurementArea;
import org.vadere.state.scenario.MeasurementArea;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;
import org.vadere.state.simulation.FootStep;
import org.vadere.state.simulation.VTrajectory;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.Vector2D;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestPedestrianCrossingTimeProcessor {
    private PedestrianCrossingTimeProcessor sut;
    private final VRectangle measurementAreaShape = new VRectangle(0, 0, 10, 10);
    private final double delta = 1e-6;

    @BeforeEach
    public void initialize(){
        sut = new PedestrianCrossingTimeProcessor();
        sut.init(new MeasurementArea(new AttributesMeasurementArea(0, measurementAreaShape)));
    }

    @Test
    public void update_pedestrianDoesNotCross_resultHasNoCrossing(){
        // arrange
        final int pedestrianId = 42;
        FootStep notCrossingStep = new FootStep(new VPoint(-5, -5), new VPoint(-4, -4), 0, 20);
        Pedestrian pedestrian = mockPedestrianWithTrajectory(pedestrianId, notCrossingStep);
        SimulationState simulationState = mockSimulationState(pedestrian);

        // act
        sut.update(simulationState);
        String[] resultEntries = sut.toStrings(new PedestrianIdKey(pedestrianId));

        // assert
        for(String entry : resultEntries){
            assertEquals("-", entry);
        }
    }


    private static Stream<Arguments> singleCrossingScenarios() {
        return Stream.of(
                Arguments.of( // from left to right
                        new FootStep(new VPoint(-5, 5), new VPoint(15, 5), 0, 20),
                        expectedEnterAndExitTime(5,15),
                        expectedExitDirection(1, 0)
                ),
                Arguments.of( // from right to left
                        new FootStep(new VPoint(15, 5), new VPoint(-5, 5), 0, 20),
                        expectedEnterAndExitTime(5,15),
                        expectedExitDirection(-1, 0)
                ),
                Arguments.of( // from bottom to top
                        new FootStep(new VPoint(5, -5), new VPoint(5, 15), 0, 20),
                        expectedEnterAndExitTime(5,15),
                        expectedExitDirection(0, 1)
                ),
                Arguments.of( // from top to bottom
                        new FootStep(new VPoint(5, 15), new VPoint(5, -5), 0, 20),
                        expectedEnterAndExitTime(5,15),
                        expectedExitDirection(0, -1)
                ),
                Arguments.of( // diagonal: from bottom-left to top-right
                        new FootStep(new VPoint(-5, -5), new VPoint(15, 15), 0, 20),
                        expectedEnterAndExitTime(5,15),
                        expectedExitDirection(1, 1)
                ),
                Arguments.of( // diagonal: from top-right to bottom-left
                        new FootStep(new VPoint(15, 15), new VPoint(-5, -5), 0, 20),
                        expectedEnterAndExitTime(5,15),
                        expectedExitDirection(-1, -1).norm()
                ),
                Arguments.of( // from top-left to bottom-right
                        new FootStep(new VPoint(-5, 15), new VPoint(15, -5), 0, 20),
                        expectedEnterAndExitTime(5,15),
                        expectedExitDirection(1, -1)
                ),
                Arguments.of( // from bottom-right to top-left
                        new FootStep(new VPoint(15, -5), new VPoint(-5, 15), 0, 20),
                        expectedEnterAndExitTime(5,15),
                        expectedExitDirection(-1, 1)
                )
                
        );
    }
    @ParameterizedTest
    @MethodSource("singleCrossingScenarios")
    public void update_singleStepCrossingOfPedestrian_correctEnterExitTimesAndDirection(
            FootStep singleFootstep,
            TimeSpanSeconds expectedEnterExitTimeSeconds,
            VPoint expectedDirection){
        // arrange
        expectedDirection = expectedDirection.norm();
        final int pedestrianId = 42;
        Pedestrian pedestrian = mockPedestrianWithTrajectory(pedestrianId, singleFootstep);
        SimulationState simulationState = mockSimulationState(pedestrian);

        // act
        sut.update(simulationState);
        String[] resultEntries = sut.toStrings(new PedestrianIdKey(pedestrianId));

        // assert
        ToStringEntryValues toStringEntryValues = ParseToStringEntries(resultEntries);
        assertEquals(expectedEnterExitTimeSeconds.startSeconds(), toStringEntryValues.enterTimeSeconds, delta);
        assertEquals(expectedEnterExitTimeSeconds.endSeconds(), toStringEntryValues.exitTimeSeconds,  delta);
        assertEquals(expectedDirection.x, toStringEntryValues.exitDirection.x,  delta);
        assertEquals(expectedDirection.y, toStringEntryValues.exitDirection.y,  delta);
    }


    private static Stream<Arguments> multipleCrossingScenarios() {
        return Stream.of(
                Arguments.of(
                        new FootStep[]{
                                // touch top left edge in two steps (has longer distance)
                                new FootStep(new VPoint(1, 11), new VPoint(1, 9), 0, 10),
                                new FootStep(new VPoint(1, 9), new VPoint(-5, 5), 10, 20),
                                // then from left to right
                                new FootStep(new VPoint(-5, 5), new VPoint(15, 5), 20, 40),
                        },
                        expectedEnterAndExitTime(25, 35),
                        expectedExitDirection(1, 0)
                ),
                Arguments.of(
                        new FootStep[]{
                                // touch top left edge
                                new FootStep(new VPoint(1, 11), new VPoint(-5, 5), 0, 20),
                                // then from left to right in two steps (has longer distance)
                                new FootStep(new VPoint(-5, 5), new VPoint(5, 5), 20, 30),
                                new FootStep(new VPoint(5, 5), new VPoint(15, 5), 30, 40),
                        },
                        expectedEnterAndExitTime(25, 35),
                        expectedExitDirection(1, 0)
                ),
                Arguments.of(
                        new FootStep[]{
                                // then from left to right in two steps (has longer distance)
                                new FootStep(new VPoint(-5, 5), new VPoint(5, 5), 0, 10),
                                new FootStep(new VPoint(5, 5), new VPoint(15, 5), 10, 20),
                                // touch top right edge
                                new FootStep(new VPoint(15, 5), new VPoint(9, 11), 20, 40),
                        },
                        expectedEnterAndExitTime(5, 15),
                        expectedExitDirection(1, 0)
                ),
                Arguments.of(
                        new FootStep[]{
                                // touch bottom right edge
                                new FootStep(new VPoint(11, 5), new VPoint(9, -5), 0, 20),
                                // then from bottom to top in multiple steps (has longer distance)
                                new FootStep(new VPoint(9, -5), new VPoint(9, 0), 20, 25),
                                new FootStep( new VPoint(9, 0), new VPoint(9, 5), 25, 30),
                                new FootStep( new VPoint(9, 5), new VPoint(9, 15), 30, 40),
                        },
                        expectedEnterAndExitTime(25, 35),
                        expectedExitDirection(0, 1)
                ),
                Arguments.of(
                        new FootStep[]{
                                // start from inside and go outside
                                new FootStep(new VPoint(1, 9), new VPoint(-5, 5), 0, 20),
                                // then from left to right (has longer distance)
                                new FootStep(new VPoint(-5, 5), new VPoint(15, 5), 20, 40),
                        },
                        expectedEnterAndExitTime(25, 35),
                        expectedExitDirection(1, 0)
                )
        );
    }
    @ParameterizedTest
    @MethodSource("multipleCrossingScenarios")
    public void update_multipleCrossingOfPedestrian_takesLongerDistanceCrossing(
            FootStep[] footSteps,
            TimeSpanSeconds expectedEnterExitTimeSeconds,
            VPoint expectedDirection){
        // arrange
        expectedDirection = expectedDirection.norm();
        final int pedestrianId = 42;
        Pedestrian pedestrian = mockPedestrianWithTrajectory(pedestrianId, footSteps);
        SimulationState simulationState = mockSimulationState(pedestrian);

        // act
        sut.update(simulationState);
        String[] resultEntries = sut.toStrings(new PedestrianIdKey(pedestrianId));

        // assert
        ToStringEntryValues toStringEntryValues = ParseToStringEntries(resultEntries);
        assertEquals(expectedEnterExitTimeSeconds.startSeconds(), toStringEntryValues.enterTimeSeconds, delta);
        assertEquals(expectedEnterExitTimeSeconds.endSeconds(), toStringEntryValues.exitTimeSeconds,  delta);
        assertEquals(expectedDirection.x, toStringEntryValues.exitDirection.x,  delta);
        assertEquals(expectedDirection.y, toStringEntryValues.exitDirection.y,  delta);
    }


    private ToStringEntryValues ParseToStringEntries(String[] entries){
        String[] headers = sut.getHeaders();
        assertEquals(headers.length, entries.length);

        Double enterTime = null;
        Double exitTime = null;
        Vector2D exitDirection = null;
        for (int i = 0; i < headers.length; i++){
            String header = headers[i];
            String entry = entries[i];
            switch (header){
                case PedestrianCrossingTimeProcessor.crossStartTime:
                    enterTime = Double.parseDouble(entry);
                    break;
                case PedestrianCrossingTimeProcessor.crossEndTime:
                    exitTime = Double.parseDouble(entry);
                    break;
                case PedestrianCrossingTimeProcessor.crossDirection:
                    Pattern pattern = Pattern.compile("\\[(?<x>.*),(?<y>.*)]");
                    Matcher matcher = pattern.matcher(entry);
                    assertTrue(matcher.matches(), "Failed to parse entry " + entry);
                    double x = Double.parseDouble(matcher.group("x"));
                    double y = Double.parseDouble(matcher.group("y"));
                    exitDirection = new Vector2D(x, y);
                    break;
            }
        }

        assertNotNull(enterTime);
        assertNotNull(exitTime);
        assertNotNull(exitDirection);
        return new ToStringEntryValues(enterTime, exitTime, exitDirection);
    }

    private record ToStringEntryValues(double enterTimeSeconds, double exitTimeSeconds, Vector2D exitDirection){
    }

    private static Pedestrian mockPedestrianWithTrajectory(int pedestrianId, FootStep... footSteps){
        Pedestrian pedestrian = mock(Pedestrian.class);
        when(pedestrian.getId()).thenReturn(pedestrianId);

        VTrajectory trajectory = new VTrajectory();
        for(FootStep footStep : footSteps){
            trajectory.add(footStep);
        }
        when(pedestrian.getTrajectoryOfSimulationStep()).thenReturn(trajectory);

        return pedestrian;
    }

    private static SimulationState mockSimulationState(Pedestrian... pedestrians) {
        SimulationState simulationState = mock(SimulationState.class);
        Topography topography = mock(Topography.class);

        when(simulationState.getTopography()).thenReturn(topography);

        // ensure step counter is increased with every access
        AtomicInteger stepCounter = new AtomicInteger(1);
        when(simulationState.getStep()).thenAnswer(invocation ->  stepCounter.getAndIncrement());

        when(topography.getElements(Pedestrian.class)).thenReturn(Arrays.stream(pedestrians).toList());

        return simulationState;
    }

    static TimeSpanSeconds expectedEnterAndExitTime(float from, float to) {
        return new TimeSpanSeconds(from, to);
    }

    static VPoint expectedExitDirection(double x, double y) {
        return new VPoint(x, y);
    }
}
