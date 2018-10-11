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
	public int toXIndex(double x) {
		return filter.toXIndex(x);
	}

	@Override
	public int toYIndex(double y) {
		return filter.toYIndex(y);
	}

	@Override
	public int toFloorXIndex(double x) {
		return filter.toFloorXIndex(x);
	}

	@Override
	public int toFloorYIndex(double y) {
		return filter.toFloorYIndex(y);
	}

	@Override
	public double toXCoord(int xIndex) {
		return filter.toXCoord(xIndex);
	}

	@Override
	public double toYCoord(int yIndex) {
		return filter.toYCoord(yIndex);
	}

	@Override
    public void destroy() {
        this.filter.destroy();
    }

    private void setValues() {
        for (int x = 0; x < getMatrixWidth(); x++) {
            for (int y = 0; y < getMatrixHeight(); y++) {
                double dx = topography.getBounds().getMinX() + x / getScale();
                double dy = topography.getBounds().getMinY() + y / getScale();

                if (topography.getObstacles().stream().map(obs -> obs.getShape()).anyMatch(s -> s.contains(dx, dy))) {
                    setInputValue(x, y, 1.0f);
                } else if (topography.isBounded() &&
                        (dx <= topography.getBounds().getMinX() + topography.getBoundingBoxWidth()
		                        || dy <= topography.getBounds().getMinY() + topography.getBoundingBoxWidth()
                                || dx >= topography.getBounds().getMaxX() - topography.getBoundingBoxWidth()
                                || dy >= topography.getBounds().getMaxY() - topography.getBoundingBoxWidth())) {
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
