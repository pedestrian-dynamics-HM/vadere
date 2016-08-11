package org.vadere.gui.topographycreator.control;

import java.awt.event.ActionEvent;

import javax.swing.undo.UndoableEdit;
import javax.swing.undo.UndoableEditSupport;

import org.vadere.gui.topographycreator.model.IDrawPanelModel;

/**
 * Action: Scales the scenario, so every VShape of the ScenarioElement will be changed.
 * 
 * 
 */
public class ActionScaleTopography extends TopographyAction {

	private static final long serialVersionUID = -5755749448391266906L;
	private final UndoableEditSupport undoSupport;

	public ActionScaleTopography(final String name, final IDrawPanelModel panelModel,
			final UndoableEditSupport undoSupport) {
		super(name, panelModel);
		this.undoSupport = undoSupport;
	}

	@Override
	public void actionPerformed(ActionEvent e) {

		double scaleFactor = getScenarioPanelModel().getScalingFactor();
		getScenarioPanelModel().scaleTopography(scaleFactor);
		UndoableEdit edit = new EditScaleScenario(getScenarioPanelModel(), scaleFactor);
		undoSupport.postEdit(edit);
		getScenarioPanelModel().notifyObservers();
	}

}
