package org.vadere.gui.topographycreator.control;

import org.jetbrains.annotations.NotNull;
import org.vadere.gui.topographycreator.model.TopographyCreatorModel;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

/**
 * @author Benedikt Zoennchen
 */
public class EditTranslateElements extends AbstractUndoableEdit {

	private static final long serialVersionUID = 5176192525116057658L;
	private final TopographyCreatorModel panelModel;
	private final double dx;
	private final double dy;

	public EditTranslateElements(@NotNull final TopographyCreatorModel panelModel,
	                               final double dx,
	                               final double dy) {
		this.panelModel = panelModel;
		this.dx = dx;
		this.dy = dy;
	}

	@Override
	public void undo() throws CannotUndoException {
		panelModel.translateElements(-dx, -dy);
	}

	@Override
	public void redo() throws CannotRedoException {
		panelModel.translateTopography(dx, dy);
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
		return "translate elements";
	}
}
