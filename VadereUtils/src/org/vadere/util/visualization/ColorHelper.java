package org.vadere.util.visualization;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class ColorHelper {

	private final static int LOW = 0;
	private final static int HIGH = 255;
	private final static int HALF = (HIGH + 1) / 2;
	// public final static int MAX_VALUE = 60;
	private static int factor;
	private final static Map<Integer, Color> map = initNumberToColorMap();
	private int maxValue;

	private static Random random = new Random();

	public ColorHelper(final int maxValue) {
		this.maxValue = maxValue;
	}

	public static Color randomColor() {
		return new Color(random.nextInt(256), random.nextInt(256), random.nextInt(256));
	}

	public Color numberToColor(final double value) {
		if (value < 0) {
			return numberToColorPercentage(0);
		} else if (value > maxValue) {
			return numberToColorPercentage(1);
		} else {
			return numberToColorPercentage(value / maxValue);
		}
	}

	public static Color numberToHurColor(final float hue) {
//		System.out.printf("groupId: %d | hue: %f%n",groupId, hue);
		return new Color(Color.HSBtoRGB(hue, 1f, 0.75f));
	}

	public int getMaxValue() {
		return maxValue;
	}

	public static Color numberToColorPercentage(final double value) {
		if (value < 0 || value > 1) {
			return null;
		}
		Double d = value * factor;
		int index = d.intValue();
		if (index == factor) {
			index--;
		}
		return map.get(index);
	}

	private static Map<Integer, Color> initNumberToColorMap() {
		HashMap<Integer, Color> localMap = new HashMap<Integer, Color>();
		int r = LOW;
		int g = LOW;
		int b = HALF;

		// factor (increment or decrement)
		int rF = 0;
		int gF = 0;
		int bF = 1;

		int count = 0;
		// 1276 steps
		while (true) {
			localMap.put(count++, new Color(r, g, b));
			if (b == HIGH) {
				gF = 1; // increment green
			}
			if (g == HIGH) {
				bF = -1; // decrement blue
				// rF = +1; // increment red
			}
			if (b == LOW) {
				rF = +1; // increment red
			}
			if (r == HIGH) {
				gF = -1; // decrement green
			}
			if (g == LOW && b == LOW) {
				rF = -1; // decrement red
			}
			if (r < HALF && g == LOW && b == LOW) {
				break; // finish
			}
			r += rF;
			g += gF;
			b += bF;
			r = rangeCheck(r);
			g = rangeCheck(g);
			b = rangeCheck(b);
		}
		initList(localMap);
		return localMap;
	}

	/**
	 * @param localMap
	 */
	private static void initList(final HashMap<Integer, Color> localMap) {
		List<Integer> list = new ArrayList<Integer>(localMap.keySet());
		Collections.sort(list);
		Integer min = list.get(0);
		Integer max = list.get(list.size() - 1);
		factor = max + 1;
	}

	/**
	 * @param value
	 * @return
	 */
	private static int rangeCheck(final int value) {
		if (value > HIGH) {
			return HIGH;
		} else if (value < LOW) {
			return LOW;
		}
		return value;
	}

	/**
	 * @param c Base color
	 * @return Color which has enough contrast to be distinct from the base color
	 */
	public static Color getContrasColor(final Color c) {
		return new Color(Math.abs(190 - c.getRed()),
				Math.abs(190 - c.getGreen()),
				Math.abs(190 - c.getBlue()));
	}
}
