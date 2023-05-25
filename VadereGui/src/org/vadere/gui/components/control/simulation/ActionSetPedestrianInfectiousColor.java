package org.vadere.gui.components.control.simulation;

import java.awt.*;
import javax.swing.*;
import org.vadere.gui.components.model.DefaultSimulationConfig;
import org.vadere.gui.components.model.SimulationModel;

public class ActionSetPedestrianInfectiousColor extends ActionSetColor {
  public ActionSetPedestrianInfectiousColor(
      String name, SimulationModel<? extends DefaultSimulationConfig> model, JPanel coloredPanel) {
    super(name, model, coloredPanel);
  }

  @Override
  protected void saveColor(Color color) {
    model.config.setInfectiousColor(color);
  }
}
