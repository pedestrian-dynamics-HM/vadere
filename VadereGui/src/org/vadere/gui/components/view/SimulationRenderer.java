package org.vadere.gui.components.view;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.stream.Stream;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.vadere.util.visualization.ColorHelper;
import org.vadere.gui.components.model.SimulationModel;
import org.vadere.gui.components.utils.CLGaussianCalculator;
import org.vadere.gui.components.utils.Resources;
import org.vadere.gui.renderer.agent.AgentRender;
import org.vadere.state.scenario.Agent;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VTriangle;

public abstract class SimulationRenderer extends DefaultRenderer {

    private static Logger logger = LogManager.getLogger(SimulationRenderer.class);
    private static Resources resources = Resources.getInstance("postvisualization");

    private static double MAX_POTENTIAL = 1000.0;
    private static double CONTOUR_STEP = 2.0;
    private static double CONTOUR_THINKNESS = 0.2;

    private SimulationModel model;
    private BufferedImage obstacleDensity = null;
    private BufferedImage potentialFieldImage = null;
    private ColorHelper colorHelper;
    private Color lastDensityColor = null;
    private int topographyId;
    private AgentRender agentRender;

    public SimulationRenderer(final SimulationModel model) {
        super(model);
        this.model = model;
        this.topographyId = -1;
        this.colorHelper = new ColorHelper(40);
        this.agentRender = new AgentRender(model);
    }

    @Override
    protected void renderPreTransformation(Graphics2D graphics2D, int width, int height) {
        if (model.isFloorFieldAvailable() && (model.config.isShowTargetPotentialField() || model.config.isShowPotentialField())) {
            synchronized (model) {
	            renderPotentialFieldOnViewport(graphics2D,
                        0, 0,
		                (int)(Math.min(model.getTopographyBound().width, model.getViewportBound().width) * model.getScaleFactor()),
                        (int)(Math.min(model.getTopographyBound().height, model.getViewportBound().height) * model.getScaleFactor()));
            }
        }
        super.renderPreTransformation(graphics2D, width, height);
    }

    @Override
    public void renderPostTransformation(final Graphics2D graphics, final int width, final int height) {
        graphics.setColor(Color.BLACK);

        // if there is no potential field than draw the default background (white)
        // otherwise do not overdraw the potential field!!!
        if (!model.isFloorFieldAvailable() || !(model.config.isShowTargetPotentialField() || model.config.isShowPotentialField())) {
            super.renderPostTransformation(graphics, width, height);
        }

        if (model.config.isShowDensity()) {
            renderDensity(graphics);
        }

        if (model.config.isShowGrid()) {
            renderGrid(graphics);
        }

        if (model.config.isShowObstacles()) {
            renderScenarioElement(model.getTopography().getObstacles(), graphics, model.config.getObstacleColor());
        }

        if (model.config.isShowStairs()) {
            renderStairs(model.getTopography().getStairs(), graphics, model.config.getStairColor());
        }

		if (model.config.isShowTargets()) {
			renderScenarioElement(model.getTopography().getTargets(), graphics, model.config.getTargetColor());
		}

        if (model.config.isShowSources()) {
            renderScenarioElement(model.getTopography().getSources(), graphics, model.config.getSourceColor());
        }

        if (model.isVoronoiDiagramAvailable() && model.isVoronoiDiagramVisible()) {
            renderVoronoiDiagram(graphics, model.getVoronoiDiagram());
        }

        renderSimulationContent(graphics);

        if (model.isElementSelected()) {
            renderSelectionBorder(graphics);
        }

        if (model.isSelectionVisible()) {
            renderSelectionShape(graphics);
        }

        if (hasLogo() && model.config.isShowLogo()) {
            renderLogo(graphics, model.getScaleFactor(), height);
        }

        graphics.dispose();
    }

    protected void renderTrajectory(final Graphics2D g, final java.util.List<VPoint> points, final Agent pedestrain) {
        renderTrajectory(g, points.stream(), pedestrain);
    }

    protected void renderTrajectory(final Graphics2D g, final Stream<VPoint> points, final Agent pedestrain) {
        Color color = g.getColor();
        Stroke stroke = g.getStroke();

        if (model.isElementSelected() && model.getSelectedElement().equals(pedestrain)) {
            g.setColor(Color.MAGENTA);
            g.setStroke(new BasicStroke(getLineWidth() / 2.0f));
        } else {
            g.setStroke(new BasicStroke(getLineWidth() / 4.0f));
        }

        Path2D.Double path = new Path2D.Double();
        path.moveTo(pedestrain.getPosition().getX(), pedestrain.getPosition().getY());
        points.forEachOrdered(
                p -> path.lineTo(p.getX(), p.getY()));

        draw(path, g);
        g.setColor(color);
        // g.setStroke(stroke);
    }

    protected void renderTriangulation(final Graphics2D g, final Collection<VTriangle> triangleList) {
        g.setColor(Color.GRAY);
        g.setStroke(new BasicStroke(getGridLineWidth()));
        triangleList.stream().forEach(triangle -> g.draw(triangle));
    }

    private void renderDensity(final Graphics2D g) {
        CLGaussianCalculator densityCalculator = new CLGaussianCalculator(model, model.config.getDensityScale());
		/*
		 * if (obstacleDensity == null || !model.config.getDensityColor().equals(lastDensityColor)
		 * || model.getTopographyId() != topographyId) {
		 * lastDensityColor = model.config.getDensityColor();
		 * //densityCalculator.setColor(lastDensityColor);
		 * obstacleDensity =
		 * densityCalculator.getObstacleDensityImage(model.config.getDensityStandardDerivation());
		 * }
		 */
		/*
		 * BufferedImage pedestrianDensity =
		 * densityCalculator.getPedestrianDensityImage(model.config.getDensityStandardDerivation(),
		 * model.config.getPedestrianTorso());
		 */

        BufferedImage densityImage = densityCalculator.getDensityImage();
	    Rectangle2D.Double bound = model.getTopographyBound();
		g.translate(bound.getX(), bound.getY());
        g.scale(1.0 / model.config.getDensityScale(), 1.0 / model.config.getDensityScale());
        g.drawImage(densityImage, 0, 0, null);
        // g.drawImage(pedestrianDensity, 0, 0, null);
	    g.scale(model.config.getDensityScale(), model.config.getDensityScale());
	    g.translate(-bound.getX(), -bound.getY());
        densityCalculator.destroy();
    }

    private void renderPotentialFieldOnViewport(final Graphics2D g, final int xPos, final int yPos, final int width, final int height) {

    	logger.info("resolution = " + width + ", " + height);
		/*
		 * This calculation we need since the viewport.y = 0 if the user scrolls to the bottom
		 */
		VRectangle bound = new VRectangle(model.getTopographyBound());
        Rectangle2D.Double viewportBound = model.getViewportBound();
        double dy = model.getTopographyBound().getHeight() - viewportBound.getHeight();

        int startX = (int) (viewportBound.getX() * model.getScaleFactor());
        int startY = (int) (Math.max((dy - viewportBound.getY()), 0) * model.getScaleFactor());

        potentialFieldImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int x = 0; x < potentialFieldImage.getWidth(); x++) {
            for (int y = 0; y < potentialFieldImage.getHeight(); y++) {
                Color c;
                VPoint pos = new VPoint(
		                viewportBound.getMinX() + x / model.getScaleFactor(),
		                viewportBound.getMinY() + (potentialFieldImage.getHeight() - 1 - y) / model.getScaleFactor());

                if(bound.contains(pos)) {
	                double potential = (double)model.getPotentialField().apply(pos);

	                if (potential >= MAX_POTENTIAL) {
		                c = model.config.getObstacleColor();
	                } else if (potential % CONTOUR_STEP <= CONTOUR_THINKNESS) {
		                c = Color.BLACK;
	                } else {
		                c = colorHelper.numberToColor(potential % 100);
	                }
	                potentialFieldImage.setRGB(x, y, c.getRGB());
                }
            }
        }
        g.drawImage(potentialFieldImage, xPos, yPos, null);
    }

	private void renderPotentialField(final Graphics2D g, final int xPos, final int yPos, final int width, final int height) {

		/*
		 * This calculation we need since the viewport.y = 0 if the user scrolls to the bottom
		 */
		VRectangle bound = new VRectangle(model.getTopographyBound());
		Rectangle2D.Double viewportBound = model.getViewportBound();
		double dy = model.getTopographyBound().getHeight() - viewportBound.getHeight();

		int startX = (int) (viewportBound.getX() * model.getScaleFactor());
		int startY = (int) (Math.max((dy - viewportBound.getY()), 0) * model.getScaleFactor());

		potentialFieldImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);


		for (int x = 0; x < potentialFieldImage.getWidth(); x++) {
			for (int y = 0; y < potentialFieldImage.getHeight(); y++) {
				Color c;
				VPoint pos = new VPoint(
						bound.getMinX() + x / model.getScaleFactor(),
						bound.getMinY() + bound.getHeight() - y / model.getScaleFactor());

				if(bound.contains(pos)) {
					double potential = (double)model.getPotentialField().apply(pos);

					if (potential >= MAX_POTENTIAL) {
						c = model.config.getObstacleColor();
					} else if (potential % CONTOUR_STEP <= CONTOUR_THINKNESS) {
						c = Color.BLACK;
					} else {
						c = colorHelper.numberToColor(potential % 100);
					}
					potentialFieldImage.setRGB(x, y, c.getRGB());
				}
			}
		}
		g.drawImage(potentialFieldImage, xPos, yPos, null);
	}

    protected abstract void renderSimulationContent(final Graphics2D g);

    private float getGridLineWidth() {
        return (float) (0.5 / model.getScaleFactor());
    }

    public AgentRender getAgentRender() {
        return agentRender;
    }

    public void setAgentRender(AgentRender agentRender) {
        this.agentRender = agentRender;
    }

    public Color getPedestrianColor(@NotNull final Agent agent) {
	    int targetId = agent.hasNextTarget() ? agent.getNextTargetId() : -1;
	    if (model.config.isUseRandomPedestrianColors()) {
		   return model.config.getRandomColor(agent.getId());
	    }

	    return model.config.getColorByTargetId(targetId)
			    .orElseGet(model.config::getPedestrianColor);
    }
}