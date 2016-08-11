package org.vadere.gui.components.control;

import java.awt.geom.Rectangle2D;

public class ViewportChangeEvent {
	private final Rectangle2D.Double viewportBound;

	public ViewportChangeEvent(final Rectangle2D.Double viewportBound) {
		this.viewportBound = viewportBound;
	}

	public Rectangle2D.Double getViewportBound() {
		return viewportBound;
	}

}
