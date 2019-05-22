package org.vadere.state.attributes.processor;

import org.jetbrains.annotations.NotNull;

/**
 *
 */
public class AttributesTestOptimizationMetricProcessor extends AttributesTestProcessor {

    private int optimizationMetricProcessorId;

    public int getOptimizationMetricNelderMeadProcessor() {
        checkSealed();
        return this.optimizationMetricProcessorId;
    }

    public void setEvacuationTimeProcessorId(final int optimizationMetricProcessorId) {
        checkSealed();
        this.optimizationMetricProcessorId = optimizationMetricProcessorId;
    }
}
