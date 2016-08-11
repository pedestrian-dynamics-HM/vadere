package org.vadere.simulator.models.density;

import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.function.BiFunction;

import org.vadere.util.math.CLConvolution;

class CLGaussianFilter extends GaussianFilter {

	private final CLConvolution convolution;

	CLGaussianFilter(final Rectangle2D scenarioBounds, final double scale, final BiFunction<Integer, Integer, Float> f,
			final boolean normalize) throws IOException {
		super(scenarioBounds, scale, f, normalize);
		this.convolution = new CLConvolution();
	}

	@Override
	public void filterImage() {
		outputMatrix = this.convolution.convolveSperate(inputMatrix, matrixWidth, matrixHeight, kernel, kernelWidth);
	}
}
