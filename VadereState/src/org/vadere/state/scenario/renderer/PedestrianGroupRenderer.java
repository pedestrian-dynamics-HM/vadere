package org.vadere.state.scenario.renderer;

import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.util.geometry.shapes.VShape;

import java.awt.*;
import java.util.concurrent.ConcurrentHashMap;

public class PedestrianGroupRenderer implements ShapeRenderer{

	private static ConcurrentHashMap<Integer, Color> color = new ConcurrentHashMap<>();
	private static final Integer COLOR_NUM = 9;

	@Override
	public void render(Agent a, final Graphics2D g){
		g.setColor(getColor(a));
		g.fill(drawShape(a));
	}


	@Override
	public VShape drawShape(Agent a) {
//		return FormHelper.getStarX(a.getPosition(), a.getRadius());
		Pedestrian ped = (Pedestrian)a;
		return FormHelper.getShape(ped.getGroupIds().getFirst(), ped.getPosition(), ped.getRadius());
	}

	private Color getHSBColor(int groupId){
		float hue = ((float)(groupId) / COLOR_NUM);
//		System.out.printf("groupId: %d | hue: %f%n",groupId, hue);
		return new Color(Color.HSBtoRGB(hue, 1f, 0.75f));
	}

	private Color getColor(Agent a){
		Pedestrian ped = (Pedestrian)a;
		int groupId = ped.getGroupIds().getFirst();
		Color c = color.get(groupId);
		if (c == null){
			c = getHSBColor(groupId);
			color.put(groupId, c);
		}
		return c;
	}


	public void setColor(Agent a, Graphics2D g) {
		g.setColor(getColor(a));
	}


}
