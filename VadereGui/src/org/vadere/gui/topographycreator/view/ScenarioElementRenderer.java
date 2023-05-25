package org.vadere.gui.topographycreator.view;

import java.awt.*;
import org.vadere.state.scenario.ScenarioElement;

@FunctionalInterface
public interface ScenarioElementRenderer {
  void render(ScenarioElement element, final Graphics2D graphics, Color color);
}
