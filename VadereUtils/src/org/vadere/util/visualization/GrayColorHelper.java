package org.vadere.util.visualization;

import java.util.List;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class GrayColorHelper {

	private final static int LOW = 0;
	private final static int HIGH = 255;
	private final static int HALF = (HIGH + 1) / 2;
	// public final static int MAX_VALUE = 60;

	private final static Map<Integer, Color> map = initNumberToColorMap();
	private static int factor;

	private int maxValue;

	public GrayColorHelper(final int maxValue) {
		this.maxValue = maxValue;
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
		for (int i = 0; i < 255; i++) {
			localMap.put(i, new Color(i, i, i));
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
}
