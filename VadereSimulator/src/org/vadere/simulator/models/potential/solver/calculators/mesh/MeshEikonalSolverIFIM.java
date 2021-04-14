package org.vadere.simulator.models.potential.solver.calculators.mesh;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.inter.IFace;
import org.vadere.meshing.mesh.inter.IHalfEdge;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.meshing.mesh.inter.IVertex;
import org.vadere.meshing.mesh.inter.IVertexContainerBoolean;
import org.vadere.meshing.mesh.inter.IVertexContainerDouble;
import org.vadere.meshing.mesh.inter.IVertexContainerObject;
import org.vadere.simulator.models.potential.solver.timecost.ITimeCostFunction;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.logging.Logger;
import org.vadere.util.math.IDistanceFunction;
import org.vadere.util.math.MathUtil;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Set;


/**
 * This class computes the traveling time T using the Informed Fast Iterative Method (IFIM) for arbitrary triangulated meshes.
 * Compare PhD thesis B. Zoennchen Section 9.5.
 * The quality of the result depends on the quality of the triangulation. For a high accuracy the triangulation
 * should not contain too many non-acute triangles.
 *
 * @param <V>   the type of the vertices of the triangulation
 * @param <E>   the type of the half-edges of the triangulation
 * @param <F>   the type of the faces of the triangulation
 *
 * @author Benedikt Zoennchen
 */
public class MeshEikonalSolverIFIM<V extends IVertex, E extends IHalfEdge, F extends IFace> extends AMeshEikonalSolver<V, E, F> {

	private static Logger logger = Logger.getLogger(MeshEikonalSolverIFIM.class);

	private int nThreds = 1;

	final String identifier;

	public static final String nameOldDefiningSimplex = "oldDefiningSimplex";
	public static final String nameDefiningSimplex = "definingSimplex";

	public static final String nameSpeedChanged = "speedChanged";
	public static final String nameOldSpeed = "oldTimeCosts";
	public static final String nameOldPotential = "oldPotential";

	private IVertexContainerObject<V, E, F, Pair> oldDefiningSimplex;
	private IVertexContainerObject<V, E, F, Pair> definingSimplex;

	private IVertexContainerDouble<V, E, F> oldPotential;
	private IVertexContainerDouble<V, E, F> oldTimeCosts;
	private IVertexContainerBoolean<V, E, F> speedChange;

	static {
		logger.setDebug();
	}

	/**
	 * Indicates that the computation of T has been completed.
	 */
	private boolean calculationFinished = false;

	/**
	 * The narrow-band of the fast marching method.
	 */
	private LinkedList<V> activeList;

	private int iteration = 0;
	private int nUpdates = 0;
	private int i = 0;
	private final double epsilon = 0;

	// delete this, its only for logging
	private BufferedWriter bufferedWriter;
	private ArrayList<Integer> updates = new ArrayList<>();
	private ArrayList<ArrayList<Integer>> narrowBandSizes = new ArrayList<>();


	// Note: The updateOrder of arguments in the constructors are exactly as they are since the generic type of a collection is only known at run-time!

	/**
	 * Constructor for certain target shapes.
	 *
	 * @param identifier
	 * @param targetShapes      shapes that define the whole target area.
	 * @param timeCostFunction  the time cost function t(x). Note F(x) = 1 / t(x).
	 * @param triangulation     the triangulation the propagating wave moves on.
	 */
	public MeshEikonalSolverIFIM(@NotNull final String identifier,
	                             @NotNull final Collection<VShape> targetShapes,
	                             @NotNull final ITimeCostFunction timeCostFunction,
	                             @NotNull final IIncrementalTriangulation<V, E, F> triangulation/*,
	                             @NotNull final Collection<VShape> destinations*/
	) {
		super(identifier, triangulation, timeCostFunction);
		this.identifier = identifier;
		this.activeList = new LinkedList<>();
		this.oldDefiningSimplex = getMesh().getObjectVertexContainer(identifier + "_" + nameOldDefiningSimplex, Pair.class);
		this.definingSimplex = getMesh().getObjectVertexContainer(identifier + "_" + nameDefiningSimplex, Pair.class);

		this.oldPotential = getMesh().getDoubleVertexContainer(identifier + "_" + nameOldPotential);
		this.oldTimeCosts = getMesh().getDoubleVertexContainer(identifier + "_" + nameOldSpeed);
		this.speedChange = getMesh().getBooleanVertexContainer(identifier + "_" + nameSpeedChanged);
		setInitialVertices(findInitialVertices(targetShapes), IDistanceFunction.createToTargets(targetShapes));

		/*File dir = new File("/Users/bzoennchen/Development/workspaces/hmRepo/PersZoennchen/PhD/trash/generated/floorFieldPlot/");
		try {
			bufferedWriter = IOUtils.getWriter("updates_ifim.csv", dir);
		} catch (IOException e) {
			e.printStackTrace();
		}*/
	}


	@Override
	public void solve() {
		double ms = System.currentTimeMillis();
		getTriangulation().enableCache();
		nUpdates = 0;
		//narrowBandSizes.add(new ArrayList<>());

		if(!solved || needsUpdate()) {
			if(!solved) {
				prepareMesh();
				unsolve();
				initialActiveList();
				march();
			} else if(needsUpdate()) {
				unsolve();
				initialActiveList();
				march();
			}
		}

		solved = true;
		//updates.add(nUpdates);
		double runTime = (System.currentTimeMillis() - ms);
		logger.debug("ifim run time = " + runTime);
		logger.debug("#nUpdates = " + nUpdates);
		logger.debug("#nVertices = " + (getMesh().getNumberOfVertices() - (int)getMesh().streamVertices().filter(v -> isInitialVertex(v)).count()));
		/*if(iteration % 100 == 0) {
			writeNarrowBandSize();
		}
		if(iteration == 3354) {
			writeUpdates();
		}*/
		iteration++;
		//logger.debug("#nVertices = " + getMesh().getNumberOfVertices());
		//logger.debug(getMesh().toPythonTriangulation(v -> getPotential(v)));
	}

	private void writeUpdates() {
		try {
			StringBuilder builder = new StringBuilder();
			builder.append("updates = [");
			for(int j = 0; j < updates.size(); j++) {
				builder.append(updates.get(j));
				if(j < updates.size()-1) {
					builder.append(",");
				}
			}
			builder.append("]\n");
			bufferedWriter.write(builder.toString());
			bufferedWriter.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void writeNarrowBandSize() {
		try {
			StringBuilder builder = new StringBuilder();
			builder.append("ns = [");
			for(int j = 0; j < narrowBandSizes.get(narrowBandSizes.size()-1).size(); j++) {
				builder.append(narrowBandSizes.get(narrowBandSizes.size()-1).get(j));
				if(j < narrowBandSizes.get(narrowBandSizes.size()-1).size()-1) {
					builder.append(",");
				}
			}
			builder.append("]\n");
			bufferedWriter.write(builder.toString());
			bufferedWriter.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void initialActiveList() {
		for(V vertex : getInitialVertices()) {
			activeList.addLast(vertex);
			//setPotential(vertex, 0);
		}
	}

	/*private void initialActiveList() {
		for(V vertex : getInitialVertices()) {
			for(V v : getMesh().getAdjacentVertexIt(vertex)) {
				if(isUndefined(v)) {
					Triple<Double, V, V> triple = updatePotentialAndDefiningSimplex(v);
					if(triple != null) {
						this.definingSimplex.setValue(v, Pair.of(triple.getMiddle(), triple.getRight()));
					}
				}
			}
		}
	}*/

	@Override
	protected Triple<Double, V, V> recomputePotentialAndDefiningSimplex(@NotNull V vertex) {
		if(requiresUpdate(vertex)) {
			return super.recomputePotentialAndDefiningSimplex(vertex);
		}
		Pair<V,V> pair = oldDefiningSimplex.getValue(vertex);
		return Triple.of(getOldPotential(vertex), pair.getLeft(), pair.getRight());
	}

	private void march() {
		/*ArrayList<Integer> narrowBandSize=null;
		if(iteration % 100 == 0) {
			narrowBandSize = narrowBandSizes.get(narrowBandSizes.size()-1);
		}*/
		while(!activeList.isEmpty()) {
			ListIterator<V> listIterator = activeList.listIterator();
			//logger.debug("#activeList = " + activeList.size());
			LinkedList<V> newActiveList = new LinkedList<>();
			while(listIterator.hasNext()) {
				V x = listIterator.next();
				double p = getPotential(x);
				double q = p;

				boolean updated = requiresUpdate(x);

				if(!isInitialVertex(x)) {
					Triple<Double, V, V> triple = recomputePotentialAndDefiningSimplex(x);

					if(triple.getLeft() < p) {
						q =  triple.getLeft();
						this.definingSimplex.setValue(x, Pair.of(triple.getMiddle(), triple.getRight()));
						setPotential(x, q);
					}
				}

				// converged
				if (Math.abs(p - q) <= epsilon) {
					setBurned(x);
					setUnburning(x);
					// check adjacent neighbors
					for(V xn : getMesh().getAdjacentVertexIt(x)) {
						/*if(!isInitialVertex(xn)) {
							boolean rdy = isReady(xn);
							rdy = isReady(xn);
						}*/
						if(getPotential(xn) > getPotential(x) && !isBurining(xn) && !isInitialVertex(xn) && isReady(xn)) {

							double pp = getPotential(xn);
							Triple<Double, V, V> triple2 = recomputePotentialAndDefiningSimplex(xn);
							double qq = triple2.getLeft();
							if(qq < pp) {
								this.definingSimplex.setValue(xn, Pair.of(triple2.getMiddle(), triple2.getRight()));
								setPotential(xn, qq);
								newActiveList.add(xn);
								/*if(iteration % 100 == 0) {
									narrowBandSize.add(newActiveList.size()+activeList.size());
								}*/

								setBurning(xn);
								setUnburned(xn);
							}
						}
					}
					listIterator.remove();
					/*if(iteration % 100 == 0) {
						narrowBandSize.add(newActiveList.size()+activeList.size());
					}*/
					if(updated) {
						nUpdates++;
					}
				}
			}
			activeList.addAll(newActiveList);
		}

		/*for(V p : getMesh().getVertices()) {
			if(!isReady(p)) {
				System.out.println("error");
				isReady(p);
			}
		}

		System.out.println(getMesh().getNumberOfVertices());
		for(V p : getMesh().getVertices()) {
			boolean noCycle = testCycle(p);
			if(!noCycle) {
				System.out.println(noCycle);
			}
		}*/

		// copy the values
		getMesh().streamVerticesParallel().forEach(v -> oldDefiningSimplex.setValue(v, definingSimplex.getValue(v)));
		// clear might not be necessary
		getMesh().streamVerticesParallel().forEach(v -> definingSimplex.setValue(v, null));

		getMesh().streamVerticesParallel().forEach(v -> setOldPotential(v, getPotential(v)));
		getMesh().streamVerticesParallel().forEach(v -> setOldTimeCost(v, getTimeCost(v)));

		//System.out.println(i+"#update / #vertices: " + nUpdates + " / " + getMesh().getNumberOfVertices());
		i++;
	}

	private boolean testCycle(V p) {
		/*if(isReady(p)) {
			return true;
		} else {*/
			LinkedList<V> notReady = new LinkedList<>();
			notReady.add(p);
			Set<V> newNotReady = new HashSet<>();
			newNotReady.add(p);
			while (!notReady.isEmpty()) {
				V pp = notReady.removeFirst();
				/*if(pp.equals(new Point(148,22))) {
					Triple<Double, Point, Point> r2 = computeGodunovDifferenceAndDep(pp, cellGrid, Direction.ANY);
					System.out.println(r2.getLeft());
				}*/

				/*if(pp.equals(new Point(148,21))) {
					Triple<Double, Point, Point> r2 = computeGodunovDifferenceAndDep(pp, cellGrid, Direction.ANY);
					System.out.println(r2.getLeft());
				}*/
				Pair<V,V> pair = this.definingSimplex.getValue(pp);

				if(pair != null) {
					V p1 = pair.getLeft();
					V p2 = pair.getRight();
					if(p1 != null) {
						if(p1.equals(p)) {
							return false;
						}
						if(!newNotReady.contains(p1)) {
							notReady.addLast(p1);
							newNotReady.add(p1);
						}
					}

					if(p2 != null) {
						if(p2.equals(p)) {
							return false;
						}
						if(!newNotReady.contains(p2)) {
							notReady.addLast(p2);
							newNotReady.add(p2);
						}
					}
				}
			}
		//}
		return true;
	}

	private boolean isReady(V p) {
		Pair<V, V> preDefiningSimplex = this.oldDefiningSimplex.getValue(p);

		if(preDefiningSimplex == null) {
			return true;
		}
		return isValid(preDefiningSimplex.getLeft()) && isValid(preDefiningSimplex.getRight());
	}

	private boolean isValid(V p) {
		return p == null || isBurned(p) || isInitialVertex(p);
	}

	boolean hasSpeedChanged(@NotNull final V v) {
		return speedChange.getValue(v);
	}

	void setSpeedChanged(@NotNull final V v, final boolean value) {
		speedChange.setValue(v, value);
	}

	double getOldPotential(@NotNull final V vertex) {
		return oldPotential.getValue(vertex);
	}

	void setOldPotential(@NotNull final V vertex, final double value) {
		oldPotential.setValue(vertex, value);
	}

	double getOldTimeCost(@NotNull final V vertex) {
		return oldTimeCosts.getValue(vertex);
	}

	void setOldTimeCost(@NotNull final V vertex, final double value) {
		oldTimeCosts.setValue(vertex, value);
	}

	private boolean requiresUpdate(@NotNull final V v) {
		for(V neighbour : getMesh().getAdjacentVertexIt(v)) {
			if(hasChanged(getOldPotential(v), neighbour)) {
				return true;
			}
		}
		return false;
	}

	private boolean hasChanged(@NotNull final double oldPotential, @NotNull final V neighbor) {
		if(!solved) {
			return true;
		}

		if(isUndefined(neighbor) && oldPotential < getOldPotential(neighbor)) {
			return false;
		}

		return Math.abs(getOldPotential(neighbor) - getPotential(neighbor)) > MathUtil.EPSILON;
	}

	private void updatePotential(@NotNull final V vertex) {
		double potential = recomputePotential(vertex);
		if(potential < getPotential(vertex)) {
			if(!isBurining(vertex)) {
				activeList.add(vertex);
			}
			setPotential(vertex, potential);
			setBurning(vertex);
		}

		if(isUndefined(vertex)) {
			logger.debug("could not set neighbour vertex" + vertex);
		}
	}

	private Triple<Double, V, V> updatePotentialAndDefiningSimplex(@NotNull final V vertex) {
		Triple<Double, V, V> triple = recomputePotentialAndDefiningSimplex(vertex);
		double potential = triple.getLeft();

		if(potential < getPotential(vertex)) {
			if(!isBurining(vertex)) {
				activeList.add(vertex);
			}
			setPotential(vertex, potential);
			setBurning(vertex);
			return triple;
		}

		if(isUndefined(vertex)) {
			logger.debug("could not set neighbour vertex" + vertex);
		}

		return null;
	}

	@Override
	public boolean needsUpdate() {
		return true;
	}

	@Override
	protected void compute() {
		march();
	}
}
