package org.vadere.meshing.mesh.triangulation.triangulator.gen;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.inter.IFace;
import org.vadere.meshing.mesh.inter.IHalfEdge;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.meshing.mesh.inter.IMesh;
import org.vadere.meshing.mesh.inter.IMeshSupplier;
import org.vadere.meshing.mesh.inter.ITriEventListener;
import org.vadere.meshing.mesh.inter.IVertex;
import org.vadere.meshing.mesh.triangulation.triangulator.inter.IRefiner;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VPolygon;
import org.vadere.util.geometry.shapes.VTriangle;
import org.vadere.util.logging.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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

/**
 * Implementation of the Voronoi-vertex point insertion method from [1] (rebay-1993).
 *
 * <ol>
 *     <li>Efficient Unstructured Mesh Generation by Means of Delaunay Triangulation and Bowyer-Watson Algorithm</li>
 * </ol>
 *
 * @param <P> the type of the points (containers)
 * @param <CE> the type of container of the half-edges
 * @param <CF> the type of the container of the faces
 * @param <V> the type of the vertices
 * @param <E> the type of the half-edges
 * @param <F> the type of the faces
 */
public class GenDelaunayRefinement<P extends IPoint, CE, CF, V extends IVertex<P>, E extends IHalfEdge<CE>, F extends IFace<CF>> implements IRefiner<P, CE, CF, V, E, F> {

	private static Logger logger = Logger.getLogger(GenDelaunayRefinement.class);
	private VPolygon bound;
	private final Collection<VPolygon> constrains;
	private final GenConstrainedDelaunayTriangulator<P, CE, CF, V, E, F> cdt;
	private Set<E> segments;
	private boolean initialized;
	private boolean generated;
	private PriorityQueue<F> heap;
	private Map<F, Double> qualities;
	private boolean createHoles;
	private double minQuality;
	private Function<IPoint, Double> circumRadiusFunc;
	private final static int MAX_POINTS = 2000;

	public GenDelaunayRefinement(@NotNull final VPolygon bound,
	                             @NotNull final Collection<VPolygon> constrains,
	                             @NotNull final IMeshSupplier<P, CE, CF, V, E, F> meshSupplier,
	                             final boolean createHoles,
	                             final double minQuality,
	                             @NotNull Function<IPoint, Double> circumRadiusFunc) {
		this.bound = bound;
		this.constrains = constrains;
		this.segments = new HashSet<>();
		this.initialized = false;
		this.generated = false;
		this.heap = new PriorityQueue<>(new FaceQualityComparator());
		this.qualities = new HashMap<>();
		this.createHoles = createHoles;
		this.minQuality = minQuality;
		this.circumRadiusFunc = circumRadiusFunc;
		List<VLine> lines = new ArrayList<>();
		for(VPolygon polygon : constrains) {
			lines.addAll(polygon.getLinePath());
		}

		lines.addAll(bound.getLinePath());

		/**
		 * This prevent the flipping of constrained edges
		 */
		Predicate<E> canIllegal = e -> !segments.contains(e) && !segments.contains(getMesh().getTwin(e));
		this.cdt = new GenConstrainedDelaunayTriangulator<>(meshSupplier.get(), GeometryUtils.bound(bound.getPoints(), GeometryUtils.DOUBLE_EPS), lines, Collections.EMPTY_SET, canIllegal);
	}

	@Override
	public void refine() {
		if(!refinementFinished()) {
			if(!initialized) {
				cdt.generate();
				//getTriangulation().addTriEventListener(this);
				segments = new HashSet<>(cdt.getConstrains());
				removeTriangles();

				for(F face : getTriangulation().getMesh().getFaces()) {
					qualities.put(face, getTriangulation().faceToQuality(face));
					add(face);
				}

				initialized = true;
			}

			F face = heap.poll();
			logger.info("#points = " + getMesh().getNumberOfVertices());
			logger.info("" + getMesh().isDestroyed(face));

			VTriangle triangle = getMesh().toTriangle(face);
			VPoint circumcenter = triangle.getCircumcenter();
			Optional<F> optionalF = getTriangulation().locateFace(circumcenter.getX(), circumcenter.getY(), face);
			if(optionalF.isPresent() && !getMesh().isBoundary(optionalF.get())) {
				E edge = getTriangulation().splitTriangle(optionalF.get(),
						getMesh().createPoint(circumcenter.getX(), circumcenter.getY()));
				V v = getMesh().getVertex(edge);

				// update qualities
				for(F f : getMesh().getFaceIt(v)) {
					heap.remove(f);
					add(f);
				}
			}
		}
	}

	@Override
	public boolean isFinished() {
		return generated;
	}

	private boolean refinementFinished() {
		return initialized == true && (heap.isEmpty() || getMesh().getNumberOfVertices() >= MAX_POINTS);
	}

	@Override
	public IIncrementalTriangulation generate() {
		while (!refinementFinished()) {
			refine();
		}
		removeTriangles();
		generated = true;
		return cdt.generate();
	}

	public void removeTriangles() {
		if(createHoles) {
			for(VPolygon hole : constrains) {
				Predicate<F> mergeCondition = f -> hole.contains(getMesh().toTriangle(f).midPoint());
				Optional<F> optFace = getMesh().streamFaces().filter(mergeCondition).findAny();
				if(optFace.isPresent()) {
					Optional<F> optionalF = getTriangulation().createHole(optFace.get(), mergeCondition, true);
				}
			}

			if(bound != null) {
				Predicate<F> mergeCondition = f -> !bound.contains(getMesh().toTriangle(f).midPoint());
				getTriangulation().shrinkBorder(mergeCondition, true);
			}
		}
	}

	public IIncrementalTriangulation<P, CE, CF, V, E, F> getTriangulation() {
		return cdt.generate();
	}

	@Override
	public IMesh<P, CE, CF, V, E, F> getMesh() {
		return cdt.getMesh();
	}

	/*@Override
	public void postSplitTriangleEvent(@NotNull final  F original, @NotNull final F f1, @NotNull final F f2, @NotNull final F f3) {
		heap.remove(original);
		add(f1);
		add(f2);
		add(f3);
	}*/

	private void add(@NotNull final F face) {
		double quality = getTriangulation().faceToQuality(face);
		if(quality < minQuality) {
			qualities.put(face, quality);
			heap.add(face);
		}
		else {
			 VTriangle triangle = getMesh().toTriangle(face);
			 VPoint circumcenter = triangle.getCircumcenter();
			 if(triangle.getCircumscribedRadius() > circumRadiusFunc.apply(circumcenter)) {
				 qualities.put(face, quality);
				 heap.add(face);
			 }
		}
	}

	/*@Override
	public void postSplitHalfEdgeEvent(F original, F f1, F f2) {

	}

	@Override
	public void postFlipEdgeEvent(F f1, F f2) {

	}

	@Override
	public void postInsertEvent(V vertex) {

	}*/

	private final class FaceQualityComparator implements Comparator<F> {

		@Override
		public int compare(F o1, F o2) {
			return Double.compare(qualities.get(o1), qualities.get(o2));
		}
	}
}
