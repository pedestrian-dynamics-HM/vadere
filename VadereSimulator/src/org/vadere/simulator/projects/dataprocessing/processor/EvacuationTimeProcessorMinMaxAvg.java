package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.NoDataKey;
import org.vadere.state.attributes.processor.AttributesEvacuationTimeProcessor;
import org.vadere.state.attributes.processor.AttributesProcessor;

import java.util.*;
import java.util.stream.DoubleStream;

/**
 * Works (inefficient) like {@link EvacuationTimeProcessor} but logs min, max and average evacuation time
 * instead of only logging the max evacuation time.
 *
 * <ul>
 *     <li>The evacuation time is the maximum travel time (from source to target) for all pedestrians.</li>
 *     <li>This processors uses the {@link PedestrianEvacuationTimeProcessor} internally to log evacuation time
 *  * for each pedestrian internally. Internally, {@link PedestrianEvacuationTimeProcessor} uses
 *  * {@link PedestrianStartTimeProcessor} to log the spawing time of each pedestrian.</li>
 * </ul>
 */
@DataProcessorClass()
public class EvacuationTimeProcessorMinMaxAvg extends NoDataKeyProcessor<String> {
    public static String ERROR_PEDESTRIANS_STILL_IN_SCENARIO = "ERROR_PEDESTRIANS_STILL_IN_SCENARIO";
    public static String ERROR_INVALID_EVACUATION_TIME_FOR_PEDESTRIANS = "ERROR_INVALID_EVACUATION_TIME_FOR_PEDESTRIANS";

    private PedestrianEvacuationTimeProcessor pedEvacTimeProc;
    private int numberOfAgentsInScenario = 0;

    public EvacuationTimeProcessorMinMaxAvg() {
        super("totalPedestrians evacuationMin evacuationMax evacuationAvg");
        setAttributes(new AttributesEvacuationTimeProcessor());
    }

    @Override
    protected void doUpdate(final SimulationState state) {
        this.pedEvacTimeProc.update(state);
        this.numberOfAgentsInScenario = state.getTopography().getPedestrianDynamicElements().getElements().size();
    }

    @Override
    public void postLoop(final SimulationState state) {
        this.pedEvacTimeProc.postLoop(state);

        if (numberOfAgentsInScenario != 0) {
            this.putValue(NoDataKey.key(), ERROR_PEDESTRIANS_STILL_IN_SCENARIO);
        } else if (pedEvacTimeProc.getValues().stream().anyMatch(element -> element.equals(Double.NaN))) {
            this.putValue(NoDataKey.key(), ERROR_INVALID_EVACUATION_TIME_FOR_PEDESTRIANS);
        } else {
            DoubleSummaryStatistics statistics = pedEvacTimeProc.getValues().stream().mapToDouble(x -> x).summaryStatistics();
            String statisticsString = String.format(Locale.US, "%d %f %f %f",
                    statistics.getCount(), statistics.getMin(), statistics.getMax(), statistics.getAverage());

            this.putValue(NoDataKey.key(), statisticsString);
        }
    }

    @Override
    public void init(final ProcessorManager manager) {
        super.init(manager);
        AttributesEvacuationTimeProcessor att = (AttributesEvacuationTimeProcessor) this.getAttributes();
        this.pedEvacTimeProc = (PedestrianEvacuationTimeProcessor) manager.getProcessor(att.getPedestrianEvacuationTimeProcessorId());
    }

    @Override
    public AttributesProcessor getAttributes() {
        if(super.getAttributes() == null) {
            setAttributes(new AttributesEvacuationTimeProcessor());
        }

        return super.getAttributes();
    }
}
