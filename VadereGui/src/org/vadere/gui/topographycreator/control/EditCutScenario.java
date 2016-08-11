package org.vadere.gui.topographycreator.control;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import org.vadere.gui.topographycreator.model.IDrawPanelModel;
import org.vadere.util.geometry.shapes.VRectangle;

/**
 * Part of the command pattern to re- and undo cutTopography.
 * 
 *
 */
public class EditCutScenario extends AbstractUndoableEdit {
	private static final long serialVersionUID = -15649157884176273L;
	private final VRectangle beforeScenarioBound;
	private VRectangle afterScenarioBound;
	private IDrawPanelModel model;

	public EditCutScenario(final IDrawPanelModel model, final VRectangle beforeScenarioBound) {
		this.beforeScenarioBound = beforeScenarioBound;
		this.model = model;
	}

	@Override
	public void undo() throws CannotUndoException {
		afterScenarioBound = new VRectangle(model.getTopographyBound());
		model.setTopographyBound(beforeScenarioBound);
		model.notifyObservers();
	}

	@Override
	public void redo() throws CannotRedoException {
		model.setTopographyBound(afterScenarioBound);
		model.notifyObservers();
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
		return "cut scenario";
	}
}
