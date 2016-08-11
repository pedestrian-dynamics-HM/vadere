package org.vadere.gui.topographycreator.control;

import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;

import javax.swing.undo.UndoableEdit;
import javax.swing.undo.UndoableEditSupport;

import org.vadere.gui.components.control.IMode;
import org.vadere.gui.components.control.RectangleSelectionMode;
import org.vadere.gui.topographycreator.model.IDrawPanelModel;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;

/**
 * In this mode the user can cut the Topography with the mouse.
 * 
 * 
 */
public class CutScenarioMode extends RectangleSelectionMode {
	private final UndoableEditSupport undoSupport;
	private final IDrawPanelModel panelModel;

	public CutScenarioMode(final IDrawPanelModel panelModel, final UndoableEditSupport undoSupport) {
		super(panelModel);
		this.panelModel = panelModel;
		this.undoSupport = undoSupport;
	}

	@Override
	public void mouseReleased(MouseEvent event) {
		super.mouseReleased(event);
		final VShape selectionShape = panelModel.getSelectionShape();
		final Rectangle2D bound = selectionShape.getBounds2D();

		UndoableEdit edit = new EditCutScenario(panelModel, new VRectangle(panelModel.getTopographyBound()));
		undoSupport.postEdit(edit);

		double x = Math.max(0, bound.getX() - panelModel.getBoundingBoxWidth());
		double y = Math.max(0, bound.getY() - panelModel.getBoundingBoxWidth());

		panelModel.setTopographyBound(new VRectangle(x, y, bound.getWidth() + 2 * panelModel.getBoundingBoxWidth(),
				bound.getHeight() + 2 * panelModel.getBoundingBoxWidth()));
		panelModel.notifyObservers();
	}

	@Override
	public IMode clone() {
		return new CutScenarioMode(panelModel, undoSupport);
	}
}
