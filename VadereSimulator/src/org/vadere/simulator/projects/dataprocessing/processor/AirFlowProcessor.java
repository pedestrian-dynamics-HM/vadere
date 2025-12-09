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
        double rectangularGridCellSize = airFlow.getRectangularGridCellSize();

        if (xVelocity != null && yVelocity != null) {
            double[][] x_velocity = airFlow.getXVelocities();
            double[][] y_velocity = airFlow.getYVelocities();
            //state.getTopography().getBounds().width = x_velocity.length;

            int xSteps = (int) Math.ceil(state.getTopography().getBounds().width / rectangularGridCellSize);
            int ySteps = (int) Math.ceil(state.getTopography().getBounds().height / rectangularGridCellSize);

            for (int i = 0; i < x_velocity.length; i++) {
                for (int j = 0; j < y_velocity[0].length; j++) {
                    double xStart = j * rectangularGridCellSize + airFlow.getXmin();
                    double xEnd = (j + 1) * rectangularGridCellSize + airFlow.getXmin();
                    double yStart = i * rectangularGridCellSize + airFlow.getYmin();
                    double yEnd = (i + 1) * rectangularGridCellSize + airFlow.getYmin();
                    double xCenter = (xStart + xEnd) / 2.0;
                    double yCenter = (yStart + yEnd) / 2.0;
                    double xVal = xVelocity[i][j];
                    double yVal = yVelocity[i][j];
                    if (xCenter < airFlow.getXmin() || xCenter > airFlow.getXmax() || yCenter < airFlow.getYmin() || yCenter > airFlow.getYmax()) {
                        xVal = 0.0;
                        yVal = 0.0;
                    }
                    putValue(new TopographyGridKey(j, i), String.format("%.2f %.2f %.2f %.2f %.5f %.5f", xStart, xEnd, yStart, yEnd, xVal, yVal));
                }
            }
        }
    }
}