package org.vadere.util.potential.calculators;

import java.awt.Point;
import java.util.List;
import java.util.PriorityQueue;
import java.util.stream.Collectors;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.math.MathUtil;
import org.vadere.util.potential.CellGrid;
import org.vadere.util.potential.CellState;
import org.vadere.util.potential.PathFindingTag;
import org.vadere.util.potential.timecost.ITimeCostFunction;
import org.vadere.util.triangulation.adaptive.IDistanceFunction;

/**
 * EikonalSolverFMM initializes a potential field on basis
 * of the fast marching algorithm. The potential field is static and therefor
 * not updated by update() (see PotentialFieldInitializerFastMarchingAdaptive).
 * Hence, the initializer may be used to realize static floor fields.
 * 
 */
public class EikonalSolverFMM extends AbstractGridEikonalSolver {
	protected final PriorityQueue<Point> narrowBand;
	protected final ITimeCostFunction timeCostFunction;

	protected CellGrid cellGrid;
	protected List<Point> targetPoints;
	protected List<VShape> targetShapes;
	protected IDistanceFunction distFunc;

	boolean isHighAccuracy = false;

	/** only for logging */
	protected static Logger logger = LogManager.getLogger(EikonalSolverFMM.class);
	protected long runtime = 0;

	/**
	 * Initializes the FM potential calculator with a time cost function F > 0.
	 */
	public EikonalSolverFMM(
			final CellGrid potentialField,
			final List<VShape> targetShapes,
			final boolean isHighAccuracy,
			final ITimeCostFunction timeCostFunction,
			final double unknownPenalty) {
		super(potentialField, unknownPenalty);
		this.cellGrid = potentialField;
		this.targetPoints = cellGrid.pointStream().filter(p -> cellGrid.getValue(p).tag == PathFindingTag.Target)
				.collect(Collectors.toList());
		this.targetShapes = targetShapes;
		this.isHighAccuracy = isHighAccuracy;

		ComparatorPotentialFieldValue comparator = new ComparatorPotentialFieldValue(
				potentialField);
		this.narrowBand = new PriorityQueue<Point>(50, comparator);
		this.timeCostFunction = timeCostFunction;

		if (targetPoints.size() == 0) {
			logger.error("PotentialFieldInitializerFastMarching::Run(): "
					+ "Warning, no target points given. Target missing or grid resolution too low.");
			return;
		}
	}

    /**
     * Initializes the FM potential calculator with a time cost function F > 0.
     */
    public EikonalSolverFMM(
            final CellGrid potentialField,
            final IDistanceFunction distFunc,
            final boolean isHighAccuracy,
            final ITimeCostFunction timeCostFunction,
            final double unknownPenalty) {
        super(potentialField, unknownPenalty);
        this.cellGrid = potentialField;
        this.targetPoints = cellGrid.pointStream().filter(p -> cellGrid.getValue(p).tag == PathFindingTag.Target)
                .collect(Collectors.toList());
        this.distFunc = distFunc;
        this.isHighAccuracy = isHighAccuracy;

        ComparatorPotentialFieldValue comparator = new ComparatorPotentialFieldValue(
                potentialField);
        this.narrowBand = new PriorityQueue<>(50, comparator);
        this.timeCostFunction = timeCostFunction;

        if (targetPoints.size() == 0) {
            logger.error("PotentialFieldInitializerFastMarching::Run(): "
                    + "Warning, no target points given. Target missing or grid resolution too low.");
            return;
        }
    }

	@Override
	public void initialize() {

		for (Point point : targetPoints) {
			//if(!targetShapes.isEmpty()) {
            setTargetNeighborsDistances(point);
			//}
			narrowBand.add(point);
		}


		/**
		 * Create whole Floor Field at the beginning.
		 */
		// TODO [priority=low] [task=feature] remove this part to enable on demand computation from the
		// beginning.
		// Be aware that in this case, the function "getValue" MUST be called,
		// it is not possible to work with the cellGrid directly.
		while (!narrowBand.isEmpty()) {
			Point tmpPoint = narrowBand.poll();
			cellGrid.getValue(tmpPoint).tag = PathFindingTag.Reached;
			setNeighborDistances(tmpPoint);
		}
	}

	/**
	 * Method to support calculation on demand (to improve the performance).
	 * 
	 * @param point
	 */
	private void furtherRun(final Point point) {
		Point tmpPoint;
		while (!narrowBand.isEmpty()
				&& cellGrid.getValue(point).tag == PathFindingTag.Undefined) {
			tmpPoint = narrowBand.poll();
			cellGrid.getValue(tmpPoint).tag = PathFindingTag.Reached;
			setNeighborDistances(tmpPoint);
		}
	}

	@Override
	public void update() {
		// logger.info("other-runtime: " + (System.currentTimeMillis() - runtime));
		long ms = System.currentTimeMillis();
		if (needsUpdate()) {
			timeCostFunction.update();
			resetDynamicPotentialField();
			initialize();
		}
		// logger.info("ffm-runtime: " + (System.currentTimeMillis() - ms));
		runtime = System.currentTimeMillis();
	}

	protected void resetDynamicPotentialField() {
		for (CellState data : cellGrid.getRawBuffer()) {
			data.potential = Double.MAX_VALUE;

			if (data.tag == PathFindingTag.Reached) {
				data.tag = PathFindingTag.Undefined;
			} else if (data.tag == PathFindingTag.Target) {
				data.potential = 0.0;
			}
		}
	}

	@Override
	public ITimeCostFunction getTimeCostFunction() {
		return timeCostFunction;
	}

	@Override
	public boolean isHighAccuracy() {
		return isHighAccuracy;
	}

	@Override
	public boolean needsUpdate() {
		return timeCostFunction.needsUpdate();
	}

	@Override
	public boolean isValidPoint(Point point) {
		return cellGrid.isValidPoint(point);
	}

	protected void setNeighborDistances(final Point point) {
		List<Point> neighbors = MathUtil.getNeumannNeighborhood(point);
		double distance;

		for (Point neighbor : neighbors) {

			if (cellGrid.isValidPoint(neighbor)) {
				if (cellGrid.getValue(neighbor).tag == PathFindingTag.Undefined) {
					distance = computeGodunovDifference(neighbor, cellGrid);
					cellGrid.setValue(neighbor, new CellState(distance,
							PathFindingTag.Reachable));
					narrowBand.add(neighbor);
				} else if (cellGrid.getValue(neighbor).tag == PathFindingTag.Reachable) {
					distance = computeGodunovDifference(neighbor, cellGrid);

					if (distance < cellGrid.getValue(neighbor).potential) {
						narrowBand.remove(neighbor);
						cellGrid.getValue(neighbor).potential = distance;
						narrowBand.add(neighbor);
					}
				}
			}
		}
	}

	protected void setTargetNeighborsDistances(final Point point) {
		List<Point> neighbors = cellGrid.getLegitNeumannNeighborhood(point);

		for (Point neighbor : neighbors) {
			if (cellGrid.getValue(neighbor).tag == PathFindingTag.Undefined) {

				cellGrid.setValue(
						neighbor,
						new CellState(minDistanceToTarget(cellGrid.pointToCoord(neighbor)) / timeCostFunction.costAt(cellGrid.pointToCoord(neighbor)),
								PathFindingTag.Reachable));
				narrowBand.add(neighbor);
			}
		}
	}

	private double minDistanceToTarget(final VPoint point) {
		double minDistance = Double.MAX_VALUE;
		double tmp;
		// create point that lies in the center of the grid cells so that the distances can be
		// computed starting there.
		VPoint dp = new VPoint(cellGrid.getWidth() / (cellGrid.getNumPointsX() - 1) / 2.0,
				cellGrid.getHeight() / (cellGrid.getNumPointsY() - 1) / 2.0);

		if(targetShapes != null && !targetShapes.isEmpty()) {
            for (VShape targetShape : targetShapes) {
                // negative distances are possible when point is inside the target
                tmp = Math.max(0, targetShape.distance(point.add(dp)));
                if (tmp < minDistance) {
                    minDistance = tmp;
                }
            }
        }
        else if(distFunc != null) {
            tmp = Math.max(0, -distFunc.apply(point));
            if (tmp < minDistance) {
                minDistance = tmp;
            }
        }

		return minDistance;
	}
}
