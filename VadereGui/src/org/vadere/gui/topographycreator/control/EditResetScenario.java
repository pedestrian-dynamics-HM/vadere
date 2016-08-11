package org.vadere.gui.topographycreator.control;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import org.vadere.gui.topographycreator.model.IDrawPanelModel;
import org.vadere.state.scenario.Topography;

/**
 * Part of the command pattern to re- and undo resetTopography.
 * 
 * 
 */
public class EditResetScenario extends AbstractUndoableEdit {
	private static final long serialVersionUID = 2514392730749834389L;
	private final Topography beforeTopography;
	private final IDrawPanelModel panelModel;

	public EditResetScenario(final IDrawPanelModel panelModel, final Topography beforeTopography) {
		this.beforeTopography = beforeTopography;
		this.panelModel = panelModel;
	}

	@Override
	public void undo() throws CannotUndoException {
		panelModel.setTopography(beforeTopography);
		panelModel.notifyObservers();
	}

	@Override
	public void redo() throws CannotRedoException {
		panelModel.resetScenario();
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
