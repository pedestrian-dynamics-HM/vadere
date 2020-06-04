package org.vadere.state.scenario;

import org.jetbrains.annotations.NotNull;
import org.vadere.util.geometry.shapes.VPoint;

public interface IMoveDynamicElementListener {
	void moveElement(@NotNull final Pedestrian element, final VPoint oldPosition);
}
