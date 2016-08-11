package org.vadere.gui.topographycreator.control;

import java.awt.event.ActionEvent;

import javax.swing.ImageIcon;

import org.vadere.gui.components.view.ActionDialog;
import org.vadere.gui.topographycreator.model.IDrawPanelModel;

/**
 * Action: Hide the PaintMethodDialog Window.
 * 
 */
public class ActionCloseDrawOptionPanel extends TopographyAction {

	private static final long serialVersionUID = -8732850975235483214L;

	private final TopographyAction action;

	public ActionCloseDrawOptionPanel(final String name, final ImageIcon icon, final IDrawPanelModel panelModel,
			final TopographyAction action) {
		super(name, icon, panelModel);
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
