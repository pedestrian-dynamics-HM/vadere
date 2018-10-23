package org.vadere.gui.components.control;

import javax.swing.*;

import org.vadere.gui.components.model.IDefaultModel;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.voronoi.VoronoiDiagram;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;

public class DrawVoronoiDiagramMode extends DefaultSelectionMode {
	private final IDefaultModel defaultModel;

	public DrawVoronoiDiagramMode(final IDefaultModel defaultModel) {
		super(defaultModel);
		this.defaultModel = defaultModel;
	}

	@Override
	public void mouseReleased(MouseEvent event) {
		if (!SwingUtilities.isRightMouseButton(event)) {
			Rectangle2D.Double bound = (Rectangle2D.Double) defaultModel.getSelectionShape().getBounds2D();
			if (bound.getHeight() > 0 && bound.getWidth() > 0) {
				VoronoiDiagram voronoiDiagram = new VoronoiDiagram(new VRectangle(bound));
				defaultModel.setVoronoiDiagram(voronoiDiagram);
				defaultModel.showVoronoiDiagram();
				defaultModel.setMouseSelectionMode(new DefaultSelectionMode(defaultModel));
			}
			defaultModel.hideSelection();
			defaultModel.notifyObservers();
		} else {
			super.mouseReleased(event);
		}
	}

	@Override
	public IMode clone() {
		return new DrawVoronoiDiagramMode(defaultModel);
	}

	@Override
	public Color getSelectionColor() {
		return Color.RED;
	}
}
