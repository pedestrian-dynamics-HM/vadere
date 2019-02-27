package org.vadere.simulator.models.density;

import org.jetbrains.annotations.NotNull;
import org.vadere.simulator.models.potential.timeCostFunction.loading.IPedestrianLoadingStrategy;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.Topography;
import org.vadere.util.logging.Logger;
import org.vadere.util.opencl.CLUtils;
import org.vadere.util.opencl.OpenCLException;

import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.Collection;
import java.util.function.BiFunction;

/**
 * IGaussianFilter is a refreshable image processing calculator that can be used
 * to solve a discrete convolution or other calculations that can be done by a
 * image processing filter.
 *
 *
 */
public interface IGaussianFilter {

	Logger logger = Logger.getLogger(IGaussianFilter.class);

    enum Type {
        OpenCL, // default
        NativeJava
    }

    /**
     * Returns the value of a specified coordinate. This coordinate will be
     * converted to natural numbers of the image.
     *
     * @param x
     *        the x-coordinate (for example the x-coordinate of a place on
     *        the floor)
     * @param y
     *        the y-coordinate (for example the y-coordinate of a place on
     *        the floor)
     * @return the value of a specified coordinate
     */
    double getFilteredValue(final double x, final double y);

    double getFilteredValue(int x, int y);

    double getInputValue(int x, int y);

    void setInputValue(final double x, final double y, final double value);

    void setInputValue(final int x, final int y, final double value);

    /** refresh or update the values of the image that triangleContains all values. */
    void filterImage();

    void clear();

    int getMatrixWidth();

    int getMatrixHeight();

    double getScale();

    double getMaxFilteredValue();

    double getMinFilteredValue();

	int toXIndex(final double x);

	int toYIndex(final double y);

	int toFloorXIndex(final double x);

	int toFloorYIndex(final double y);

	double toXCoord(int xIndex);

	double toYCoord(int yIndex);


    /**
     * This method has to be called if the Filter will no longer called!
     */
    void destroy();

    static <E extends Agent> IGaussianFilter create(@NotNull final Rectangle2D scenarioBounds,
                                                    @NotNull Collection<E> pedestrians, final double scale,
                                                    final double standardDerivation,
                                                    @NotNull final AttributesAgent attributesPedestrian,
                                                    @NotNull final IPedestrianLoadingStrategy loadingStrategy) {
        return create(scenarioBounds, pedestrians, scale, standardDerivation, attributesPedestrian, loadingStrategy,
                Type.OpenCL);
    }

    /*
     * Factory-methods
     */
    static <E extends Agent> IGaussianFilter create(@NotNull final Rectangle2D scenarioBounds,
                                                    @NotNull Collection<E> pedestrians, final double scale,
                                                    final double standardDerivation,
                                                    @NotNull final AttributesAgent attributesPedestrian,
                                                    @NotNull final IPedestrianLoadingStrategy loadingStrategy, final Type type) {

        double scaleFactor = attributesPedestrian.getRadius() * 2
                * attributesPedestrian.getRadius() * 2
                * Math.sqrt(3)
                * 0.5
                / (2 * Math.PI * standardDerivation * standardDerivation);

	    BiFunction<Integer, Integer, Float> f =
			    (centerI, i) -> (float) (Math.sqrt(scaleFactor) * Math.exp(-((centerI - i) / scale)
					    * ((centerI - i) / scale) / (2 * standardDerivation * standardDerivation)));

	    IGaussianFilter clFilter = getFilter(scenarioBounds, f, type, scale);
	    return new PedestrianGaussianFilter(pedestrians, clFilter, loadingStrategy);
    }

	/**
	 * Returns the desired filter if possible. If the filter <tt>type</tt> is not supported
	 * it returns a default java implementation of the filter which should on any device / platform
	 * / machine.
	 *
	 * @param scenarioBounds    the bound the filter is working on
	 * @param f                 the function for generating the kernel values
	 * @param type              the type of the filter e.g. OpenCL (GPU filter) or native java filter
	 * @param scale             the scale of the filtered grid which directly maps to the size of the matrices which take part in the convolution
	 *
	 * @return the desired filter or (if it is not supported) a native java implementation
	 */
    static IGaussianFilter getFilter(@NotNull final Rectangle2D scenarioBounds,
                                     @NotNull final BiFunction<Integer, Integer, Float> f,
                                     @NotNull final Type type,
                                     final double scale) {
	    IGaussianFilter clFilter;
	    switch (type) {
		    case OpenCL: {
			    if(CLUtils.isOpenCLSupported()) {
				    try {
					    clFilter = new CLGaussianFilter(scenarioBounds, scale, f, false);
				    } catch (OpenCLException e) {
					    e.printStackTrace();
					    logger.warn("Error while initializing OpenCL: " + e.getMessage());
					    clFilter = new JGaussianFilter(scenarioBounds, scale, f, false);
				    }
				    catch (UnsatisfiedLinkError linkError) {
					    linkError.printStackTrace();
					    logger.warn("Linking error (native lib problem) while initializing OpenCL: " + linkError.getMessage());
					    clFilter = new JGaussianFilter(scenarioBounds, scale, f, false);
				    }
			    }
			    else {
				    clFilter = new JGaussianFilter(scenarioBounds, scale, f, false);
			    }
		    } break;
		    default:
			    clFilter = new JGaussianFilter(scenarioBounds, scale, f, false);
	    }
		return clFilter;
    }

    static IGaussianFilter create(@NotNull final Topography scenario,
                                  final double scale,
                                  final boolean scenarioHasBoundary,
                                  final double standardDerivation) {
        return create(scenario, scale, scenarioHasBoundary, standardDerivation, Type.OpenCL);
    }

    static IGaussianFilter create(final Topography scenario,
                                  final double scale,
                                  final boolean scenarioHasBoundary,
                                  final double standardDerivation,
                                  final Type type) {

    	double varianz = standardDerivation * standardDerivation;
	    BiFunction<Integer, Integer, Float> f = (centerI, i) -> (float) ((1.0 / (2 * Math.PI * varianz))
			    * Math.exp(-((centerI - i) / scale) * ((centerI - i) / scale) / (2 * varianz)));

	    IGaussianFilter clFilter = getFilter(scenario.getBounds(), f, type, scale);
	    return new ObstacleGaussianFilter(scenario, clFilter);
    }

    static IGaussianFilter create(
            final Topography scenario, final double scale,
            final double standardDerivation) {
        return create(scenario, scale, true, standardDerivation);
    }
}
