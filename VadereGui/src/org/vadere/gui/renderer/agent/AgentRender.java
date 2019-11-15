package org.vadere.gui.renderer.agent;


import org.jetbrains.annotations.NotNull;
import org.vadere.gui.components.model.SimulationModel;
import org.vadere.gui.components.view.DefaultRenderer;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.ScenarioElement;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.logging.Logger;

import java.awt.*;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;


public class AgentRender implements Renderer {

	private static Logger logger = Logger.getLogger(AgentRender.class);
	private static final Integer COLOR_NUM = 9;
	private Random random;

	private final SimulationModel model;
	private final Color defaultColor;
	private ConcurrentHashMap<Integer, Color> colorMap;

	public AgentRender(@NotNull final SimulationModel model) {
		this.model = model;
		this.defaultColor = model.config.getPedestrianDefaultColor();
		this.colorMap = new ConcurrentHashMap<>();
		this.colorMap.put(-1, defaultColor);
		this.random = new Random();
	}

	@Override
	public void render(@NotNull final ScenarioElement element, @NotNull final Color color, @NotNull final Graphics2D g) {

		if (model.config.isShowGroups()) {
			try {
				Pedestrian ped = (Pedestrian) element;
				renderGroup(ped, g, color);
			} catch (ClassCastException cce) {
				logger.error("Error casting to Pedestrian");
				cce.printStackTrace();
				model.config.setShowGroups(false);
				renderDefault(element, g, color);
			}
		} else {
			renderDefault(element, g, color);
		}
	}

	private void renderGroup(Pedestrian ped, Graphics2D g, Color color) {
		g.setColor(Color.DARK_GRAY);
		g.fill(ped.getShape());
		g.setColor(color);
		DefaultRenderer.fill(getShape(ped), g);
	}

	private void renderDefault(final ScenarioElement element, Graphics2D g, Color c) {
		g.setColor(c);
		VShape shape = element.getShape();
		if(model.config.isInterpolatePositions()) {
			VPoint pos = ((Pedestrian)element).getInterpolatedFootStepPosition(model.getSimTimeInSec());
			shape = shape.translate(pos.subtract(((Pedestrian)element).getPosition()));
		}
		/*VCircle circle = (VCircle) element.getShape();
		Pedestrian ped = (Pedestrian) element;
		Ellipse2D.Float ellipse = new Ellipse2D.Float((float)ped.getPosition().x, (float)ped.getPosition().y, (float)ped.getRadius() * 2, (float)ped.getRadius() * 2);
		Rectangle2D.Double rect = new Rectangle2D.Double(ped.getPosition().x, ped.getPosition().y, ped.getRadius() * 2, ped.getRadius() * 2);*/
		//g.fill(new VCircle(ped.getPosition(), ped.getRadius()));
		DefaultRenderer.fill(shape, g);
		//g.draw(ellipse);
		//DefaultRenderer.fill(ellipse, g);
		//g.fill(ellipse);
		//g.fill(rect);
	}

	public VShape getShape(Pedestrian ped) {
		VShape shape = ped.getShape();
		VPoint pos = ped.getPosition();
		if(model.config.isInterpolatePositions()) {
			pos = ped.getInterpolatedFootStepPosition(model.getSimTimeInSec());
			shape = shape.translate(pos.subtract(ped.getPosition()));
		}

		if (ped.getGroupIds().isEmpty() || (!ped.getGroupSizes().isEmpty() && ped.getGroupSizes().getFirst() == 1)) {
			return shape;
		} else if (ped.getGroupIds().getFirst() == 1) {
			return shape;
		} else {
			return FormHelper.getShape(ped.getGroupIds().getFirst(), pos, ped.getRadius());
		}
	}
}
