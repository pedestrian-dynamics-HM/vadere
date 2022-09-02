package org.vadere.util.geometry.shapes;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.jetbrains.annotations.NotNull;
@JsonSerialize(as= VShape.class)
public class VDisc extends VCircle {

	public VDisc(final VPoint center, double radius) {
		super(center, radius);
	}

	public VDisc(double x, double y, double radius) {
		super(x, y, radius);
	}

	@Override
	public double distance(@NotNull final IPoint pos) {
		return getCenter().distance(pos) - getRadius();
	}

}
