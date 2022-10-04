package org.vadere.gui.topographycreator.control;

import org.vadere.gui.components.control.IMode;
import org.vadere.gui.topographycreator.model.IDrawPanelModel;

import javax.swing.undo.UndoableEditSupport;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * Action: Cuts the Topography so the TopographyBound will be changed.
 * 
 * 
 */
public class ActionSelectCut extends TopographyAction {

	private static final long serialVersionUID = 2258668034342242491L;
	private final IMode mode;

	public ActionSelectCut(String name, String iconPath, String shortDescription, IDrawPanelModel panelModel,
						   final UndoableEditSupport undoSupport) {
		super(name, iconPath, shortDescription, panelModel);
		mode = new CutScenarioMode(panelModel, undoSupport);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		getScenarioPanelModel().setMouseSelectionMode(mode);
		getScenarioPanelModel().setCursorColor(Color.RED);
		getScenarioPanelModel().notifyObservers();
	}
}
