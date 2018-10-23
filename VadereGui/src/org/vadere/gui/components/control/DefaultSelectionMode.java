package org.vadere.gui.components.control;

import javax.swing.*;

import org.vadere.gui.components.model.IDefaultModel;
import org.vadere.util.geometry.shapes.VRectangle;

import java.awt.event.MouseEvent;

public class DefaultSelectionMode extends RectangleSelectionMode {
	public static double MIN_RECT_AREA = 0.5;
	private JViewport viewport;

	public DefaultSelectionMode(final IDefaultModel model) {
		super(model);
	}

	@Override
	public void mouseReleased(final MouseEvent event) {
		panelModel.hideSelection();
		panelModel.deselectSelectedElement();

		VRectangle rect = (VRectangle) panelModel.getSelectionShape();

		if (SwingUtilities.isRightMouseButton(event) && rect.getWidth() * rect.getHeight() >= MIN_RECT_AREA) {
			panelModel.fireChangeViewportEvent(rect);
		}
	}
}
