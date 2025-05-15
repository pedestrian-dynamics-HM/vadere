package org.vadere.simulator.models.airflow;

import org.vadere.simulator.models.Model;
import org.vadere.simulator.projects.Domain;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.AirFlow;
import org.vadere.simulator.context.VadereContext;

import java.util.List;
import java.util.Random;


public abstract class AbstractAirFlowModel implements Model {

    protected AirFlow airFlow;

    @Override
    public void initialize(List<Attributes> attributesList, Domain domain, AttributesAgent attributesPedestrian, Random random) {
        String scenarioPath = VadereContext.getCtx(domain.getTopography()).getString("scenarioPath");
        this.airFlow = new AirFlow(scenarioPath, "", domain.getTopography().getBoundingBoxWidth());
        domain.getTopography().setAirFlow(airFlow);
    }

    @Override
    public void preLoop(double simTimeInSec) {
        setupAirFlow();
    }

    @Override
    public void postLoop(double simTimeInSec) {
        // ignore
    }

    @Override
    public void update(double simTimeInSec) {
        // ignore
    }

    public abstract void setupAirFlow();

}
