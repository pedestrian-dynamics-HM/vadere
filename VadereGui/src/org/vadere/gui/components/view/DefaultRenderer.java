package org.vadere.gui.components.view;

import org.vadere.gui.components.model.IDefaultModel;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.ScenarioElement;
import org.vadere.state.scenario.Stairs;
import org.vadere.util.geometry.Vector2D;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.math.MathUtil;
import org.vadere.util.potential.CellGrid;
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

public abstract class DefaultRenderer {

	private IDefaultModel defaultModel;
	private BufferedImage logo;
	private static final double rotNeg90 = - Math.PI /2;

	public DefaultRenderer(final IDefaultModel defaultModel) {
		this(defaultModel, true, false);
	}

	public DefaultRenderer(final IDefaultModel defaultModel, final boolean doubleBuffering,
			final boolean hideBoundingBoxBorder) {
		this.defaultModel = defaultModel;
		this.logo = null;
	}

	/**
	 * Render the content. If doublebuffering is true, the whole content will be drawn on a new
	 * image.
	 * Otherwise the content will be drawn on the graphics object directly.
	 * 
	 * @param targetGraphics2D
	 * @param width
	 * @param height
	 */
	public void render(final Graphics2D targetGraphics2D, final int width, final int height) {
		render(targetGraphics2D, 0, 0, width, height);
	}

	public void render(final Graphics2D targetGraphics2D, final int x, final int y, final int width, final int height) {

		 //if(doubleBuffering) {
		 targetGraphics2D.drawImage(renderImage(width, height), x, y, null);
         //} else {
		//targetGraphics2D.translate(x, y);
		//renderGraphics(targetGraphics2D, width, height);
		// }
		targetGraphics2D.dispose();
	}

	public void renderGraphics(final Graphics2D targetGraphics2D, final int width, final int height) {
		targetGraphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		targetGraphics2D.setColor(Color.GRAY);
		targetGraphics2D.fillRect(0, 0, width, height);

		renderPreTransformation(targetGraphics2D, width, height);

		transformGraphics(targetGraphics2D);

		renderPostTransformation(targetGraphics2D, width, height);
	}

	public BufferedImage renderImage(final int width, final int height) {
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics2D bufferGraphics2D = (Graphics2D) image.getGraphics();

		renderGraphics(bufferGraphics2D, width, height);
		return image;
	}

	public void setLogo(final BufferedImage logo) {
		this.logo = logo;
	}

	protected void renderPreTransformation(final Graphics2D graphics2D, final int width, final int height) {}

	protected void renderPostTransformation(final Graphics2D graphics2D, final int width, final int height) {
		graphics2D.setColor(Color.WHITE);
		graphics2D.fill(new VRectangle(defaultModel.getBoundingBoxWidth(),
				defaultModel.getBoundingBoxWidth(),
				(defaultModel.getTopographyBound().getWidth() - defaultModel.getBoundingBoxWidth() * 2),
				(defaultModel.getTopographyBound().getHeight() - defaultModel.getBoundingBoxWidth() * 2)));

	}

	protected void transformGraphics(final Graphics2D graphics2D) {
		Rectangle2D.Double topographyBound = defaultModel.getTopographyBound();
		mirrowHorizonzal(graphics2D, (int) (topographyBound.getHeight() * defaultModel.getScaleFactor()));
		graphics2D.scale(defaultModel.getScaleFactor(), defaultModel.getScaleFactor());

		/*
		 * This calculation we need since the viewport.y = 0 if the user scrolls to the bottom
		 */
		Rectangle2D.Double viewportBound = defaultModel.getViewportBound();
		double dy = topographyBound.getHeight() - viewportBound.getHeight();

		graphics2D.translate(-viewportBound.getX(), Math.max((dy - viewportBound.getY()), 0));
		// graphics2D.translate(+viewportBound.getX(), -Math.max((dy - viewportBound.getY()), 0));

	}

	protected void renderScenarioElement(final Iterable<? extends ScenarioElement> elements, final Graphics2D g,
			final Color color) {
		final Color tmpColor = g.getColor();
		g.setColor(color);

		for (ScenarioElement element : elements) {
			g.fill(element.getShape());
		}

		g.setColor(tmpColor);
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
		graphics.fill(stairs.getShape());

		Area hatchArea = getStairShapeWithThreads(stairs);

		graphics.setColor(color);
		graphics.fill(hatchArea);
		graphics.setColor(tmpColor);
	}

	protected void renderFilledShape(ScenarioElement element, final Graphics2D graphics, Color color){
		final Color tmpColor = graphics.getColor();
		graphics.setColor(color);
		graphics.fill(element.getShape());
		graphics.setColor(tmpColor);
	}

	protected void renderSelectionShape(final Graphics2D graphics) {
		graphics.setColor(defaultModel.getMouseSelectionMode().getSelectionColor());
		graphics.setStroke(new BasicStroke(getSelectionBorderLineWidht()));
		graphics.draw(defaultModel.getSelectionShape());
	}

	protected void renderSelectionBorder(final Graphics2D graphics) {
		graphics.setColor(Color.MAGENTA);
		graphics.setStroke(new BasicStroke(getSelectedShapeBorderLineWidth()));
		graphics.draw(defaultModel.getSelectedElement().getShape());
	}

	protected void renderLogo(final Graphics2D graphics, double scale, double height) {
		/*
		 * This calculation we need since the viewport.y = 0 if the user scrolls to the bottom
		 */
		double dy = defaultModel.getTopographyBound().getHeight() - defaultModel.getViewportBound().getHeight();

		graphics.translate(defaultModel.getViewportBound().getX(),
				-Math.max((dy - defaultModel.getViewportBound().getY()), 0));
		graphics.scale(1.0 / scale, 1.0 / scale);
		graphics.translate(0, +defaultModel.getTopographyBound().getHeight() * defaultModel.getScaleFactor());
		graphics.scale(1.0, -1.0);

		graphics.translate(0, 2.0);
		graphics.scale(0.25, 0.25);
		graphics.drawImage(logo, 0, 0, null);
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

		for (double y = 0; y <= defaultModel.getTopographyBound().height + 0.01; y +=
				defaultModel.getGridResolution()) {
			for (double x = 0; x <= defaultModel.getTopographyBound().width + 0.01; x +=
					defaultModel.getGridResolution()) {
				g.draw(new Line2D.Double(x - defaultModel.getGridResolution() * 0.2, y,
						x + defaultModel.getGridResolution()
								* 0.2,
						y));
				g.draw(new Line2D.Double(x, y - defaultModel.getGridResolution() * 0.2, x,
						y + defaultModel.getGridResolution()
								* 0.2));
			}
		}
	}

	protected float getLineWidth() {
		return (float) (2.0 / defaultModel.getScaleFactor());
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
				g.fill(new Rectangle2D.Double(coord.x, coord.y, pixToW, pixToW));
			}
		}
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
							g.draw(new Line2D.Double(last.getOrigin().x, last.getOrigin().y, next.getOrigin().x, next
									.getOrigin().y));

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
							g.draw(new Line2D.Double(last.getOrigin().x, last.getOrigin().y, next.getOrigin().x, next
									.getOrigin().y));

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

	private float getSelectionBorderLineWidht() {
		return getLineWidth() / 4;
	}

	private static void mirrowHorizonzal(final Graphics2D graphics2D, final int height) {
		graphics2D.scale(1, -1);
		graphics2D.translate(0, -height);
	}

	private float getGridLineWidth() {
		return (float) (0.5 / defaultModel.getScaleFactor());
	}
}
