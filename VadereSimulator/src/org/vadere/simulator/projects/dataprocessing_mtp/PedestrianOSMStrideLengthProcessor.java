package org.vadere.simulator.projects.dataprocessing_mtp;

import org.vadere.simulator.control.SimulationState;
import org.vadere.simulator.models.Model;
import org.vadere.simulator.models.osm.OptimalStepsModel;
import org.vadere.state.scenario.Pedestrian;

import java.util.Collection;

public class PedestrianOSMStrideLengthProcessor extends Processor<TimestepPedestrianIdDataKey, Double> {
    private OptimalStepsModel osm;

    public PedestrianOSMStrideLengthProcessor() {
        super("stridelength");

        this.osm = null;
    }

    @Override
    protected void doUpdate(final SimulationState state) {
        Collection<Pedestrian> peds = state.getTopography().getElements(Pedestrian.class);
        // TODO: if osm != null then compute stridelength
        peds.forEach(ped -> this.setValue(new TimestepPedestrianIdDataKey(state.getStep(), ped.getId()), this.osm == null ? Double.NaN : 0.0));
    }

    @Override
    void init(final AttributesProcessor attributes, final ProcessorManager manager) {
        Model model = manager.getModel();
        if (model instanceof OptimalStepsModel)
            this.osm = (OptimalStepsModel) model;
    }
}
