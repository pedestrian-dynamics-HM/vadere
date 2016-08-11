package org.vadere.gui.topographycreator.control;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import org.vadere.gui.topographycreator.model.IDrawPanelModel;

/**
 * Part of the command pattern to re- and undo scaleTopography.
 * 
 */
public class EditScaleScenario extends AbstractUndoableEdit {

	private static final long serialVersionUID = 4809196364650751248L;
	private final double scale;
	private final IDrawPanelModel model;

	public EditScaleScenario(final IDrawPanelModel model, final double scale) {
		this.model = model;
		this.scale = scale;
	}

	@Override
	public void undo() throws CannotUndoException {
		model.scaleTopography(1.0 / scale);
		model.notifyObservers();
	}

	@Override
	public void redo() throws CannotRedoException {
		model.scaleTopography(scale);
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
		return "draw shape";
	}
}
