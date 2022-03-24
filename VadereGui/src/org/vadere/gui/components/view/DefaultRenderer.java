package org.vadere.gui.components.view;

import org.jetbrains.annotations.NotNull;
import org.vadere.gui.components.model.IDefaultModel;
import org.vadere.meshing.mesh.gen.MeshRenderer;
import org.vadere.meshing.mesh.inter.IMesh;
import org.vadere.state.scenario.*;
import org.vadere.util.config.VadereConfig;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.geometry.shapes.Vector2D;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VPolygon;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.math.MathUtil;
import org.vadere.util.data.cellgrid.CellGrid;
import org.vadere.util.voronoi.Face;
import org.vadere.util.voronoi.HalfEdge;
import org.vadere.util.voronoi.RectangleLimits;
import org.vadere.util.voronoi.VoronoiDiagram;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Collection;

/**
 * @author Benedikt Zoennchen
 */
public abstract class DefaultRenderer {

	private IDefaultModel defaultModel;
	private BufferedImage logo;
	private static final double rotNeg90 = - Math.PI /2;
	private boolean renderNodes = VadereConfig.getConfig().getBoolean("Gui.showNodes");
	private double nodeRadius = VadereConfig.getConfig().getDouble("Gui.node.radius");

	/**
	 * <p>Default constructor.</p>
	 *
	 * @param defaultModel
	 */
	public DefaultRenderer(@NotNull final IDefaultModel defaultModel) {
		this.defaultModel = defaultModel;
		this.logo = null;
	}

	/**
	 * <p></p>
	 *
	 * @param targetGraphics2D
	 * @param width
	 * @param height
	 */
	public void render(final Graphics2D targetGraphics2D, final int width, final int height) {
		synchronized (defaultModel) {
			render(targetGraphics2D, 0, 0, width, height);
		}
	}

	public void render(final Graphics2D targetGraphics2D, final int x, final int y, final int width, final int height) {
		synchronized (defaultModel) {
			if (defaultModel.getTopographyBound() != null){
				targetGraphics2D.drawImage(renderImage(width, height), x, y, null);
			}
			targetGraphics2D.dispose();
		}
	}

	public void renderGraphics(final Graphics2D targetGraphics2D, final int width, final int height) {
		synchronized (defaultModel) {
			targetGraphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			// (1) clear background
			targetGraphics2D.setColor(Color.GRAY);
			//targetGraphics2D.fill();
			targetGraphics2D.fillRect(0, 0, width, height);

			// (2) render everything which can be rendered before the transformation
			renderPreTransformation(targetGraphics2D, width, height);

			// (3)
			transformGraphics(targetGraphics2D);

			// (4) render everything which can be rendered after the transformation
			renderPostTransformation(targetGraphics2D, width, height);
		}
	}

	public BufferedImage renderImage(final int width, final int height) {
		synchronized (defaultModel) {
			BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
			Graphics2D bufferGraphics2D = (Graphics2D) image.getGraphics();
			renderGraphics(bufferGraphics2D, width, height);
			return image;
		}
	}

	public void setLogo(final BufferedImage logo) {
		this.logo = logo;
	}

	protected void renderPreTransformation(final Graphics2D graphics2D, final int width, final int height) {}

	protected void renderPostTransformation(final Graphics2D graphics2D, final int width, final int height) {
		synchronized (defaultModel) {
			graphics2D.setColor(Color.WHITE);
			Rectangle2D.Double topographyBound = defaultModel.getTopographyBound();

			// Maybe, the simulation thread has not populated all required data yet,
			// but the rendering process has already been triggered.
			if (topographyBound != null) {
				fill(new VRectangle(
						topographyBound.getMinX() + defaultModel.getBoundingBoxWidth(),
						topographyBound.getMinY() + defaultModel.getBoundingBoxWidth(),
						(topographyBound.getWidth() - defaultModel.getBoundingBoxWidth() * 2),
						(topographyBound.getHeight() - defaultModel.getBoundingBoxWidth() * 2)), graphics2D);
			}
		}
	}

	protected void transformGraphics(final Graphics2D graphics2D) {
		synchronized (defaultModel) {
			Rectangle2D.Double topographyBound = defaultModel.getTopographyBound();
			Rectangle2D.Double viewportBound = defaultModel.getViewportBound();

			// Maybe, the simulation thread has not populated all required data yet,
			// but the rendering process has already been triggered.
			if (topographyBound != null && viewportBound != null) {
				mirrowHorizonzal(graphics2D, (int) (topographyBound.getHeight() * defaultModel.getScaleFactor()));
				graphics2D.scale(defaultModel.getScaleFactor(), defaultModel.getScaleFactor());

				// We need this calculation because maybe the viewport.y = 0 (i.e., the user scrolled to the bottom)
				double dy = topographyBound.getHeight() - viewportBound.getHeight();

				graphics2D.translate(-viewportBound.getX(), Math.max((dy - viewportBound.getY()), - viewportBound.getY()));
			}
		}
	}

	protected void renderScenarioElement(final Iterable<? extends ScenarioElement> elements, final Graphics2D g,
			final Color color) {
		final Color tmpColor = g.getColor();

		for (ScenarioElement element : elements) {
			VShape shape = element.getShape();
			g.setColor(color);
			fill(shape, g);
			if(renderNodes) {
				for(VPoint node : shape.getPath()) {
					g.setColor(Color.RED);
					g.fill(new VCircle(node, nodeRadius));
				}
			}
		}

		g.setColor(tmpColor);
	}

	public static void fill(@NotNull final Shape shape, @NotNull final Graphics2D g) {
		if(shape instanceof VCircle) {
			g.fill(toPolygon((VCircle) shape));
		}
		else {
			g.fill(shape);
		}
	}

	public static void draw(@NotNull final Shape shape, @NotNull final Graphics2D g) {
		if(shape instanceof VCircle) {
			g.draw(toPolygon((VCircle) shape));
		}
		else {
			g.draw(shape);
		}
	}

	private static VPolygon toPolygon(final VCircle circle) {
		int n = 15;
		double alpha = 2 * Math.PI / n;
		VPoint p = new VPoint(0, circle.getRadius());

		Path2D.Double path = new Path2D.Double();
		VPoint center = circle.getCenter();

		path.moveTo(center.x + p.x, center.y + p.y);
		for(int i = 1; i < n; i++) {
			p = p.rotate(alpha);
			path.lineTo(center.x + p.x, center.y + p.y);
			///path.moveTo(pointList.get(i).x, pointList.get(i).y);
		}

		//path.closePath();

		return new VPolygon(path);
	}

	protected  void renderStairs(final Iterable<Stairs> stairs, final Graphics2D g,
								 final Color color){
		for (Stairs s : stairs) {
			renderStair(s, g, color);
		}
	}

	public static Area getStairShapeWithThreads(Stairs stairs){
		Area hatchArea = new Area(stairs.getShape());
		double stroke = stairs.getTreadDepth() * 0.05;
		double halfTreadDepth = stairs.getTreadDepth()/2;

		for (Stairs.Tread tread : stairs.getTreads()) {

			VLine tLine = tread.treadline;
			Vector2D vec = tLine.asVector();
			vec = vec.normalize(stroke);
			vec = vec.rotate(rotNeg90);
			Vector2D trans = vec.normalize(halfTreadDepth);
			Path2D p = new Path2D.Double();
			p.moveTo(tLine.x1, tLine.y1);
			p.lineTo(tLine.x2, tLine.y2);
			p.lineTo(tLine.x2 + vec.x, tLine.y2  + vec.y);
			p.lineTo(tLine.x1 + vec.x, tLine.y1 + vec.y);
			p.closePath();

			p.transform(AffineTransform.getTranslateInstance(trans.x, trans.y));
			hatchArea.subtract(new Area(p));
		}
		return hatchArea;
	}

	protected void renderStair(ScenarioElement element, final Graphics2D graphics, Color color){
		Stairs stairs = (Stairs) element;

		final Color tmpColor = graphics.getColor();
		graphics.setColor(Color.black);
		fill(stairs.getShape(), graphics);

		Area hatchArea = getStairShapeWithThreads(stairs);

		graphics.setColor(color);
		fill(hatchArea, graphics);
		graphics.setColor(tmpColor);
	}

	protected void renderFilledShape(ScenarioElement element, final Graphics2D graphics, Color color){
		final Color tmpColor = graphics.getColor();
		graphics.setColor(color);
		fill(element.getShape(), graphics);
		if(renderNodes) {
			for(VPoint node : element.getShape().getPath()) {
				graphics.setColor(Color.RED);
				graphics.fill(new VCircle(node, nodeRadius));
			}
		}
		graphics.setColor(tmpColor);
	}

	protected void renderMeasurementAreas(final Iterable<? extends ScenarioElement> elements, final Graphics2D g,
										  final Color color){
		for (ScenarioElement e : elements){
			renderMeasurementArea(e, g, color);
		}
	}

	protected void renderMeasurementArea(ScenarioElement element, final Graphics2D graphics, Color color){
		final Color tmpColor = graphics.getColor();
		MeasurementArea area = (MeasurementArea) element;
		graphics.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(),
				defaultModel.getConfig().getMeasurementAreaAlpha()));
		if (area.getShape() instanceof VPolygon){
			VPolygon p = (VPolygon) area.getShape();
			if (p.getPoints().size() == 2){
				graphics.setStroke(new BasicStroke(3*getLineWidth()));
				graphics.draw(p);
			}
		}
		fill(element.getShape(), graphics);

		graphics.setColor(tmpColor);
	}

	protected void renderAerosolClouds(final Iterable<? extends ScenarioElement> elements, final Graphics2D g,
										  final Color color){
		for (ScenarioElement e : elements){
			renderAerosolCloud(e, g, color);
		}
	}

	protected void renderAerosolCloud(ScenarioElement element, final Graphics2D graphics, Color color){
		final Color tmpColor = graphics.getColor();
		AerosolCloud cloud = (AerosolCloud) element;
		float maxAlpha = defaultModel.getConfig().getAerosolCloudAlphaMax();
		float minAlpha = 0; // no lower threshold

		double maxPathogenConcentration = defaultModel.getConfig().getAerosolCloudMaxPathogenConcentration();
		double pathogenConcentration = cloud.getPathogenConcentration();
		pathogenConcentration = Math.min(pathogenConcentration, maxPathogenConcentration); // make sure that pathogenConcentration is not exceeded
		int currentAlpha = (int) ((pathogenConcentration / maxPathogenConcentration) * (maxAlpha - minAlpha) + minAlpha);

		graphics.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), currentAlpha));
		if (cloud.getShape() instanceof VPolygon){
			VPolygon p = (VPolygon) cloud.getShape();
			if (p.getPoints().size() == 2){
				graphics.setStroke(new BasicStroke(3*getLineWidth()));
				graphics.draw(p);
			}
		}
		fill(element.getShape(), graphics);

		graphics.setColor(tmpColor);
	}

	protected void renderAllDroplets(final Iterable<? extends ScenarioElement> elements, final Graphics2D g,
									 final Color color){
		for (ScenarioElement e : elements){
			renderDroplets(e, g, color);
		}
	}

	protected void renderDroplets(ScenarioElement element, final Graphics2D graphics, Color color){
		final Color tmpColor = graphics.getColor();
		Droplets droplets = (Droplets) element;
		int currentAlpha = 100;

		graphics.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), currentAlpha));
		if (droplets.getShape() instanceof VPolygon){
			VPolygon p = (VPolygon) droplets.getShape();
			if (p.getPoints().size() == 2){
				graphics.setStroke(new BasicStroke(3*getLineWidth()));
				graphics.draw(p);
			}
		}
		fill(element.getShape(), graphics);

		graphics.setColor(tmpColor);
	}

	protected void renderSelectionShape(final Graphics2D graphics) {
		graphics.setColor(defaultModel.getMouseSelectionMode().getSelectionColor());
		graphics.setStroke(new BasicStroke(getSelectionBorderLineWidth()));
		draw(defaultModel.getSelectionShape(), graphics);
	}

	protected void renderSelectionBorder(final Graphics2D graphics) {
		graphics.setColor(Color.MAGENTA);
		graphics.setStroke(new BasicStroke(getSelectedShapeBorderLineWidth()));
		draw(defaultModel.getSelectedElement().getShape(), graphics);
	}

	protected void renderLogo(final Graphics2D graphics, double scale, double height) {
		Rectangle2D.Double topographyBound = defaultModel.getTopographyBound();
		Rectangle2D.Double viewportBound = defaultModel.getViewportBound();

		// Maybe, the simulation thread has not populated all required data yet,
		// but the rendering process has already been triggered.
		if (topographyBound != null && viewportBound != null) {
			// We need this calculation because maybe the viewport.y = 0 (i.e., the user scrolled to the bottom)
			double dy = topographyBound.getHeight() - viewportBound.getHeight();


			// undo the viewport translation
			graphics.translate(viewportBound.getX(),
					-Math.max((dy - viewportBound.getY()), -viewportBound.getY()));
			graphics.scale(1.0 / scale, 1.0 / scale);
			graphics.translate(0, +topographyBound.getHeight() * defaultModel.getScaleFactor());
			graphics.scale(1.0, -1.0);

			graphics.translate(0, 2.0);
			graphics.scale(0.25, 0.25);

			graphics.drawImage(logo, 0, 0, null);

			// undo all scaling and translation
			graphics.scale(1.0 / 0.25, 1.0 / 0.25);
			graphics.translate(0, 1.0 / 2.0);
			graphics.scale(1.0, -1.0);
			graphics.translate(0, -topographyBound.getHeight() * defaultModel.getScaleFactor());
			graphics.translate(-viewportBound.getX(),
					Math.max((dy - viewportBound.getY()), -viewportBound.getY()));
		}
	}

	protected boolean hasLogo() {
		return logo != null;
	}

	protected static void paintPedestrianIds(final Graphics2D g, Collection<Agent> peds) {
		peds.forEach(ped -> DefaultRenderer.paintAgentId(g, ped));
	}

	protected static void paintAgentId(final Graphics2D g, final Agent p) {
		// draw pedestrian ids
		Color c = g.getColor();
		g.scale(1, -1);
		g.setColor(new Color(255, 127, 0));
		Font theFont = new Font("Arial", Font.BOLD, 1);
		g.setFont(theFont);
		AffineTransform affineTransform = g.getTransform();
		String number = Integer.toString(p.getId());
		int digitCount = number.length();
		double scale = 0.4 / Math.sqrt(digitCount);
		g.scale(scale, scale);
		g.drawString(number, (float) ((p.getPosition().x - 0.11 * digitCount) * 1 / scale),
				(float) -((p.getPosition().y - (0.14 - 0.01 * digitCount)) * 1 / scale));
		g.setTransform(affineTransform);
		g.scale(1, -1);
		g.setColor(c);
	}

	protected void renderPedestrianInOutGroup(final Graphics2D g, Pedestrian pedestrian) {
		Color groupMembershipColor = defaultModel.getConfig().getGroupMembershipColor(pedestrian.getGroupMembership());
		g.setColor(groupMembershipColor);
		g.setStroke(new BasicStroke(getSelectedShapeBorderLineWidth()));
		draw(pedestrian.getShape(), g);
	}


	protected static void drawArrow(Graphics2D g2, double theta, double x0, double y0) {
		float barb = 0.3f;
		double phi = Math.PI / 6;
		double x = x0 - barb * Math.cos(theta + phi);
		double y = y0 - barb * Math.sin(theta + phi);
		Stroke drawingStroke = new BasicStroke(0.05f);
		Color color = g2.getColor();
		g2.setColor(Color.BLACK);
		g2.setStroke(drawingStroke);
		g2.draw(new Line2D.Double(x0, y0, x, y));
		x = x0 - barb * Math.cos(theta - phi);
		y = y0 - barb * Math.sin(theta - phi);
		g2.draw(new Line2D.Double(x0, y0, x, y));
		g2.setColor(color);
	}

	protected void renderGrid(final Graphics2D g) {
		g.setColor(Color.LIGHT_GRAY);
		g.setStroke(new BasicStroke(getGridLineWidth()));
		Rectangle2D.Double bound = defaultModel.getTopographyBound();

		// Maybe, the simulation thread has not populated all required data yet,
		// but the rendering process has already been triggered.
		if (bound != null) {
			for (double y = bound.getMinY(); y <= bound.getMaxY() + 0.01; y +=
					defaultModel.getGridResolution()) {
				for (double x = bound.getMinX(); x <= bound.getMaxX() + 0.01; x +=
						defaultModel.getGridResolution()) {
					draw(new Line2D.Double(x - defaultModel.getGridResolution() * 0.2, y,
							x + defaultModel.getGridResolution()
									* 0.2, y), g);
					draw(new Line2D.Double(x, y - defaultModel.getGridResolution() * 0.2, x,
							y + defaultModel.getGridResolution()
									* 0.2), g);
				}
			}
		}
	}

	protected float getLineWidth() {
		return (float) (1.0 / defaultModel.getScaleFactor());
	}

    /*protected void paintPotentialField(final Graphics2D g, final Function<VPoint, Double> potentialField, final VRectangle bound) {
        float norm;

        if (potentialField == null) {
            return;
        }

        double pixToW = 1.0 / defaultModel.getScaleFactor();
        double potOld, maxPotential = 0, invPotential;
        double maxBorder = Math.max(bound.getWidth(), bound.getHeight());



        for (double y = bound.getMinY(); y < bound.getMaxY(); y += pixToW) {
            for (double x = bound.getMinX(); x < bound.getMaxX(); x += pixToW) {
                potOld = potentialField.apply(new VPoint(x,y));

                if ((potOld > maxPotential) && (potOld != Double.MAX_VALUE)) {
                    maxPotential = potOld;
                }
            }
        }

        for (double y = bound.getMinY(); y < bound.getMaxY(); y += pixToW) {
            for (double x = bound.getMinX(); x < bound.getMaxX(); x += pixToW) {
                double contourDist = 0.02f;
                boolean isContourLine = false;
                double potential[] = new double[3];

                if (!isContourLine) {
                    invPotential = 1 - potential[0];

                    if (invPotential < 1 / 7.0) {
                        norm = (float) (invPotential * 6.0);
                        g.setColor(new Color(0.14285f - norm * 7 / 6.0f * 0.14285f, 0.14285f - norm * 7 / 6.0f
                                * 0.14285f, norm + 0.14285f));
                    }
                    else if (invPotential < 2 / 7.0) {
                        norm = (float) ((invPotential - 1.0 / 7.0) * 6.0 + 0.14285f);
                        g.setColor(new Color(norm, 0, 1.0f));
                    }
                    else if (invPotential < 3 / 7.0) {
                        norm = (float) ((invPotential - 2.0 / 7.0) * 7.0);
                        g.setColor(new Color(1.0f, 0, 1.0f - norm));
                    }
                    else if (invPotential < 4 / 7.0) {
                        norm = (float) ((invPotential - 3.0 / 7.0) * 7.0);
                        g.setColor(new Color(1.0f, norm, 0));
                    }
                    else if (invPotential < 5 / 7.0) {
                        norm = (float) ((invPotential - 4.0 / 7.0) * 7.0);
                        g.setColor(new Color(1.0f - norm, 1.0f, 0));
                    }
                    else if (invPotential < 6 / 7.0) {
                        norm = (float) ((invPotential - 5.0 / 7.0) * 7.0);
                        g.setColor(new Color(0, 1.0f, norm));
                    }
                    else {
                        norm = (float) ((invPotential - 6.0 / 7.0) * 7.0);
                        g.setColor(new Color(norm, 1.0f, 1.0f));
                    }
                }
                else {
                    g.setColor(new Color(0.3f, 0.3f, 0.3f));
                }

                g.fill(new Rectangle2D.Double(x, y, pixToW, pixToW));
            }
        }
    }*/

	protected void paintPotentialField(final Graphics2D g, final CellGrid potentialField) {
		float norm;

		if (potentialField == null) {
			return;
		}

		/* Scale factor to transform pixel to potential field coordinates. */
		double pixToW = 1.0 / defaultModel.getScaleFactor();
		double potOld, maxPotential = 0, invPotential;
		/* Maximum of potential field dimensions. */
		double maxBorder = Math.max(potentialField.getHeight(), potentialField.getWidth());

		Point2D.Double coord = new Point2D.Double();

		/*
		 * Get maximum of potential field which is < Double.MAX_VALUE. For that,
		 * scenario started through all pixels and check corresponding potential.
		 */
		for (coord.y = 0; coord.y < potentialField.getHeight(); coord.y += pixToW) {
			for (coord.x = 0; coord.x < potentialField.getWidth(); coord.x += pixToW) {
				potOld = potentialField.getValue(potentialField.getNearestPoint(coord.x, coord.y)).potential;

				if ((potOld > maxPotential) && (potOld != Double.MAX_VALUE)) {
					maxPotential = potOld;
				}
			}
		}

		/*
		 * Colorize all pixel according to the corresponding potential value and
		 * contour line membership.
		 */
		for (coord.y = 0; coord.y < potentialField.getHeight(); coord.y += pixToW) {
			for (coord.x = 0; coord.x < potentialField.getWidth(); coord.x += pixToW) {
				Point p = potentialField.getNearestPoint(coord.x, coord.y);

				double contourDist = 0.02f;
				boolean isContourLine = false;
				double potential[] = new double[3];

				/*
				 * Within this block, check if current pixel belongs to a
				 * contour line. Do not exceed bounds.
				 */
				if ((p.x > 0) && (p.x < potentialField.getNumPointsX() - 1) && (p.y > 0)
						&& (p.y < potentialField.getNumPointsY() - 1)) {

					/*
					 * Retrieve potentials of the current pixel as well as its
					 * upper and right neighbor pixels.
					 */
					potential[0] = potentialField.getValue(p.x, p.y).potential;
					potential[1] = potentialField.getValue(p.x + 1, p.y).potential;
					potential[2] = potentialField.getValue(p.x, p.y + 1).potential;

					/* Map potential values by a sigmoidal transfer function. */
					for (int i = 0; i < 3; ++i) {
						/*
						 * At the targets, negative potential exist. Cut these
						 * here.
						 */
						if (potential[i] < 0) {
							potential[i] = 0.0f;
						}

						/*
						 * Cut the potential values Double.MAX_VALUE to the next
						 * lower maximal potential.
						 */
						if (potential[i] > maxPotential) {
							potential[i] = maxPotential;
						}

						/*
						 * Apply the transfer function to the potential value.
						 * The scale factor of 2/maxBorder was found by trial
						 * and leads to similar contrasts of the drawn potential
						 * field even if very different parameter settings for
						 * the potential field are chosen, e.g. for
						 * 'obstacleBodyPotential'. The subtraction by 0.5 and
						 * final scaling by 2.0 map the results of the sigmoid
						 * function to the range [0;1].
						 */
						potential[i] = (MathUtil.sigmoid(potential[i] * 2 / maxBorder) - 0.5) * 2.0;
					}

					/*
					 * If one of the neighbor pixel are mapped to another 'part'
					 * of the subdivided potential value space, the pixel is
					 * marked as part of a contour line.
					 */
					if (((int) (potential[0] / contourDist) != (int) (potential[1] / contourDist))
							|| ((int) (potential[0] / contourDist) != (int) (potential[2] / contourDist))) {
						isContourLine = true;
					}
				}

				/*
				 * A color value for the pixel is chosen according to the mapped
				 * potential value. If the pixel belongs to a contour line, this
				 * step is skipped.
				 */
				if (!isContourLine) {
					invPotential = 1 - potential[0];

					/*
					 * The following code subdivides the mapped potential value
					 * which is now in the range [0;1] into 7 parts. For each
					 * part a color gradient is generated which fades from the
					 * current main color to the main color of the next part.
					 * The first part: the invPotential is in the range
					 * [0;1/7.0]. Multiply invPotential with 6 and use the
					 * result as part of the blue part of the color. When
					 * invPotential is 1/7.0, blue becomes 1.0.
					 */
					if (invPotential < 1 / 7.0) {
						/*
						 * Do no multiply with 7 to not to start at black but
						 * gray.
						 */
						norm = (float) (invPotential * 6.0);
						/*
						 * When norm==0, red, green and blue are 0.14285f. When
						 * norm becomes 6/7, red and green are faded out to 0,
						 * and blue becomes 1.0.
						 */
						g.setColor(new Color(0.14285f - norm * 7 / 6.0f * 0.14285f, 0.14285f - norm * 7 / 6.0f
								* 0.14285f, norm + 0.14285f));
					}
					/*
					 * Part 2: Keep blue 1.0f and use invPotential to blend red
					 * to 1.0.
					 */
					else if (invPotential < 2 / 7.0) {
						norm = (float) ((invPotential - 1.0 / 7.0) * 6.0 + 0.14285f);
						g.setColor(new Color(norm, 0, 1.0f));
					}
					/*
					 * Part 3: Keep red 1.0f and use invPotential to blend blue
					 * to 0.
					 */
					else if (invPotential < 3 / 7.0) {
						norm = (float) ((invPotential - 2.0 / 7.0) * 7.0);
						g.setColor(new Color(1.0f, 0, 1.0f - norm));
					}
					/*
					 * Part 4: Keep blue 0 and use invPotential to blend green
					 * to 1.0.
					 */
					else if (invPotential < 4 / 7.0) {
						norm = (float) ((invPotential - 3.0 / 7.0) * 7.0);
						g.setColor(new Color(1.0f, norm, 0));
					}
					/*
					 * Part 5: Keep green 1.0 and use invPotential to blend red
					 * to 0.0.
					 */
					else if (invPotential < 5 / 7.0) {
						norm = (float) ((invPotential - 4.0 / 7.0) * 7.0);
						g.setColor(new Color(1.0f - norm, 1.0f, 0));
					}
					/*
					 * Part 6: Keep red 0 and use invPotential to blend blue to
					 * 1.0.
					 */
					else if (invPotential < 6 / 7.0) {
						norm = (float) ((invPotential - 5.0 / 7.0) * 7.0);
						g.setColor(new Color(0, 1.0f, norm));
					}
					/*
					 * Part 7: Keep blue 1.0 and use invPotential to blend red
					 * to 1.0.
					 */
					else {
						norm = (float) ((invPotential - 6.0 / 7.0) * 7.0);
						g.setColor(new Color(norm, 1.0f, 1.0f));
					}
				}
				/* If pixel belongs to a contour line, use color grey. */
				else {
					g.setColor(new Color(0.3f, 0.3f, 0.3f));
				}

				/* Draw rectangle as pixel to image. */
				fill(new Rectangle2D.Double(coord.x, coord.y, pixToW, pixToW), g);
			}
		}
	}

	protected void renderMesh(
			@NotNull final Graphics2D g,
			@NotNull final IMesh<?, ?, ?> mesh,
			@NotNull final VRectangle bound) {

		MeshRenderer<?, ?, ?> meshRenderer = new MeshRenderer<>(mesh, false);

		meshRenderer.renderPostTransform(g, bound);
		//meshRenderer.renderGraphics(g, bound);
	}

	protected void renderVoronoiDiagram(final Graphics2D g, final VoronoiDiagram voronoiDiagram) {
		synchronized (voronoiDiagram) {
			if (voronoiDiagram != null) {
				g.setColor(Color.BLACK);
				g.setStroke(new BasicStroke((float) 0.05));
				RectangleLimits limits = voronoiDiagram.getLimits();
				VRectangle rectangle = new VRectangle(limits.xLow, limits.yLow, limits.xHigh - limits.xLow,
						limits.yHigh - limits.yLow);
				g.draw(rectangle);
			}

			// check whether jts exists and has computed faces yet
			if (voronoiDiagram != null && voronoiDiagram.getFaces() != null) {

				for (Face f : voronoiDiagram.getFaces()) {

					boolean go = true;
					boolean closed = false;
					HalfEdge last = f.getOuterComponent();
					HalfEdge next = last.getNext();
					HalfEdge outerComponent = last;

					while (go) {
						if (next == null || last.getOrigin() == null) {
							go = false;
							closed = true;
						} else {

							draw(new Line2D.Double(last.getOrigin().x, last.getOrigin().y, next.getOrigin().x, next.getOrigin().y), g);

							if (next == outerComponent) {
								go = false;
							} else {
								last = next;
								next = next.getNext();
							}
						}
					}

					last = outerComponent;
					next = last.getPrevious();

					go = true;

					while (go && !closed) {
						if (next == null || next.getOrigin() == null) {
							go = false;
						} else {
							draw(new Line2D.Double(last.getOrigin().x, last.getOrigin().y, next.getOrigin().x, next.getOrigin().y), g);

							if (next == outerComponent) {
								go = false;
							} else {
								last = next;
								next = next.getPrevious();
							}
						}
					}
				}
			}
		}
	}


	private float getSelectedShapeBorderLineWidth() {
		return getLineWidth() * 2;
	}

	private float getSelectionBorderLineWidth() {
		return getLineWidth() / 4;
	}

	private static void mirrowHorizonzal(final Graphics2D graphics2D, final int height) {
		graphics2D.scale(1.0, -1.0);
		graphics2D.translate(0.0, -height);
	}

	private float getGridLineWidth() {
		return (float) (0.5 / defaultModel.getScaleFactor());
	}
}
