package org.vadere.state.attributes.processor;

import org.jetbrains.annotations.NotNull;

/**
 *
 */
public class AttributesTestOptimizationMetricProcessor extends AttributesTestProcessor {

    private int optimizationMetricProcessorId;
    private int testEvacuationProcessorId;

    /** Following have to be fulfilled (i.e. the mean has to be below this. If the mean increases the value, the
     * processor will fail. */
    private double maxMeanPointDistance;
    private double maxMeanDifferenceFuncValue;

    /** These are just for information and reference -- there is a print out the console that compares these
     *  the statistics.
     */

    private double infoMinPointDistanceL2;
    private double infoMaxPointDistanceL2;

    private double infoMinFuncDifference;
    private double infoMaxFuncDifference;

    private double infoStddevPointDistance;
    private double infoStddevDifferenceFuncValue;

    public int getOptimizationMetricProcessorId() {
        return optimizationMetricProcessorId;
    }

    public int getTestEvacuationProcessorId() {
        return testEvacuationProcessorId;
    }

    public double getMaxMeanPointDistance() {
        return maxMeanPointDistance;
    }

    public double getMaxMeanDifferenceFuncValue() {
        return maxMeanDifferenceFuncValue;
    }

    public double getInfoMinPointDistanceL2() {
        return infoMinPointDistanceL2;
    }

    public double getInfoMaxPointDistanceL2() {
        return infoMaxPointDistanceL2;
    }

    public double getInfoStddevPointDistance() {
        return infoStddevPointDistance;
    }

    public double getInfoStddevDifferenceFuncValue() {
        return infoStddevDifferenceFuncValue;
    }

    public double getInfoMaxFuncDifference() {
        return infoMaxFuncDifference;
    }

    public double getInfoMinFuncDifference() {
        return infoMinFuncDifference;
    }
}
