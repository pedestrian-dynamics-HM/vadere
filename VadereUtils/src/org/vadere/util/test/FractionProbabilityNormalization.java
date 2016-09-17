package org.vadere.util.test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.util.Pair;

/**
 * Helper class to normalize fractions to probabilities so that the sum of all
 * probabilities is 1.
 */
public class FractionProbabilityNormalization {

	public static <T> Map<T, Double> normalize(List<Pair<T, Double>> listWithFractions) {
		double sum = 0;
		for (Pair<T, Double> o : listWithFractions) {
			sum += o.getValue();
		}
		
		Map<T, Double> map = new HashMap<>();
		for (Pair<T, Double> o : listWithFractions) {
			map.put(o.getKey(), o.getValue() / sum);
		}

		return map;
	}
	
	public static double[] normalize(double[] fractions) {
		double sum = 0;
		for (double d : fractions) {
			sum += d;
		}
		
		double[] result = new double[fractions.length];
		for (int i = 0; i < result.length; i++) {
			result[i] = fractions[i] / sum;
		}
		
		return result;
	}

}
