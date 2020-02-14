package org.vadere.util.math;

import org.jetbrains.annotations.NotNull;
import org.vadere.util.geometry.shapes.IPoint;

public interface IDistanceFunctionCached extends IDistanceFunction {
	double apply(@NotNull final IPoint point, final Object caller);
}
