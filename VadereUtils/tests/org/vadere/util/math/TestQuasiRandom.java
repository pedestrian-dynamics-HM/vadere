package org.vadere.util.math;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Random;

import org.junit.Test;
import org.vadere.util.math.MathUtil;

public class TestQuasiRandom {

	private Random rndSource = new Random(1);

	@Test
	public void testQuasiRandom2D() throws IOException {
		int N = 1000000;
		int count = 10;
		double[][] allpositions = new double[N][2];
		double[] mean = new double[2];
		double[] std = new double[2];

		for (int k = 0; k < N / count; k++) {
			double width = 200.0;
			double height = 100.0;
			double randomFrac = 1.0;
			double[][] positions = MathUtil.quasiRandom2D(rndSource, count,
					width, height, randomFrac);

			for (int m = 0; m < count; m++) {
				allpositions[k * count + m][0] = positions[m][0];
				allpositions[k * count + m][1] = positions[m][1];

				mean[0] = mean[0] + positions[m][0];
				mean[1] = mean[1] + positions[m][1];
			}
		}
		mean[0] /= allpositions.length;
		mean[1] /= allpositions.length;

		// FileUtils.writeStringToFile(new File("test.csv"),
		// GeometryPrinter.grid2string(allpositions));

		// check standard distribution and mean
		for (int k = 0; k < N; k++) {
			std[0] = std[0] + (allpositions[k][0] - mean[0]) * (allpositions[k][0] - mean[0]);
			std[1] = std[1] + (allpositions[k][1] - mean[1]) * (allpositions[k][1] - mean[1]);
		}
		std[0] = Math.sqrt(std[0] / allpositions.length);
		std[1] = Math.sqrt(std[1] / allpositions.length);

		// System.out.println("mean:" + mean[0] + ", " + mean[1]);
		// System.out.println("std:" + std[0] + ", " + std[1]);

		assertEquals("mean x is not correct", 100, mean[0], 1e-1);
		assertEquals("mean y is not correct", 50, mean[1], 1e-1);
		assertEquals("std x is not correct", 57.7147, std[0], 1e-1);
		assertEquals("std y is not correct", 28.8755, std[1], 1e-1);
	}

}
