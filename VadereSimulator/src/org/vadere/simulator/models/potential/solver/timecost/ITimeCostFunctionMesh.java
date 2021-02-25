package org.vadere.simulator.models.potential.solver.timecost;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.inter.IVertex;
import org.vadere.util.geometry.shapes.IPoint;

/**
 * @author Benedikt Zoennchen
 * @param <V>
 */
public interface ITimeCostFunctionMesh<V extends IVertex> extends ITimeCostFunction {

	double costAt(V p);

	default double costAt(V p, Object caller) {
		return costAt(p);
	}

	static <V extends IVertex> ITimeCostFunctionMesh convert(@NotNull final ITimeCostFunction timeCostFunction) {
		return new ITimeCostFunctionMesh<V>() {
			@Override
			public double costAt(V p) {
				return timeCostFunction.costAt(p);
			}

			@Override
			public double costAt(V p, Object obj) {
				return timeCostFunction.costAt(p, obj);
			}

			@Override
			public double costAt(IPoint p) {
				return timeCostFunction.costAt(p);
			}

			public double costAt(IPoint p, Object obj) {
				return timeCostFunction.costAt(p, obj);
			}

		};
	}
}
