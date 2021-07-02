package org.vadere.simulator.control.psychology.cognition.models;

import org.vadere.state.psychology.cognition.SelfCategory;
import org.vadere.state.psychology.perception.types.*;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;

import java.util.Collection;

/**
 * The {@link InformationCognitionModel} just passes the perceived stimulus to the
 * behavioral/locomotion layer without further processing.
 */
public class InformationCognitionModel implements ICognitionModel {

    private Topography topography;

    @Override
    public void initialize(Topography topography) {
        this.topography = topography;
    }

    @Override
    public void update(Collection<Pedestrian> pedestrians) {
        for (Pedestrian pedestrian : pedestrians) {

            SelfCategory nextSelfCategory = SelfCategory.UNINFORMED;


            if (!pedestrian.getKnowledgeBase().knowsAbout("informed")) {
                nextSelfCategory = SelfCategory.INFORMED;
            }


            pedestrian.setSelfCategory(nextSelfCategory);



        }
    }

}
