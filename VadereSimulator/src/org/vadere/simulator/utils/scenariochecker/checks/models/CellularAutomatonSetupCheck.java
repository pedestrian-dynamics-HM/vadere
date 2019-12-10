package org.vadere.simulator.utils.scenariochecker.checks.models;

import org.vadere.simulator.models.groups.cgm.CentroidGroupModel;
import org.vadere.simulator.models.osm.CellularAutomaton;
import org.vadere.simulator.projects.Scenario;
import org.vadere.simulator.utils.scenariochecker.ScenarioCheckerMessage;
import org.vadere.simulator.utils.scenariochecker.ScenarioCheckerReason;
import org.vadere.simulator.utils.scenariochecker.checks.AbstractScenarioCheck;
import org.vadere.state.attributes.models.AttributesCA;
import org.vadere.state.attributes.models.AttributesCGM;
import org.vadere.state.attributes.models.AttributesOSM;
import org.vadere.state.scenario.Source;
import org.vadere.state.scenario.Topography;

import java.util.List;
import java.util.Optional;
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
                if (!source.getAttributes().isSpawnAtGridPositionsCA()) {
                    messages.add(msgBuilder.simulationAttrError().target(source)
                            .reason(ScenarioCheckerReason.CA_SPAWNING, "If the cellular automaton model is used, the CA spawning should be activated. Otherwise agents will not be placed on a well-defined grid.")
                            .build());
                }
            }

            /* make sure that the free-flow speed is set to 1.0m/s and standard deviation is set to 0.0
            (agent's speed in CA can only be varied by changing the simulationTimeStep)
            */

            if (scenario.getAttributesPedestrian().getSpeedDistributionStandardDeviation() > 0.0 || scenario.getAttributesPedestrian().getSpeedDistributionMean() != 1.0) {
                messages.add(msgBuilder.simulationAttrError()
                        .reason(ScenarioCheckerReason.CA_SPAWNING, "If the cellular automaton model is used, the free-flow speed should be set to 1.0 m/s and standard deviation to 0.0 m/s. At the moment, individual speeds through event-driven setup is not supported. If you want to change the speed of the pedestrians, adapt the parameter simTimeStepLength in Simulation.")
                        .build());
            }

        }
        return messages;
    }
}
