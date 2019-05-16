package org.vadere.simulator.models.bhm;

import org.jetbrains.annotations.NotNull;
import org.vadere.util.geometry.shapes.VPoint;

public interface DirectionAddend {
	public VPoint getDirectionAddend(@NotNull final VPoint targetDirection);
}
