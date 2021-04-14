package org.vadere.simulator.models.potential.solver.calculators.mesh;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.inter.IFace;
import org.vadere.meshing.mesh.inter.IHalfEdge;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.meshing.mesh.inter.IVertex;
import org.vadere.meshing.mesh.inter.IVertexContainerObject;
import org.vadere.simulator.models.potential.solver.timecost.ITimeCostFunction;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.io.CollectionUtils;
import org.vadere.util.logging.Logger;
import org.vadere.util.math.IDistanceFunction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;


/**
 * This class computes the traveling time T using a (multi threaded) lock free variant of the fast iterative method for arbitrary triangulated meshes.
 * The quality of the result depends on the quality of the triangulation. For a high accuracy the triangulation
 * should not contain too many non-acute triangles.
 *
 * @param <V>   the type of the vertices of the triangulation
 * @param <E>   the type of the half-edges of the triangulation
 * @param <F>   the type of the faces of the triangulation
 *
 * @author Benedikt Zoennchen
 */
public class MeshEikonalSolverFIMLockFree<V extends IVertex, E extends IHalfEdge, F extends IFace> extends AMeshEikonalSolver<V, E, F> {

	private static Logger logger = Logger.getLogger(MeshEikonalSolverFIMLockFree.class);
	public final String nameAtomicBoolean = "nameAtomic";
	private int nThreds = 1;
	private Random random = new Random(1);
	final String identifier;

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
	private ArrayList<LinkedList<V>> activeLists;

	private final IVertexContainerObject<V, E, F, AtomicBoolean> atomicBooleans;

	private int iteration = 0;
	private int nUpdates = 0;
	private ForkJoinPool forkJoinPool;


	// Note: The updateOrder of arguments in the constructors are exactly as they are since the generic type of a collection is only known at run-time!

	/**
	 * Constructor for certain target shapes.
	 *
	 * @param identifier
	 * @param targetShapes      shapes that define the whole target area.
	 * @param timeCostFunction  the time cost function t(x). Note F(x) = 1 / t(x).
	 * @param triangulation     the triangulation the propagating wave moves on.
	 */
	public MeshEikonalSolverFIMLockFree(@NotNull final String identifier,
	                                    @NotNull final Collection<VShape> targetShapes,
	                                    @NotNull final ITimeCostFunction timeCostFunction,
	                                    @NotNull final IIncrementalTriangulation<V, E, F> triangulation
	) {
		super(identifier, triangulation, timeCostFunction);
		this.identifier = identifier;
		this.forkJoinPool = ForkJoinPool.commonPool();
		//this.nThreds = forkJoinPool.getParallelism();
		logger.debug("parallel fim using " + nThreds + " threads.");
		this.activeLists = new ArrayList<>(nThreds);
		this.forkJoinPool = new ForkJoinPool(nThreds);
		this.atomicBooleans = getMesh().getObjectVertexContainer(identifier + "_" + nameAtomicBoolean, AtomicBoolean.class);

		for(int i = 0; i < nThreds; i++) {
			activeLists.add(new LinkedList<>());
		}

		//TODO a more clever init!
		List<V> initialVertices = new ArrayList<>();
		for(VShape shape : targetShapes) {
			getMesh().streamVertices()
					.filter(v -> shape.contains(getMesh().toPoint(v)))
					.forEach(v -> {
						for(V u : getMesh().getAdjacentVertexIt(v)) {
							initialVertices.add(u);
							setAsInitialVertex(u);
						}
						initialVertices.add(v);
						setAsInitialVertex(v);
					});
		}
		setInitialVertices(initialVertices, IDistanceFunction.createToTargets(targetShapes));
	}


	@Override
	public void solve() {
		try {
			double ms = System.currentTimeMillis();
			getTriangulation().enableCache();
			nUpdates = 0;

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
			double runTime = (System.currentTimeMillis() - ms);
			logger.debug("fim parallel run time with " + nThreds + " threads = " + runTime);
			logger.debug("#nUpdates = " + nUpdates);
			logger.debug("#nVertices = " + getMesh().getNumberOfVertices());
			//logger.debug(getMesh().toPythonTriangulation(v -> getPotential(v)));
		} catch (ExecutionException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void unsolve() {
		getMesh().streamVerticesParallel().forEach(v -> {
			atomicBooleans.setValue(v, new AtomicBoolean(true));
		});

		getMesh().streamVerticesParallel().filter(v -> !isInitialVertex(v)).forEach(v -> {
			setUndefined(v);
			setPotential(v, Double.MAX_VALUE);
			setUnburning(v);
		});
		solved = false;
	}

	// sequential task
	private void initialActiveList() throws ExecutionException, InterruptedException {
		// run the task for exactly nThread thread
		List<List<V>> partition = CollectionUtils.split(getInitialVertices(), nThreds);
		forkJoinPool.submit(() -> IntStream.range(0, activeLists.size()).parallel().forEach(i -> {
					partition.get(i).stream().flatMap(v -> getMesh().streamVertices(v)).forEach(v -> {
					if(isUndefined(v)) {
						updatePotential(v, i);
					}
				});

		})).get();
	}

	private void march() throws ExecutionException, InterruptedException {
		while (!allEmpty()) {
			loadBalance();
			marchStep();
		}
	}

	private boolean allEmpty() throws ExecutionException, InterruptedException {
		return forkJoinPool.submit(() -> activeLists.parallelStream().allMatch(activeList -> activeList.isEmpty())).get();
	}

	private void loadBalance() throws ExecutionException, InterruptedException {
		int offset = random.nextInt(activeLists.size()) * 2 + 1;
		forkJoinPool.submit(() -> {
			IntStream.range(0, activeLists.size()).parallel().forEach(i -> {
				// i is odd
				if(i % 2 == 1) {
					int j = (i + offset) % nThreds;
					//System.out.println("i" + i + " j" + j);
					LinkedList<V> activeList = activeLists.get(i);
					LinkedList<V> otherActiveList = activeLists.get(j);
					while (otherActiveList.size() > activeList.size()) {
						activeList.addLast(otherActiveList.poll());
					}
					while(otherActiveList.size() < activeList.size()) {
						otherActiveList.addLast(activeList.poll());
					}
				}
			});
		}).get();
	}


	private void marchStep() throws ExecutionException, InterruptedException {
		forkJoinPool.submit(() -> {
			IntStream.range(0, activeLists.size()).parallel().forEach(i -> {
				LinkedList<V> activeList = activeLists.get(i);

				ListIterator<V> listIterator = activeList.listIterator();
				LinkedList<V> newActiveList = new LinkedList<>();
				while(listIterator.hasNext()) {
					V x = listIterator.next();
					double p = getPotential(x);
					double q =  Math.min(p, recomputePotential(x));

					// not converged
					if(!isBurned(x) && p > q) {
						setPotential(x, q);
					}
					// converged
					else {
						// check adjacent neighbors
						for(V xn : getMesh().getAdjacentVertexIt(x)) {
							if(getPotential(xn) > getPotential(x) && !isBurining(xn)) {
								p = getPotential(xn);
								q = recomputePotential(xn);
								if(p > q) {
									setPotential(xn, q);

									// atomic compare and set!
									//logger.debug(Thread.currentThread().getName());
									AtomicBoolean atomicBoolean = atomicBooleans.getValue(xn);
									if(!atomicBoolean.compareAndExchange(false, true)) {
										newActiveList.add(xn);
										//setBurning(xn);
									}
								}
							}
						}
						listIterator.remove();
						nUpdates++;
						setBurned(x);
						setUnburning(x);
					}
				}
				activeList.addAll(newActiveList);
			});
		}).get();
	}

	@Override
	protected boolean isBurining(@NotNull final V vertex) {
		AtomicBoolean atomicBoolean = atomicBooleans.getValue(vertex);
		return atomicBoolean.get();
	}

	@Override
	protected void setBurning(@NotNull final V vertex) {
		AtomicBoolean atomicBoolean = atomicBooleans.getValue(vertex);
		atomicBoolean.set(true);
	}

	@Override
	protected void setUnburning(@NotNull final V vertex) {
		AtomicBoolean atomicBoolean = atomicBooleans.getValue(vertex);
		atomicBoolean.set(false);
	}

	private void updatePotential(@NotNull final V vertex, final int i) {
		double potential = recomputePotential(vertex);
		if(potential < getPotential(vertex)) {

			// this has to be an atomic operation!
			AtomicBoolean atomicBoolean = atomicBooleans.getValue(vertex);
			if(!atomicBoolean.compareAndExchange(false, true)) {
				activeLists.get(i).add(vertex);
			}
			setPotential(vertex, potential);
			//setBurning(vertex);
		}

		if(isUndefined(vertex)) {
			logger.debug("could not set neighbour vertex" + vertex);
		}
	}

	@Override
	protected void compute() {
		try {
			march();
		} catch (ExecutionException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
