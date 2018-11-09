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
public class EditTranslateTopography extends AbstractUndoableEdit {

	private static final long serialVersionUID = 5176192525116057658L;
	private final TopographyCreatorModel panelModel;
	private final double xOld;
	private final double yOld;
	private final double xNew;
	private final double yNew;

	public EditTranslateTopography(@NotNull final TopographyCreatorModel panelModel,
	                               final double xOld,
	                               final double yOld,
	                               final double xNew,
	                               final double yNew) {
		this.panelModel = panelModel;
		this.xOld = xOld;
		this.yOld = yOld;
		this.xNew = xNew;
		this.yNew = yNew;
	}

	@Override
	public void undo() throws CannotUndoException {
		panelModel.translateTopography(xOld, yOld);
		VRectangle viewportBound = new VRectangle(panelModel.getViewportBound()).translate(new VPoint(xOld - xNew, yOld - yNew));
		panelModel.setViewportBound(viewportBound);
	}

	@Override
	public void redo() throws CannotRedoException {
		panelModel.translateTopography(xNew, yNew);
		VRectangle viewportBound = new VRectangle(panelModel.getViewportBound()).translate(new VPoint(xNew - xOld, yNew - yOld));
		panelModel.setViewportBound(viewportBound);
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
		return "translate topography";
	}
}
