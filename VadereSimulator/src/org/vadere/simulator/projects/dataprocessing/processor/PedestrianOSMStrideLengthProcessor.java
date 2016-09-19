package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.simulator.control.SimulationState;
import org.vadere.simulator.models.Model;
import org.vadere.simulator.models.osm.OptimalStepsModel;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepPedestrianIdDataKey;
import org.vadere.state.scenario.Pedestrian;

import java.util.Collection;

public class PedestrianOSMStrideLengthProcessor extends DataProcessor<TimestepPedestrianIdDataKey, Double> {
    private OptimalStepsModel osm;

    public PedestrianOSMStrideLengthProcessor() {
        super("strideLength");

        this.osm = null;
    }

    @Override
    protected void doUpdate(final SimulationState state) {
        Collection<Pedestrian> peds = state.getTopography().getElements(Pedestrian.class);
        // TODO: if osm != null then compute stridelength
        peds.forEach(ped -> this.addValue(new TimestepPedestrianIdDataKey(state.getStep(), ped.getId()), this.osm == null ? Double.NaN : 0.0));

        // TODO Use this comment from the old implementation for this implementation
//        @Override
//        public Map<String, Table> getOutputTables() {
//            outputTables.clear();
//
//            List<PedestrianOSM> pedestrians = ListUtils.select(
//                    topography.getElements(Pedestrian.class), PedestrianOSM.class);
//            for (PedestrianOSM pedestrian : pedestrians) {
//
//                List<Double>[] pedStrides = pedestrian.getStrides();
//                if (pedStrides.length > 0 && !pedStrides[0].isEmpty()) {
//
//                    Table strides = new Table("strideLength", "strideTime");
//
//                    for (int i = 0; i < pedStrides[0].size(); i++) {
//                        strides.addRow();
//                        strides.addColumnEntry("strideLength", pedStrides[0].get(i));
//                        strides.addColumnEntry("strideTime", pedStrides[1].get(i));
//                    }
//
//                    outputTables.put(String.valueOf(pedestrian.getId()), strides);
//                }
//
//                pedestrian.clearStrides();
//
//            }
//
//            return outputTables;
//        }
    }

    @Override
    public void init(final ProcessorManager manager) {
        Model model = manager.getMainModel();
        if (model instanceof OptimalStepsModel)
            this.osm = (OptimalStepsModel) model;
    }
}
