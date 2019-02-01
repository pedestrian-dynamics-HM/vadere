package org.vadere.simulator.models.density;

import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.function.BiFunction;

import org.vadere.util.logging.Logger;
import org.vadere.util.opencl.CLConvolution;
import org.vadere.util.opencl.OpenCLException;

class CLGaussianFilter extends GaussianFilter {

    private final CLConvolution convolution;
    private Logger logger = Logger.getLogger(CLConvolution.class);

    CLGaussianFilter(final Rectangle2D scenarioBounds, final double scale, final BiFunction<Integer, Integer, Float> f,
                     final boolean normalize) throws IOException, OpenCLException {
        super(scenarioBounds, scale, f, normalize);
        this.convolution = new CLConvolution(matrixWidth, matrixHeight, kernelWidth, kernel);
        this.convolution.init();
    }

    @Override
    public void filterImage() {
	    try {
		    outputMatrix = this.convolution.convolve(inputMatrix);
	    } catch (OpenCLException e) {
		    logger.error(e.getMessage());
		    e.printStackTrace();
	    }
    }

    @Override
    public void destroy() {
	    try {
		    this.convolution.clearCL();
	    } catch (OpenCLException e) {
		    logger.error(e.getMessage());
		    e.printStackTrace();
	    }
    }
}
