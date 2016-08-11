package org.vadere.gui.topographycreator.control;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import org.vadere.gui.topographycreator.model.IDrawPanelModel;
import org.vadere.state.scenario.ScenarioElement;
import org.vadere.state.types.ScenarioElementType;

/**
 * Part of the command pattern to re- and undo addScenarioElement.
 * 
 * 
 */
public class EditDrawShape extends AbstractUndoableEdit {

	private static final long serialVersionUID = -6088637662979988634L;
	private final ScenarioElementType type;
	private final IDrawPanelModel panelModel;
	private ScenarioElement topographyElement;

	public EditDrawShape(final IDrawPanelModel panelModel, final ScenarioElementType type) {
		this.type = type;
		this.panelModel = panelModel;
	}

	@Override
	public void undo() throws CannotUndoException {
		topographyElement = panelModel.deleteLastShape(type);
		panelModel.notifyObservers();
	}

	@Override
	public void redo() throws CannotRedoException {
		panelModel.switchType(type);
		panelModel.addShape(topographyElement);
		panelModel.notifyObservers();
	}

	@Override
	public boolean canUndo() {
		return true;
	}

	@Override
	public boolean canRedo() {
		return true;
	}

	@Override
	public String getPresentationName() {
		return "draw shape";
	}

}
