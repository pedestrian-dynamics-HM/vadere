package org.vadere.simulator.models.density;

import java.awt.geom.Rectangle2D;
import java.util.function.BiFunction;

import org.vadere.util.math.Convolution;

public class JGaussianFilter extends GaussianFilter {

	JGaussianFilter(Rectangle2D scenarioBounds, double scale, final BiFunction<Integer, Integer, Float> f,
			final boolean normalize) {
		super(scenarioBounds, scale, f, normalize);
	}

	@Override
	public void filterImage() {
		outputMatrix = Convolution.convolve(inputMatrix, kernel, matrixWidth, matrixHeight, kernelWidth);
	}
}
