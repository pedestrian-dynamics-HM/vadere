package org.vadere.gui.topographycreator.control;

import java.awt.Color;
import java.awt.event.ActionEvent;

import javax.swing.ImageIcon;
import javax.swing.undo.UndoableEditSupport;

import org.vadere.gui.components.control.IMode;
import org.vadere.gui.topographycreator.model.IDrawPanelModel;

/**
 * Action: Cuts the Topography so the TopographyBound will be changed.
 * 
 * 
 */
public class ActionSelectCut extends TopographyAction {

	private static final long serialVersionUID = 2258668034342242491L;
	private final IMode mode;

	public ActionSelectCut(String name, ImageIcon icon, IDrawPanelModel panelModel,
			final UndoableEditSupport undoSupport) {
		super(name, icon, panelModel);
		mode = new CutScenarioMode(panelModel, undoSupport);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		getScenarioPanelModel().setMouseSelectionMode(mode);
		getScenarioPanelModel().setCursorColor(Color.RED);
		getScenarioPanelModel().notifyObservers();
	}
}
