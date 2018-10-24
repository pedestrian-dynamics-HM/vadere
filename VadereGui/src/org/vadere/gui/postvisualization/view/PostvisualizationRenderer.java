package org.vadere.gui.postvisualization.view;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.meshing.color.ColorHelper;
import org.vadere.gui.components.view.DefaultRenderer;
import org.vadere.gui.components.view.SimulationRenderer;
import org.vadere.gui.postvisualization.model.PostvisualizationModel;
import org.vadere.gui.renderer.agent.AgentRender;
import org.vadere.state.scenario.Agent;
import org.vadere.state.simulation.Step;
import org.vadere.state.simulation.Trajectory;
import org.vadere.util.geometry.shapes.VPoint;

public class PostvisualizationRenderer extends SimulationRenderer {

	private static final double MIN_ARROW_LENGTH = 0.1;
	private static Logger logger = LogManager.getLogger(PostvisualizationRenderer.class);
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

			if (color != null) {
				g.setColor(color);
			}

			// choose current trajectories or current+old trajectories
			Stream<Trajectory> trajectoriesStream;
			if (model.config.isShowAllTrajectories()) {
				trajectoriesStream = model.getAppearedPedestrians();
			} else {
				trajectoriesStream = model.getAlivePedestrians();
			}
			model.getStep().ifPresent(step -> trajectoriesStream.forEach(t -> renderTrajectory(g, color, t, step)));
		}
	}

	private void renderTrajectory(final Graphics2D g, final Color color, final Trajectory trajectory, final Step step) {

		Optional<Agent> optionalPedestrian = trajectory.getAgent(step);
		AgentRender agentRender = getAgentRender();

		if (optionalPedestrian.isPresent()) {
			Agent pedestrian = optionalPedestrian.get();

			int targetId = pedestrian.hasNextTarget() ? pedestrian.getNextTargetId() : -1;

			// choose the color
			Optional<Color> c = model.config.isUseEvacuationTimeColor() ?
					Optional.of(colorHelper.numberToColor(trajectory.getLifeTime().orElse(0))) :
					Optional.empty();

			Color nonGroupColor = model.getColorByPredicate(pedestrian).orElse(getPedestrianColor(pedestrian));

			g.setColor(nonGroupColor);

			// renderImage the pedestrian
			if (model.config.isShowPedestrians()) {
				if (model.config.isShowFaydedPedestrians() || !trajectory.isPedestrianDisappeared(step)) {
					agentRender.render(pedestrian, nonGroupColor, g);
					if (model.config.isShowPedestrianIds()) {
						DefaultRenderer.paintAgentId(g, pedestrian);
					}
				}
			}

			// renderImage the trajectory
			if (model.config.isShowTrajectories() && step.getStepNumber() > 0) {
				renderTrajectory(g, trajectory.getPositionsReverse(step), pedestrian);
			}

			// renderImage the arrows indicating the walking direction
			if (model.config.isShowWalkdirection() &&
					(model.config.isShowFaydedPedestrians() || !trajectory.isPedestrianDisappeared(step))) {
				int pedestrianId = pedestrian.getId();
				VPoint lastPosition = lastPedestrianPositions.get(pedestrianId);

				VPoint position = pedestrian.getPosition();

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
								position.getX() - pedestrian.getRadius() * 2 * direction.getX(),
								position.getY() - pedestrian.getRadius() * 2 * direction.getY());
					}
				}
			}
		} else {
			logger.error("Optional<Pedestrian> should not be empty at this point! Step: " + step + ", Ped: "
					+ trajectory.getPedestrianId());
		}
	}


}
