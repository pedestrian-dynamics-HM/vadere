package org.vadere.gui.projectview.control;

import java.awt.event.ActionEvent;
import javax.swing.*;
import org.vadere.gui.projectview.model.ProjectViewModel;

public class ActionRunAllScenarios extends AbstractAction {

  private final ProjectViewModel model;

  public ActionRunAllScenarios(final String name, final ProjectViewModel model) {
    super(name);
    this.model = model;
  }

  @Override
  public void actionPerformed(final ActionEvent e) {
    if (model.runScenarioIsOk(true)) model.getProject().runAllScenarios();
  }
}
