package org.vadere.gui.components.control;

import org.vadere.gui.components.model.IDefaultModel;
import org.vadere.util.logging.Logger;

import java.awt.*;
import java.awt.geom.Rectangle2D;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Updates the viewport in the model if the viewport of the JScrollpane changes (pixel coordinates).
 * 
 */
public class JViewportChangeListener implements ChangeListener {

	private Logger logger = Logger.getLogger(JViewportChangeListener.class);
	private final JScrollBar verticalScrollBar;
	private final IDefaultModel defaultModel;

	public JViewportChangeListener(final IDefaultModel defaultModel, final JScrollBar verticalScrollbar) {
		this.defaultModel = defaultModel;
		this.verticalScrollBar = verticalScrollbar;
	}

	@Override
	public void stateChanged(final ChangeEvent e) {
		Rectangle2D.Double topographyBound = defaultModel.getTopographyBound();
		Rectangle2D.Double viewp = defaultModel.getViewportBound();
		JViewport viewPort = (JViewport) e.getSource();
		if (topographyBound != null) {
			Rectangle rect = viewPort.getViewRect();
			double x = Math.max(topographyBound.getMinX(), topographyBound.getMinX() + rect.getX() / defaultModel.getScaleFactor());

			double barHeight;
			if(verticalScrollBar.getHeight() > 0) {
				barHeight = verticalScrollBar.getHeight();
			}
			else {
				barHeight = rect.getHeight();
			}

			double y = Math.max(topographyBound.getMinY(), topographyBound.getMinY() + topographyBound.getHeight()
					- ((rect.getY() + barHeight) / defaultModel.getScaleFactor()));
			double w = rect.getWidth() / defaultModel.getScaleFactor();
			double h = rect.getHeight() / defaultModel.getScaleFactor();

			defaultModel.setViewportBound(new Rectangle2D.Double(x, y, w, h));
			defaultModel.notifyObservers();
		}
	}
}
