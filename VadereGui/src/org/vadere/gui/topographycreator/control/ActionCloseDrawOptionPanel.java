package org.vadere.gui.topographycreator.control;

import org.vadere.gui.components.view.ActionDialog;
import org.vadere.gui.topographycreator.model.IDrawPanelModel;

import java.awt.event.ActionEvent;

/**
 * Action: Hide the PaintMethodDialog Window.
 * 
 */
public class ActionCloseDrawOptionPanel extends TopographyAction {

	private static final long serialVersionUID = -8732850975235483214L;

	private final TopographyAction action;

	public ActionCloseDrawOptionPanel(final String name, final String iconPath, final IDrawPanelModel panelModel,
			final TopographyAction action) {
		super(name, iconPath, panelModel);
		this.action = action;
	}

	public ActionCloseDrawOptionPanel(final String name, final IDrawPanelModel panelModel,
			final TopographyAction action) {
		super(name, panelModel);
		this.action = action;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		ActionDialog.hideDialog();
		action.actionPerformed(e);
	}
}
