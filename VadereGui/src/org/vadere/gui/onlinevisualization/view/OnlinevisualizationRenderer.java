package org.vadere.gui.onlinevisualization.view;

import org.jetbrains.annotations.NotNull;
import org.vadere.gui.components.view.DefaultRenderer;
import org.vadere.gui.components.view.SimulationRenderer;
import org.vadere.gui.onlinevisualization.model.OnlineVisualizationModel;
import org.vadere.gui.renderer.agent.AgentRender;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.util.geometry.shapes.VPoint;

import java.awt.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.stream.Stream;

public class OnlinevisualizationRenderer extends SimulationRenderer {

	private final OnlineVisualizationModel model;
	private static final double MIN_ARROW_LENGTH = 0.1;
	private final Map<Integer, VPoint> lastPedestrianPositions;
	private final Map<Integer, VPoint> pedestrianDirections;
	private final Map<Integer, LinkedList<VPoint>> pedestrianPositions;

	public OnlinevisualizationRenderer(final OnlineVisualizationModel model) {
		super(model);
		this.model = model;
		this.pedestrianDirections = new HashMap<>();
		this.lastPedestrianPositions = new HashMap<>();
		this.pedestrianPositions = new HashMap<>();
	}

	@Override
	public void render(final Graphics2D targetGraphics2D, int x, int y, int width, int height) {
	    synchronized (model.getDataSynchronizer()) {
            super.render(targetGraphics2D, x, y, width, height);
        }

	}

	@Override
	public void render(final Graphics2D targetGraphics2D, int width, int height) {
        synchronized (model.getDataSynchronizer()) {
            super.render(targetGraphics2D, width, height);
        }
	}

	@Override
	protected void renderSimulationContent(final Graphics2D g) {
		if (model.config.isShowPedestrians()) {
			renderPedestrians(g);
			// DefaultRenderer.paintPedestrianIds(g, model.getPedestrians());
		}
	}


	private void renderPedestrians(final Graphics2D g) {
		AgentRender agentRender = getAgentRender();
		for (Pedestrian ped : model.getPedestrians()) {
			Color agentColor = getPedestrianColor(ped);
			g.setColor(agentColor);
			VPoint position = ped.getPosition();
			agentRender.render(ped, agentColor, g);

			if (!pedestrianPositions.containsKey(ped.getId())) {
				pedestrianPositions.put(ped.getId(), new LinkedList());
			}

			// reverse the point order
			if(!model.config.isInterpolatePositions()) {
				pedestrianPositions.get(ped.getId()).addFirst(ped.getPosition());
			} else {
				if(ped.getFootstepHistory().getCapacity() > 0) {
					pedestrianPositions.get(ped.getId()).addFirst(ped.getFootstepHistory().getYoungestFootStep().getStart());
				} else {
					pedestrianPositions.get(ped.getId()).addFirst(ped.getPosition());
				}
			}

			if (model.config.isShowTrajectories()) {
				renderTrajectory(g, pedestrianPositions.get(ped.getId()), ped);
			}

			if (model.config.isShowWalkdirection()) {
				int pedestrianId = ped.getId();
				VPoint lastPosition = lastPedestrianPositions.get(pedestrianId);
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
						DefaultRenderer.drawArrow(g, theta, position.getX() - ped.getRadius() * 2 * direction.getX(),
								position.getY() - ped.getRadius() * 2 * direction.getY());
					}
				}
			}
		}
	}

	/*
	 * This method should replace pedestrianPositions.get(ped.getId()).addFirst(ped.getPosition());
	 * However the simulation runs in an separated thread. Therefore, foot steps might be cleared
	 * before they can be drawn! Solution: deep clone of the topography, which is costly and difficult.
	 */
	private Stream<VPoint> getFootStepsPosition(@NotNull final Agent agent) {
		if(agent instanceof Pedestrian) {
			return ((Pedestrian) agent).getTrajectory().stream().map(footStep -> footStep.getStart());
		}
		else {
			return Stream.of(agent.getPosition());
		}
	}
}
