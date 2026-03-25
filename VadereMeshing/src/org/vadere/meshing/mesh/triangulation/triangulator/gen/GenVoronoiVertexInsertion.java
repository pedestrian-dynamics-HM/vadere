package org.vadere.meshing.mesh.triangulation.triangulator.gen;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.impl.PSLG;
import org.vadere.meshing.mesh.inter.mesh.*;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.meshing.mesh.inter.mesh.builder.ITriangleMeshBuilder;
import org.vadere.meshing.mesh.inter.mesh.data.IMeshDataStorage;
import org.vadere.meshing.mesh.triangulation.triangulator.inter.IRefiner;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VPolygon;
import org.vadere.util.geometry.shapes.VTriangle;
import org.vadere.util.logging.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Implementation of the Voronoi-vertex point insertion method from [1] (rebay-1993).
 *
 * <ol>
 *     <li>Efficient Unstructured Mesh Generation by Means of Delaunay Triangulation and Bowyer-Watson Algorithm</li>
 * </ol>
 *
 * @param <V> the type of the vertices
 * @param <E> the type of the half-edges
 * @param <F> the type of the faces
 */
public class GenVoronoiVertexInsertion<V extends IVertex, E extends IHalfEdge, F extends IFace> implements IRefiner<V, E, F> {

	private static Logger logger = Logger.getLogger(GenVoronoiVertexInsertion.class);
	private final PSLG pslg;
	private final GenConstrainedDelaunayTriangulator<V, E, F> cdt;

	// Improvements: maybe mark edges which should not be flipped instead of using a Set is slower.
	private Set<E> segments;

	private boolean initialized;
	private boolean generated;

	// Improvements: use multiple unsorted queues to improve performance
	private PriorityQueue<F> heap;

	// Improvements: maybe save VTriangle inside the face container
	private Map<F, VTriangle> triangles;

	private boolean createHoles;
	private double minRadius;
	private Function<IPoint, Double> circumRadiusFunc;
	private final static int MAX_POINTS = 200_000;
	private DelaunayPlacement<V, E, F> placementStrategy;

	public GenVoronoiVertexInsertion(@NotNull final PSLG pslg,
									 @NotNull final Supplier<ITriangleMeshBuilder<V, E, F>> meshSupplier,
									 final boolean createHoles,
									 @NotNull Function<IPoint, Double> circumRadiusFunc) {

		this.segments = new HashSet<>();
		this.initialized = false;
		this.generated = false;
		this.heap = new PriorityQueue<>(new FaceCircumradiusComparator());
		this.triangles = new HashMap<>();
		this.createHoles = createHoles;
		this.minRadius = minRadius;
		this.circumRadiusFunc = circumRadiusFunc;
		this.pslg = pslg.addLines(generateLines(pslg));

		this.cdt = new GenConstrainedDelaunayTriangulator<>(meshSupplier, pslg, false);
		this.placementStrategy = new DelaunayPlacement<>(cdt.getMesh());
	}

	@Override
	public void refine() {
		if(!initialized) {
			cdt.generate(true);
			segments = new HashSet<>(cdt.getConstrains());
			getMesh().faces()
					.stream()
					.filter(f -> pslg.getSegmentBound().contains(getMesh().faces().toTriangle(f).midPoint()))
					.forEach(f -> add(f));

			getTriangulation().setCanIllegalPredicate(e -> !segments.contains(e) && !segments.contains(getMesh().edges().getTwin(e)));
			initialized = true;
		}
		else if(!refinementFinished()) {
			F face = heap.poll();

			//logger.info("#points = " + getMesh().getNumberOfVertices());
			//logger.info("" + getMesh().isDestroyed(face));
			//VTriangle triangle = getMesh().toTriangle(face);
			VTriangle triangle = triangles.get(face);
			VPoint circumcenter = this.placementStrategy.computePlacement(getMesh().edges().getAnyOf(face), triangle);
			Optional<F> locatedFace = locateFace(circumcenter.getX(), circumcenter.getY(), face);

			if(locatedFace.isPresent()) {
				//logger.info("insertVertex" + circumcenter);
				IPoint splitPoint = getMeshBuilder().getMesh().createPoint(circumcenter.getX(), circumcenter.getY());
				E edge = getMeshBuilder().changeConnectivity().splitTriangle(locatedFace.get(), splitPoint);
				V v = getMesh().vertices().getEndOf(edge);

				// update triangles
				for(F f : getMesh().faces().adjacentIterableFor(v)) {
					VTriangle tri = getMesh().faces().toTriangle(f);
					if(pslg.getSegmentBound().contains(tri.midPoint())) {
						heap.remove(f);
						add(f);
					}
				}
			}
		} else if(!isFinished()) {
			finish(true);
		} else {
			logger.info("finished");
		}
	}

	private Optional<F> locateFace(final double x, final double y, @NotNull final F face) {
		/*for(F f : getTriangulation().getMesh().getFaces()) {
			VTriangle triangle = getTriangulation().getMesh().toTriangle(f);
			if(triangle.contains(x, y)) {
				return Optional.of(f);
			}
		}
		return Optional.empty();*/
		Optional<F> optFace = getMesh().readConnectivity().locateMarch(x, y, face);
		if(optFace.isPresent() && !getMesh().faces().isBoundary(optFace.get())) {
			return optFace;
		}
		else {
			return Optional.empty();
		}
	}

	@Override
	public boolean isFinished() {
		return generated;
	}

	private boolean refinementFinished() {
		return initialized == true && (heap.isEmpty() || getMesh().vertices().getAll().size() >= MAX_POINTS);
	}

	@Override
	public IIncrementalTriangulation generate() {
		return generate(true);
	}

	@Override
	public IIncrementalTriangulation<V, E, F> generate(boolean finalize) {
		if(!generated) {
			while (!refinementFinished()) {
				refine();
			}
			finish(finalize);
		}
		return getTriangulation();
	}

	private void finish(boolean finalize) {
		generated = true;
		if(finalize) {
			getTriangulation().finish();
			removeTriangles();
		}
	}

	public void removeTriangles() {
		if(createHoles) {
			var connectivity = getMeshBuilder().changeConnectivity();

			for(VPolygon hole : pslg.getHoles()) {
				Predicate<F> mergeCondition = f -> !getMesh().faces().isBoundary(f) && hole.contains(getMesh().faces().toTriangle(f).midPoint());
				Optional<F> optFace = getMesh().faces().stream().filter(mergeCondition).findAny();
				if(optFace.isPresent()) {
					connectivity.createHole(optFace.get(), mergeCondition, true);
				}
			}
			Predicate<F> mergeCondition = f -> !pslg.getSegmentBound().contains(getMesh().faces().toTriangle(f).midPoint());
			connectivity.shrinkBorder(mergeCondition, true);
		}
	}

	public IIncrementalTriangulation<V, E, F> getTriangulation() {
		return cdt.getTriangulation();
	}

	public ITriangleMeshBuilder<V, E, F> getMeshBuilder() {
		return cdt.getMeshBuilder();
	}

	@Override
	public IMeshDataStorage<V, E, F> getMeshDataStorage() {
		return cdt.getMeshDataStorage();
	}

	private void add(@NotNull final F face) {
		VTriangle triangle = getMesh().faces().toTriangle(face);
		double radius = triangle.getCircumscribedRadius();
		VPoint circumcenter = triangle.getCircumcenter();
		if(radius > minRadius && triangle.getCircumscribedRadius() > circumRadiusFunc.apply(circumcenter)) {
			triangles.put(face, triangle);
			heap.add(face);
		}
	}

	// TODO duplicated code
	private List<VLine> generateLines(@NotNull final PSLG pslg) {
		List<VLine> polyLines = new ArrayList<>();

		polyLines.addAll(pslg.getSegmentBound().getLinePath());

		for(VPolygon polygon : pslg.getHoles()) {
			polyLines.addAll(polygon.getLinePath());
		}

		List<VLine> lines = new ArrayList<>();
		for(VLine line : polyLines) {
			List<VLine> splitLines = new ArrayList<>();
			splitLines.add(line);
			while (!splitLines.isEmpty()) {
				List<VLine> newSplitLines = new ArrayList<>();
				for(VLine splitLine : splitLines) {
					VPoint midPoint = splitLine.midPoint();
					double desiredLen = circumRadiusFunc.apply(midPoint) * Math.sqrt(3);
					double len = splitLine.length();
					if(len >  desiredLen) {
						newSplitLines.add(new VLine(splitLine.getVPoint1(), midPoint));
						newSplitLines.add(new VLine(midPoint, splitLine.getVPoint2()));
					} else {
						lines.add(splitLine);
					}
				}
				splitLines = newSplitLines;
			}
		}

		return lines;
	}

	private final class FaceCircumradiusComparator implements Comparator<F> {

		@Override
		public int compare(F o1, F o2) {
			return Double.compare(-triangles.get(o1).getCircumscribedRadius(), -triangles.get(o2).getCircumscribedRadius());
		}
	}
}
