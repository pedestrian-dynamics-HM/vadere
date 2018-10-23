package org.vadere.gui.components.control;

import java.awt.event.MouseEvent;

import javax.swing.SwingUtilities;

import org.vadere.gui.components.model.IDefaultModel;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;

public class RectangleSelectionMode extends DefaultModeAdapter {

	public RectangleSelectionMode(final IDefaultModel model) {
		super(model);
	}

	@Override
	public void mousePressed(final MouseEvent event) {
		panelModel.setMousePosition(event.getPoint());
		if (SwingUtilities.isMiddleMouseButton(event)) {
			super.mousePressed(event);
		} else {
			panelModel.setStartSelectionPoint(event.getPoint());

			// Set default dimension of selection shape to current GridResolution.
			// If mouse is dragged smaller selection shapes are still possible.
			double width = panelModel.getGridResolution();
			double height = width;
			VRectangle shape = new VRectangle(panelModel.getMousePosition().x,
					panelModel.getMousePosition().y, width, height);
			panelModel.setSelectionShape(shape);
		}
		panelModel.notifyObservers();
	}

	@Override
	public void mouseReleased(final MouseEvent event) {
		panelModel.hideSelection();
		panelModel.notifyObservers();
	}

	@Override
	public void mouseDragged(MouseEvent event) {
		panelModel.setMousePosition(event.getPoint());
		panelModel.showSelection();

		if (SwingUtilities.isMiddleMouseButton(event)) {
			super.mouseDragged(event);
		} else if (panelModel.isSelectionVisible()) {
			VPoint startSelectionPosition = panelModel.getStartSelectionPoint();
			VPoint cursorPosition = panelModel.getMousePosition();

			double x = Math.min(startSelectionPosition.x, cursorPosition.x);
			double y = Math.min(startSelectionPosition.y, cursorPosition.y);
			double minValue = Math.min(0.1, panelModel.getGridResolution());
			double width = Math.max(Math.abs(startSelectionPosition.x - cursorPosition.x), minValue);
			double height = Math.max(Math.abs(startSelectionPosition.y - cursorPosition.y), minValue);

			// to get nice floating numbers.
			double factor = Math.max(10, 1 / panelModel.getGridResolution());
			width = Math.round(width * factor) / factor;
			height = Math.round(height * factor) / factor;

			panelModel.setSelectionShape(new VRectangle(x, y, width, height));

		}
		panelModel.notifyObservers();
	}
}
