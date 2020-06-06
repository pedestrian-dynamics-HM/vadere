package org.vadere.simulator.models.potential.solver.timecost;

import org.vadere.meshing.mesh.inter.IVertex;

/**
 * @author Benedikt Zoennchen
 * @param <V>
 */
public interface ITimeCostFunctionMesh<V extends IVertex> extends ITimeCostFunction {

	double costAt(V p);

	default double costAt(V p, Object caller) {
		return costAt(p);
	}
}
