package org.vadere.gui.components.control;

import javax.swing.*;

import org.vadere.gui.components.model.IDefaultModel;

import java.awt.*;
import java.awt.geom.Rectangle2D;

/**
 * The ViewportChangeListener handles the change of the viewport when the user select a viewport
 * bound (world coordinates).
 *
 */
public class ViewportChangeListener implements IViewportChangeListener {

	private final JScrollPane scrollPane;
	private final IDefaultModel defaultModel;

	public ViewportChangeListener(final IDefaultModel defaultModel, final JScrollPane scrollPane) {
		this.scrollPane = scrollPane;
		this.defaultModel = defaultModel;
	}

	@Override
	public void viewportChange(final ViewportChangeEvent event) {

		final JViewport viewport = scrollPane.getViewport();

		final Rectangle2D.Double viewportBound = event.getViewportBound();
		final Rectangle2D.Double topographyBound = defaultModel.getTopographyBound();
		double s1 = scrollPane.getWidth() / viewportBound.getWidth();
		double s2 = scrollPane.getHeight() / viewportBound.getHeight();
		double scale = Math.min(s1, s2);
		boolean scaleChanges = defaultModel.setScale(scale);
		if (scaleChanges || !viewport.equals(defaultModel.getViewportBound())) {

			double dx = viewportBound.getMinX() - topographyBound.getMinX();
			double dy = viewportBound.getMinY() - topographyBound.getMinY();
			if (scaleChanges) {
				defaultModel.notifyScaleListeners();
			}

			scale = defaultModel.getScaleFactor();

			viewport.getView().setPreferredSize(new Dimension(
					(int) (topographyBound.getWidth() * scale),
					(int) (topographyBound.getHeight() * scale)));

			viewport.setViewSize(new Dimension(
					(int) (topographyBound.getWidth() * scale),
					(int) (topographyBound.getHeight() * scale)));

			viewport.setViewPosition(new Point(
					(int) (dx * scale),
					(int) ((topographyBound.getHeight() - (dy + viewportBound.getHeight()))
							* scale)));

			defaultModel.notifyObservers();
		}
	}
}
