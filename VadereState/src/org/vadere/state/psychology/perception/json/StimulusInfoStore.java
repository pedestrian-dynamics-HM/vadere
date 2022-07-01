package org.vadere.state.psychology.perception.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.vadere.state.psychology.perception.types.*;
import org.vadere.state.util.JacksonObjectMapper;
import org.vadere.util.geometry.shapes.VRectangle;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This class bundles multiple @see StimulusInfo objects.
 *
 * This class is just a wrapper to get a convenient JSON de-/serialization.
 * The JSON serialization should look like this:
 *
 *      "stimulusInfos": [
 *                {
 *                     "timeframe": {
 *                         "startTime":...,
 *                         "endTime":...,
 *                         "repeat":...,
 *                         "waitTimeBetweenRepetition":...
 *                     },
 *                     "stimuli": [
 *                         {"type":"ElapsedTime" },
 *                         {"type":"WaitInArea", "area": ... },
 *                         ...
 *                     ]
 *                },
 *                {
 *                    ...
 *                }
 *      ]
 */
public class StimulusInfoStore {

    // Member Variables
    private List<StimulusInfo> stimulusInfos;

    // Constructors
    public StimulusInfoStore() {
        this.stimulusInfos = new ArrayList<>();
    }

    // Getter
    public List<StimulusInfo> getStimulusInfos() {
        return stimulusInfos;
    }

    // Setter
    public void setStimulusInfos(List<StimulusInfo> stimulusInfos) {
        this.stimulusInfos = stimulusInfos;
    }

    public static void main(String... args) {
        // Create "Timeframe" and "Stimulus" objects and encapsulate them in "StimulusInfo" objects.
        Timeframe timeframe = new Timeframe(5, 30, false, 0);

        List<Stimulus> stimuli = new ArrayList<>();
        stimuli.add(new Wait());
        stimuli.add(new WaitInArea(0, new VRectangle(12.5, 0, 5, 6)));
        // stimuli.add(new WaitInArea(0, new VCircle(5, 5, 5)));

        Location location = new Location(new VRectangle(0,0,1000,1000));

        StimulusInfo stimulusInfo1 = new StimulusInfo();
        stimulusInfo1.setTimeframe(timeframe);
        stimulusInfo1.setLocation(location);
        stimulusInfo1.setStimuli(stimuli);

        List<StimulusInfo> stimulusInfos = new ArrayList<>();
        stimulusInfos.add(stimulusInfo1);

        StimulusInfoStore stimulusInfoStore = new StimulusInfoStore();
        stimulusInfoStore.setStimulusInfos(stimulusInfos);

        // Use annotations at stimulus classes to specify how JSON <-> Java mapping should look like.
        // "VShape" are mapped by "JacksonObjectMapper" implementation.
        ObjectMapper mapper = new JacksonObjectMapper();

        // De/-Serialize an "StimulusInfoStore":
        try {
            String jsonDataString = mapper.writeValueAsString(stimulusInfoStore);

            System.out.println("Serialized \"StimulusInfoStore\":");
            System.out.println(jsonDataString);
            System.out.println();

            System.out.println("Serialized \"StimulusInfo\" elements in the \"StimulusInfoStore\":");
            StimulusInfoStore deserializedStimulusInfoStore = mapper.readValue(jsonDataString, StimulusInfoStore.class);
            for (StimulusInfo stimulusInfo : deserializedStimulusInfoStore.getStimulusInfos()) {
                System.out.print(stimulusInfo.getTimeframe());
                System.out.print(stimulusInfo.getStimuli());
            }
            System.out.println();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Serialize specific stimuli:
        System.out.println("Examples of some stimulus serializations:");

        try {
            LinkedList<Integer> newTargetIds = new LinkedList<>();
            newTargetIds.add(1);
            newTargetIds.add(2);

            String changeTargetEventAsJsonString = mapper.writeValueAsString(new ChangeTarget(0.0, newTargetIds));
            System.out.println(changeTargetEventAsJsonString);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

    }




}
