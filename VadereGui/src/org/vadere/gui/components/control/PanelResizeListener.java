package org.vadere.gui.components.control;

import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.geom.Rectangle2D;

import org.vadere.gui.components.model.IDefaultModel;

public class PanelResizeListener implements ComponentListener {

	private final IDefaultModel defaultModel;

	public PanelResizeListener(final IDefaultModel defaultModel) {
		this.defaultModel = defaultModel;
	}

	@Override
	public void componentResized(final ComponentEvent event) {
		double panelWidth = event.getComponent().getWidth();
		double panelHeight = event.getComponent().getHeight();
		defaultModel.setWindowBound(new Rectangle2D.Double(0, 0, panelWidth, panelHeight));
		defaultModel.notifyObservers();
	}

	@Override
	public void componentMoved(ComponentEvent e) {

	}

	@Override
	public void componentShown(ComponentEvent e) {

	}

	@Override
	public void componentHidden(ComponentEvent e) {

	}
}
