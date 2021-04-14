package org.vadere.meshing.mesh.triangulation.edgeLengthFunctions;

import org.jetbrains.annotations.NotNull;
import org.vadere.util.geometry.shapes.IPoint;

public class EdgeLengthFunctionCombine implements IEdgeLengthFunction {

	private final IEdgeLengthFunction[] edgeLengthFunctions;

	public EdgeLengthFunctionCombine(@NotNull final IEdgeLengthFunction... edgeLengthFunctions) {
		this.edgeLengthFunctions = edgeLengthFunctions;
	}

	@Override
	public Double apply(IPoint point) {
		double min = Double.POSITIVE_INFINITY;
		for(IEdgeLengthFunction edgeLengthFunction : edgeLengthFunctions) {
			min = Math.min(min, edgeLengthFunction.apply(point));
		}
		return min;
	}
}
