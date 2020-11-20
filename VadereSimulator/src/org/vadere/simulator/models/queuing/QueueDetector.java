package org.vadere.simulator.models.queuing;

import java.awt.*;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;

import org.vadere.simulator.models.density.IGaussianFilter;
import org.vadere.simulator.models.potential.timeCostFunction.loading.IPedestrianLoadingStrategy;
import org.vadere.state.attributes.models.AttributesTimeCost;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VPolygon;
import org.vadere.util.data.cellgrid.CellGrid;
import org.vadere.util.data.cellgrid.PathFindingTag;
import org.vadere.simulator.models.potential.solver.calculators.cartesian.EikonalSolverFMM;
import org.vadere.simulator.models.potential.solver.timecost.ITimeCostFunction;
import org.vadere.util.math.IDistanceFunction;

public class QueueDetector extends EikonalSolverFMM {

    private AttributesAgent attributesPedestrian;
    private Topography topography;
    private final PriorityQueue<Point> targetPoints;
    private VPolygon polytope;
    private static double QUEUE_DENSITY = 0.03;
    private static double radius = 2.0;
    protected LinkedList<VPoint> orderedPoints;

    /**
     * Initializes the FM potential calculator with a time cost function F > 0.
     *
     * @param potentialField
     * @param distFunc
     * @param isHighAccuracy
     * @param timeCostFunction
     */
    public QueueDetector(
            CellGrid potentialField,
            IDistanceFunction distFunc,
            boolean isHighAccuracy,
            ITimeCostFunction timeCostFunction,
            AttributesAgent attributesPedestrian,
            Topography topography,
            double weight,
            double unknownPenalty) {
        super(potentialField, distFunc, isHighAccuracy, timeCostFunction, weight, unknownPenalty);
        this.orderedPoints = new LinkedList<>();
        this.attributesPedestrian = attributesPedestrian;
        this.topography = topography;
        this.targetPoints = new PriorityQueue<>();
        this.polytope = null;
    }

    public double getResolution() {
        return cellGrid.getResolution();
    }

    @Override
    public void solve() {
        IPedestrianLoadingStrategy loadingStrategy = IPedestrianLoadingStrategy.create();
        IGaussianFilter filter = IGaussianFilter.create(
                topography.getBounds(),
                topography.getElements(Pedestrian.class),
                1.0 / cellGrid.getResolution(),
                new AttributesTimeCost().getStandardDeviation(),
                attributesPedestrian,
                loadingStrategy, IGaussianFilter.Type.OpenCL);

        filter.filterImage();
        cellGrid.pointStream().forEach(p -> {
            int x = p.x;
            int y = p.y;

            VPoint point = cellGrid.pointToCoord(x, y);
            if (cellGrid.getValue(x, y).tag != PathFindingTag.Target
                    && filter.getFilteredValue(point.x, point.y) <= QUEUE_DENSITY) {
                cellGrid.getValue(x, y).tag = PathFindingTag.Obstacle;
            } else if (cellGrid.getValue(x, y).tag != PathFindingTag.Target) {
                // System.out.println("found:" + point);
                cellGrid.getValue(x, y).tag = PathFindingTag.Undefined;
            }
        });
        orderedPoints.clear();
        filter.destroy();
        super.solve();
    }

    @Override
    protected void setNeighborDistances(Point point) {
        super.setNeighborDistances(point);
        VPoint worldCoord = cellGrid.pointToCoord(point);
        orderedPoints.removeIf(p -> p.distance(worldCoord) <= radius);

        if (Math.max(0, -distFunc.apply(worldCoord)) <= radius) {
            orderedPoints.addFirst(worldCoord);
        }
    }

    public void setPolytope(VPolygon polytope) {
        this.polytope = polytope;
    }

    @Override
    public boolean needsUpdate() {
        return true;
    }

    public List<VPoint> getTargetPoints() {
        return orderedPoints;
    }



	/*
	 * public List<VPoint> getTargetPoints() {
	 * return this.orderedPoints.stream().sorted((p1, p2) ->
	 * {
	 * double computeGodunovDifference = cellGrid.getValue(p2.x, p2.y).potential -
	 * cellGrid.getValue(p1.x, p1.y).potential;
	 * if (computeGodunovDifference < 0) {
	 * return -1;
	 * } else if (computeGodunovDifference > 0) {
	 * return 1;
	 * } else {
	 * return 0;
	 * }
	 * }).
	 * map(p -> new VPoint(p.x * cellGrid.getResolution(), p.y *
	 * cellGrid.getResolution())).collect(Collectors.toList());
	 * }
	 */
}
