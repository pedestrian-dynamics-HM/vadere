package org.vadere.meshing.mesh.triangulation.triangulator.gen;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.inter.IFace;
import org.vadere.meshing.mesh.inter.IHalfEdge;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.meshing.mesh.inter.IMesh;
import org.vadere.meshing.mesh.inter.IMeshSupplier;
import org.vadere.meshing.mesh.inter.IVertex;
import org.vadere.meshing.mesh.triangulation.triangulator.inter.IRefiner;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VPolygon;
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

public class GenDirichletRefinement <P extends IPoint, CE, CF, V extends IVertex<P>, E extends IHalfEdge<CE>, F extends IFace<CF>> implements IRefiner<P, CE, CF, V, E, F> {

	private static Logger logger = Logger.getLogger(GenDelaunayRefinement.class);
	private VPolygon bound;
	private final Collection<VPolygon> constrains;
	private final GenConstrainedDelaunayTriangulator<P, CE, CF, V, E, F> cdt;
	private Set<E> segments;
	private boolean initialized;
	private boolean generated;
	private PriorityQueue<F> active;
	private Set<F> activeSet;
	private Set<F> accepted;
	private Map<F, Double> circumcenters;
	private boolean createHoles;
	private double minQuality;
	private Function<IPoint, Double> circumRadiusFunc;
	private final static int MAX_POINTS = 20000;
	private double delta = 1.5;

	public GenDirichletRefinement(@NotNull final VPolygon bound,
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
		this.active = new PriorityQueue<>(new GenDirichletRefinement.FaceQualityComparator());
		this.accepted = new HashSet<>();
		this.activeSet = new HashSet<>();
		this.circumcenters = new HashMap<>();
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

	private boolean isAccepted(@NotNull final E edge) {
		return circumRadiusFunc.apply(getMesh().toLine(edge).midPoint()) / getMesh().toTriangle(getMesh().getFace(edge)).getCircumscribedRadius() < 1.5;
	}

	@Override
	public void refine() {
		if(!refinementFinished()) {
			if(!initialized) {
				cdt.generate();
				segments = new HashSet<>(cdt.getConstrains());
				removeTriangles();

				for(F face : getTriangulation().getMesh().getFaces()) {
					if(!isAccepted(face) && isActive(face)) {
						circumcenters.put(face, getMesh().toTriangle(face).getCircumscribedRadius());
						active.add(face);
						activeSet.add(face);
					}
				}
				initialized = true;
			}

			if(!active.isEmpty()) {
				F face = active.poll();
				activeSet.remove(face);
				refine(face);
			}
		}
	}

	private void refine(@NotNull F face) {
		E shortestEdge = null;
		VLine shortestLine = null;
		for(E edge : getMesh().getEdgeIt(face)) {
			VLine tmpLine = getMesh().toLine(edge);
			if(isAccepted(getMesh().getTwinFace(edge)) && (shortestEdge == null || shortestLine.length() > tmpLine.length())) {
				shortestEdge = edge;
				shortestLine = tmpLine;
			}
		}

		VPoint midpoint = shortestLine.midPoint();
		VPoint c = getMesh().toTriangle(face).getCircumcenter();
		double pq = 0.5 * shortestLine.length();
		double pc = new VLine(c, new VPoint(getMesh().getPoint(shortestEdge))).length();

		double r = circumRadiusFunc.apply(midpoint);
		if(getMesh().isAtBoundary(shortestEdge)) {
			double s = ((2 * pq) / Math.sqrt(3));
			if(s / circumRadiusFunc.apply(getMesh().toTriangle(face).midPoint()) < delta) {
				r = s;
			}
		}

		double mc = new VLine(c, midpoint).length();
		double rmax = (pq * pq + pc * pc) / (2 * mc);
		r = Math.min(Math.max(r, 0.5 * shortestLine.length()), rmax);
		double d = Math.sqrt(r * r - pq * pq) + r;
		VPoint e;
		VPoint x;
		if(!getMesh().isBoundary(getMesh().getTwinFace(shortestEdge))) {
			VPoint cc = getMesh().toTriangle(getMesh().getTwinFace(shortestEdge)).getCircumcenter();
			e = c.subtract(cc).norm();
			x = midpoint.add(e.scalarMultiply(d));
		} else {
			if(c.distanceSq(midpoint) < GeometryUtils.DOUBLE_EPS) {
				x = midpoint;
			} else {
				// would otherwise result in a very large angle at the boundary
				if(d / Math.sqrt((3*pq * pq)) < 0.8) {
					x = midpoint;
				}
				else {
					e = c.subtract(midpoint).norm();
					x = midpoint.add(e.scalarMultiply(d));
				}
			}
		}

		Optional<F> optionalF = getTriangulation().locateFace(x.getX(), x.getY(), getMesh().getFace(shortestEdge));

		if(optionalF.isPresent() && !getMesh().isBoundary(optionalF.get())) {
			V v = getMesh().createVertex(x.getX(), x.getY());
			getTriangulation().insert(v, optionalF.get());

			// no point was inserted
			if(getMesh().getEdge(v) == null) {
				accepted.add(face);

				for(F f : getMesh().getFaceIt(face)) {
					if(activeSet.remove(f)) {
						active.remove(f);
					}

					if(!isAccepted(f) && isActive(f)) {
						circumcenters.put(f, getMesh().toTriangle(f).getCircumscribedRadius());
						active.add(f);
						activeSet.add(f);
					}
				}

			} else {
				// update circumcenters
				for(E ev : getMesh().getEdgeIt(v)) {

					E eRing = getMesh().getPrev(ev);
					F f1 = getMesh().getFace(eRing);
					F f2 = getMesh().getTwinFace(eRing);

					if(activeSet.remove(f1)) {
						active.remove(f1);
					}

					if(!isAccepted(f1) && isActive(f1)) {
						circumcenters.put(f1, getMesh().toTriangle(f1).getCircumscribedRadius());
						active.add(f1);
						activeSet.add(f1);
					}

					if(activeSet.remove(f2)) {
						active.remove(f2);
					}

					if(!isAccepted(f2) && isActive(f2)) {
						circumcenters.put(f2, getMesh().toTriangle(f2).getCircumscribedRadius());
						active.add(f2);
						activeSet.add(f2);
					}
				}
			}


		}
	}

	@Override
	public boolean isFinished() {
		return generated;
	}

	private boolean refinementFinished() {
		return initialized == true && (active.isEmpty() || getMesh().getNumberOfVertices() >= MAX_POINTS);
	}

	@Override
	public IIncrementalTriangulation generate() {
		while (!refinementFinished()) {
			refine();
		}
		removeTriangles();
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

	private boolean isAccepted(@NotNull final F face) {
		if(getMesh().isBoundary(face) || accepted.contains(face)) {
			return true;
		}
		else {
			double r = getMesh().toTriangle(face).getCircumscribedRadius();
			boolean accepted =  r / circumRadiusFunc.apply(getMesh().toTriangle(face).midPoint()) < delta && getTriangulation().faceToQuality(face) >= minQuality;
			if(accepted) {
				this.accepted.add(face);
			}
			return accepted;
		}
	}

	private boolean isActive(@NotNull final F face) {
		if(getMesh().isBoundary(face)) {
			return false;
		}

		for(F neighbour : getMesh().getFaceIt(face)) {
			if(isAccepted(neighbour)) {
				return true;
			}
		}

		return false;
	}

	private final class FaceQualityComparator implements Comparator<F> {

		@Override
		public int compare(F o1, F o2) {
			return Double.compare(-circumcenters.get(o1), -circumcenters.get(o2));
		}
	}
}
