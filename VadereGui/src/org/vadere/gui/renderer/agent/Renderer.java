package org.vadere.gui.renderer.agent;

import java.awt.*;
import org.vadere.state.scenario.ScenarioElement;

public interface Renderer {

  void render(final ScenarioElement element, final Color color, final Graphics2D g);
}
