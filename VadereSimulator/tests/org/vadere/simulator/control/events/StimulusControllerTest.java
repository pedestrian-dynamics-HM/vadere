package org.vadere.simulator.control.events;

import org.junit.Test;
import org.vadere.simulator.projects.ScenarioStore;
import org.vadere.state.psychology.stimuli.json.StimulusInfo;
import org.vadere.state.psychology.stimuli.json.StimulusInfoStore;
import org.vadere.state.psychology.stimuli.types.*;
import org.vadere.state.psychology.stimuli.types.Stimulus;
import org.vadere.util.geometry.shapes.VRectangle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class StimulusControllerTest {

    private ScenarioStore getScenarioStoreContainingRecurringEvent(boolean isRecurring) {
        return new ScenarioStore("name",
                "description",
                "mainModel",
                null,
                null,
                null,
                getEventInfoStoreContainingRecurringEvent(isRecurring));
    }

    private ScenarioStore getScenarioStore(StimulusInfoStore store) {
        return new ScenarioStore("name",
                "description",
                "mainModel",
                null,
                null,
                null,
                store);
    }

    private StimulusInfoStore getEventInfoStoreContainingRecurringEvent(boolean isRecurring) {
        // Create "Timeframe" and "Stimulus" objects and encapsulate them in "StimulusInfo" objects.
        Timeframe timeframe = new Timeframe(5, 30, isRecurring, 0);
        List<Stimulus> stimuli = new ArrayList<>();

        StimulusInfo stimulusInfo1 = new StimulusInfo();
        stimulusInfo1.setTimeframe(timeframe);
        stimulusInfo1.setStimuli(stimuli);

        List<StimulusInfo> stimulusInfos = new ArrayList<>();
        stimulusInfos.add(stimulusInfo1);

        StimulusInfoStore stimulusInfoStore = new StimulusInfoStore();
        stimulusInfoStore.setStimulusInfos(stimulusInfos);

        return stimulusInfoStore;
    }

    private StimulusInfoStore getEventInfoStore(List<StimulusInfo> eventList){
        StimulusInfoStore store = new StimulusInfoStore();
        store.setStimulusInfos(eventList);
        return store;
    }

    private StimulusInfo getEventInfo(Timeframe timeframe, Stimulus... stimuli){
        StimulusInfo stimulusInfo = new StimulusInfo();
        stimulusInfo.setTimeframe(timeframe);
        stimulusInfo.setStimuli(Arrays.asList(stimuli));
        return stimulusInfo;
    }

    @Test
    public void eventControllerConstructorFindsNoEventsIfPassingEmptyScenarioStore() {
        ScenarioStore emptyScenarioStore = new ScenarioStore("emptyScenarioStore");

        EventController eventController = new EventController(emptyScenarioStore);

        assertEquals(0, eventController.getOneTimeEvents().size());
        assertEquals(0, eventController.getRecurringEvents().size());
    }

    @Test
    public void eventControllerConstructorDetectsOneTimeEventsProperly() {
        boolean isRecurringEvent = false;
        ScenarioStore scenarioStoreContainingOneOneTimeEvent = getScenarioStoreContainingRecurringEvent(isRecurringEvent);

        EventController eventController = new EventController(scenarioStoreContainingOneOneTimeEvent);

        assertEquals(1, eventController.getOneTimeEvents().size());
        assertEquals(0, eventController.getRecurringEvents().size());
    }

    @Test
    public void eventControllerConstructorDetectsRecurringEventsProperly() {
        boolean isRecurringEvent = true;
        ScenarioStore scenarioStoreContainingOneRecurringEvent = getScenarioStoreContainingRecurringEvent(isRecurringEvent);

        EventController eventController = new EventController(scenarioStoreContainingOneRecurringEvent);

        assertEquals(0, eventController.getOneTimeEvents().size());
        assertEquals(1, eventController.getRecurringEvents().size());
    }

    @Test
    public void eventControllerConstructorDetectsOneTimeAndRecurringEventsProperly() {
        // Create a list containing one one-time and one recurring event.
        List<StimulusInfo> oneTimeAndRecurringEvents = new ArrayList<>();
        oneTimeAndRecurringEvents.addAll(getEventInfoStoreContainingRecurringEvent(false).getStimulusInfos());
        oneTimeAndRecurringEvents.addAll(getEventInfoStoreContainingRecurringEvent(true).getStimulusInfos());

        StimulusInfoStore stimulusInfoStoreWithBothEvents = new StimulusInfoStore();
        stimulusInfoStoreWithBothEvents.setStimulusInfos(oneTimeAndRecurringEvents);

        ScenarioStore scenarioStoreContainingOneTimeAndRecurringEvent = new ScenarioStore("name",
                "description",
                "mainModel",
                null,
                null,
                null,
                stimulusInfoStoreWithBothEvents);

        EventController eventController = new EventController(scenarioStoreContainingOneTimeAndRecurringEvent);

        assertEquals(1, eventController.getOneTimeEvents().size());
        assertEquals(1, eventController.getRecurringEvents().size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void timeframeIsActiveAtSimulationTimeThrowsExceptionIfNoRecurringEventTimeframe() {
        boolean isRecurringEvent = false;
        double simulationTime = 0.8;

        Timeframe timeframe = new Timeframe(0.75, 1.25, isRecurringEvent, 1.0);

        EventController.timeframeIsActiveAtSimulationTime(timeframe, simulationTime);
    }

    @Test
    public void timeframeIsActiveAtSimulationTimeReturnsTrueIfSimulationTimeFallsOntoStartTimePeriodically() {
        boolean recurringEvent = true;
        double startTime = 0.75;
        double endTime = 1.25;
        double waitTimeBetweenRepetition = 1.0;
        double simulationTime = startTime;
        double periodicity = (endTime - startTime) + waitTimeBetweenRepetition;

        Timeframe timeframe = new Timeframe(startTime, endTime, recurringEvent, waitTimeBetweenRepetition);

        for (int i = 0; i < 50; i++) {
            double currentSimulationTime = simulationTime + (i * periodicity);

            boolean timeframeIsActive = EventController.timeframeIsActiveAtSimulationTime(timeframe, currentSimulationTime);
            assertTrue(timeframeIsActive);
        }
    }

    @Test
    public void timeframeIsActiveAtSimulationTimeReturnsTrueIfSimulationTimeFallsOntoEndTimePeriodically() {
        boolean recurringEvent = true;
        double startTime = 0.75;
        double endTime = 1.25;
        double waitTimeBetweenRepetition = 1.0;
        double simulationTime = endTime;
        double periodicity = (endTime - startTime) + waitTimeBetweenRepetition;

        Timeframe timeframe = new Timeframe(startTime, endTime, recurringEvent, waitTimeBetweenRepetition);

        for (int i = 0; i < 50; i++) {
            double currentSimulationTime = simulationTime + (i * periodicity);

            boolean timeframeIsActive = EventController.timeframeIsActiveAtSimulationTime(timeframe, currentSimulationTime);
            assertTrue(timeframeIsActive);
        }
    }

    @Test
    public void timeframeIsActiveAtSimulationTimeReturnsTrueIfSimulationTimeFallsBetweenStartAndEndTimePeriodically() {
        boolean recurringEvent = true;
        double startTime = 0.75;
        double endTime = 1.25;
        double waitTimeBetweenRepetition = 1.0;
        double simulationTime = 1.0;
        double periodicity = (endTime - startTime) + waitTimeBetweenRepetition;

        Timeframe timeframe = new Timeframe(startTime, endTime, recurringEvent, waitTimeBetweenRepetition);

        for (int i = 0; i < 50; i++) {
            double currentSimulationTime = simulationTime + (i * periodicity);

            boolean timeframeIsActive = EventController.timeframeIsActiveAtSimulationTime(timeframe, currentSimulationTime);
            assertTrue(timeframeIsActive);
        }
    }

    @Test
    public void timeframeIsActiveAtSimulationTimeReturnsFalseIfSimulationTimeIsBeforeStartTime() {
        boolean recurringEvent = true;
        double startTime = 0.75;
        double endTime = 1.25;
        double waitTimeBetweenRepetition = 1.0;
        double simulationTime = 0.10;
        double increment = 0.05;

        Timeframe timeframe = new Timeframe(startTime, endTime, recurringEvent, waitTimeBetweenRepetition);

        while (simulationTime < startTime) {
            boolean timeframeIsActive = EventController.timeframeIsActiveAtSimulationTime(timeframe, simulationTime);
            assertFalse(timeframeIsActive);

            simulationTime += increment;
        }
    }

    @Test
    public void timeframeIsActiveAtSimulationTimeReturnsTrueIfSimulationTimeBetweenStartAndEndTime() {
        boolean recurringEvent = true;
        double startTime = 0.75;
        double endTime = 1.25;
        double waitTimeBetweenRepetition = 1.0;
        double simulationTime = startTime;
        double increment = 0.05;

        Timeframe timeframe = new Timeframe(startTime, endTime, recurringEvent, waitTimeBetweenRepetition);

        while (simulationTime < endTime) {
            boolean timeframeIsActive = EventController.timeframeIsActiveAtSimulationTime(timeframe, simulationTime);
            assertTrue(timeframeIsActive);

            simulationTime += increment;
        }
    }

    @Test
    public void timeframeIsActiveAtSimulationTimeReturnsFalseIfWaitTimeIsZeroButSimulationTimeBeforeStartTime() {
        boolean recurringEvent = true;
        double startTime = 0.75;
        double endTime = 1.25;
        double waitTimeBetweenRepetition = 0.0;
        double simulationTime = 0;
        double increment = 0.05;

        Timeframe timeframe = new Timeframe(startTime, endTime, recurringEvent, waitTimeBetweenRepetition);

        while (simulationTime < startTime) {
            boolean timeframeIsActive = EventController.timeframeIsActiveAtSimulationTime(timeframe, simulationTime);
            assertFalse(timeframeIsActive);

            simulationTime += increment;
        }
    }

    @Test
    public void timeframeIsActiveAtSimulationTimeReturnsTrueIfWaitTimeIsZeroAndSimulationTimeGreaterThanStartTime() {
        boolean recurringEvent = true;
        double startTime = 0.75;
        double endTime = 1.25;
        double waitTimeBetweenRepetition = 0.0;
        double simulationTime = startTime;
        double increment = 0.10;

        Timeframe timeframe = new Timeframe(startTime, endTime, recurringEvent, waitTimeBetweenRepetition);

        while (simulationTime < 500 * endTime) {
            boolean timeframeIsActive = EventController.timeframeIsActiveAtSimulationTime(timeframe, simulationTime);
            assertTrue(timeframeIsActive);

            simulationTime += increment;
        }
    }

    @Test
    public void getEventsForTimeAlwaysCreatesASingleElapsedTimeEventWithRequestedSimulationTime() {
        boolean isRecurringEvent = false;
        ScenarioStore scenarioStoreContainingOneOneTimeEvent = getScenarioStoreContainingRecurringEvent(isRecurringEvent);

        EventController eventController = new EventController(scenarioStoreContainingOneOneTimeEvent);

        double expectedSimulationTime = 1.0;

        List<Stimulus> activeStimuli = eventController.getEventsForTime(expectedSimulationTime);

        if (activeStimuli.size() == 1) {
            Stimulus stimulus = activeStimuli.get(0);

            assertEquals(ElapsedTime.class, stimulus.getClass());
            assertEquals(expectedSimulationTime, stimulus.getTime(), 10e-1);
        } else {
            fail("Expected only one event for simulationTime = " + expectedSimulationTime);
        }
    }

    @Test
    public void getEventsForTimeTimestampsEachActiveEvent() {
        boolean isRecurringEvent = false;
        ScenarioStore scenarioStoreContainingOneOneTimeEvent = getScenarioStoreContainingRecurringEvent(isRecurringEvent);

        // Store one concrete one-time event in "StimulusInfoStore" to check if its timestamp is updated.
        Wait wait = new Wait();
        List<Stimulus> stimuli = new ArrayList<>();
        stimuli.add(wait);

        StimulusInfo activeStimulusInfo = scenarioStoreContainingOneOneTimeEvent.getStimulusInfoStore().getStimulusInfos().get(0);
        activeStimulusInfo.setStimuli(stimuli);

        EventController eventController = new EventController(scenarioStoreContainingOneOneTimeEvent);

        double expectedSimulationTime = 5.0;

        List<Stimulus> activeStimuli = eventController.getEventsForTime(expectedSimulationTime);

        for (Stimulus stimulus : activeStimuli) {
            assertEquals(expectedSimulationTime, stimulus.getTime(), 10e-1);
        }

        assertEquals(2, activeStimuli.size());
    }

    @Test
    public void getEventsForTimeDoesNotTimestampInactiveEvents() {
        boolean isRecurringEvent = false;
        ScenarioStore scenarioStoreContainingOneOneTimeEvent = getScenarioStoreContainingRecurringEvent(isRecurringEvent);

        // Store one concrete one-time event in "StimulusInfoStore" to check if its timestamp is updated.
        Wait wait = new Wait();
        List<Stimulus> stimuli = new ArrayList<>();
        stimuli.add(wait);

        StimulusInfo inactiveStimulusInfo = scenarioStoreContainingOneOneTimeEvent.getStimulusInfoStore().getStimulusInfos().get(0);
        inactiveStimulusInfo.setStimuli(stimuli);
        inactiveStimulusInfo.setTimeframe(new Timeframe(0, 1, false, 0));

        EventController eventController = new EventController(scenarioStoreContainingOneOneTimeEvent);

        double expectedSimulationTime = 5.0;

        List<Stimulus> activeStimuli = eventController.getEventsForTime(expectedSimulationTime);

        assertEquals(1, activeStimuli.size());
        assertEquals(0, wait.getTime(), 10e-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void IsActiveTimeframeOnSingleEvent(){
        EventController.timeframeIsActiveAtSimulationTime(
                new Timeframe(2, 7, false,0), 1);
        fail("Should not be reached");
    }

    @Test
    public void IsActiveTimeframeOnRepeatedEvent(){
        Timeframe frame = new Timeframe(3, 4, true, 1);
        assertFalse(EventController.timeframeIsActiveAtSimulationTime(frame, 0.4));
        assertFalse(EventController.timeframeIsActiveAtSimulationTime(frame, 8.4));

        assertTrue(EventController.timeframeIsActiveAtSimulationTime(frame, 3.1));
        assertTrue(EventController.timeframeIsActiveAtSimulationTime(frame, 5.3));
    }

    @Test
    public void getEventsAtTime(){

        Stimulus e1 = new Wait(2.0);
        Stimulus e2 = new Wait(3.0);
        Stimulus e3 = new WaitInArea(12, new VRectangle(1,1,100,10.0));
        StimulusInfo stimulusInfo1 = getEventInfo(
                new Timeframe(2, 7, false,0),
                e1, e2);

        StimulusInfo stimulusInfo2 = getEventInfo(
                new Timeframe(3, 4, true, 1),
                e3);



        StimulusInfoStore store = getEventInfoStore(Arrays.asList(stimulusInfo1, stimulusInfo2));
        EventController eventController = new EventController(getScenarioStore(store));
        String errMsg = "expected event at this TimeStep";

        List<Stimulus> stimuli;
        //only default event
        stimuli = eventController.getEventsForTime(0.5);
        assertEquals(1, stimuli.size());
        assertTimeStamp(stimuli, 0.5);

        //only stimulusInfo1
        stimuli = eventController.getEventsForTime(2.5);
        assertEquals(3, stimuli.size());
        assertTimeStamp(stimuli, 2.5);

        //both stimulusInfo1 stimulusInfo2
        stimuli = eventController.getEventsForTime(3.5);
        assertEquals(4, stimuli.size());
        assertTrue(errMsg, stimuli.contains(e1));
        assertTrue(errMsg, stimuli.contains(e2));
        assertTrue(errMsg, stimuli.contains(e3));
        assertTimeStamp(stimuli, 3.5);

        //only stimulusInfo1
        stimuli = eventController.getEventsForTime(4.5);
        assertEquals(3, stimuli.size());
        assertTrue(errMsg, stimuli.contains(e1));
        assertTrue(errMsg, stimuli.contains(e2));
        assertTimeStamp(stimuli, 4.5);

        //one time event is over only stimuli from stimulusInfo2
        stimuli = eventController.getEventsForTime(7.8);
        assertEquals(2, stimuli.size());
        assertTrue(errMsg, stimuli.contains(e3));
        assertTimeStamp(stimuli, 7.8);

        //no event (only the default time event)
        //one time event is over only stimuli from stimulusInfo2
        stimuli = eventController.getEventsForTime(8.3);
        assertEquals(1, stimuli.size());
        assertTimeStamp(stimuli, 8.3);

    }

    private void assertTimeStamp(List<Stimulus> stimuli, double simTime){
        stimuli.forEach(e -> assertEquals(e.getTime(), simTime, 1e-3));
    }
}