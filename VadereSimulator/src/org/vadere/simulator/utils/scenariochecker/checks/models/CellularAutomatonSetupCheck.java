package org.vadere.simulator.utils.scenariochecker.checks.models;

import org.vadere.simulator.models.osm.CellularAutomaton;
import org.vadere.simulator.projects.Scenario;
import org.vadere.simulator.utils.scenariochecker.ScenarioCheckerMessage;
import org.vadere.simulator.utils.scenariochecker.ScenarioCheckerReason;
import org.vadere.simulator.utils.scenariochecker.checks.AbstractScenarioCheck;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.DynamicElement;
import org.vadere.state.scenario.Source;
import org.vadere.state.scenario.Topography;
import org.vadere.state.types.ScenarioElementType;

import java.util.List;
import java.util.PriorityQueue;

/**
 * @author hm-mgoedel
 * Warnings if settings of scenario do not match with cellular automaton requirements
 *  todo: Add methods to change spwaning mode of source
 */

public class CellularAutomatonSetupCheck extends AbstractScenarioCheck {

    @Override
    public PriorityQueue<ScenarioCheckerMessage> runScenarioCheckerTest(Scenario scenario) {
        PriorityQueue<ScenarioCheckerMessage> messages = new PriorityQueue<>();

        String mainModelstring = scenario.getScenarioStore().getMainModel();

        if(mainModelstring != null && mainModelstring.equals(CellularAutomaton.class.getName())) {

            /* make sure that the CA spawning is activated */
            Topography topography = scenario.getTopography();
            List<Source> sourceList = topography.getSources();


            for (Source source : sourceList) {
                if (!source.getAttributes().getSpawnerAttributes().isEventPositionGridCA()) {
                    messages.add(msgBuilder.simulationAttrError().target(source)
                            .reason(ScenarioCheckerReason.CA_SPAWNING, "If the cellular automaton model is used, the CA spawning should be activated. Otherwise agents will not be placed on a well-defined grid.")
                            .build());
                }
            }

            /* make sure that the free-flow speed is set to 1.0m/s and standard deviation is set to 0.0
            (agent's speed in CA can only be varied by changing the simulationTimeStep)
            */

            if (scenario.getAttributesPedestrian().getSpeedDistributionStandardDeviation() > 0.0) { // || scenario.getAttributesPedestrian().getSpeedDistributionMean() != 1.0) {
                messages.add(msgBuilder.simulationAttrError()
                        .reason(ScenarioCheckerReason.CA_SPAWNING, "If the cellular automaton model is used, the free-flow speed should be set to 1.0 m/s and standard deviation to 0.0 m/s. At the moment, individual speeds through event-driven setup is not supported. If you want to change the speed of the pedestrians, adapt the parameter simTimeStepLength in Simulation.")
                        .build());
            }

            /* make sure that agent's radius is 0.2 */
            if (scenario.getAttributesPedestrian().getRadius() != 0.2){
                messages.add(msgBuilder.simulationAttrError()
                        .reason(ScenarioCheckerReason.CA_SPAWNING, "If the cellular automaton model is used, the radius should be set to 0.2 m.")
                        .build());
            }

            /*dynamical elements */
            for(DynamicElement e: scenario.getTopography().getPedestrianDynamicElements().getInitialElements() ){
                if(e.getType() == ScenarioElementType.PEDESTRIAN) {
                    AttributesAgent att_e = (AttributesAgent) e.getAttributes();

                    /* warning: dynamical elements might not be place on the grid */
                    messages.add(msgBuilder.simulationAttrWarning()
                            .target(e)
                            .reason(ScenarioCheckerReason.CA_SPAWNING, "Make sure that the dynamical elements are set on the CA grid to avoid overlaps and other issues.")
                            .build());
                    if(att_e.getRadius() != 0.2){
                        messages.add(msgBuilder.simulationAttrError()
                                .target(e)
                                .reason(ScenarioCheckerReason.CA_SPAWNING, "If the cellular automaton model is used, the radius should be set to 0.2 m.")
                                .build());
                    }
                    if(att_e.getSpeedDistributionMean() != 1.0 ||att_e.getSpeedDistributionStandardDeviation() != 0.0 || ((Agent) e).getFreeFlowSpeed() != 1.0){
                        messages.add(msgBuilder.simulationAttrError()
                                .target(e)
                                .reason(ScenarioCheckerReason.CA_SPAWNING, "If the cellular automaton model is used, the free-flow speed should be set to 1.0 m/s and standard deviation to 0.0 m/s. At the moment, individual speeds through event-driven setup is not supported. If you want to change the speed of the pedestrians, adapt the parameter simTimeStepLength in Simulation.")
                                .build());
                    }
                }
            }

        }
        return messages;
    }
}
