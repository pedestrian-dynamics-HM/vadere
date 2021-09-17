package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.projects.dataprocessing.datakey.TimeGridKey;
import org.vadere.state.attributes.processor.AttributesAreaDensityGridCountingProcessor;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.util.geometry.LinkedCellsGrid;

@DataProcessorClass(label = "AreaDensityGridCountingSparseProcessor")
public class AreaDensityGridCountingSparseProcessor extends AreaDensityGridCountingProcessor<TimeGridKey, Integer> {

    public AreaDensityGridCountingSparseProcessor() {
        super();
        setAttributes(new AttributesAreaDensityGridCountingProcessor());
    }

    @Override
    protected void doUpdate(SimulationState state) {
        int step = state.getStep();
        AttributesAreaDensityGridCountingProcessor attr = (AttributesAreaDensityGridCountingProcessor)getAttributes();
        LinkedCellsGrid<Pedestrian> cellsElements = new LinkedCellsGrid<Pedestrian>(
                state.getTopography().getBounds().x,
                state.getTopography().getBounds().y,
                state.getTopography().getBounds().width,
                state.getTopography().getBounds().height,
                attr.getCellSize());
        state.getTopography().getPedestrianDynamicElements().getElements().forEach(cellsElements::addObject);
        int[][] count = cellsElements.getCellObjectCount();
        int pedCount;
        for (int r = 0; r < count.length; r++) {
            for (int c = 0; c < count[r].length; c++) {
                pedCount = count[r][c];
                if (pedCount > 0) {
                    TimeGridKey key = new TimeGridKey(step, r * attr.getCellSize(), c * attr.getCellSize(), attr.getCellSize());
                    this.putValue(key, pedCount);
                }
            }
        }
    }

}
