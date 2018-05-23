package org.vadere.gui.renderer.agent;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.gui.components.model.SimulationModel;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.util.geometry.shapes.VShape;

import java.awt.*;
import java.util.concurrent.ConcurrentHashMap;


public class AgentRender implements Renderer {

	private static Logger logger = LogManager.getLogger(AgentRender.class);
	private static final Integer COLOR_NUM = 9;

	private final SimulationModel model;
	private final Color defaultColor;
	private ConcurrentHashMap<Integer, Color> colorMap;

	public AgentRender(SimulationModel model) {
		this.model = model;
		this.defaultColor = model.config.getPedestrianDefaultColor();
		this.colorMap = new ConcurrentHashMap<>();
		this.colorMap.put(-1, defaultColor);
	}

	@Override
	public void render(Agent a, Graphics2D g) {
		if (model.config.isShowGroups()) {
			try {
				Pedestrian ped = (Pedestrian) a;
				renderGroup(ped, g);
			} catch (ClassCastException cce) {
				logger.error("Error casting to Pedestrian");
				cce.printStackTrace();
				model.config.setShowGroups(false);
				renderDefault(a, g, defaultColor);
			}
		} else {
			renderDefault(a, g, defaultColor);
		}
	}

	private void renderGroup(Pedestrian ped, Graphics2D g) {
		g.setColor(getColor(ped));
		g.fill(getShape(ped));
	}

	private void renderDefault(Agent a, Graphics2D g, Color c) {
		g.setColor(c);
		g.fill(a.getShape());
	}

	private Color getHSBColor(int groupId) {
		float hue = ((float) (groupId) / COLOR_NUM);
//		System.out.printf("groupId: %d | hue: %f%n",groupId, hue);
		return new Color(Color.HSBtoRGB(hue, 1f, 0.75f));
	}

	private Color getColor(Pedestrian ped) {
		if (ped.getGroupIds().isEmpty()) {
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

	private VShape getShape(Pedestrian ped) {
		if (ped.getGroupIds().isEmpty()) {
			return ped.getShape();
		} else if (ped.getGroupIds().getFirst() == 1) {
			return ped.getShape();
		} else {
			return FormHelper.getShape(ped.getGroupIds().getFirst(), ped.getPosition(), ped.getRadius());
		}
	}
}
