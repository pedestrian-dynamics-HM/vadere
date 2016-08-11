package org.vadere.gui.topographycreator.control;

import java.awt.event.ActionEvent;

import javax.swing.undo.UndoableEdit;
import javax.swing.undo.UndoableEditSupport;

import org.vadere.gui.topographycreator.model.IDrawPanelModel;
import org.vadere.gui.topographycreator.model.TopographyElementFactory;
import org.vadere.state.scenario.ScenarioElement;
import org.vadere.state.types.ScenarioElementType;

/**
 * Adds a new ScenarioElment to the Model and set the json content of the panelModel.
 * This is a helper class to avoid duplicated code in the DrawXXXMode-Classes!
 * 
 *
 */
class ActionAddElement extends TopographyAction {

	private static final long serialVersionUID = -6926634829300780308L;
	private final UndoableEditSupport undoSupport;

	ActionAddElement(final String name, IDrawPanelModel panelModel, final UndoableEditSupport undoSupport) {
		super(name, panelModel);
		this.undoSupport = undoSupport;
	}

	@Override
	public void actionPerformed(ActionEvent e) {

		ScenarioElementType type = getScenarioPanelModel().getCurrentType();
		UndoableEdit edit = new EditDrawShape(getScenarioPanelModel(), type);
		undoSupport.postEdit(edit);

		IDrawPanelModel model = getScenarioPanelModel();

		model.getCurrentType();
		model.hideSelection();
		ScenarioElement element = TopographyElementFactory.getInstance().createScenarioShape(model.getCurrentType(),
				model.getSelectionShape());
		model.addShape(element);
		model.setSelectedElement(element);
	}
}
