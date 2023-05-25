package org.vadere.gui.components.view;

import java.util.Observable;
import java.util.Observer;
import javax.swing.*;
import org.vadere.gui.components.model.IDefaultModel;

public class ScenarioScrollPane extends JScrollPane implements Observer {

  public ScenarioScrollPane(final JComponent component, final IDefaultModel defaultModel) {
    super(component);
  }

  @Override
  public void update(Observable o, Object arg) {}
}
