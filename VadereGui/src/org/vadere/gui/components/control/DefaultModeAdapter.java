package org.vadere.gui.components.control;

import javax.swing.*;

import org.vadere.gui.components.model.IDefaultModel;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

public class DefaultModeAdapter implements IMode {

	protected final IDefaultModel panelModel;
	private Point lastDragPos;
	private double hDiff = 0, vDiff = 0;

	public DefaultModeAdapter(final IDefaultModel model) {
		this.panelModel = model;
	}

	@Override
	public void mouseClicked(final MouseEvent event) {
		panelModel.setMousePosition(event.getPoint());
		panelModel.setSelectedElement(panelModel.getMousePosition());
		panelModel.notifyObservers();
	}

	@Override
	public void mouseEntered(MouseEvent e) {}

	@Override
	public void mouseExited(MouseEvent e) {}

	@Override
	public void mousePressed(MouseEvent e) {
		lastDragPos = e.getPoint();
	}

	@Override
	public void mouseReleased(MouseEvent e) {}

	@Override
	public void mouseDragged(MouseEvent e) {
		panelModel.setMousePosition(e.getPoint());

		JScrollBar hBar = panelModel.getScrollPane().getHorizontalScrollBar();
		JScrollBar vBar = panelModel.getScrollPane().getVerticalScrollBar();

		double diffX = (lastDragPos.getX() + hDiff) - e.getX();
		double diffY = (lastDragPos.getY() + vDiff) - e.getY();
		lastDragPos = e.getPoint();

		// the cap of 100 and the 0.001 are "empirical" values :) the idea is that faster dragging
		// also results in faster scrolling. values bigger than 0.001 seem to cause weird side
		// effects
		hDiff = hBar.getWidth() * (Math.abs(diffX) < 100 ? diffX : Math.signum(diffX) * 100) * 0.001;
		vDiff = vBar.getHeight() * (Math.abs(diffY) < 100 ? diffY : Math.signum(diffY) * 100) * 0.001;

		hBar.setValue((int) (hBar.getValue() + hDiff));
		vBar.setValue((int) (vBar.getValue() + vDiff));

		panelModel.notifyObservers();
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		panelModel.setMousePosition(e.getPoint());
		panelModel.notifyObservers();
	}

	/**
	 * Use following shortcuts for zooming and scrolling:
	 * <ul>
	 *     <li>Ctrl + Mouse Wheel Scroll: Zoom in/out.</li>
	 *     <li>Mouse Wheel Scroll: Scroll vertically.</li>
	 *     <li>Shift + Mouse Wheel Scroll: Scroll horizontally.</li>
	 *     <li>Use Alt key to decrease the step size while scrolling.</li>
	 * </ul>
	 * @param e
	 */
	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
		if (e.isControlDown()) {
			/* Use a smaller scale factor if the ALT key is pressed simultaneously */
			double zoomFactor = e.isAltDown() ? 1.05 : 1.2;

			if (e.getWheelRotation() > 0) {
				if (panelModel.zoomOut(zoomFactor)) {
					panelModel.notifyScaleListeners();
					panelModel.notifyObservers();
				}
				delay();
			} else if (e.getWheelRotation() < 0) {
				if (panelModel.zoomIn(zoomFactor)) {
					panelModel.notifyScaleListeners();
					panelModel.notifyObservers();
				}
				delay();
			}
		} else {
			MouseWheelEvent scrollEvent = (e.isAltDown()) ? getSmallStepScrollEvent(e) : getBigStepScrollEvent(e);
			panelModel.getScrollPane().dispatchEvent(scrollEvent);
		}
	}

	private MouseWheelEvent getSmallStepScrollEvent(MouseWheelEvent baseEvent) {
		MouseWheelEvent smallScrollEvent = new MouseWheelEvent(
				(Component) baseEvent.getSource(),
				baseEvent.getID(),
				baseEvent.getWhen(),
				baseEvent.getModifiersEx(),
				baseEvent.getX(),
				baseEvent.getY(),
				baseEvent.getClickCount(),
				baseEvent.isPopupTrigger(),
				MouseWheelEvent.WHEEL_UNIT_SCROLL,
				baseEvent.getScrollAmount(),
				baseEvent.getWheelRotation());
		return smallScrollEvent;
	}

	private MouseWheelEvent getBigStepScrollEvent(MouseWheelEvent baseEvent) {
		MouseWheelEvent smallScrollEvent = new MouseWheelEvent(
				(Component) baseEvent.getSource(),
				baseEvent.getID(),
				baseEvent.getWhen(),
				baseEvent.getModifiersEx(),
				baseEvent.getX(),
				baseEvent.getY(),
				baseEvent.getClickCount(),
				baseEvent.isPopupTrigger(),
				MouseWheelEvent.WHEEL_BLOCK_SCROLL,
				baseEvent.getScrollAmount(),
				baseEvent.getWheelRotation());
		return smallScrollEvent;
	}

	private void delay() {
		try {
			Thread.sleep(10);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
	}

	@Override
	public Cursor getCursor() {
		return Cursor.getDefaultCursor();
	}

	@Override
	public Color getSelectionColor() {
		return Color.GRAY;
	}

	@Override
	public IMode clone() {
		return new DefaultModeAdapter(panelModel);
	}
}

