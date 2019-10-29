package org.vadere.state.psychology.perception;

import org.junit.Before;
import org.junit.Test;
import org.vadere.state.psychology.perception.types.ElapsedTime;
import org.vadere.state.psychology.perception.types.Stimulus;
import org.vadere.state.psychology.perception.types.Wait;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class StimulusTest {

    List<Stimulus> emptyList;
    List<Stimulus> oneElapsedTimeStimulus;
    List<Stimulus> multipleElapsedTimeStimuli;
    List<Stimulus> mixedStimuli;

    @Before
    public void setUp() {
        double time = 0;
        ElapsedTime elapsedTime1 = new ElapsedTime(time);
        ElapsedTime elapsedTime2 = new ElapsedTime(time);
        Wait wait1 = new Wait(time);

        emptyList = new ArrayList<>();
        oneElapsedTimeStimulus = new ArrayList<>();
        multipleElapsedTimeStimuli = new ArrayList<>();
        mixedStimuli = new ArrayList<>();

        oneElapsedTimeStimulus.add(elapsedTime1);

        multipleElapsedTimeStimuli.add(elapsedTime1);
        multipleElapsedTimeStimuli.add(elapsedTime2);

        mixedStimuli.add(elapsedTime1);
        mixedStimuli.add(wait1);
        mixedStimuli.add(elapsedTime2);
    }


    @Test
    public void listContainsEventReturnsFalseIfListIsEmpty() {
        boolean actualResult = Stimulus.listContainsStimulus(emptyList, Stimulus.class);

        assertFalse(actualResult);
    }

    @Test
    public void listContainsEventReturnsFalseIfPassingNull() {
        boolean actualResult = Stimulus.listContainsStimulus(oneElapsedTimeStimulus, null);

        assertFalse(actualResult);
    }

    @Test
    public void listContainsEventReturnsFalseIfEventNotInList() {
        boolean actualResult = Stimulus.listContainsStimulus(oneElapsedTimeStimulus, Wait.class);

        assertFalse(actualResult);
    }

    @Test
    public void listContainsEventReturnsTrueIfPassedEventInList() {
        boolean actualResult = Stimulus.listContainsStimulus(oneElapsedTimeStimulus, ElapsedTime.class);

        assertTrue(actualResult);
    }

    @Test
    public void listContainsEventReturnsTrueIfPassedEventInListMultipleTimes() {
        boolean actualResult = Stimulus.listContainsStimulus(multipleElapsedTimeStimuli, ElapsedTime.class);

        assertTrue(actualResult);
    }

    @Test
    public void listContainsEventReturnsTrueIfPassedEventIsInMixedList() {
        boolean actualResult = Stimulus.listContainsStimulus(mixedStimuli, Wait.class);

        assertTrue(actualResult);
    }
}