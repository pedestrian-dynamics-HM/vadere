package org.vadere.state.scenario.renderer;

import org.vadere.state.scenario.Agent;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VShape;

public class PedestrianTriangleRenderer extends AgentDefaultRenderer {

	@Override
	public VShape drawShape(Agent a) {
		VPoint pos = a.getPosition();
		double r = a.getRadius();
		return FormHelper.getTriangle(a.getPosition(), a.getRadius());
	}



}
