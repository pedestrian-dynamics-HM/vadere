package org.vadere.gui.topographycreator.view;

import org.vadere.gui.components.model.DefaultConfig;
import org.vadere.gui.components.view.DefaultRenderer;
import org.vadere.gui.topographycreator.model.IDrawPanelModel;
import org.vadere.state.scenario.ScenarioElement;
import org.vadere.state.types.ScenarioElementType;
import org.vadere.util.geometry.shapes.VPoint;

import java.awt.*;
import java.awt.geom.Line2D;

public class TopographyCreatorRenderer  extends DefaultRenderer {

	private final IDrawPanelModel<DefaultConfig> panelModel;


	/** the buffer of the grid image. */
	private Image gridImage;

	/** the times in millis the thread sleeps after each repaint. */
	private final static long REPAINT_SLEEP_TIME = 25; // 25 => 40 FPS

	private int boundId = -1;
	private ScenarioElementRenderer[] renderers;

	/**
	 * Creates a new DrawPanel and start the repaint thead.
	 * 
	 * @param panelModel the panelModel of the panel
	 */
	public TopographyCreatorRenderer(final IDrawPanelModel panelModel){
		super(panelModel);
		this.panelModel = panelModel;
		this.renderers = new ScenarioElementRenderer[ScenarioElementType.values().length];
		renderers[ScenarioElementType.OBSTACLE.ordinal()] = this::renderFilledShape;
		renderers[ScenarioElementType.PEDESTRIAN.ordinal()] = this::renderFilledShape;
		renderers[ScenarioElementType.SOURCE.ordinal()] = this::renderFilledShape;
		renderers[ScenarioElementType.TARGET.ordinal()] = this::renderFilledShape;
		renderers[ScenarioElementType.TARGET_CHANGER.ordinal()] = this::renderFilledShape;
		renderers[ScenarioElementType.ABSORBING_AREA.ordinal()] = this::renderFilledShape;
		renderers[ScenarioElementType.STAIRS.ordinal()] = this::renderStair;
		renderers[ScenarioElementType.TELEPORTER.ordinal()] = this::renderFilledShape;
		renderers[ScenarioElementType.CAR.ordinal()] = this::renderFilledShape;
		renderers[ScenarioElementType.MEASUREMENT_AREA.ordinal()] = this::renderMeasurementArea;
	}

	@Override
	public void renderPostTransformation(final Graphics2D graphics, final int width, final int height) {
		super.renderPostTransformation(graphics, width, height);
		graphics.setColor(Color.BLACK);

		renderGrid(graphics);

		if (panelModel.isElementSelected()) {
			renderSelectionBorder(graphics);
		}

		for (ScenarioElement element : panelModel) {
			Color color = panelModel.getScenarioElementColor(element.getType());
			renderers[element.getType().ordinal()].render(element, graphics, color);
		}

		if (panelModel.isPrototypeVisble()) {
			graphics.setColor(Color.GRAY);
			fill(panelModel.getPrototypeShape(), graphics);
		}

		if (panelModel.isElementSelected()) {
			renderSelectionBorder(graphics);
		}

		if (panelModel.isSelectionVisible()) {
			renderSelectionShape(graphics);
		}

		if (panelModel.getCursor().getType() == Cursor.DEFAULT_CURSOR) {
			renderCursor(graphics, panelModel.getGridResolution(), getLineWidth());
		}

		graphics.dispose();
	}

	private void renderCursor(Graphics2D g, double resolution, float lineWidth) {
		g.setColor(panelModel.getCursorColor());
		g.setStroke(new BasicStroke(lineWidth));
		final VPoint cursorPosition = panelModel.getMousePosition();
		double absolutCursorX = cursorPosition.x;
		double absolutCursorY = cursorPosition.y;
		draw(new Line2D.Double(absolutCursorX - resolution * 0.2, absolutCursorY, absolutCursorX + resolution * 0.2, absolutCursorY), g);
		draw(new Line2D.Double(absolutCursorX, absolutCursorY - resolution * 0.2, absolutCursorX, absolutCursorY + resolution * 0.2), g);
	}

	/*
	 * private void paintGrid(Graphics2D graphics, double width, double height, double resolution,
	 * float lineWidth) {
	 * 
	 * // cache the image so the calculation has not to be done anytime! This is for performance
	 * reason.
	 * if(gridImage == null || panelModel.getBoundId() != boundId)
	 * {
	 * boundId = panelModel.getBoundId();
	 * gridImage = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
	 * 
	 * Graphics2D g = (Graphics2D) gridImage.getGraphics();
	 * g.setTransform(graphics.getTransform());
	 * 
	 * g.setColor(Color.LIGHT_GRAY);
	 * g.setStroke(new BasicStroke(lineWidth));
	 * 
	 * for (double y = 0; y <= height + 0.01; y += resolution) {
	 * for (double x = 0; x <= width + 0.01; x += resolution) {
	 * g.draw(new Line2D.Double(x - resolution * 0.2, y, x + resolution * 0.2, y));
	 * g.draw(new Line2D.Double(x, y - resolution * 0.2, x, y + resolution * 0.2));
	 * }
	 * }
	 * }
	 * AffineTransform transform = graphics.getTransform();
	 * graphics.setTransform(new AffineTransform());
	 * graphics.drawImage(gridImage, 0, 0, null);
	 * graphics.setTransform(transform);
	 * }
	 * 
	 * public void drawThickLine(Graphics2D g, double x1, double y1, double x2, double y2, float
	 * lineWidth, Color c) {
	 * g.setColor(c);
	 * g.setStroke(new BasicStroke(lineWidth));
	 * g.draw(new Line2D.Double(x1, y1, x2, y2));
	 * }
	 */
}
