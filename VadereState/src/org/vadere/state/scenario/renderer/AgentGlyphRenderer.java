package org.vadere.state.scenario.renderer;

import org.vadere.state.scenario.Agent;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;

public class AgentGlyphRenderer implements GlyphRenderer{

	@Override
	public void render(Agent a, Graphics2D g, Color c) {

	}

	@Override
	public void render(Agent a, Graphics2D g) {
		String s = "\u20df";
		Font font = new Font("Serif", Font.PLAIN, 12);
		FontRenderContext frc = g.getFontRenderContext();

		GlyphVector gv = font.createGlyphVector(frc, s);
		g.drawGlyphVector(gv, (float)a.getPosition().x, (float)a.getPosition().y);
	}

}
