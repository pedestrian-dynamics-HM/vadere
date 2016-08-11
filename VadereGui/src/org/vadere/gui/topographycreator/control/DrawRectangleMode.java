package org.vadere.gui.topographycreator.control;

import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import javax.swing.*;
import javax.swing.undo.UndoableEditSupport;

import org.vadere.gui.components.control.DefaultSelectionMode;
import org.vadere.gui.components.control.IMode;
import org.vadere.gui.topographycreator.model.IDrawPanelModel;

/**
 * In this mode Retangles will be generated.
 * 
 * 
 */
public class DrawRectangleMode extends DefaultSelectionMode {
	private final UndoableEditSupport undoSupport;
	private final IDrawPanelModel panelModel;

	public DrawRectangleMode(final IDrawPanelModel panelModel, final UndoableEditSupport undoSupport) {
		super(panelModel);
		this.panelModel = panelModel;
		this.undoSupport = undoSupport;
		// panelModel.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
	}

	@Override
	public void mouseReleased(MouseEvent event) {
		if (!SwingUtilities.isRightMouseButton(event)) {
			Rectangle2D.Double bound = (Rectangle2D.Double) panelModel.getSelectionShape().getBounds2D();
			if (bound.getHeight() > 0 && bound.getWidth() > 0) {
				new ActionAddElement("add element", panelModel, undoSupport).actionPerformed(null);
			}
			panelModel.notifyObservers();
		} else {
			super.mouseReleased(event);
		}
	}

	@Override
	public IMode clone() {
		return new DrawRectangleMode(panelModel, undoSupport);
	}
}
