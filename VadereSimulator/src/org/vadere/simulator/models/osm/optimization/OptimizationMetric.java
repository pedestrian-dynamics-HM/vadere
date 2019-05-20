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

    public OptimizationMetric(final VPoint optimalPoint, double optimalFuncValue){

        this.optimalPoint = optimalPoint;
        this.optimalFuncValue = optimalFuncValue;

        this.foundPoint = null;  // Can only be set afterwards optimal point
        this.foundFuncValue = -1;
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

    public void setFoundPoint(VPoint foundPoint) {
        this.foundPoint = foundPoint;
    }

    public void setFoundFuncValue(double foundFuncValue) {
        this.foundFuncValue = foundFuncValue;
    }
}
