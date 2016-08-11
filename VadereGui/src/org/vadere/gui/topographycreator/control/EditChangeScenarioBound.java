package org.vadere.gui.topographycreator.control;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import org.vadere.gui.topographycreator.model.IDrawPanelModel;
import org.vadere.util.geometry.shapes.VRectangle;

/**
 * Part of the command pattern to re- and undo setTopographyBound.
 * 
 * 
 */
public class EditChangeScenarioBound extends AbstractUndoableEdit {
	private static final long serialVersionUID = -8794215770821216524L;
	private final IDrawPanelModel panelModel;
	private final VRectangle oldScenarioBound;
	private final VRectangle newScenarioBound;

	public EditChangeScenarioBound(final IDrawPanelModel panelModel, final VRectangle oldScenarioBound,
			final VRectangle newScenarioBound) {
		this.panelModel = panelModel;
		this.oldScenarioBound = oldScenarioBound;
		this.newScenarioBound = newScenarioBound;
	}

	@Override
	public void undo() throws CannotUndoException {
		panelModel.setTopographyBound(oldScenarioBound);
		panelModel.notifyObservers();
	}

	@Override
	public void redo() throws CannotRedoException {
		panelModel.setTopographyBound(newScenarioBound);
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
		return "reset size";
	}

}
