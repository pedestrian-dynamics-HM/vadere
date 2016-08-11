package org.vadere.gui.topographycreator.control;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import org.vadere.gui.topographycreator.model.IDrawPanelModel;
import org.vadere.state.scenario.ScenarioElement;
import org.vadere.state.types.ScenarioElementType;

/**
 * Part of the command pattern to re- and undo setRemoveScenarioElement.
 * 
 *
 */
public class EditDeleteShape extends AbstractUndoableEdit {

	private static final long serialVersionUID = 5176192525116057658L;
	private ScenarioElementType type;
	private final IDrawPanelModel panelModel;
	private ScenarioElement element;

	public EditDeleteShape(final IDrawPanelModel panelModel, final ScenarioElement element) {
		this.panelModel = panelModel;
		this.type = element.getType();
		this.element = element;
	}

	@Override
	public void undo() throws CannotUndoException {
		panelModel.switchType(type);
		panelModel.addShape(element);
		panelModel.notifyObservers();
	}

	@Override
	public void redo() throws CannotRedoException {
		element = panelModel.deleteLastShape(type);
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
