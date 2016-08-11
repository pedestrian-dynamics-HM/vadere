package org.vadere.gui.postvisualization.utils;

import java.awt.Color;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class ColorGenerator {
	private static Set<Color> colors = new HashSet<>();
	private static Random random = new Random();

	public Color getNextRandomColor() {
		Color color = new Color(random.nextInt(256), random.nextInt(256), random.nextInt(256));
		while (colors.contains(color)) {
			color = new Color(random.nextInt(256), random.nextInt(256), random.nextInt(256));
		}
		colors.add(color);

		return color;
	}
}
