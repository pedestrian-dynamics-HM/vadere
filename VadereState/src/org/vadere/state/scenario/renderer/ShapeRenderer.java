package org.vadere.state.scenario.renderer;

import org.vadere.state.scenario.Agent;
import org.vadere.util.geometry.shapes.VShape;

import java.awt.*;

public interface ShapeRenderer extends Renderer {

	VShape drawShape(final Agent a);


	default void setfill(final Agent a, final Graphics2D g){
		g.fill(drawShape(a));
	}

	default void render(final Agent a, final Graphics2D g, Color c){
		g.setColor(c);
		g.fill(drawShape(a));
	}

	default void render(final Agent a, final Graphics2D g){
		g.setColor(Color.BLUE);
		g.fill(drawShape(a));
	}
}
