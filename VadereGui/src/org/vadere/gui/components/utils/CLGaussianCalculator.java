package org.vadere.gui.components.utils;

import java.awt.image.BufferedImage;

import org.vadere.gui.components.model.DefaultSimulationConfig;
import org.vadere.gui.components.model.SimulationModel;
import org.vadere.util.visualization.ColorHelper;
import org.vadere.simulator.models.density.IGaussianFilter;
import org.vadere.state.attributes.scenario.AttributesAgent;

public class CLGaussianCalculator {

    private final double scale;
    private final int scenarioWidth;
    private final int scenarioHeight;
    private SimulationModel<DefaultSimulationConfig> model;

    private IGaussianFilter filterObstacles;
    private IGaussianFilter filterPedestrians;

    public CLGaussianCalculator(final SimulationModel model,
                                final double scale) {

        this.scenarioWidth = (int) model.getTopographyBound().getWidth();
        this.scenarioHeight = (int) model.getTopographyBound().getHeight();
        this.scale = scale;
        this.model = model;
        this.filterObstacles = IGaussianFilter.create(model.getTopography(), scale, model.getTopography().isBounded(),
                0.7, IGaussianFilter.Type.OpenCL);
    }

    public BufferedImage getDensityImage() {
        IGaussianFilter filterPedestrians = IGaussianFilter.create(
                model.getTopography().getBounds(),
                model.getAgents(),
                scale,
                0.7f,
                new AttributesAgent(-1),
                (ped) -> 1.0,
                IGaussianFilter.Type.OpenCL);
        filterPedestrians.filterImage();
        filterObstacles.filterImage();
        filterPedestrians.destroy();
        return convertFilterToImage(filterPedestrians, filterObstacles);
    }

    private BufferedImage convertFilterToImage(final IGaussianFilter filterPedestrians,
                                               final IGaussianFilter filterObstacles) {
        int width = Math.max(filterObstacles.getMatrixWidth(), filterPedestrians.getMatrixWidth());
        int height = Math.max(filterObstacles.getMatrixHeight(), filterPedestrians.getMatrixHeight());
        BufferedImage image = createImage(width, height);
        int maxColorValue = 20;
        ColorHelper colorHelper = new ColorHelper(maxColorValue);

        // double bound = filter.getMaxFilteredValue();
        double max = 1.00;
        double factor = maxColorValue / max;
        //System.out.println(filterPedestrians.getMaxFilteredValue()); // 0.1259
		double maxValue = Double.MIN_VALUE;
        for (int x = 0; x < filterPedestrians.getMatrixWidth(); x++) {
            for (int y = 0; y < filterPedestrians.getMatrixHeight(); y++) {
                double pedValue = filterPedestrians.getFilteredValue(x, y);
                double obsValue = filterObstacles.getFilteredValue(x, y);
                double value = pedValue + obsValue;
                // value = pedValue;
                image.setRGB(x, y, colorHelper.numberToColor(value ).getRGB());
				if(maxValue < value) {
					maxValue = value;
				}
                /*
				 * if(value <= 0.0) {
				 * image.setRGB(x, y, Color.WHITE.getRGB());
				 * } else {
				 * image.setRGB(x, y, colorHelper.numberToColor(value * factor).getRGB());
				 * }
				 */
            }
        }
        System.out.println(maxValue);
        return image;
    }

    public void destroy() {
        this.filterObstacles.destroy();
    }

    /**
     * Helper method which create a new standard BufferedImage with the needed
     * configurations.
     *
     * @return the image which is prepared for additional drawing
     */
    private BufferedImage createImage(final int width, final int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        return image;
    }

}
