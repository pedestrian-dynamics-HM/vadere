package org.vadere.simulator.utils.scenariochecker.checks.models.psychology;

import org.vadere.simulator.control.psychology.cognition.models.SocialDistancingCognitionModel;
import org.vadere.simulator.models.osm.CellularAutomaton;
import org.vadere.simulator.projects.Scenario;
import org.vadere.simulator.utils.scenariochecker.ScenarioCheckerMessage;
import org.vadere.simulator.utils.scenariochecker.ScenarioCheckerReason;
import org.vadere.simulator.utils.scenariochecker.checks.AbstractScenarioCheck;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.psychology.perception.json.StimulusInfo;
import org.vadere.state.psychology.perception.types.DistanceRecommendation;
import org.vadere.state.psychology.perception.types.Stimulus;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.DynamicElement;
import org.vadere.state.scenario.Source;
import org.vadere.state.scenario.Topography;
import org.vadere.state.types.ScenarioElementType;

import java.util.List;
import java.util.PriorityQueue;
import java.util.stream.Collectors;

/**
 * @author Christina Maria Mayr
 * Warnings if social distance is out or range
 */

public class StimulusCheck extends AbstractScenarioCheck {


    private final static double LOW_BOUND = 1.25;
    private final static double HIGH_BOUND = 2.0;

    @Override
    public PriorityQueue<ScenarioCheckerMessage> runScenarioCheckerTest(Scenario scenario) {
        PriorityQueue<ScenarioCheckerMessage> messages = new PriorityQueue<>();


        String mainModelstring  = scenario.getScenarioStore().getAttributesPsychology().getPsychologyLayer().getCognition();


        if(mainModelstring != null && mainModelstring.equals(SocialDistancingCognitionModel.class.getSimpleName())) {

            List<StimulusInfo> stimulusInfos = scenario.getScenarioStore().getStimulusInfoStore().getStimulusInfos();

            int counter = 0;
            for (StimulusInfo stimulusInfo : stimulusInfos) {

                List<Stimulus> socialDistance = stimulusInfo.getStimuli().stream().filter(stimulus -> stimulus instanceof DistanceRecommendation).collect(Collectors.toList());

                counter += socialDistance.size();

                double socialDistanceVal;
                if (socialDistance.size() == 1){
                    socialDistanceVal = ((DistanceRecommendation) socialDistance.get(0)).getSocialDistance();
                    // make sure that the social distance is in [1.25, 2.0]
                    if (socialDistanceVal < 1.25 || socialDistanceVal > 2.0) {

                        messages.add(msgBuilder.perceptionAttrError()
                                .reason(ScenarioCheckerReason.SOCIAL_DISTANCING, String.format(" [%3.2f - %3.2f] current value: %3.2f", LOW_BOUND, HIGH_BOUND, socialDistanceVal))
                                .build());
                    }
                } else if (socialDistance.size() > 1){
                    // avoid contradictory social distances within in one time frame
                    messages.add(msgBuilder.perceptionAttrError()
                            .reason(ScenarioCheckerReason.SOCIAL_DISTANCING, "In each time frame, only one stimulus of type DistanceRecommendation is allowed. " + socialDistance.size() + " stimuli found in " + stimulusInfo.toString())
                            .build());
                }

            }

            if (counter == 0){
                // no social distance defined although the respective cognition model is set
                messages.add(msgBuilder.perceptionAttrError()
                        .reason(ScenarioCheckerReason.SOCIAL_DISTANCING,  mainModelstring + " is defined as CognitionModel. No stimuli of type DistanceRecommendation under the perception tab.")
                        .build());
            }

        }
        return messages;
    }
}
