package org.vadere.gui.topographycreator.control;

import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;

import org.vadere.gui.components.control.IMode;
import org.vadere.gui.components.control.RectangleSelectionMode;
import org.vadere.gui.topographycreator.model.IDrawPanelModel;
import org.vadere.util.geometry.shapes.VRectangle;

/**
 * In this mode the user can redefine the viewport with his mouse.
 * Unused action!
 * 
 *
 */
public class SelectViewportMode extends RectangleSelectionMode {

	private final IDrawPanelModel panelModel;

	public SelectViewportMode(final IDrawPanelModel panelModel) {
		super(panelModel);
		this.panelModel = panelModel;
	}

	@Override
	public void mouseReleased(MouseEvent event) {
		super.mouseReleased(event);
		final Shape selectionShape = panelModel.getSelectionShape();
		final Rectangle2D.Double bound = (Rectangle2D.Double) selectionShape.getBounds2D();

		if (bound.getWidth() * panelModel.getScaleFactor() * bound.getHeight() * panelModel.getScaleFactor() >= 40) {
			panelModel.fireChangeViewportEvent(new VRectangle(bound));
			panelModel.notifyObservers();
		}
	}

	@Override
	public IMode clone() {
		return new SelectViewportMode(panelModel);
	}
}
