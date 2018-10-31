package org.vadere.gui.topographycreator.control;

import org.vadere.gui.topographycreator.model.IDrawPanelModel;

import java.awt.event.ActionEvent;

import javax.swing.*;

public class ActionTopographyMakroMenu extends TopographyAction {

	public ActionTopographyMakroMenu(String name, ImageIcon icon, IDrawPanelModel<?> panelModel) {
		super(name, icon, panelModel);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		getScenarioPanelModel().getTopography().generateUniqueIdIfNotSet();
		getScenarioPanelModel().notifyObservers();
	}
}
