package org.vadere.gui.topographycreator.control;

import org.vadere.gui.topographycreator.model.IDrawPanelModel;
import org.vadere.util.geometry.shapes.VRectangle;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

/**
 * @author Benedikt Zoennchen
 */
public class EditResizeTopographyBound extends AbstractUndoableEdit {

	private static final long serialVersionUID = 5176192525116057658L;
	private final IDrawPanelModel panelModel;
	private final VRectangle oldBound;
	private final VRectangle newBound;

	public EditResizeTopographyBound(final IDrawPanelModel<?> panelModel, final VRectangle oldBound, final VRectangle newBound) {
		this.panelModel = panelModel;
		this.oldBound = oldBound;
		this.newBound = newBound;
	}

	@Override
	public void undo() throws CannotUndoException {
		panelModel.setTopographyBound(oldBound);
	}

	@Override
	public void redo() throws CannotRedoException {
		panelModel.setTopographyBound(newBound);
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
		return "resize topography bound";
	}
}
