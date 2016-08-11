package org.vadere.gui.topographycreator.control;

import java.awt.Color;
import java.awt.event.ActionEvent;

import javax.swing.ImageIcon;
import javax.swing.undo.UndoableEditSupport;

import org.vadere.gui.components.control.IMode;
import org.vadere.gui.topographycreator.model.IDrawPanelModel;

/**
 * Action: Selects the SelectShapeMode, so after this action the user can select ScenarioElements.
 * 
 *
 */
public class ActionSelectSelectShape extends TopographyAction {

	private static final long serialVersionUID = 7909552006335330920L;
	private final IMode mode;


	public ActionSelectSelectShape(final String name, ImageIcon icon, final IDrawPanelModel panelModel,
			final UndoableEditSupport undoSupport) {
		super(name, icon, panelModel);
		mode = new SelectElementMode(panelModel, undoSupport);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		getScenarioPanelModel().setMouseSelectionMode(mode);
		getScenarioPanelModel().setCursorColor(Color.MAGENTA);
		getScenarioPanelModel().notifyObservers();
	}
}
