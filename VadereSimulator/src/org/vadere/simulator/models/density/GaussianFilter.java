package org.vadere.simulator.models.density;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.util.math.Convolution;

import java.awt.geom.Rectangle2D;
import java.util.function.BiFunction;
import java.util.stream.IntStream;

abstract class GaussianFilter implements IGaussianFilter {

    /**
     * the scale of the images respect to the scenario width and heigt and based on the
     * gridresolution of the potential field grid.
     */
    protected final double scale;

    /** the width of the scenario. */
    protected final int scenarioWidth;

    /** the height of the scenario. */
    protected final int scenarioHeight;

    protected float[] inputMatrix;

    protected float[] outputMatrix;

    protected float[] kernel;

    protected int kernelWidth;
    protected int kernelHeight;

    protected int matrixWidth;
    protected int matrixHeight;

    private static Logger logger = LogManager.getLogger(GaussianFilter.class);

    GaussianFilter(final Rectangle2D scenarioBounds, final double scale, final BiFunction<Integer, Integer, Float> f,
                   final boolean noramized) {
        this.scale = scale;
        this.scenarioWidth = (int) (Math.ceil(scenarioBounds.getWidth())) + 1;
        this.scenarioHeight = (int) (Math.ceil(scenarioBounds.getHeight())) + 1;
        this.matrixWidth = (int) (Math.ceil(scenarioWidth * scale));
        this.matrixHeight = (int) (Math.ceil(scenarioHeight * scale));
        this.inputMatrix = new float[matrixWidth * matrixHeight];

        kernelWidth = (int) (9 * scale) + 1;
        // kernelWidth = 31;
        kernelWidth = kernelWidth % 2 == 0 ? kernelWidth + 1 : kernelWidth;
        kernelHeight = kernelWidth;
        this.kernel = Convolution.floatGaussian1DKernel(kernelWidth, f, noramized);
        //this.kernel = Convolution.generateFloatGaussianKernel(kernelWidth, 0.1f);
    }

    @Override
    public void clear() {
        this.inputMatrix = new float[matrixWidth * matrixHeight];
    }

    @Override
    public double getFilteredValue(final double x, final double y) {
        if (x < 0 || y < 0) {
            throw new IllegalArgumentException("x(" + x + ") or y(" + y + ") < 0.");
        }

        return getFilteredValue((int) Math.round(x * scale), (int) Math.round(y * scale));
    }

    public double getFilteredValue(final int x, final int y) {
        if (x < 0 || y < 0) {
            throw new IllegalArgumentException("x(" + x + ") or y(" + y + ") < 0.");
        }

        int index = matrixWidth * y + x;
        if (index >= outputMatrix.length) {
            logger.warn("index(" + index + ") is out matrix range");
            return 0.0;
            // throw new IllegalArgumentException("index("+index+") is out matrix range");
        }
		/*
		 * if(outputMatrix[index] > 0) {
		 * logger.info("index(" + index + "): " + outputMatrix[index]);
		 * }
		 */
        return outputMatrix[index];
    }

    @Override
    public void setInputValue(final int x, final int y, double value) {
        inputMatrix[matrixWidth * y + x] = (float) value;
    }

    @Override
    public void setInputValue(final double x, final double y, double value) {
        inputMatrix[matrixWidth * (int) Math.round(y * scale) + (int) Math.round(x * scale)] = (float) value;
    }

    @Override
    public double getScale() {
        return scale;
    }

    @Override
    public int getMatrixWidth() {
        return matrixWidth;
    }

    @Override
    public int getMatrixHeight() {
        return matrixHeight;
    }

    @Override
    public double getMaxFilteredValue() {
        return IntStream.range(0, outputMatrix.length).mapToDouble(i -> outputMatrix[i]).max().orElse(0.0);
    }

    @Override
    public double getMinFilteredValue() {
        return IntStream.range(0, outputMatrix.length).mapToDouble(i -> outputMatrix[i]).min().orElse(0.0);
    }

    @Override
    public double getInputValue(int x, int y) {
        return inputMatrix[matrixWidth * y + x];
    }
}
