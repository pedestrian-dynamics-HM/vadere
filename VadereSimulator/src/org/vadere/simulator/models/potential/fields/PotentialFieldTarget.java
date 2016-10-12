package org.vadere.simulator.models.potential.fields;

import org.vadere.simulator.control.ActiveCallback;
import org.vadere.simulator.models.Model;
import org.vadere.state.scenario.Agent;
import org.vadere.util.geometry.Vector2D;
import org.vadere.util.geometry.shapes.VPoint;

public interface PotentialFieldTarget extends ActiveCallback, Model {
	boolean needsUpdate();

	double getTargetPotential(final VPoint pos, final Agent ped);

	Vector2D getTargetPotentialGradient(final VPoint pos, final Agent ped);
}
