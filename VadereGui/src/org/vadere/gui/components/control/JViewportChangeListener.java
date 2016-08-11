package org.vadere.gui.components.control;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.vadere.gui.components.model.IDefaultModel;

import java.awt.*;
import java.awt.geom.Rectangle2D;

/**
 * Updates the viewport in the model if the viewport of the JScrollpane changes (pixel coordinates).
 * 
 */
public class JViewportChangeListener implements ChangeListener {

	private final JScrollBar verticalScrollBar;
	private final IDefaultModel defaultModel;

	public JViewportChangeListener(final IDefaultModel defaultModel, final JScrollBar verticalScrollbar) {
		this.defaultModel = defaultModel;
		this.verticalScrollBar = verticalScrollbar;
	}

	@Override
	public void stateChanged(final ChangeEvent e) {
		Rectangle2D.Double topographyBound = defaultModel.getTopographyBound();
		if (topographyBound != null) {
			Rectangle rect = ((JViewport) e.getSource()).getViewRect();
			double x = rect.getX() / defaultModel.getScaleFactor();
			double y = topographyBound.getHeight()
					- ((rect.getY() + verticalScrollBar.getHeight()) / defaultModel.getScaleFactor());
			double w = rect.getWidth() / defaultModel.getScaleFactor();
			double h = rect.getHeight() / defaultModel.getScaleFactor();
			defaultModel.setViewportBound(new Rectangle2D.Double(x, y, w, h));
			defaultModel.notifyObservers();
		}
	}
}
