package org.vadere.gui.topographycreator.control;

import org.vadere.gui.components.control.IMode;
import org.vadere.gui.topographycreator.model.IDrawPanelModel;

import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * Action: Switch to selection mode to SelectViewportMode. In this mode
 * the user can zoom in by selecting a area of the topography.
 * Unused!
 * 
 *
 */
public class ActionMinimizeSize extends TopographyAction {

	private static final long serialVersionUID = 6347273710084940359L;
	private final IMode mode;

	public ActionMinimizeSize(String name, String iconPath, IDrawPanelModel panelModel) {
		super(name, iconPath, panelModel);
		mode = new SelectViewportMode(panelModel);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		getScenarioPanelModel().setMouseSelectionMode(mode);
		getScenarioPanelModel().setCursorColor(Color.GRAY);
		getScenarioPanelModel().notifyObservers();
	}
}
