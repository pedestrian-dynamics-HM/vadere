package org.vadere.gui.topographycreator.control;

import org.vadere.gui.components.control.IMode;
import org.vadere.gui.topographycreator.model.IDrawPanelModel;

import javax.swing.undo.UndoableEditSupport;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * Action: Selects the SelectShapeMode, so after this action the user can select ScenarioElements.
 * 
 *
 */
public class ActionSelectSelectShape extends TopographyAction {

	private static final long serialVersionUID = 7909552006335330920L;
	private final IMode mode;

/*
	public ActionSelectSelectShape(final String name, String iconPath, String shortDescription, final IDrawPanelModel panelModel,
                                   final UndoableEditSupport undoSupport) {
		this(name, iconPath,shortDescription,panelModel,  undoSupport);
	}
*/
	public ActionSelectSelectShape(final String name, String iconPath,String shortDescription, final IDrawPanelModel panelModel,
			final UndoableEditSupport undoSupport) {
		super(name,iconPath, shortDescription,panelModel);
		mode = new SelectElementMode(panelModel, undoSupport);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		getScenarioPanelModel().setMouseSelectionMode(mode);
		getScenarioPanelModel().setCursorColor(Color.MAGENTA);
		getScenarioPanelModel().notifyObservers();
	}
}
