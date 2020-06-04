package org.vadere.simulator.models.density;

import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.function.BiFunction;

import org.vadere.util.logging.Logger;
import org.vadere.util.math.Convolution;
import org.vadere.util.opencl.CLConvolution;
import org.vadere.util.opencl.OpenCLException;

class CLGaussianFilter extends GaussianFilter {

    private final CLConvolution convolution;
    private Logger logger = Logger.getLogger(CLGaussianFilter.class);

    CLGaussianFilter(final Rectangle2D scenarioBounds, final double scale, final BiFunction<Integer, Integer, Float> f,
                     final boolean normalize) throws OpenCLException {
        super(scenarioBounds, scale, f, normalize);
        this.convolution = new CLConvolution(matrixWidth, matrixHeight, kernelWidth, kernel);
        this.convolution.init();
    }

    @Override
    public void filterImage() {

	    try {
		    long ms = System.currentTimeMillis();
		    outputMatrix = convolution.convolve(inputMatrix);
		    ms = System.currentTimeMillis() - ms;
		    logger.debug("filtering required " + ms + "[ms]");
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
