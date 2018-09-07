package org.vadere.gui.topographycreator.view;

import org.vadere.state.scenario.ScenarioElement;

import java.awt.*;

@FunctionalInterface
public interface ScenarioElementRenderer {
	void render(ScenarioElement element, final Graphics2D graphics, Color color);
}
