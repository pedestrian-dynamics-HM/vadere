package org.vadere.simulator.utils.scenariochecker.checks.dataProcessors;

import org.vadere.simulator.projects.Scenario;
import org.vadere.simulator.projects.dataprocessing.flags.UsesMeasurementArea;
import org.vadere.simulator.projects.dataprocessing.processor.DataProcessor;
import org.vadere.simulator.projects.dataprocessing.processor.DataProcessorFactory;
import org.vadere.simulator.utils.scenariochecker.ScenarioCheckerMessage;
import org.vadere.simulator.utils.scenariochecker.ScenarioCheckerReason;
import org.vadere.simulator.utils.scenariochecker.checks.AbstractScenarioCheck;
import org.vadere.state.scenario.MeasurementArea;
import org.vadere.util.factory.processors.Flag;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;
import java.util.stream.Collectors;

public class DataProcessorsLinkedToMeasurementArea extends AbstractScenarioCheck {

    @Override
    public PriorityQueue<ScenarioCheckerMessage> runScenarioCheckerTest(Scenario scenario) {
        PriorityQueue<ScenarioCheckerMessage> ret = new PriorityQueue<>();
        List<DataProcessor<?, ?>> processors
                = scenario.getDataProcessingJsonManager().getDataProcessors();

        List<Integer> measurementAreas =  scenario.getTopography().getMeasurementAreas()
                .stream()
                .map(MeasurementArea::getId)
                .collect(Collectors.toList());



        processors.stream().filter(UsesMeasurementArea.class::isInstance).forEach( p -> {
            UsesMeasurementArea pArea = (UsesMeasurementArea)p;

            for (int areaId : pArea.getReferencedMeasurementAreaId()) {
                if (!measurementAreas.contains(areaId)) {
                    ret.add(msgBuilder.dataProcessorAttrError()
                            .reason(ScenarioCheckerReason.PROCESSOR_MEASUREMENT_AREA,
                                    String.format(" [Processor id: %d]", p.getId()))
                            .build()
                    );
                }
            }
        });


        return ret;
    }
}
