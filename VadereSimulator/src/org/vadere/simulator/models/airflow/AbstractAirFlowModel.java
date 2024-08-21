package org.vadere.simulator.models.airflow;

import org.vadere.simulator.models.Model;
import org.vadere.simulator.projects.Domain;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.AirFlow;

import java.util.List;
import java.util.Random;


public abstract class AbstractAirFlowModel implements Model {

    protected AirFlow airFlow;

    @Override
    public void initialize(List<Attributes> attributesList, Domain domain, AttributesAgent attributesPedestrian, Random random) {
        airFlow = domain.getTopography().getAirFlow();
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
