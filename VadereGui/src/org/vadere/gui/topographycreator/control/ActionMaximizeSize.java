package org.vadere.gui.topographycreator.control;

import org.vadere.gui.topographycreator.model.IDrawPanelModel;

import java.awt.event.ActionEvent;

/**
 * Action: Reset the size of the scenario to the default value.
 * 
 * 
 */
public class ActionMaximizeSize extends TopographyAction {

	private static final long serialVersionUID = 3142209351101181343L;


	public ActionMaximizeSize(final String name, final String iconPath,String shortDescription, final IDrawPanelModel panelModel) {
		super(name, iconPath, shortDescription, panelModel);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		getScenarioPanelModel().resetTopographySize();
		getScenarioPanelModel().notifyObservers();
	}
}
