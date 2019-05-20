package org.vadere.simulator.models.osm.optimization;

import org.vadere.util.geometry.shapes.VPoint;

/**
 * @author Daniel Lehmberg
 * //TODO
 */


public class OptimizationMetric {

    private VPoint optimalPoint;
    private double optimalFuncValue;

    private VPoint foundPoint;
    private double foundFuncValue;

    public OptimizationMetric(){

    }

    public OptimizationMetric(final VPoint optimalPoint, double optimalFuncValue, VPoint foundPoint,
                              double foundFuncValue){
        this.optimalPoint = optimalPoint;
        this.optimalFuncValue = optimalFuncValue;

        this.foundPoint = foundPoint;
        this.foundFuncValue = foundFuncValue;
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
}
