package org.vadere.state.scenario;

import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.math.IDistanceFunction;

public interface ObstacleDistanceFunction extends IDistanceFunction {

    double getDistance(IPoint point);

    @Override
    default Double apply(IPoint point) {
        return getDistance(point);
    }
}
