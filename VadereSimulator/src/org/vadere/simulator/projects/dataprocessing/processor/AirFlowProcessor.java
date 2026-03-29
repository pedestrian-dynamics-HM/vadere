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

        if (xVelocity == null || yVelocity == null) {
            return;
        }
        double rectangularGridCellSize = airFlow.getRectangularGridCellSize();
        for (int row = 0; row < xVelocity.length; row++) {
            for (int column = 0; column < yVelocity[0].length; column++) {
                double xStart = column * rectangularGridCellSize + airFlow.getXmin();
                double xEnd = (column + 1) * rectangularGridCellSize + airFlow.getXmin();
                double yStart = row * rectangularGridCellSize + airFlow.getYmin();
                double yEnd = (row + 1) * rectangularGridCellSize + airFlow.getYmin();

                double xCenter = (xStart + xEnd) / 2.0;
                double yCenter = (yStart + yEnd) / 2.0;

                double xVal = xVelocity[row][column];
                double yVal = yVelocity[row][column];

                if (xCenter < airFlow.getXmin() || xCenter > airFlow.getXmax() || yCenter < airFlow.getYmin() || yCenter > airFlow.getYmax()) {
                    xVal = 0.0;
                    yVal = 0.0;
                }

                putValue(new TopographyGridKey(column, row), String.format("%.2f %.2f %.2f %.2f %.5f %.5f", xStart, xEnd, yStart, yEnd, xVal, yVal));
            }
        }
    }
}