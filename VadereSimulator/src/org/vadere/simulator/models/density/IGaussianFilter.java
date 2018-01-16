package org.vadere.simulator.models.density;

import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.Collection;
import java.util.function.BiFunction;

import org.vadere.simulator.models.potential.timeCostFunction.loading.IPedestrianLoadingStrategy;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.Topography;

/**
 * IGaussianFilter is a refershable image processing calculator that can be used
 * to solve a discrete convolution or other calculations that can be done by a
 * image processing filter.
 *
 *
 */
public interface IGaussianFilter {

    enum Type {
        OpenCL, // default
        NativeJava // not jet implemented
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

    /**
     * This method has to be called if the Filter will no longer called!
     */
    void destroy();

    static <E extends Agent> IGaussianFilter create(final Rectangle2D scenarioBounds,
                                                    Collection<E> pedestrians, final double scale,
                                                    final double standardDerivation,
                                                    final AttributesAgent attributesPedestrian,
                                                    final IPedestrianLoadingStrategy loadingStrategy) {
        return create(scenarioBounds, pedestrians, scale, standardDerivation, attributesPedestrian, loadingStrategy,
                Type.OpenCL);
    }

    /*
     * Factory-methods
     */
    static <E extends Agent> IGaussianFilter create(final Rectangle2D scenarioBounds,
                                                    Collection<E> pedestrians, final double scale,
                                                    final double standardDerivation,
                                                    final AttributesAgent attributesPedestrian,
                                                    final IPedestrianLoadingStrategy loadingStrategy, final Type type) {

        double scaleFactor = attributesPedestrian.getRadius() * 2
                * attributesPedestrian.getRadius() * 2
                * Math.sqrt(3)
                * 0.5
                / (2 * Math.PI * standardDerivation * standardDerivation);

        switch (type) {
            case OpenCL: {
                try {
                    BiFunction<Integer, Integer, Float> f =
                            (centerI, i) -> (float) (Math.sqrt(scaleFactor) * Math.exp(-((centerI - i) / scale)
                                    * ((centerI - i) / scale) / (2 * standardDerivation * standardDerivation)));
                    IGaussianFilter clFilter = new CLGaussianFilter(scenarioBounds, scale, f, false);
                    return new PedestrianGaussianFilter(pedestrians, clFilter, loadingStrategy);
                } catch (IOException e) {
                    // cannot go on, this should never happen!
                    throw new RuntimeException(e);
                }
            }
            default:
                BiFunction<Integer, Integer, Float> f =
                        (centerI, i) -> (float) (Math.sqrt(scaleFactor) * Math.exp(-((centerI - i) / scale)
                                * ((centerI - i) / scale) / (2 * standardDerivation * standardDerivation)));
                IGaussianFilter clFilter = new JGaussianFilter(scenarioBounds, scale, f, false);
                return new PedestrianGaussianFilter(pedestrians, clFilter, loadingStrategy);
        }
    }

    static IGaussianFilter create(
            final Topography scenario, final double scale,
            final boolean scenarioHasBoundary, final double standardDerivation) {
        return create(scenario, scale, scenarioHasBoundary, standardDerivation, Type.OpenCL);
    }

    static IGaussianFilter create(
            final Topography scenario, final double scale,
            final boolean scenarioHasBoundary, final double standardDerivation, final Type type) {
        switch (type) {
            case OpenCL: {
                try {
                    double varianz = standardDerivation * standardDerivation;
                    BiFunction<Integer, Integer, Float> f = (centerI, i) -> (float) ((1.0 / (2 * Math.PI * varianz))
                            * Math.exp(-((centerI - i) / scale) * ((centerI - i) / scale) / (2 * varianz)));
                    IGaussianFilter clFilter = new CLGaussianFilter(scenario.getBounds(), scale, f, true);
                    return new ObstacleGaussianFilter(scenario, clFilter);
                } catch (IOException e) {
                    // cannot go on, this should never happen!
                    throw new RuntimeException(e);
                }
            }
            default:
                double varianz = standardDerivation * standardDerivation;
                BiFunction<Integer, Integer, Float> f = (centerI, i) -> (float) ((1.0 / (2 * Math.PI * varianz))
                        * Math.exp(-((centerI - i) / scale) * ((centerI - i) / scale) / (2 * varianz)));
                IGaussianFilter clFilter = new JGaussianFilter(scenario.getBounds(), scale, f, true);
                return new ObstacleGaussianFilter(scenario, clFilter);
        }
    }

    static IGaussianFilter create(
            final Topography scenario, final double scale,
            final double standardDerivation) {
        return create(scenario, scale, true, standardDerivation);
    }
}
