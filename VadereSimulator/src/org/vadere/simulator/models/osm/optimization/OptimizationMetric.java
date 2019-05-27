package org.vadere.simulator.models.osm.optimization;

import org.vadere.simulator.projects.dataprocessing.processor.tests.TestOptimizationMetricNelderMeadProcessor;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.logging.Logger;

/**
 * Stores the values of the true solution (which can be computed analytically or by brute force) and the solution that
 * is found by a optimizer algorithm. It holds both the (tru/found) point and (true/found) function value.
 *
 * The difference between the two quantities allow to measure the quality of an optimization algorithm.
 */

public class OptimizationMetric {

    /* Meta data for the metric */
    private double simTime;
    private int pedId;

    /* Metric data from which differences can be taken */
    private VPoint optimalPoint;
    private double optimalFuncValue;

    private VPoint foundPoint;
    private double foundFuncValue;

    private final double tolerance;

    public OptimizationMetric(int pedId, double simTime, final VPoint optimalPoint, double optimalFuncValue,
                              final VPoint foundPoint, final double foundFuncValue){

        this.pedId = pedId;
        this.simTime = simTime;

        this.optimalPoint = optimalPoint;
        this.optimalFuncValue = optimalFuncValue;

        this.foundPoint = foundPoint;
        this.foundFuncValue = foundFuncValue;

        tolerance = 1E-2;

        if( optimalFuncValue - foundFuncValue > tolerance ){
            Logger.getLogger(TestOptimizationMetricNelderMeadProcessor.class).warn(
                    "Found optimal value is better than brute force. This can indicate that the " +
                            "brute force is not fine grained enough. BRUTE FORCE: " + optimalFuncValue +
                            " OPTIMIZER: " + foundFuncValue);
        }
    }

    public double getSimTime() {
        return simTime;
    }

    public int getPedId() {
        return pedId;
    }

    public VPoint getOptimalPoint() {
        return optimalPoint;
    }

    public double getOptimalFuncValue() {
        return optimalFuncValue;
    }

    public VPoint getFoundPoint() {
        return foundPoint;
    }

    public double getFoundFuncValue() {
        return foundFuncValue;
    }

    public String[] getValueString(){
        String[] valueLine = {""+optimalPoint.x, ""+optimalPoint.y, ""+optimalFuncValue,
                              ""+foundPoint.x, ""+foundPoint.y, ""+foundFuncValue};
        return valueLine;
    }
}
