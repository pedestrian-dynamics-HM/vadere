package org.vadere.gui.renderer.agent;

import org.vadere.state.scenario.Agent;

import java.awt.*;

public interface Renderer {

	void render(final Agent a, final Graphics2D g);

}
