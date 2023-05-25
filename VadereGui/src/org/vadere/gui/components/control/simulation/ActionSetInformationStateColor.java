package org.vadere.gui.components.control.simulation;

import java.awt.*;
import javax.swing.*;
import org.vadere.gui.components.model.DefaultSimulationConfig;
import org.vadere.gui.components.model.SimulationModel;
import org.vadere.state.psychology.information.InformationState;

public class ActionSetInformationStateColor extends ActionSetColor {
  private final JComboBox<InformationState> comboBox;

  public ActionSetInformationStateColor(
      final String name,
      final SimulationModel<? extends DefaultSimulationConfig> model,
      final JPanel coloredPanel,
      final JComboBox<InformationState> comboBox) {
    super(name, model, coloredPanel);
    this.comboBox = comboBox;
  }

  @Override
  protected void saveColor(Color color) {
    InformationState informationState = comboBox.getItemAt(comboBox.getSelectedIndex());

    if (informationState != null) {
      model.config.setInformationStateColor(informationState, color);
    }
  }
}
