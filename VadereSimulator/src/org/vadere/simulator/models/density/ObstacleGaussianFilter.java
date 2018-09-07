package org.vadere.simulator.models.density;

import org.vadere.state.scenario.Topography;

public class ObstacleGaussianFilter implements IGaussianFilter {

    private final IGaussianFilter filter;
    private final Topography topography;
    private boolean filtered;

    public ObstacleGaussianFilter(final Topography topography, final IGaussianFilter filter) {
        this.filter = filter;
        this.topography = topography;
        this.filtered = false;
    }

    @Override
    public double getFilteredValue(double x, double y) {
        return filter.getFilteredValue(x, y);
    }

    @Override
    public double getFilteredValue(int x, int y) {
        return filter.getFilteredValue(x, y);
    }

    @Override
    public void setInputValue(double x, double y, double value) {
        filter.setInputValue(x, y, value);
    }

    @Override
    public void setInputValue(int x, int y, double value) {
        filter.setInputValue(x, y, value);
    }

    @Override
    public void filterImage() {
        if (!filtered) {
            setValues();
            filter.filterImage();
            filtered = true;
        }
    }

    @Override
    public int getMatrixWidth() {
        return filter.getMatrixWidth();
    }

    @Override
    public int getMatrixHeight() {
        return filter.getMatrixHeight();
    }

    @Override
    public double getScale() {
        return filter.getScale();
    }

    @Override
    public double getMaxFilteredValue() {
        return filter.getMaxFilteredValue();
    }

    @Override
    public double getMinFilteredValue() {
        return filter.getMinFilteredValue();
    }

    @Override
    public void destroy() {
        this.filter.destroy();
    }

    private void setValues() {
        for (int x = 0; x < getMatrixWidth(); x++) {
            for (int y = 0; y < getMatrixHeight(); y++) {
                double dx = x / getScale();
                double dy = y / getScale();

                if (topography.getObstacles().stream().map(obs -> obs.getShape()).anyMatch(s -> s.contains(dx, dy))) {
                    setInputValue(x, y, 1.0f);
                } else if (topography.isBounded() &&
                        (dx <= topography.getBoundingBoxWidth() || dy <= topography.getBoundingBoxWidth()
                                || dx >= topography.getBounds().getWidth() - topography.getBoundingBoxWidth()
                                || dy >= topography.getBounds().getHeight() - topography.getBoundingBoxWidth())) {
                    setInputValue(x, y, 1.0f);
                } else {
                    setInputValue(x, y, 0.0f);
                }
            }
        }
    }

    @Override
    public void clear() {
        filter.clear();
    }

    @Override
    public double getInputValue(int x, int y) {
        return filter.getInputValue(x, y);
    }
}
