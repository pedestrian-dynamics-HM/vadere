package org.vadere.gui.renderer.agent;

import org.vadere.state.scenario.ScenarioElement;

import java.awt.*;

public interface Renderer {

	void render(final ScenarioElement element, final Color color, final Graphics2D g);

}
