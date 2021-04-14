package org.vadere.simulator.models.potential.solver.calculators.cartesian;

import org.apache.commons.lang3.tuple.Triple;
import org.vadere.meshing.mesh.inter.IVertexContainerDouble;
import org.vadere.simulator.models.potential.solver.timecost.ITimeCostFunction;
import org.vadere.util.data.cellgrid.CellGrid;
import org.vadere.util.data.cellgrid.CellState;
import org.vadere.util.data.cellgrid.PathFindingTag;
import org.vadere.util.logging.Logger;
import org.vadere.util.math.IDistanceFunction;

import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.stream.Collectors;

/**
 * The Informed Fast Iterative Method on a Cartesian Grid, compare PhD thesis B. Zoennchen Section 9.5
 *
 * @author Benedikt Zoennchen
 */
public class EikonalSolverIFIM extends AGridEikonalSolver {

	private final IDistanceFunction distFunc;
	private List<Point> targetPoints;
	private static Logger logger = Logger.getLogger(EikonalSolverIFIM.class);
	private ITimeCostFunction timeCostFunction;
	private boolean isActiveList[][];
	private Point definingVertices[][][];
	private Point prefDefiningVertices[][][];
    private final CellGrid cellGrid;
	private final double epsilon;

	private int nUpdates = 0;
	private int i = 0;

	private LinkedList<Point> activeList;

	public EikonalSolverIFIM(
			final CellGrid cellGrid,
			final IDistanceFunction distFunc,
			final ITimeCostFunction timeCostFunction,
            final double unknownPenalty,
            final double weight) {
	    super(cellGrid, unknownPenalty, weight);
	    this.cellGrid = cellGrid;
		this.timeCostFunction = timeCostFunction;
		this.distFunc = distFunc;
		this.targetPoints = cellGrid.pointStream().filter(p -> cellGrid.getValue(p).tag == PathFindingTag.Target).collect(Collectors.toList());
		this.activeList = new LinkedList<>();
		//this.epsilon = cellGrid.getResolution() / 1000;
		this.epsilon = 0;
		this.definingVertices = new Point[cellGrid.getNumPointsX()][cellGrid.getNumPointsY()][2];
		this.prefDefiningVertices = null;
		if (targetPoints.size() == 0) {
			logger.error("PotentialFieldInitializerFastMarching::Run(): "
					+ "Warning, no target points given. Target missing or grid resolution too low.");
			return;
		}
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

	private void init() {
		// set distances of the target neighbor points
		resetDynamicPotentialField();
		targetPoints.stream()
				.flatMap(p -> cellGrid.getLegitNeumannNeighborhood(p).stream())
				.filter(neighbor -> cellGrid.getValue(neighbor).tag != PathFindingTag.Obstacle)
				.filter(neighbor -> cellGrid.getValue(neighbor).tag != PathFindingTag.Target)
				.forEach(neighbor -> {
					if(isReady(neighbor)) {
						if (cellGrid.getValue(neighbor).tag != PathFindingTag.NARROW) {
							activeList.add(neighbor);
						}
						cellGrid.setValue(neighbor, new CellState(Math.max(0, -distFunc.apply(cellGrid.pointToCoord(neighbor))), PathFindingTag.NARROW));
					}
				});
	}

	private void loop() {
		int pointsLooked = 0;

		while (!activeList.isEmpty()) {

			pointsLooked += activeList.size();
			// logger.info("number of treated points: " + pointsLooked);
			// logger.info("active points: " + activeList.size());

			ListIterator<Point> activeListIterator = activeList.listIterator();
			LinkedList<Point> newActiveList = new LinkedList<>();

			while (activeListIterator.hasNext()) {
				Point activePoint = activeListIterator.next();
				double p = cellGrid.getValue(activePoint).potential;

				Triple<Double, Point, Point> r = computeGodunovDifferenceAndDep(activePoint, cellGrid, Direction.ANY);
				if(r.getLeft() < p) {
					this.definingVertices[activePoint.x][activePoint.y][0] = r.getMiddle();
					this.definingVertices[activePoint.x][activePoint.y][1] = r.getRight();
				}

				double q = Math.min(r.getLeft(), p);
				cellGrid.getValue(activePoint).potential = q;
				nUpdates++;
				// converged
				if (Math.abs(p - q) <= epsilon) {
					cellGrid.setValue(activePoint, new CellState(cellGrid.getValue(activePoint).potential, PathFindingTag.Reached));
					for (Point neighbour : cellGrid.getLegitNeumannNeighborhood(activePoint)) {
						if (cellGrid.getValue(neighbour).tag != PathFindingTag.NARROW
								&& cellGrid.getValue(neighbour).tag != PathFindingTag.Target
								&& cellGrid.getValue(neighbour).tag != PathFindingTag.Obstacle) {

							if(isReady(neighbour)) {
								double pp = cellGrid.getValue(neighbour).potential;
								Triple<Double, Point, Point> r2 = computeGodunovDifferenceAndDep(neighbour, cellGrid, Direction.ANY);
								double qq = r2.getLeft();
								//nUpdates++;

								// converged
								//if (qq < (pp - epsilon)) {
								if (qq < pp) {
									this.definingVertices[neighbour.x][neighbour.y][0] = r2.getMiddle();
									this.definingVertices[neighbour.x][neighbour.y][1] = r2.getRight();
									//cellGrid.setValue(neighbour, new CellState(qq, PathFindingTag.NARROW));
									cellGrid.setValue(neighbour, new CellState(qq, PathFindingTag.NARROW));
									newActiveList.add(neighbour);
								}
							} /*else {
								System.out.println("not rdy:" + neighbour + " / " + cellGrid.pointToCoord(neighbour) + ", " + testCycle(neighbour));
								if(cellGrid.getValue(activePoint).tag != PathFindingTag.NARROW) {
									newActiveList.add(activePoint);
									cellGrid.setValue(activePoint, new CellState(cellGrid.getValue(activePoint).potential, PathFindingTag.NARROW));
								}
							}*/
						}
					}

					activeListIterator.remove();
				}
			}
			newActiveList.forEach(p -> {
				CellState state = cellGrid.getValue(p);
				cellGrid.setValue(p, new CellState(state.potential, PathFindingTag.NARROW));
			});

			activeList.addAll(newActiveList);
		}

		//System.arraycopy(definingVertices, 0, prefDefiningVertices, 0, definingVertices.length * definingVertices[0].length * definingVertices[0][0].length);
		//if(i==0) {
			prefDefiningVertices = definingVertices;

			//this code was just to test if everything worked.
			/*for(Point p : cellGrid.pointStream().collect(Collectors.toList())) {
				if(!isReady(p)) {
					System.out.println("error");
					isReady(p);
				}
			}*/

			definingVertices = new Point[cellGrid.getNumPointsX()][cellGrid.getNumPointsY()][2];
		//}


		//System.out.println(i+"#update / #vertices: " + nUpdates + " / " + cellGrid.pointStream().filter(p -> cellGrid.getValue(p).tag == PathFindingTag.Reached).count());
		nUpdates = 0;
		i++;
	}

	private boolean testCycle(Point p) {
		if(isReady(p)) {
			return true;
		} else {
			LinkedList<Point> notReady = new LinkedList<>();
			notReady.add(p);
			LinkedList<Point> newNotReady = new LinkedList<>();
			newNotReady.addLast(p);
			while (!notReady.isEmpty()) {
				Point pp = notReady.removeFirst();
				if(pp.equals(new Point(148,22))) {
					Triple<Double, Point, Point> r2 = computeGodunovDifferenceAndDep(pp, cellGrid, Direction.ANY);
					System.out.println(r2.getLeft());
				}

				if(pp.equals(new Point(148,21))) {
					Triple<Double, Point, Point> r2 = computeGodunovDifferenceAndDep(pp, cellGrid, Direction.ANY);
					System.out.println(r2.getLeft());
				}
				Point p1 = prefDefiningVertices[pp.x][pp.y][0];
				Point p2 = prefDefiningVertices[pp.x][pp.y][1];
				if(p1 != null) {
					notReady.addLast(p1);
					newNotReady.addLast(p2);
				}

				if(p2 != null) {
					notReady.addLast(p2);
					newNotReady.addLast(p2);
				}
			}
		}
		return true;
	}

	private boolean isReady(Point p) {
		boolean result = prefDefiningVertices == null || (isValid(prefDefiningVertices[p.x][p.y][0]) && isValid(prefDefiningVertices[p.x][p.y][1]));
		/*if(!result) {
			System.out.println(p.x + ", " + p.y + ": " + prefDefiningVertices[p.x][p.y][0] + " / " + prefDefiningVertices[p.x][p.y][1]);
		}*/
		return prefDefiningVertices == null || (isValid(prefDefiningVertices[p.x][p.y][0]) && isValid(prefDefiningVertices[p.x][p.y][1]));
	}

	private boolean isValid(Point p) {
		//return true;
		return p == null || cellGrid.getValue(p).tag == PathFindingTag.Reached || cellGrid.getValue(p).tag == PathFindingTag.Target;
		//return p == null || cellGrid.getValue(p).tag != PathFindingTag.Undefined;
				/*
				cellGrid.getValue(p).tag == PathFindingTag.NARROW ||
				cellGrid.getValue(p).tag == PathFindingTag.Reached ||
				cellGrid.getValue(p).tag == PathFindingTag.Target ||
				cellGrid.getValue(p).tag == PathFindingTag.Obstacle;*/
	}

	@Override
	public void solve() {
		init();
		loop();

		/*
		 * try {
		 * BufferedWriter bw = new BufferedWriter(new FileWriter(new File("./potential.csv")));
		 * for(int row = 0; row < cellGrid.getNumPointsY(); row++) {
		 * for(int col = 0; col < cellGrid.getNumPointsX(); col++) {
		 * bw.write(cellGrid.getValue(col, row).potential+" ");
		 * }
		 * bw.write("\n");
		 * }
		 * bw.close();
		 * 
		 * } catch (IOException e) {
		 * e.printStackTrace();
		 * }
		 */
	}

	@Override
	public void update() {
		timeCostFunction.update();
		solve();
	}

	@Override
	public boolean needsUpdate() {
		return timeCostFunction.needsUpdate() || true;
	}

	@Override
	public ITimeCostFunction getTimeCostFunction() {
		return timeCostFunction;
	}

	@Override
	public boolean isHighAccuracy() {
		return false;
	}
}
