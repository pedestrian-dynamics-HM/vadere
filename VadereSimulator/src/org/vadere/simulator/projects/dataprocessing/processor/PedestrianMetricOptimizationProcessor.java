package org.vadere.simulator.projects.dataprocessing.processor;


import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.models.osm.PedestrianOSM;
import org.vadere.simulator.models.osm.optimization.OptimizationMetric;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.EventtimePedestrianIdKey;
import org.vadere.state.scenario.Pedestrian;

import java.util.ArrayList;
import java.util.Collection;

/**
 *
 * This processor requires the true and found solution (both point and function value).
 * The solution are stored in OptimizationMetric and have to be set in StepCircleOptimizer.
 *
 * Note: currently only the Nelder Mead and PedestrianOSM support this feature, but it should be easy to generalize
 * this to other optimizer or more general pedestrian.
 */

@DataProcessorClass()
public class PedestrianMetricOptimizationProcessor extends DataProcessor<EventtimePedestrianIdKey, OptimizationMetric>{

    private boolean receivedValues;

    public PedestrianMetricOptimizationProcessor() {
        super("optX", "optY", "optFunc", "foundX", "foundY", "foundFunc");
        receivedValues = false;
    }

    @Override
    protected void doUpdate(final SimulationState state) {

        Collection<Pedestrian> pedestrians = state.getTopography().getPedestrianDynamicElements().getElements();

        for (Pedestrian pedestrian : pedestrians) {
            ArrayList<OptimizationMetric> pedestrianMetrics = ((PedestrianOSM) pedestrian).getOptimizationMetricElements();

            if(pedestrianMetrics == null){
                throw new RuntimeException("Pedestrian OptimizationMetric is null. This means that there the " +
                        "configuration is not to measure the quality is not active.");
            }else if (!pedestrianMetrics.isEmpty()) {
                receivedValues = true;
                for (OptimizationMetric singleMetric : pedestrianMetrics) {
                    putValue(new EventtimePedestrianIdKey(singleMetric.getSimTime(), singleMetric.getPedId()), singleMetric);
                }
            } //else (if empty) do nothing, no event occured for this pedestrians in the last simulation step.

        }
    }

    @Override
    public void postLoop(final SimulationState state){
        if(!this.receivedValues){
            throw new RuntimeException("PedestrianMetricOptimizationProcessor received no values. This can be because" +
                    "Vadere is not configured to compare to the brute force solution or the selected optimizer does " +
                    "not support it. (See file StepCircleOptimizer)");
        }
    }

    @Override
    public void init(final ProcessorManager manager) {
        super.init(manager);
    }

    @Override
    public String[] toStrings(EventtimePedestrianIdKey key) {
        OptimizationMetric metric = this.getValue(key);
        return metric.getValueString();
    }
}
