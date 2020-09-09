package org.vadere.simulator.utils.scenariochecker.checks.dataProcessors;

import org.vadere.simulator.projects.Scenario;
import org.vadere.simulator.projects.dataprocessing.processor.DataProcessor;
import org.vadere.simulator.utils.scenariochecker.ScenarioCheckerMessage;
import org.vadere.simulator.utils.scenariochecker.ScenarioCheckerReason;
import org.vadere.simulator.utils.scenariochecker.checks.AbstractScenarioCheck;
import org.vadere.state.attributes.AttributesStrategyModel;

import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;

/**
 * @author Christina Mayr
 * Warnings if data processors used in strategy model are not defined in data output
 */

public class StrategyModelDataProcessorCheck extends AbstractScenarioCheck {

    @Override
    public PriorityQueue<ScenarioCheckerMessage> runScenarioCheckerTest(Scenario scenario) {
        PriorityQueue<ScenarioCheckerMessage> messages = new PriorityQueue<>();

        AttributesStrategyModel attr = scenario.getScenarioStore().getAttributesStrategyModel();


        if (attr.isUseStrategyModel()) {


            LinkedList<Integer> requiredDataProcessorIds = attr.getRequiredDataProcessorIds();
            List<DataProcessor<?, ?>> processors = scenario.getDataProcessingJsonManager().getDataProcessors();


            for (Integer i : requiredDataProcessorIds) {

                try {
                    DataProcessor<?, ?> p = processors.get(i - 1); // processor id = (index +1)
                } catch (Exception e) {
                    messages.add(msgBuilder.dataProcessorAttrError()
                            .reason(ScenarioCheckerReason.DATAPROCESSOR_MISSING,
                                    String.format(" [Strategy model requires data processor id: %d. Processor not defined.]", i)).build());
                }


            }


        }
        return messages;
    }
}
