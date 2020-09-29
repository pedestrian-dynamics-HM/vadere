package org.vadere.simulator.models.potential.fields;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vadere.simulator.models.potential.solver.calculators.EikonalSolver;
import org.vadere.simulator.models.potential.solver.calculators.PotentialFieldCalculatorNone;
import org.vadere.simulator.models.potential.solver.calculators.cartesian.EikonalSolverFIM;
import org.vadere.simulator.models.potential.solver.calculators.cartesian.EikonalSolverFMM;
import org.vadere.simulator.models.potential.solver.calculators.cartesian.EikonalSolverFSM;
import org.vadere.simulator.models.potential.solver.calculators.cartesian.EikonalSolverIFIM;
import org.vadere.simulator.models.potential.solver.timecost.UnitTimeCostFunction;
import org.vadere.simulator.utils.cache.CacheException;
import org.vadere.simulator.utils.cache.ICellGridCacheObject;
import org.vadere.simulator.utils.cache.ScenarioCache;
import org.vadere.state.attributes.models.AttributesFloorField;
import org.vadere.state.scenario.Agent;
import org.vadere.util.data.cellgrid.CellGrid;
import org.vadere.util.data.cellgrid.CellState;
import org.vadere.util.data.cellgrid.FloorDiscretizer;
import org.vadere.util.data.cellgrid.PathFindingTag;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.logging.Logger;
import org.vadere.util.math.DistanceFunctionTarget;
import org.vadere.util.math.IDistanceFunction;

import java.util.Collection;

/**
 * @author Benedikt Zoennchen
 *
 * PotentialFieldDistanceEikonalEq computes the nearest distnace to any obstacle by computing
 * the distance at certain discrete points lying on an Cartesian grid. Values inbetween are
 * bilinear interpolated. To compute the distance at these grid points the eikonal equation is
 * used by choosing obstacles to be the destination area of the propageting wave front.
 */
public class PotentialFieldDistanceEikonalEq implements IPotentialField {

	private static Logger logger = Logger.getLogger(PotentialFieldDistanceEikonalEq.class);
	private EikonalSolver eikonalSolver;
	private final CellGrid cellGrid;

	public PotentialFieldDistanceEikonalEq(@NotNull final Collection<VShape> obstacles,
										   @NotNull final VRectangle bounds,
										   @NotNull final AttributesFloorField attributesFloorField,
										   @NotNull final ScenarioCache cache) {
		cellGrid = new CellGrid(bounds.getWidth(),
				bounds.getHeight(), attributesFloorField.getPotentialFieldResolution(), new CellState(), bounds.getMinX(), bounds.getMinY());

		boolean isInitialized = false;
		logger.info("solve floor field (PotentialFieldDistanceEikonalEq)");
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
				compute(obstacles, attributesFloorField);
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
			compute(obstacles, attributesFloorField);
			logger.info("floor field initialization time:" + (System.currentTimeMillis() - ms + "[ms]"));
		}
	}

	private void compute(@NotNull final Collection<VShape> obstacles,
	                     @NotNull final AttributesFloorField attributesFloorField) {
		for (VShape shape : obstacles) {
			FloorDiscretizer.setGridValuesForShape(cellGrid, shape,
					new CellState(0.0, PathFindingTag.Target));
		}

		boolean isHighAccuracyFM = attributesFloorField.getCreateMethod().isHighAccuracy();

		/**
		 * The distance function returns values < 0 if the point is inside the domain,
		 * i.e. outside of any obstacle and values > 0 if the point lies inside an obstacle.
		 */
		IDistanceFunction distFunc = new DistanceFunctionTarget(cellGrid, obstacles);

		/* copy the static grid */
		switch (attributesFloorField.getCreateMethod()) {
			case NONE:
				eikonalSolver = new PotentialFieldCalculatorNone();
				break;
			case INFORMED_FAST_ITERATIVE_METHOD:
				eikonalSolver = new EikonalSolverIFIM(cellGrid, distFunc, new UnitTimeCostFunction(), attributesFloorField.getObstacleGridPenalty(), attributesFloorField.getTargetAttractionStrength());
				break;
			case FAST_ITERATIVE_METHOD:
				eikonalSolver = new EikonalSolverFIM(cellGrid, distFunc, isHighAccuracyFM, new UnitTimeCostFunction(), attributesFloorField.getObstacleGridPenalty(), attributesFloorField.getTargetAttractionStrength());
				break;
			case FAST_SWEEPING_METHOD:
				eikonalSolver = new EikonalSolverFSM(cellGrid, distFunc, isHighAccuracyFM, new UnitTimeCostFunction(), attributesFloorField.getObstacleGridPenalty(), attributesFloorField.getTargetAttractionStrength());
				break;
			default:
				eikonalSolver = new EikonalSolverFMM(cellGrid, distFunc, isHighAccuracyFM, new UnitTimeCostFunction(), attributesFloorField.getObstacleGridPenalty(), attributesFloorField.getTargetAttractionStrength());
		}

		long ms = System.currentTimeMillis();
		eikonalSolver.solve();
		logger.info("floor field initialization time:" + (System.currentTimeMillis() - ms + "[ms]"));
	}

	@Override
	public double getPotential(@NotNull final IPoint pos, @Nullable final Agent agent) {
		return eikonalSolver.getPotential(pos);
	}

	public EikonalSolver getEikonalSolver() {
		return eikonalSolver;
	}
}
