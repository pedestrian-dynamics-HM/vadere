package org.vadere.simulator.utils.scenariochecker.checks.dataProcessors;

import org.vadere.simulator.projects.Scenario;
import org.vadere.simulator.projects.dataprocessing.processor.DataProcessor;
import org.vadere.simulator.projects.dataprocessing.processor.DataProcessorFactory;
import org.vadere.simulator.utils.scenariochecker.ScenarioCheckerMessage;
import org.vadere.simulator.utils.scenariochecker.ScenarioCheckerReason;
import org.vadere.simulator.utils.scenariochecker.checks.AbstractScenarioCheck;
import org.vadere.state.scenario.MeasurementArea;
import org.vadere.util.factory.processors.ProcessorFlag;

import java.util.List;
import java.util.PriorityQueue;

public class DataProcessorsLinkedToMeasurementArea extends AbstractScenarioCheck {

    @Override
    public PriorityQueue<ScenarioCheckerMessage> runScenarioCheckerTest(Scenario scenario) {
        PriorityQueue<ScenarioCheckerMessage> ret = new PriorityQueue<>();
        List<DataProcessor<?, ?>> processors
                = scenario.getDataProcessingJsonManager().getDataProcessors();

        List<MeasurementArea> measurementAreas =  scenario.getTopography().getMeasurementAreas();

        DataProcessorFactory factory = DataProcessorFactory.instance();
        for (DataProcessor<?, ?> p : processors) {
            if (factory.containsFlag(p.getClass(), ProcessorFlag.needMeasurementArea)){
                    if(!p.sanityCheck(measurementAreas)){
                        ret.add(msgBuilder.dataProcessorAttrError()
                                .reason(ScenarioCheckerReason.PROCESSOR_MEASUREMENT_AREA,
                                        String.format(" [Processor id: %d]",p.getId()))
                                .build()
                        );
                    }
            }
        }
        return ret;
    }
}
