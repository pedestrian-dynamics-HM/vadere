package org.vadere.simulator.models.potential.fields;

import java.util.List;
import java.util.Random;

import org.vadere.simulator.control.ActiveCallback;
import org.vadere.simulator.models.Model;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.Vector2D;
import org.vadere.util.geometry.shapes.VPoint;

public interface PotentialFieldTarget extends ActiveCallback, Model {
	boolean needsUpdate();

	double getTargetPotential(List<Integer> targetIds,
			final VPoint pos, final Agent ped);

	Vector2D getTargetPotentialGradient(final List<Integer> targetIds,
			final VPoint pos, final Agent ped);
}
