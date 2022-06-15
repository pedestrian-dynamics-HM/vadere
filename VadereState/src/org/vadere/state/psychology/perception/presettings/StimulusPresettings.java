package org.vadere.state.psychology.perception.presettings;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.lwjgl.system.CallbackI;
import org.vadere.state.psychology.perception.json.StimulusInfo;
import org.vadere.state.psychology.perception.json.StimulusInfoStore;
import org.vadere.state.psychology.perception.types.*;
import org.vadere.state.util.JacksonObjectMapper;
import org.vadere.util.geometry.shapes.VRectangle;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provide JSON presettings for commonly used stimuli.
 *
 * This class can be used as helper for GUI elements.
 */
public class StimulusPresettings {
    /** Map an event class (e.g., Threat) to a JSON string. */
    public static Map<Class, String> PRESETTINGS_MAP;

    // Static initializer for "PRESETTINGS_MAP".
    static {
        PRESETTINGS_MAP = new HashMap<>();

        Stimulus[] stimuliToUse = new Stimulus[] {
                new Threat(),
                new Wait(),
                new WaitInArea(0, new VRectangle(0, 0, 10, 10)),
                new ChangeTarget(),
                new ChangeTargetScripted(),
                new DistanceRecommendation(),
                new InformationStimulus(),
        };

        for (Stimulus stimulus : stimuliToUse) {
            // Container for a timeframe and the corresponding stimuli.
            StimulusInfo stimulusInfo = new StimulusInfo();

            List<Stimulus> stimuli = new ArrayList<>();
            stimuli.add(stimulus);

            stimulusInfo.setTimeframe(new Timeframe(0, 10, false, 0));
            stimulusInfo.setLocation(new Location(new VRectangle(0,0,1000,500)));
            stimulusInfo.setSubpopulationFilter(new SubpopulationFilter());
            stimulusInfo.setStimuli(stimuli);

            // Container for multiple stimulus infos.
            List<StimulusInfo> stimulusInfos = new ArrayList<>();
            stimulusInfos.add(stimulusInfo);

            StimulusInfoStore stimulusInfoStore = new StimulusInfoStore();
            stimulusInfoStore.setStimulusInfos(stimulusInfos);



            try {
                ObjectMapper mapper = new JacksonObjectMapper();
                // String jsonDataString = mapper.writeValueAsString(stimulusInfoStore);
                String jsonDataString = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(stimulusInfoStore);

                PRESETTINGS_MAP.put(stimulus.getClass(), jsonDataString);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }
}
