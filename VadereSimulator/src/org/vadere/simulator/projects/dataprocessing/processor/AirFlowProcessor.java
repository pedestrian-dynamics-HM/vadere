package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.TopographyGridKey;
import org.vadere.state.scenario.AirFlow;


@DataProcessorClass()
public class AirFlowProcessor extends DataProcessor<TopographyGridKey, String> {

    AirFlow airFlow;

    public AirFlowProcessor() {
        super("xPosStart", "xPosEnd", "yPosStart", "yPosEnd", "xVelocity", "yVelocity");
    }

    @Override
    public void init(ProcessorManager manager) {
        airFlow = manager.getTopography().getAirFlow();
    }

    @Override
    protected void doUpdate(SimulationState state) {
        // ignore
    }

    @Override
    public void postLoop(SimulationState state) {


        double[][] xVelocity = airFlow.getXVelocities();
        double[][] yVelocity = airFlow.getYVelocities();
        double gridSize = airFlow.getGridSize();

        if (xVelocity != null && yVelocity != null) {
            for (int i = 0; i < xVelocity.length; i++) {
                for (int j = 0; j < xVelocity[i].length; j++) {
                    putValue(new TopographyGridKey(j, i), String.format("%.2f %.2f %.2f %.2f %.5f %.5f", j*gridSize, (j+1)*gridSize, i*gridSize, (i+1)*gridSize, xVelocity[i][j], yVelocity[i][j]));
                }
            }
        }
    }
}
