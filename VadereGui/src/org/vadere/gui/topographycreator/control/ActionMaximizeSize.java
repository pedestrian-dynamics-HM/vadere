package org.vadere.gui.topographycreator.control;

import java.awt.event.ActionEvent;

import javax.swing.ImageIcon;

import org.vadere.gui.topographycreator.model.IDrawPanelModel;

/**
 * Action: Reset the size of the scenario to the default value.
 * 
 * 
 */
public class ActionMaximizeSize extends TopographyAction {

	private static final long serialVersionUID = 3142209351101181343L;


	public ActionMaximizeSize(final String name, final ImageIcon icon, final IDrawPanelModel panelModel) {
		super(name, icon, panelModel);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		getScenarioPanelModel().resetTopographySize();
		getScenarioPanelModel().notifyObservers();
	}
}
