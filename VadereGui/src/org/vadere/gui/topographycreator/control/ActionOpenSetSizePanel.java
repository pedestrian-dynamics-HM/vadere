package org.vadere.gui.topographycreator.control;

import java.awt.event.ActionEvent;

import org.vadere.gui.topographycreator.model.IDrawPanelModel;
import org.vadere.gui.topographycreator.view.SetScenarioBoundDialog;

/**
 * Action: Opens the SetScenarioBoundDialog.
 * 
 *
 */
public class ActionOpenSetSizePanel extends TopographyAction {

	private static final long serialVersionUID = -778991941304111935L;

	public ActionOpenSetSizePanel(String name, IDrawPanelModel panelModel) {
		super(name, panelModel);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		new SetScenarioBoundDialog(getScenarioPanelModel()).setVisible(true);
	}

}
