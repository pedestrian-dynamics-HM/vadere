package org.vadere.simulator.models.psychology.selfcategorization;

import org.vadere.simulator.models.SpeedAdjuster;
import org.vadere.simulator.models.osm.PedestrianOSM;
import org.vadere.simulator.models.osm.optimization.StepCircleOptimizer;
import org.vadere.simulator.models.potential.fields.IPotentialFieldTarget;
import org.vadere.simulator.models.potential.fields.PotentialFieldAgent;
import org.vadere.simulator.models.potential.fields.PotentialFieldObstacle;
import org.vadere.state.attributes.models.AttributesOSM;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.Topography;

import java.util.List;
import java.util.Random;

/**
 * Use OSM as locomotion model. Therefore, extend "PedestrianOSM" maybe to inherit "updatePosition" and so on.
 */
public class PedestrianSelfCatThreat extends PedestrianOSM {

    public PedestrianSelfCatThreat(AttributesOSM attributesOSM, AttributesAgent attributesPedestrian, Topography topography, Random random, IPotentialFieldTarget potentialFieldTarget, PotentialFieldObstacle potentialFieldObstacle, PotentialFieldAgent potentialFieldPedestrian, List<SpeedAdjuster> speedAdjusters, StepCircleOptimizer stepCircleOptimizer) {
        super(attributesOSM, attributesPedestrian, topography, random, potentialFieldTarget, potentialFieldObstacle, potentialFieldPedestrian, speedAdjusters, stepCircleOptimizer);
    }

}
