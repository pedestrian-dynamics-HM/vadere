package org.vadere.gui.topographycreator.control;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;

import org.vadere.gui.components.control.DefaultModeAdapter;
import org.vadere.gui.components.control.IMode;
import org.vadere.gui.components.utils.Resources;
import org.vadere.gui.topographycreator.model.IDrawPanelModel;

/**
 * In this mode the user is able to zoom in by clicking his mouse and not only by using the mouse
 * wheel.
 * 
 * 
 */
public class ZoomInMode extends DefaultModeAdapter {
	private static Resources resources = Resources.getInstance("topographycreator");
	private final IDrawPanelModel panelModel;
	private Cursor cursor;

	public ZoomInMode(IDrawPanelModel panelModel) {
		super(panelModel);
		this.panelModel = panelModel;

		Toolkit toolkit = Toolkit.getDefaultToolkit();
		cursor = toolkit.createCustomCursor(toolkit.getImage(Resources.class.getResource("/icons/zoom_in_cursor.png")),
				new Point(0, 0), "zoom in");
	}

	@Override
	public void mouseClicked(MouseEvent event) {
		panelModel.setMousePosition(event.getPoint());
		if (panelModel.zoomIn()) {
			panelModel.notifyScaleListeners();
		}
		panelModel.notifyObservers();
	}

	@Override
	public Cursor getCursor() {
		return cursor;
	}

	@Override
	public IMode clone() {
		return new ZoomInMode(panelModel);
	}
}
