package org.vadere.state.scenario.renderer;

import org.vadere.state.scenario.Agent;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VShape;

import java.awt.*;

public class AgentDefaultRenderer implements ShapeRenderer{

	public AgentDefaultRenderer(){
	}


	public void setColor(Agent a, Graphics2D g) {
		g.setColor(Color.GREEN);
	}

	@Override
	public VShape drawShape(Agent a) {
		return new VCircle(a.getPosition(), a.getAttributes().getRadius());
	}

	@Override
	public void setColor(Graphics2D g, Color c) {
		g.setColor(c);
	}

}
