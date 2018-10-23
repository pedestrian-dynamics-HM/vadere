package org.vadere.gui.renderer.agent;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.vadere.gui.components.model.SimulationModel;
import org.vadere.gui.components.view.DefaultRenderer;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.ScenarioElement;
import org.vadere.util.geometry.shapes.VShape;

import java.awt.*;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;


public class AgentRender implements Renderer {

	private static Logger logger = LogManager.getLogger(AgentRender.class);
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
				renderGroup(ped, g);
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

	private void renderGroup(Pedestrian ped, Graphics2D g) {
		g.setColor(getGroupColor(ped));
		DefaultRenderer.fill(getShape(ped), g);
	}

	private void renderDefault(final ScenarioElement element, Graphics2D g, Color c) {
		g.setColor(c);
		/*VCircle circle = (VCircle) element.getShape();
		Pedestrian ped = (Pedestrian) element;
		Ellipse2D.Float ellipse = new Ellipse2D.Float((float)ped.getPosition().x, (float)ped.getPosition().y, (float)ped.getRadius() * 2, (float)ped.getRadius() * 2);
		Rectangle2D.Double rect = new Rectangle2D.Double(ped.getPosition().x, ped.getPosition().y, ped.getRadius() * 2, ped.getRadius() * 2);*/
		//g.fill(new VCircle(ped.getPosition(), ped.getRadius()));
		DefaultRenderer.fill(element.getShape(), g);
		//g.draw(ellipse);
		//DefaultRenderer.fill(ellipse, g);
		//g.fill(ellipse);
		//g.fill(rect);
	}

	private Color getHSBColor(int groupId) {
//		float hue = ((float) (groupId) / COLOR_NUM);
		float hue = random.nextFloat();
//		System.out.printf("groupId: %d | hue: %f%n",groupId, hue);
		return new Color(Color.HSBtoRGB(hue, 1f, 0.75f));
	}

	public Color getGroupColor(Pedestrian ped) {
		if (ped.getGroupIds().isEmpty() || (!ped.getGroupSizes().isEmpty() && ped.getGroupSizes().getFirst() == 1)) {
			return defaultColor;
		}

		int groupId = ped.getGroupIds().getFirst();
		Color c = colorMap.get(groupId);
		if (c == null) {
			c = getHSBColor(groupId);
			colorMap.put(groupId, c);
		}
		return c;
	}

	public VShape getShape(Pedestrian ped) {
		if (ped.getGroupIds().isEmpty() || (!ped.getGroupSizes().isEmpty() && ped.getGroupSizes().getFirst() == 1)) {
			return ped.getShape();
		} else if (ped.getGroupIds().getFirst() == 1) {
			return ped.getShape();
		} else {
			return FormHelper.getShape(ped.getGroupIds().getFirst(), ped.getPosition(), ped.getRadius());
		}
	}
}
