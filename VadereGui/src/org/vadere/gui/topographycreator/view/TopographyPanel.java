package org.vadere.gui.topographycreator.view;

import javax.swing.*;

import org.vadere.gui.components.control.IMode;
import org.vadere.gui.components.view.DefaultRenderer;
import org.vadere.gui.components.view.ScaleablePanel;
import org.vadere.gui.topographycreator.model.TopographyCreatorModel;

import java.awt.*;
import java.util.Observable;
import java.util.Observer;

public class TopographyPanel extends ScaleablePanel implements Observer {

	private static final long serialVersionUID = 3772313433958735043L;
	private TopographyCreatorRenderer renderer;
	private final TopographyCreatorModel model;

	/** a reference copy of the selection mode of the panelModel. */
	private IMode selectionMode;

	public TopographyPanel(final TopographyCreatorModel model, final DefaultRenderer renderer,
			final JScrollPane scrollPane) {
		super(model, renderer, scrollPane);
		this.model = model;
		this.selectionMode = model.getMouseSelectionMode();
		addMouseListener(selectionMode);
		addMouseMotionListener(selectionMode);
		addMouseWheelListener(selectionMode);
	}

	/*
	 * @Override
	 * protected void paintComponent(Graphics g) {
	 * super.paintComponent(g);
	 * renderer.render((Graphics2D) g, getWidth(), getHeight(), true);
	 * }
	 */

	@Override
	public void update(Observable observable, Object object) {
		setMouseSelectionMode(model.getMouseSelectionMode());
		setCursor(model.getCursor());
		repaint();
	}

	private void setMouseSelectionMode(final IMode selectionMode) {
		if (!selectionMode.equals(this.selectionMode)) {
			removeMouseListener(this.selectionMode);
			removeMouseMotionListener(this.selectionMode);
			removeMouseWheelListener(this.selectionMode);
			addMouseListener(selectionMode);
			addMouseMotionListener(selectionMode);
			addMouseWheelListener(selectionMode);
			this.selectionMode = selectionMode;
		}
	}
}
