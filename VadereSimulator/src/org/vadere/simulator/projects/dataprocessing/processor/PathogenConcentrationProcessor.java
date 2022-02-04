package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.TimeGridKey;
import org.vadere.state.attributes.processor.AttributesPathogenConcentrationProcessor;
import org.vadere.state.scenario.AerosolCloud;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.Collection;
import java.util.stream.Collectors;

@DataProcessorClass(label = "PathogenConcentrationProcessor")
public class PathogenConcentrationProcessor extends DataProcessor<TimeGridKey, Double> {

    private double nextProcessingTime = 0;

    public PathogenConcentrationProcessor() {
        super("gridConcentration");
        setAttributes(new AttributesPathogenConcentrationProcessor());
    }

    @Override
    protected void doUpdate(SimulationState state) {
        int step = state.getStep();
        AttributesPathogenConcentrationProcessor attr = (AttributesPathogenConcentrationProcessor)getAttributes();
        double gridResolution = attr.getGridResolution();
        double topoWidth = state.getTopography().getBounds().getWidth();
        double topoHeight = state.getTopography().getBounds().getHeight();
        double topoX = state.getTopography().getBounds().getX();
        double topoY = state.getTopography().getBounds().getY();

        if (state.getSimTimeInSec() > nextProcessingTime) {
            for (double yPos = topoY + gridResolution / 2; yPos <= topoY + topoHeight - gridResolution / 2; yPos = yPos + gridResolution) {
                for (double xPos = topoX + gridResolution / 2; xPos <= topoX + topoWidth - gridResolution / 2; xPos = xPos + gridResolution) {
                    double pathogenConcentration = 0;
                    VPoint node = new VPoint(xPos, yPos);
                    Collection<AerosolCloud> cloudsAtNode = state.getTopography().getAerosolClouds().stream().filter(c -> c.getShape().contains(node)).collect(Collectors.toSet());

                    for (AerosolCloud cloud : cloudsAtNode) {
                        pathogenConcentration = pathogenConcentration + cloud.getPathogenConcentration();
                    }
                    this.putValue(new TimeGridKey(step, node.x, node.y, gridResolution * gridResolution), pathogenConcentration);
                }
            }

            nextProcessingTime = nextProcessingTime + attr.getTimeResolution();
        }
    }

    @Override
    public void init(ProcessorManager manager) {
        super.init(manager);
    }
}
