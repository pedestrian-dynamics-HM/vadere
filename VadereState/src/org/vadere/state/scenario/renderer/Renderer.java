package org.vadere.state.scenario.renderer;

import org.vadere.state.scenario.Agent;

import java.awt.*;

public interface Renderer{

	void render(final Agent a, final Graphics2D g, Color c);
	void render(final Agent a, final Graphics2D g);


	default void setColor(final Graphics2D g, Color c){
		g.setColor(c);
	}
}
