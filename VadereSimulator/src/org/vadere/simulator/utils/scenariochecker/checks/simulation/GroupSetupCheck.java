package org.vadere.simulator.utils.scenariochecker.checks.simulation;

import org.vadere.simulator.models.groups.cgm.CentroidGroupModel;
import org.vadere.simulator.models.osm.OptimalStepsModel;
import org.vadere.simulator.projects.Scenario;

import org.vadere.simulator.utils.scenariochecker.ScenarioCheckerMessage;
import org.vadere.simulator.utils.scenariochecker.ScenarioCheckerReason;
import org.vadere.simulator.utils.scenariochecker.checks.AbstractScenarioCheck;
import org.vadere.state.attributes.models.AttributesCGM;
import org.vadere.state.attributes.models.AttributesOSM;
import org.vadere.state.scenario.Source;
import org.vadere.state.scenario.Topography;

import java.util.Optional;
import java.util.PriorityQueue;

/**
 * @author hm-mgoedel
 * Errors if group settings are used in the source with
 *  - another model than OSM (currently not implemented)
 *  - adding CentroidGroupModel as a submodel to the OSM
 *  - added AttributesCGM to the model attributes
 *  todo: Add methods to resolve missing information to the scenario file (as automatic id assigment for other checks)
 */

public class GroupSetupCheck extends AbstractScenarioCheck {

    @Override
    public PriorityQueue<ScenarioCheckerMessage> runScenarioCheckerTest(Scenario scenario) {
        PriorityQueue<ScenarioCheckerMessage> ret = new PriorityQueue<>();
        Topography topography = scenario.getTopography();

        for(Source s: topography.getSources()){

            if(s.getAttributes().getGroupSizeDistribution().size() > 1) {

                // check if OSM is used (currently groups only work with OSM) and attributes are present
                Optional attr_osm_opt = scenario.getModelAttributes().stream().filter(attr -> attr.getClass().equals(AttributesOSM.class)).findFirst();
                if (scenario.getScenarioStore().getMainModel().equals(OptimalStepsModel.class.getName()) && attr_osm_opt.isPresent()) {
                    AttributesOSM attr_osm = (AttributesOSM) attr_osm_opt.get();

                    // check if submodel was added
                    boolean contains_submodel = attr_osm.getSubmodels().stream().anyMatch(submodel -> submodel.equals(CentroidGroupModel.class.getName()));
                    if (!contains_submodel) {
                        ret.add(msgBuilder.simulationAttrError()
                                .target(s)
                                .reason(ScenarioCheckerReason.GROUP_SETUP_IGNORED, "CentroidGroupModel has to be added to the submodels of the Optimal Steps Model in order to simulate groups. Group settings will be ignored!")
                                .build());
                    }

                    // check if CGM attributes were added
                    boolean contains_attr = scenario.getModelAttributes().stream().anyMatch(attr -> attr.getClass().equals(AttributesCGM.class));
                    if (!contains_attr) {
                        ret.add(msgBuilder.simulationAttrError()
                                .target(s)
                                .reason(ScenarioCheckerReason.GROUP_SETUP_IGNORED, "AttributesCGM need to be added to the models and configured in order to simulate groups. Group settings will be ignored!")
                                .build());
                    }

                } else { // Other model present
                    ret.add(msgBuilder.simulationAttrError()
                            .reason(ScenarioCheckerReason.GROUP_SETUP_IGNORED, "Group setup works currently only with the Optimal Steps Model. Group settings will be ignored! ")
                            .build());
                }
            }
        }
        return ret;
    }
}
