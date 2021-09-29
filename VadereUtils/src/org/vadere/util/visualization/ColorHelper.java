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

	public static Color standardColorInterpolation (Color a, Color b, float t)
	{
		return new Color
				(
						(int) (a.getRed() + (b.getRed() - a.getRed()) * t),
						(int) (a.getGreen() + (b.getGreen()  - a.getGreen() ) * t),
						(int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t),
						(int) (a.getAlpha() + (b.getAlpha() - a.getAlpha()) * t)
				);
	}

	/**
	 * Color interpolation following https://www.alanzucconi.com/2016/01/06/colour-interpolation/
	 */
	public static Color improvedColorInterpolation(Color aRgb, Color bRgb, float t){
		// convert rgb colors to hsb, where hsb[i]: i = 0 hue, i = 1 saturation, i = 2 brightness / value
		float[] a = new float[3];
		float[] b = new float[3];
		java.awt.Color.RGBtoHSB(aRgb.getRed(), aRgb.getGreen(), aRgb.getBlue(), a);
		java.awt.Color.RGBtoHSB(bRgb.getRed(), bRgb.getGreen(), bRgb.getBlue(), b);

		// Hue interpolation
		float h;
		float d = b[0] - a[0];
		if (a[0] > b[0])
		{
			// Swap (a.h, b.h)
			float h3 = b[0]; // h3 = b.h2
			b[0] = a[0];
			a[0] = h3;
			d = -d;
			t = 1 - t;
		}
		if (d > 0.5) // 180deg
		{
			a[0] = a[0] + 1; // 360deg
			h = ( a[0] + t * (b[0] - a[0]) ) % 1; // 360deg
		}
		// if (d <= 0.5) // 180deg
		else {
			h = a[0] + t * d;
		}

		// Interpolate
		// int alpha = (int) (aRgb.getAlpha() + t * (bRgb.getAlpha() - aRgb.getAlpha()));
		return new Color(java.awt.Color.HSBtoRGB(h, a[1] + t * (b[1]-a[1]), a[2] + t * (b[2]-a[2])));
	}
}
