package org.vadere.gui.topographycreator.control;

import java.awt.event.ActionEvent;

import javax.swing.ImageIcon;

import org.vadere.gui.components.control.IMode;
import org.vadere.gui.topographycreator.model.IDrawPanelModel;

/**
 * Action: Switches the SelectionMode to a specific mode.
 * 
 *
 */
public class ActionSwitchSelectionMode extends TopographyAction {

	private static final long serialVersionUID = 1619505203538546037L;
	private final IMode mode;
	private final TopographyAction action;

	public ActionSwitchSelectionMode(final String name, final ImageIcon icon, final IDrawPanelModel panelModel,
			final IMode mode, final TopographyAction action) {
		super(name, icon, panelModel);
		this.mode = mode;
		this.action = action;
	}

	public ActionSwitchSelectionMode(final String name, final IDrawPanelModel panelModel, final IMode mode,
			final TopographyAction action) {
		super(name, panelModel);
		this.mode = mode;
		this.action = action;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		getScenarioPanelModel().setMouseSelectionMode(mode);
		action.actionPerformed(e);
	}
}
