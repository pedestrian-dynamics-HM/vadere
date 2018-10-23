package org.vadere.gui.components.view;

import javax.swing.*;

import org.vadere.gui.components.control.IScaleChangeListener;
import org.vadere.gui.components.model.IDefaultModel;
import org.vadere.util.geometry.shapes.VPoint;

import java.awt.*;

public abstract class ScaleablePanel extends JPanel implements IScaleChangeListener {

	private DefaultRenderer renderer;
	private JScrollPane scrollPane;
	private final IDefaultModel model;
	private Point lastMousePos;
	private VPoint newRelMousePos;


	public ScaleablePanel(final IDefaultModel defaultModel, final DefaultRenderer renderer,
			final JScrollPane scrollPane) {
		this.renderer = renderer;
		this.model = defaultModel;
		this.scrollPane = scrollPane;
	}

	protected DefaultRenderer getRenderer() {
		return renderer;
	}

	protected void setRenderer(final DefaultRenderer renderer) {
		this.renderer = renderer;
	}

	public void setScrollPane(final JScrollPane scrollPane) {
		this.scrollPane = scrollPane;
	}

	@Override
	public void scaleChange(final double scale) {
		if (model.getTopography() != null && scrollPane.getMousePosition() != null) {
			double widthBefore = getPreferredSize().getWidth();
			JViewport viewport = scrollPane.getViewport();

			// boolean keepFocusPoint = false;

			VPoint relMousePos; // mouse pos within the viewport, in pixel coordinates
			Point mousePos = scrollPane.getMousePosition();
			if (mousePos.equals(lastMousePos)) { // if the mouse didn't move, use the previous zoom-target-point
				relMousePos = newRelMousePos;
				// keepFocusPoint = true;
			} else {
				lastMousePos = mousePos;
				relMousePos = new VPoint(mousePos);
			}

			// double diffToViewportCenterX = relMousePos.getX() -
			// viewport.getBounds().getCenterX();
			// double diffToViewportCenterY = relMousePos.getY() -
			// viewport.getBounds().getCenterY();

			VPoint absMousePos = relMousePos.add(new VPoint(viewport.getViewPosition())); // mouse pos on the canvas (independent of viewport), in pixel coordinates

			setPreferredSize(new Dimension(
					(int) (model.getTopographyBound().getWidth() * scale),
					(int) (model.getTopographyBound().getHeight() * scale)));
			repaint();

			double scaleChange = getPreferredSize().getWidth() / widthBefore;

			VPoint newAbsMousePos = absMousePos.scalarMultiply(scaleChange);
			VPoint diffAbsMousePos = newAbsMousePos.subtract(absMousePos); // the difference between the old and new absolute mouse pos is equivalent to what we want to translate the viewport-viewPosition, because then the focus point stays at the same position

			double widthDiff = getPreferredSize().getWidth() - viewport.getBounds().getWidth();
			boolean outOfViewportInWidth = widthDiff > 0;
			double viewportMoveX = 0; // the ideal diffAbsMousePos can only be applied if the canvas is beyond the viewport borders with more than diffAbsMousePos
			if (outOfViewportInWidth) {
				viewportMoveX = diffAbsMousePos.getX();// + (keepFocusPoint && scaleChange > 1 ? diffToViewportCenterX * 0.2 : 0);
				if (viewportMoveX > widthDiff) {
					viewportMoveX = widthDiff;
				}
			}

			double heightDiff = getPreferredSize().getHeight() - viewport.getBounds().getHeight();
			boolean outOfViewportInHeight = heightDiff > 0;
			double viewportMoveY = 0;
			if (outOfViewportInHeight) {
				viewportMoveY = diffAbsMousePos.getY();// + (keepFocusPoint && scaleChange > 1 ? diffToViewportCenterY * 0.2 : 0);
				if (viewportMoveY > heightDiff) {
					viewportMoveY = heightDiff;
				}
			}

			viewport.setViewPosition(new Point((int) (viewport.getViewPosition().getX() + viewportMoveX),
					(int) (viewport.getViewPosition().getY() + viewportMoveY)));

			newRelMousePos = newAbsMousePos.subtract(new VPoint(viewport.getViewPosition())); // in case of no new mouse movement, this will be used in next zoom
		}
		revalidate();
	}

	@Override
	protected void paintComponent(Graphics g) {
		if (renderer != null && scrollPane != null) {
			// System.out.println(getWidth() + " vs " + getParent().getWidth());
			JViewport viewport = scrollPane.getViewport();
			Point viewposition = viewport.getViewPosition();
			renderer.render((Graphics2D) g, (int) viewposition.getX(), (int) viewposition.getY(), viewport.getWidth(),
					viewport.getHeight());
		}
		super.paintComponent(g);
	}

}
