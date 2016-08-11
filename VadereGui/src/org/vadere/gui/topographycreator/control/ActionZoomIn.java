package org.vadere.gui.topographycreator.control;

import java.awt.event.ActionEvent;

import javax.swing.ImageIcon;

import org.vadere.gui.components.control.IMode;
import org.vadere.gui.topographycreator.model.IDrawPanelModel;

/**
 * Action: Sets the selection mode to ZoomInMode, so the user can zoom in if he hit the mouse
 * button.
 * 
 * 
 */
public class ActionZoomIn extends TopographyAction {

	private static final long serialVersionUID = 6346468270486683058L;
	private final IMode mode;

	public ActionZoomIn(final String name, final ImageIcon icon, final IDrawPanelModel panelModel) {
		super(name, icon, panelModel);
		mode = new ZoomInMode(panelModel);
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		getScenarioPanelModel().setMouseSelectionMode(mode);
		getScenarioPanelModel().notifyObservers();
	}

}
