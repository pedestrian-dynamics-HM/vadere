package org.vadere.gui.topographycreator.control;

import java.awt.event.ActionEvent;

import javax.swing.ImageIcon;
import javax.swing.undo.UndoableEdit;
import javax.swing.undo.UndoableEditSupport;

import org.vadere.gui.components.utils.Resources;
import org.vadere.gui.topographycreator.model.IDrawPanelModel;
import org.vadere.state.scenario.Topography;

/**
 * Action: Resets the Topography. This means all ScenarioElements will be removed.
 * 
 * 
 */
public class ActionResetTopography extends TopographyAction {
	private static Resources resources = Resources.getInstance("topographycreator");
	private static final long serialVersionUID = -5557013510457451231L;

	private final UndoableEditSupport undoSupport;

	public ActionResetTopography(String name, ImageIcon icon, IDrawPanelModel panelModel,
			UndoableEditSupport undoSupport) {
		super(name, icon, panelModel);
		this.undoSupport = undoSupport;
	}

	public ActionResetTopography(String name, IDrawPanelModel panelModel, UndoableEditSupport undoSupport) {
		super(name, panelModel);
		this.undoSupport = undoSupport;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Topography beforeTopography = getScenarioPanelModel().build();
		getScenarioPanelModel().resetScenario();
		UndoableEdit edit = new EditResetScenario(getScenarioPanelModel(), beforeTopography);
		undoSupport.postEdit(edit);
		resources.removeProperty("last_save_point");
		getScenarioPanelModel().notifyObservers();
	}
}
