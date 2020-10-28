package org.vadere.simulator.control.psychology.perception;

import org.junit.Test;
import org.vadere.simulator.projects.ScenarioStore;
import org.vadere.state.psychology.perception.json.StimulusInfo;
import org.vadere.state.psychology.perception.json.StimulusInfoStore;
import org.vadere.state.psychology.perception.types.*;
import org.vadere.state.psychology.perception.types.Stimulus;
import org.vadere.util.geometry.shapes.VRectangle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class StimulusControllerTest {

    private ScenarioStore getScenarioStore(StimulusInfoStore store) {
        return new ScenarioStore("name",
                "description",
                "mainModel",
                null,
                null,
                null,
                null,
                store);
    }

    private ScenarioStore getScenarioStoreContainingRecurringStimulus(boolean isRecurring) {
        return new ScenarioStore("name",
                "description",
                "mainModel",
                null,
                null,
                null,
                null,
                getStimulusInfoStoreContainingRecurringStimulus(isRecurring));
    }

    private StimulusInfoStore getStimulusInfoStoreContainingRecurringStimulus(boolean isRecurring) {
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

    private StimulusInfoStore getStimulusInfoStore(List<StimulusInfo> stimulusList){
        StimulusInfoStore store = new StimulusInfoStore();
        store.setStimulusInfos(stimulusList);
        return store;
    }

    private StimulusInfo getStimulusInfo(Timeframe timeframe, Stimulus... stimuli){
        StimulusInfo stimulusInfo = new StimulusInfo();
        stimulusInfo.setTimeframe(timeframe);
        stimulusInfo.setStimuli(Arrays.asList(stimuli));
        return stimulusInfo;
    }

    @Test
    public void stimulusControllerConstructorFindsNoStimuliIfPassingEmptyScenarioStore() {
        ScenarioStore emptyScenarioStore = new ScenarioStore("emptyScenarioStore");

        StimulusController stimulusController = new StimulusController(emptyScenarioStore);

        assertEquals(0, stimulusController.getOneTimeStimuli().size());
        assertEquals(0, stimulusController.getRecurringStimuli().size());
    }

    @Test
    public void stimulusControllerConstructorDetectsOneTimeStimuliProperly() {
        boolean isRecurringStimulus = false;
        ScenarioStore scenarioStoreContainingOneOneTimeStimulus = getScenarioStoreContainingRecurringStimulus(isRecurringStimulus);

        StimulusController stimulusController = new StimulusController(scenarioStoreContainingOneOneTimeStimulus);

        assertEquals(1, stimulusController.getOneTimeStimuli().size());
        assertEquals(0, stimulusController.getRecurringStimuli().size());
    }

    @Test
    public void stimulusControllerConstructorDetectsRecurringStimuliProperly() {
        boolean isRecurringStimulus = true;
        ScenarioStore scenarioStoreContainingOneRecurringStimulus = getScenarioStoreContainingRecurringStimulus(isRecurringStimulus);

        StimulusController stimulusController = new StimulusController(scenarioStoreContainingOneRecurringStimulus);

        assertEquals(0, stimulusController.getOneTimeStimuli().size());
        assertEquals(1, stimulusController.getRecurringStimuli().size());
    }

    @Test
    public void stimulusControllerConstructorDetectsOneTimeAndRecurringStimuliProperly() {
        // Create a list containing one one-time and one recurring stimulus.
        List<StimulusInfo> oneTimeAndRecurringStimuli = new ArrayList<>();
        oneTimeAndRecurringStimuli.addAll(getStimulusInfoStoreContainingRecurringStimulus(false).getStimulusInfos());
        oneTimeAndRecurringStimuli.addAll(getStimulusInfoStoreContainingRecurringStimulus(true).getStimulusInfos());

        StimulusInfoStore stimulusInfoStoreWithBothStimuli = new StimulusInfoStore();
        stimulusInfoStoreWithBothStimuli.setStimulusInfos(oneTimeAndRecurringStimuli);

        ScenarioStore scenarioStoreContainingOneTimeAndRecurringStimulus = new ScenarioStore("name",
                "description",
                "mainModel",
                null,
                null,
                null,
                null,
                stimulusInfoStoreWithBothStimuli);

        StimulusController stimulusController = new StimulusController(scenarioStoreContainingOneTimeAndRecurringStimulus);

        assertEquals(1, stimulusController.getOneTimeStimuli().size());
        assertEquals(1, stimulusController.getRecurringStimuli().size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void timeframeIsActiveAtSimulationTimeThrowsExceptionIfNoRecurringStimulusTimeframe() {
        boolean isRecurringStimulus = false;
        double simulationTime = 0.8;

        Timeframe timeframe = new Timeframe(0.75, 1.25, isRecurringStimulus, 1.0);

        StimulusController.timeframeIsActiveAtSimulationTime(timeframe, simulationTime);
    }

    @Test
    public void timeframeIsActiveAtSimulationTimeReturnsTrueIfSimulationTimeFallsOntoStartTimePeriodically() {
        boolean recurringStimulus = true;
        double startTime = 0.75;
        double endTime = 1.25;
        double waitTimeBetweenRepetition = 1.0;
        double simulationTime = startTime;
        double periodicity = (endTime - startTime) + waitTimeBetweenRepetition;

        Timeframe timeframe = new Timeframe(startTime, endTime, recurringStimulus, waitTimeBetweenRepetition);

        for (int i = 0; i < 50; i++) {
            double currentSimulationTime = simulationTime + (i * periodicity);

            boolean timeframeIsActive = StimulusController.timeframeIsActiveAtSimulationTime(timeframe, currentSimulationTime);
            assertTrue(timeframeIsActive);
        }
    }

    @Test
    public void timeframeIsActiveAtSimulationTimeReturnsTrueIfSimulationTimeFallsOntoEndTimePeriodically() {
        boolean recurringStimulus = true;
        double startTime = 0.75;
        double endTime = 1.25;
        double waitTimeBetweenRepetition = 1.0;
        double simulationTime = endTime;
        double periodicity = (endTime - startTime) + waitTimeBetweenRepetition;

        Timeframe timeframe = new Timeframe(startTime, endTime, recurringStimulus, waitTimeBetweenRepetition);

        for (int i = 0; i < 50; i++) {
            double currentSimulationTime = simulationTime + (i * periodicity);

            boolean timeframeIsActive = StimulusController.timeframeIsActiveAtSimulationTime(timeframe, currentSimulationTime);
            assertTrue(timeframeIsActive);
        }
    }

    @Test
    public void timeframeIsActiveAtSimulationTimeReturnsTrueIfSimulationTimeFallsBetweenStartAndEndTimePeriodically() {
        boolean recurringStimulus = true;
        double startTime = 0.75;
        double endTime = 1.25;
        double waitTimeBetweenRepetition = 1.0;
        double simulationTime = 1.0;
        double periodicity = (endTime - startTime) + waitTimeBetweenRepetition;

        Timeframe timeframe = new Timeframe(startTime, endTime, recurringStimulus, waitTimeBetweenRepetition);

        for (int i = 0; i < 50; i++) {
            double currentSimulationTime = simulationTime + (i * periodicity);

            boolean timeframeIsActive = StimulusController.timeframeIsActiveAtSimulationTime(timeframe, currentSimulationTime);
            assertTrue(timeframeIsActive);
        }
    }

    @Test
    public void timeframeIsActiveAtSimulationTimeReturnsFalseIfSimulationTimeIsBeforeStartTime() {
        boolean recurringStimulus = true;
        double startTime = 0.75;
        double endTime = 1.25;
        double waitTimeBetweenRepetition = 1.0;
        double simulationTime = 0.10;
        double increment = 0.05;

        Timeframe timeframe = new Timeframe(startTime, endTime, recurringStimulus, waitTimeBetweenRepetition);

        while (simulationTime < startTime) {
            boolean timeframeIsActive = StimulusController.timeframeIsActiveAtSimulationTime(timeframe, simulationTime);
            assertFalse(timeframeIsActive);

            simulationTime += increment;
        }
    }

    @Test
    public void timeframeIsActiveAtSimulationTimeReturnsTrueIfSimulationTimeBetweenStartAndEndTime() {
        boolean recurringStimulus = true;
        double startTime = 0.75;
        double endTime = 1.25;
        double waitTimeBetweenRepetition = 1.0;
        double simulationTime = startTime;
        double increment = 0.05;

        Timeframe timeframe = new Timeframe(startTime, endTime, recurringStimulus, waitTimeBetweenRepetition);

        while (simulationTime < endTime) {
            boolean timeframeIsActive = StimulusController.timeframeIsActiveAtSimulationTime(timeframe, simulationTime);
            assertTrue(timeframeIsActive);

            simulationTime += increment;
        }
    }

    @Test
    public void timeframeIsActiveAtSimulationTimeReturnsFalseIfWaitTimeIsZeroButSimulationTimeBeforeStartTime() {
        boolean recurringStimulus = true;
        double startTime = 0.75;
        double endTime = 1.25;
        double waitTimeBetweenRepetition = 0.0;
        double simulationTime = 0;
        double increment = 0.05;

        Timeframe timeframe = new Timeframe(startTime, endTime, recurringStimulus, waitTimeBetweenRepetition);

        while (simulationTime < startTime) {
            boolean timeframeIsActive = StimulusController.timeframeIsActiveAtSimulationTime(timeframe, simulationTime);
            assertFalse(timeframeIsActive);

            simulationTime += increment;
        }
    }

    @Test
    public void timeframeIsActiveAtSimulationTimeReturnsTrueIfWaitTimeIsZeroAndSimulationTimeGreaterThanStartTime() {
        boolean recurringStimulus = true;
        double startTime = 0.75;
        double endTime = 1.25;
        double waitTimeBetweenRepetition = 0.0;
        double simulationTime = startTime;
        double increment = 0.10;

        Timeframe timeframe = new Timeframe(startTime, endTime, recurringStimulus, waitTimeBetweenRepetition);

        while (simulationTime < 500 * endTime) {
            boolean timeframeIsActive = StimulusController.timeframeIsActiveAtSimulationTime(timeframe, simulationTime);
            assertTrue(timeframeIsActive);

            simulationTime += increment;
        }
    }

    @Test
    public void getStimuliForTimeAlwaysCreatesASingleElapsedTimeStimulusWithRequestedSimulationTime() {
        boolean isRecurringStimulus = false;
        ScenarioStore scenarioStoreContainingOneOneTimeStimulus = getScenarioStoreContainingRecurringStimulus(isRecurringStimulus);

        StimulusController stimulusController = new StimulusController(scenarioStoreContainingOneOneTimeStimulus);

        double expectedSimulationTime = 1.0;

        List<Stimulus> activeStimuli = stimulusController.getStimuliForTime(expectedSimulationTime);

        if (activeStimuli.size() == 1) {
            Stimulus stimulus = activeStimuli.get(0);

            assertEquals(ElapsedTime.class, stimulus.getClass());
            assertEquals(expectedSimulationTime, stimulus.getTime(), 10e-1);
        } else {
            fail("Expected only one stimulus for simulationTime = " + expectedSimulationTime);
        }
    }

    @Test
    public void getStimuliForTimeTimestampsEachActiveStimulus() {
        boolean isRecurringStimulus = false;
        ScenarioStore scenarioStoreContainingOneOneTimeStimulus = getScenarioStoreContainingRecurringStimulus(isRecurringStimulus);

        // Store one concrete one-time stimulus in "StimulusInfoStore" to check if its timestamp is updated.
        Wait wait = new Wait();
        List<Stimulus> stimuli = new ArrayList<>();
        stimuli.add(wait);

        StimulusInfo activeStimulusInfo = scenarioStoreContainingOneOneTimeStimulus.getStimulusInfoStore().getStimulusInfos().get(0);
        activeStimulusInfo.setStimuli(stimuli);

        StimulusController stimulusController = new StimulusController(scenarioStoreContainingOneOneTimeStimulus);

        double expectedSimulationTime = 5.0;

        List<Stimulus> activeStimuli = stimulusController.getStimuliForTime(expectedSimulationTime);

        for (Stimulus stimulus : activeStimuli) {
            assertEquals(expectedSimulationTime, stimulus.getTime(), 10e-1);
        }

        assertEquals(2, activeStimuli.size());
    }

    @Test
    public void getStimuliForTimeDoesNotTimestampInactiveStimuli() {
        boolean isRecurringStimulus = false;
        ScenarioStore scenarioStoreContainingOneOneTimeStimulus = getScenarioStoreContainingRecurringStimulus(isRecurringStimulus);

        // Store one concrete one-time stimulus in "StimulusInfoStore" to check if its timestamp is updated.
        Wait wait = new Wait();
        List<Stimulus> stimuli = new ArrayList<>();
        stimuli.add(wait);

        StimulusInfo inactiveStimulusInfo = scenarioStoreContainingOneOneTimeStimulus.getStimulusInfoStore().getStimulusInfos().get(0);
        inactiveStimulusInfo.setStimuli(stimuli);
        inactiveStimulusInfo.setTimeframe(new Timeframe(0, 1, false, 0));

        StimulusController stimulusController = new StimulusController(scenarioStoreContainingOneOneTimeStimulus);

        double expectedSimulationTime = 5.0;

        List<Stimulus> activeStimuli = stimulusController.getStimuliForTime(expectedSimulationTime);

        assertEquals(1, activeStimuli.size());
        assertEquals(0, wait.getTime(), 10e-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void timeframeIsActiveAtSimulationTimeThrowExceptionIfTimeframeHasRepeatFalse(){
        StimulusController.timeframeIsActiveAtSimulationTime(
                new Timeframe(2, 7, false,0), 1);
        fail("Should not be reached");
    }

    @Test
    public void timeframeIsActiveAtSimulationTimeReturnsTrueIfTimeframeIsActive(){
        Timeframe frame = new Timeframe(3, 4, true, 1);
        assertFalse(StimulusController.timeframeIsActiveAtSimulationTime(frame, 0.4));
        assertFalse(StimulusController.timeframeIsActiveAtSimulationTime(frame, 8.4));

        assertTrue(StimulusController.timeframeIsActiveAtSimulationTime(frame, 3.1));
        assertTrue(StimulusController.timeframeIsActiveAtSimulationTime(frame, 5.3));
    }

    @Test
    public void getStimuliForTimeReturnsCorrectStimuli(){
        Stimulus stimulus1 = new Wait(2.0);
        Stimulus stimulus2 = new Wait(3.0);
        Stimulus stimulus3 = new WaitInArea(12, new VRectangle(1,1,100,10.0));

        StimulusInfo stimulusInfo1 = getStimulusInfo(
                new Timeframe(2, 7, false,0),
                stimulus1, stimulus2);

        StimulusInfo stimulusInfo2 = getStimulusInfo(
                new Timeframe(3, 4, true, 1),
                stimulus3);

        StimulusInfoStore store = getStimulusInfoStore(Arrays.asList(stimulusInfo1, stimulusInfo2));
        StimulusController stimulusController = new StimulusController(getScenarioStore(store));
        String errorMessage = "expected stimuli at this time step";

        List<Stimulus> stimuli;
        //only default event
        stimuli = stimulusController.getStimuliForTime(0.5);
        assertEquals(1, stimuli.size());
        assertTimeStamp(stimuli, 0.5);

        //only stimulusInfo1
        stimuli = stimulusController.getStimuliForTime(2.5);
        assertEquals(3, stimuli.size());
        assertTimeStamp(stimuli, 2.5);

        //both stimulusInfo1 stimulusInfo2
        stimuli = stimulusController.getStimuliForTime(3.5);
        assertEquals(4, stimuli.size());
        assertTrue(errorMessage, stimuli.contains(stimulus1));
        assertTrue(errorMessage, stimuli.contains(stimulus2));
        assertTrue(errorMessage, stimuli.contains(stimulus3));
        assertTimeStamp(stimuli, 3.5);

        //only stimulusInfo1
        stimuli = stimulusController.getStimuliForTime(4.5);
        assertEquals(3, stimuli.size());
        assertTrue(errorMessage, stimuli.contains(stimulus1));
        assertTrue(errorMessage, stimuli.contains(stimulus2));
        assertTimeStamp(stimuli, 4.5);

        //one time event is over only stimuli from stimulusInfo2
        stimuli = stimulusController.getStimuliForTime(7.8);
        assertEquals(2, stimuli.size());
        assertTrue(errorMessage, stimuli.contains(stimulus3));
        assertTimeStamp(stimuli, 7.8);

        //no event (only the default time event)
        //one time event is over only stimuli from stimulusInfo2
        stimuli = stimulusController.getStimuliForTime(8.3);
        assertEquals(1, stimuli.size());
        assertTimeStamp(stimuli, 8.3);

    }

    private void assertTimeStamp(List<Stimulus> stimuli, double simTime){
        stimuli.forEach(e -> assertEquals(e.getTime(), simTime, 1e-3));
    }
}