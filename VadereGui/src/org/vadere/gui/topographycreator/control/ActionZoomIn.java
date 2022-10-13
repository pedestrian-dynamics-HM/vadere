package org.vadere.gui.topographycreator.control;

import org.vadere.gui.components.control.IMode;
import org.vadere.gui.topographycreator.model.IDrawPanelModel;

import java.awt.event.ActionEvent;

/**
 * Action: Sets the selection mode to ZoomInMode, so the user can zoom in if he hit the mouse
 * button.
 * 
 * 
 */
public class ActionZoomIn extends TopographyAction {

	private static final long serialVersionUID = 6346468270486683058L;
	private final IMode mode;

	public ActionZoomIn(final String name, final String iconPath,final String shortDescription, final IDrawPanelModel panelModel) {
		super(name, iconPath, shortDescription, panelModel);
		mode = new ZoomInMode(panelModel);
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		getScenarioPanelModel().setMouseSelectionMode(mode);
		getScenarioPanelModel().notifyObservers();
	}

}
