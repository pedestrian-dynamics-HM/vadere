package org.vadere.gui.topographycreator.control;

import java.awt.event.ActionEvent;

import javax.swing.undo.UndoableEditSupport;

import org.vadere.gui.topographycreator.model.IDrawPanelModel;
import org.vadere.gui.topographycreator.view.SetScenarioScaleDialog;

/**
 * Action: Opens the SetScenarioScaleDialog. In this dialog helps the user to scale the whole
 * topography.
 * 
 *
 */
public class ActionOpenSetTopographyScale extends TopographyAction {

	private static final long serialVersionUID = -516965745171911526L;
	private final UndoableEditSupport undoableEditSupport;

	public ActionOpenSetTopographyScale(final String name, final IDrawPanelModel panelModel,
			final UndoableEditSupport undoableEditSupport) {
		super(name, panelModel);
		this.undoableEditSupport = undoableEditSupport;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		new SetScenarioScaleDialog(getScenarioPanelModel(), undoableEditSupport).setVisible(true);
	}
}
