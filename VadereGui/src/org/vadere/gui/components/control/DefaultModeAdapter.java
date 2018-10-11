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
		if(panelModel.isTopgraphyAvailable()) {
			panelModel.setMousePosition(event.getPoint());
			panelModel.setSelectedElement(panelModel.getMousePosition());
			panelModel.notifyObservers();
		}
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
		if(panelModel.isTopgraphyAvailable()) {
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
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		if(panelModel.isTopgraphyAvailable()) {
			panelModel.setMousePosition(e.getPoint());
			panelModel.notifyObservers();
		}
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
		if(panelModel.isTopgraphyAvailable()) {
			if (e.getWheelRotation() > 0) {
				if (panelModel.zoomOut()) {
					panelModel.notifyScaleListeners();
					panelModel.notifyObservers();
				}
				delay();
			} else if (e.getWheelRotation() < 0) {
				if (panelModel.zoomIn()) {
					panelModel.notifyScaleListeners();
					panelModel.notifyObservers();
				}
				delay();
			}
		}
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

