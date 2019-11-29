package org.vadere.gui.postvisualization.view;

import org.vadere.gui.components.view.DefaultRenderer;
import org.vadere.gui.components.view.SimulationRenderer;
import org.vadere.gui.postvisualization.model.PostvisualizationModel;
import org.vadere.gui.postvisualization.model.TableTrajectoryFootStep;
import org.vadere.gui.renderer.agent.AgentRender;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.simulation.FootStep;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.logging.Logger;
import org.vadere.util.visualization.ColorHelper;

import java.awt.*;
import java.awt.geom.Path2D;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import tech.tablesaw.api.Row;
import tech.tablesaw.api.Table;

public class PostvisualizationRenderer extends SimulationRenderer {

	private static final double MIN_ARROW_LENGTH = 0.1;
	private static Logger logger = Logger.getLogger(PostvisualizationRenderer.class);
	private PostvisualizationModel model;

	private final Map<Integer, VPoint> lastPedestrianPositions;

	private final Map<Integer, VPoint> pedestrianDirections;

	private ColorHelper colorHelper;

	public PostvisualizationRenderer(final PostvisualizationModel model) {
		super(model);
		this.model = model;
		this.pedestrianDirections = new HashMap<>();
		this.lastPedestrianPositions = new HashMap<>();
		this.colorHelper = new ColorHelper(model.getStepCount());
	}

	public PostvisualizationModel getModel() {
		return model;
	}

	@Override
	protected void renderSimulationContent(final Graphics2D g) {
		this.colorHelper = new ColorHelper(model.getStepCount());
		renderPedestrians(g, null);
	}

	private void renderPedestrians(final Graphics2D g, final Color color) {
		if (!model.isEmpty()) {
			renderTrajectories(g);
		}
	}

	private void renderTrajectories(final Graphics2D g) {
		Color color = g.getColor();
		AgentRender agentRender = getAgentRender();

		// sorted (by ped id) agent table
		TableTrajectoryFootStep trajectories = model.getTrajectories();

		Table slice;
		if (model.config.isShowAllTrajectories()) {
			slice = model.getAppearedPedestrians();
		} else {
			slice = model.getAlivePedestrians();
		}

		Collection<Pedestrian> agents = model.getPedestrians();

		Map<Integer, Color> agentColors = new HashMap<>();
		agents.forEach(agent -> agentColors.put(agent.getId(),  getPedestrianColor(agent)));

		Color c = g.getColor();
		Stroke stroke = g.getStroke();
		if (model.config.isShowTrajectories()) {
			for(Row row : slice) {
				boolean isLastStep = row.getDouble(trajectories.endTimeCol) > model.getSimTimeInSec();
				double startX = row.getDouble(trajectories.startXCol);
				double startY = row.getDouble(trajectories.startYCol);
				double endX = row.getDouble(trajectories.endXCol);
				double endY = row.getDouble(trajectories.endYCol);

				if(isLastStep && model.config.isInterpolatePositions()) {
					VPoint interpolatedPos = FootStep.interpolateFootStep(startX, startY, endX, endY, row.getDouble(trajectories.startTimeCol), row.getDouble(trajectories.endTimeCol), model.getSimTimeInSec());
					endX = interpolatedPos.getX();
					endY = interpolatedPos.getY();
				}

				int pedId = row.getInt(trajectories.pedIdCol);

				if (model.isElementSelected() && model.getSelectedElement().getId() == pedId) {
					g.setColor(Color.MAGENTA);
					g.setStroke(new BasicStroke(getLineWidth() / 2.0f));
				} else {
					Color cc = agentColors.get(pedId);
					if(cc == null) {
						System.out.println("wtf");
					}
					g.setColor(agentColors.get(pedId));
					g.setStroke(new BasicStroke(getLineWidth() / 4.0f));
				}

				Path2D.Double path = new Path2D.Double();
				path.moveTo(startX, startY);
				path.lineTo(endX, endY);
				draw(path, g);
			}
		}
		g.setColor(c);
		g.setStroke(stroke);


		// render agents i.e. circles
		if (model.config.isShowPedestrians()) {
			for(Pedestrian agent : agents) {
				if (model.config.isShowFaydedPedestrians() || model.isAlive(agent.getId())) {
					agentRender.render(agent, agentColors.get(agent.getId()), g);
					if (model.config.isShowPedestrianIds()) {
						DefaultRenderer.paintAgentId(g, agent);
					}
				}

				// renderImage the arrows indicating the walking direction
				if (model.config.isShowWalkdirection() &&
						(model.config.isShowFaydedPedestrians() || trajectories.getDeathTime(agent.getId()) > model.getSimTimeInSec())) {
					int pedestrianId = agent.getId();
					VPoint lastPosition = lastPedestrianPositions.get(pedestrianId);
					VPoint position = agent.getPosition();

					lastPedestrianPositions.put(pedestrianId, position);

					if (lastPosition != null) {
						VPoint direction;
						if (lastPosition.distance(position) < MIN_ARROW_LENGTH) {
							direction = pedestrianDirections.get(pedestrianId);
						} else {
							direction = new VPoint(lastPosition.getX() - position.getX(),
									lastPosition.getY() - position.getY());
							direction = direction.norm();
							pedestrianDirections.put(pedestrianId, direction);
						}

						if (!pedestrianDirections.containsKey(pedestrianId)) {
							pedestrianDirections.put(pedestrianId, direction);
						}
						if (direction != null) {
							double theta = Math.atan2(-direction.getY(), -direction.getX());
							DefaultRenderer.drawArrow(g, theta,
									position.getX() - agent.getRadius() * 2 * direction.getX(),
									position.getY() - agent.getRadius() * 2 * direction.getY());
						}
					}
				}
			}
		}
		g.setColor(color);
	}
}
