package org.vadere.simulator.models.density;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.simulator.models.potential.timeCostFunction.loading.IPedestrianLoadingStrategy;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.Collection;
import java.util.function.Predicate;

public class PedestrianGaussianFilter<E extends Pedestrian> implements IGaussianFilter {

	private final IGaussianFilter filter;
	private final Collection<E> pedestrians;
	private final Predicate<E> pedestrianPredicate;
	private final IPedestrianLoadingStrategy pedestrianLoadingStrategy;
	private static Logger logger = LogManager.getLogger(PedestrianGaussianFilter.class);

	public PedestrianGaussianFilter(final Collection<E> pedestrians, final IGaussianFilter filter,
			final IPedestrianLoadingStrategy pedestrianLoadingStrategy) {
		this(pedestrians, filter, pedestrianLoadingStrategy, p -> true);
	}

	public PedestrianGaussianFilter(final Collection<E> pedestrians, final IGaussianFilter filter,
			final IPedestrianLoadingStrategy pedestrianLoadingStrategy, final Predicate<E> pedestrianPredicate) {
		this.filter = filter;
		this.pedestrians = pedestrians;
		this.pedestrianPredicate = pedestrianPredicate;
		this.pedestrianLoadingStrategy = pedestrianLoadingStrategy;
	}

	@Override
	public double getFilteredValue(int x, int y) {

		double value = filter.getFilteredValue(x, y);
		/*
		 * if(value > 0) {
		 * logger.info("pedVal: " + value);
		 * }
		 */
		// return value < 1.0f ? 0 : value;
		return value;
	}

	@Override
	public double getFilteredValue(double x, double y) {

		double value = filter.getFilteredValue(x, y);
		/*
		 * if(value > 0) {
		 * logger.info("pedVal: " + value);
		 * }
		 */
		// return value < 1.0f ? 0 : value;
		return value;
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

		setValues();
		filter.filterImage();
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

	private void setValue(E pedestrian) {
		VPoint position = pedestrian.getPosition();
		VPoint filteredPosition = new VPoint(Math.max(0, position.x), Math.max(0, position.y));

		// better approximation
		double indexX = filteredPosition.x * getScale();
		double indexY = filteredPosition.y * getScale();

		if (indexX == ((int) indexX) && indexY == ((int) indexY)) {
			setInputValue(filteredPosition.x, filteredPosition.y,
					getInputValue((int) indexX, (int) indexY) + pedestrianLoadingStrategy.calculateLoading(pedestrian));
		} else if (indexX == ((int) indexX) && indexY != ((int) indexY)) {
			splitY(filteredPosition, (int) indexX, (int) Math.floor(indexY), pedestrian);
			splitY(filteredPosition, (int) indexX, (int) Math.ceil(indexY), pedestrian);
		} else if (indexX != ((int) indexX) && indexY == ((int) indexY)) {
			splitX(filteredPosition, (int) Math.floor(indexX), (int) indexY, pedestrian);
			splitX(filteredPosition, (int) Math.ceil(indexX), (int) indexY, pedestrian);
		} else {
			splitXY(filteredPosition, (int) Math.floor(indexX), (int) Math.floor(indexY), pedestrian);
			splitXY(filteredPosition, (int) Math.floor(indexX), (int) Math.ceil(indexY), pedestrian);
			splitXY(filteredPosition, (int) Math.ceil(indexX), (int) Math.floor(indexY), pedestrian);
			splitXY(filteredPosition, (int) Math.ceil(indexX), (int) Math.ceil(indexY), pedestrian);
		}

		// setInputValue(filteredPosition.x, filteredPosition.y,
		// pedestrianLoadingStrategy.calculateLoading(pedestrian));
	}

	private void splitXY(final VPoint filteredPosition, final int indexX, final int indexY, Pedestrian pedestrian) {
		if (checkIndices(indexX, indexY)) {
			double dx = Math.abs(filteredPosition.x * getScale() - indexX);
			double dy = Math.abs(filteredPosition.y * getScale() - indexY);

			double weight = ((1.0 - dx) + (1.0 - dy)) / 4.0;
			// double weight = Math.exp(-(dx * dx + dy * dy) / (2 * 0.7 * 0.7));
			setInputValue(indexX, indexY,
					getInputValue(indexX, indexY) + pedestrianLoadingStrategy.calculateLoading(pedestrian) * weight);
		}
	}

	private void splitX(final VPoint filteredPosition, final int indexX, final int indexY, Pedestrian pedestrian) {
		if (checkIndices(indexX, indexY)) {
			double dx = Math.abs(filteredPosition.x * getScale() - indexX);
			double weight = (1.0 - dx);
			// double weight = Math.exp(-(dx * dx + dy * dy) / (2 * 0.7 * 0.7));
			setInputValue(indexX, indexY,
					getInputValue(indexX, indexY) + pedestrianLoadingStrategy.calculateLoading(pedestrian) * weight);
		}
	}

	private void splitY(final VPoint filteredPosition, final int indexX, final int indexY, Pedestrian pedestrian) {
		if (checkIndices(indexX, indexY)) {
			double dy = Math.abs(filteredPosition.y * getScale() - indexY);

			double weight = (1.0 - dy);
			// double weight = Math.exp(-(dx * dx + dy * dy) / (2 * 0.7 * 0.7));
			setInputValue(indexX, indexY,
					getInputValue(indexX, indexY) + pedestrianLoadingStrategy.calculateLoading(pedestrian) * weight);
		}
	}

	private boolean checkIndices(int x, int y) {
		return x >= 0 && y >= 0 && x < filter.getMatrixWidth() && y < filter.getMatrixHeight();
	}

	private void setValues() {
		clear();
		pedestrians.stream().filter(pedestrianPredicate).forEach(p -> setValue(p));
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
