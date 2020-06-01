package org.vadere.simulator.models.potential.fields;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vadere.simulator.utils.cache.CacheException;
import org.vadere.simulator.utils.cache.ICellGridCacheObject;
import org.vadere.simulator.utils.cache.ScenarioCache;
import org.vadere.state.attributes.models.AttributesFloorField;
import org.vadere.state.scenario.Agent;
import org.vadere.util.data.cellgrid.CellGrid;
import org.vadere.util.data.cellgrid.CellState;
import org.vadere.util.data.cellgrid.PathFindingTag;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.logging.Logger;

import java.awt.*;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Benedikt Zoennchen
 *
 * PotentialFieldDistanceEikonalEq computes the nearest distnace to any obstacle by computing
 * the distance at certain discrete points lying on an Cartesian grid. Values inbetween are
 * bilinear interpolated. To compute the distance at these grid points the the exact distances
 * to all obstacles are computed choosing the minimum.
 *
 * Note: This can be computational expensive if there are many and or complex obstacles.
 */
public class PotentialFieldDistancesBruteForce implements IPotentialField {

	private static Logger logger = Logger.getLogger(PotentialFieldDistancesBruteForce.class);
	private final CellGrid cellGrid;
	private final Collection<VShape> obstacles;

	public PotentialFieldDistancesBruteForce(@NotNull final Collection<VShape> obstacles,
											 @NotNull final VRectangle bounds,
											 @NotNull final AttributesFloorField attributesFloorField,
											 @NotNull final ScenarioCache cache) {

		this.obstacles = obstacles;
		this.cellGrid = new CellGrid(bounds.getWidth(), bounds.getHeight(), attributesFloorField.getPotentialFieldResolution(), new CellState(), bounds.getMinX(), bounds.getMinY());

		boolean isInitialized = false;
		logger.info("solve floor field (PotentialFieldDistancesBruteForce)");
		if (cache.isNotEmpty()){
			double ms = System.currentTimeMillis();
			String cacheIdentifier = cache.distToIdentifier("BruteForce");
			ICellGridCacheObject cacheObject = (ICellGridCacheObject) cache.getCache(cacheIdentifier); // todo allow user setting in scenario.
			if(cacheObject.readable()){
				// cache found
				try{
					cacheObject.initializeObjectFromCache(cellGrid);
					isInitialized = true;
					logger.info("floor field initialization time:" + (System.currentTimeMillis() - ms + "[ms] (cache load time)"));
				} catch (CacheException e){
					logger.errorf("Error loading cache solve manually. " + e);
				}
			} else if(cacheObject.writable()) {
				// no cache found
				ms = System.currentTimeMillis();
				logger.infof("No cache found for scenario solve floor field");
				this.cellGrid.pointStream().forEach(this::computeDistanceToGridPoint);
				logger.info("floor field initialization time:" + (System.currentTimeMillis() - ms + "[ms]"));
				isInitialized = true;
				try{
					ms = System.currentTimeMillis();
					logger.info("save floor field cache:");
					cacheObject.persistObject(cellGrid);
					logger.info("save floor field cache time:" + (System.currentTimeMillis() - ms + "[ms]"));
				} catch (CacheException e){
					logger.errorf("Error saving cache.", e);
				}
			}
		}

		if (!isInitialized){
			long ms = System.currentTimeMillis();

			List<Point> points = this.cellGrid.pointStream().collect(Collectors.toList());
			int totalPoints = points.size();
			int processedPoints = 0;
			double checkpointInPercentage = 0.0;


			for (Point point : points) {
				checkpointInPercentage = printProgressIfCheckpointReached(processedPoints, totalPoints, checkpointInPercentage);
				computeDistanceToGridPoint(point);
				processedPoints++;
			}

			logger.info("floor field initialization time:" + (System.currentTimeMillis() - ms + "[ms]"));
		}
	}

	private double printProgressIfCheckpointReached(int processedPoints, int totalPoints, double checkpointInPercentage) {
		double newCheckpoint = checkpointInPercentage;

		double progressInPercentage = ((double) processedPoints / totalPoints) * 100;

		if (progressInPercentage >= checkpointInPercentage) {
			logger.info(String.format("Progress: %2.0f%% -> %d/%d [points]", progressInPercentage, processedPoints, totalPoints));
			double stepSize = 10.0;
			newCheckpoint += stepSize;
		}

		return newCheckpoint;
	}

	private void computeDistanceToGridPoint(@NotNull final Point gridPoint) {
		VPoint point = cellGrid.pointToCoord(gridPoint);
		double distance = obstacles.stream().map(shape -> shape.distance(point)).min(Double::compareTo).orElse(Double.MAX_VALUE);
		cellGrid.setValue(gridPoint, new CellState(distance, PathFindingTag.Reachable));
	}

	@Override
	public double getPotential(@NotNull IPoint pos, @Nullable Agent agent) {
		return cellGrid.getInterpolatedValueAt(pos).getLeft();
	}

}
