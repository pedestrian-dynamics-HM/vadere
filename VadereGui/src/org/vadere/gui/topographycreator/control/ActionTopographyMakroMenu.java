package org.vadere.gui.topographycreator.control;

import java.awt.event.ActionEvent;
import org.vadere.gui.topographycreator.model.IDrawPanelModel;

public class ActionTopographyMakroMenu extends TopographyAction {

  public ActionTopographyMakroMenu(
      String name, String iconPath, String shortDescription, IDrawPanelModel<?> panelModel) {
    super(name, iconPath, shortDescription, panelModel);
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    getScenarioPanelModel().getTopography().generateUniqueIdIfNotSet();
    getScenarioPanelModel().notifyObservers();
  }
}
