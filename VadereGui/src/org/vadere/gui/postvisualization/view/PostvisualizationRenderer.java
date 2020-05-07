package org.vadere.gui.postvisualization.view;

import org.vadere.gui.components.view.DefaultRenderer;
import org.vadere.gui.components.view.SimulationRenderer;
import org.vadere.gui.postvisualization.model.PostvisualizationModel;
import org.vadere.gui.postvisualization.model.TableTrajectoryFootStep;
import org.vadere.gui.renderer.agent.AgentRender;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.simulation.FootStep;
import org.vadere.util.geometry.shapes.VPoint;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.Table;

import java.awt.*;
import java.awt.geom.Path2D;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class PostvisualizationRenderer extends SimulationRenderer {

	private static final double MIN_ARROW_LENGTH = 0.1;
	private PostvisualizationModel model;

	private final Map<Integer, VPoint> lastPedestrianPositions;
	private final Map<Integer, VPoint> pedestrianDirections;

	public PostvisualizationRenderer(final PostvisualizationModel model) {
		super(model);
		this.model = model;
		this.pedestrianDirections = new HashMap<>();
		this.lastPedestrianPositions = new HashMap<>();
	}

	public PostvisualizationModel getModel() {
		return model;
	}

	@Override
	protected void renderSimulationContent(final Graphics2D g) {

		if (!model.isEmpty()) {
			Color savedColor = g.getColor();

			Table slice = (model.config.isShowAllTrajectories()) ? model.getAppearedPedestrians() : model.getAlivePedestrians() ;
			Collection<Pedestrian> pedestrians = model.getPedestrians();

			Map<Integer, Color> pedestrianColors = new HashMap<>();
			pedestrians.forEach(ped -> pedestrianColors.put(ped.getId(),  getPedestrianColor(ped)));

			renderTrajectories(g, slice, pedestrianColors);
			renderPedestrians(g, pedestrians, pedestrianColors);

			g.setColor(savedColor);
		}

	}

	private void renderTrajectories(Graphics2D g, Table slice, Map<Integer, Color> pedestrianColors) {

		Color savedColor = g.getColor();
		Stroke savedStroke = g.getStroke();

		TableTrajectoryFootStep trajectories = model.getTrajectories();

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
					Color cc = pedestrianColors.get(pedId);
					if(cc == null) {
						System.out.println("wtf");
					}
					g.setColor(pedestrianColors.get(pedId));
					g.setStroke(new BasicStroke(getLineWidth() / 4.0f));
				}

				Path2D.Double path = new Path2D.Double();
				path.moveTo(startX, startY);
				path.lineTo(endX, endY);
				draw(path, g);
			}
		}

		g.setColor(savedColor);
		g.setStroke(savedStroke);
	}

	private void renderPedestrians(Graphics2D g, Collection<Pedestrian> pedestrians, Map<Integer, Color> agentColors) {

		AgentRender agentRender = getAgentRender();

		if (model.config.isShowPedestrians()) {
			for(Pedestrian pedestrian : pedestrians) {
				if (model.config.isShowFaydedPedestrians() || model.isAlive(pedestrian.getId())) {
					agentRender.render(pedestrian, agentColors.get(pedestrian.getId()), g);

					if (model.config.isShowPedestrianIds()) {
						DefaultRenderer.paintAgentId(g, pedestrian);
					}

					if (model.config.isShowPedestrianInOutGroup()) {
						renderPedestrianInOutGroup(g, pedestrian);
					}
				}

				if (model.config.isShowWalkdirection() &&
						(model.config.isShowFaydedPedestrians() || model.getTrajectories().getDeathTime(pedestrian.getId()) > model.getSimTimeInSec())) {
					renderWalkingDirection(g, pedestrian);
				}
			}
		}
	}

	private void renderWalkingDirection(Graphics2D g, Pedestrian pedestrian) {

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

}
