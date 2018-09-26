package org.vadere.util.geometry;

import org.apache.commons.math3.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vadere.util.geometry.mesh.gen.PFace;
import org.vadere.util.geometry.mesh.gen.PHalfEdge;
import org.vadere.util.geometry.mesh.gen.PMesh;
import org.vadere.util.geometry.mesh.gen.PVertex;
import org.vadere.util.geometry.mesh.inter.IPolyConnectivity;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VPolygon;
import org.vadere.util.potential.calculators.EikonalSolver;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The Weiler-Atherton-Algorithm (https://en.wikipedia.org/wiki/Weiler%E2%80%93Atherton_clipping_algorithm)
 * merges a set of polygons. Note this merging does not support holes. Two polygons will be merged if they overlap.
 * If polygon A contains polygon B, the result will be equals to polygon A.
 *
 * @author Benedikt Zoennchen
 */
public class WeilerAtherton {

	private List<VPolygon> polygons;
	private final static double  EPSILON = 1.0E-12;

	public static class WeilerPoint extends VPoint {
		private boolean isIntersectionPoint;
		private boolean isInside;
		@Nullable private PVertex<WeilerPoint> twinPoint;

		public WeilerPoint(@NotNull final VPoint point, final boolean isIntersectionPoint, final boolean inside) {
			super(point.x, point.y);
			this.isIntersectionPoint = isIntersectionPoint;
			this.isInside = inside;
			this.twinPoint = null;
		}

		public void setIntersectionPoint(final boolean intersectionPoint) {
			isIntersectionPoint = intersectionPoint;
		}

		public void setInside(final boolean inside) {
			isInside = inside;
		}

		public void setTwinPoint(@NotNull final PVertex<WeilerPoint> twinPoint) {
			this.twinPoint = twinPoint;
		}

		public boolean isIntersectionPoint() {
			return isIntersectionPoint;
		}

		@Nullable
		public PVertex<WeilerPoint> getTwinPoint() {
			return twinPoint;
		}

		public boolean isInside() {
			return isInside;
		}
	}

	/**
	 * The default constructor.
	 *
	 * @param polygons a List of {@link VPolygon} which will be merged.
	 */
	public WeilerAtherton(@NotNull final List<VPolygon> polygons) {
		this.polygons = polygons;
	}

	/**
	 * Constructs two faces which represents two polygons including their intersection points.
	 * Each intersection point is connected to his twin (the connection point of the other face).
	 *
	 * @param subject       the subject polygon
	 * @param subjectMesh   the subject mesh used to construct the face
	 * @param clipping      the clipping polygon
	 * @param clippingMesh  the clipping mesh used to constrcut the face
	 *
	 * @return two faces which represents two polygons including their intersection points (in CW order)
	 */
	public Pair<PFace<WeilerPoint>, PFace<WeilerPoint>> constructIntersectionFaces(
			@NotNull final VPolygon subject,
			@NotNull final PMesh<WeilerPoint> subjectMesh,
			@NotNull final VPolygon clipping,
			@NotNull final PMesh<WeilerPoint> clippingMesh) {

		Set<VPoint> clipPointSet = new HashSet<>();
		Set<VPoint> subPointSet = new HashSet<>();

		clipPointSet.addAll(clipping.getPath());
		subPointSet.addAll(subject.getPath());

		PFace<WeilerPoint> subjectFace = subjectMesh.toFace(subject.getPath()
				.stream()
				.map(p -> new WeilerPoint(p, false, clipPointSet.contains(p) || clipping.contains(p)))
				.collect(Collectors.toList()));

		PFace<WeilerPoint> clippingFace = clippingMesh.toFace(clipping.getPath()
				.stream()
				.map(p -> new WeilerPoint(p, false, subPointSet.contains(p) || subject.contains(p)))
				.collect(Collectors.toList()));

		PHalfEdge<WeilerPoint> start = subjectMesh.getEdge(subjectFace);
		PHalfEdge<WeilerPoint> next = start;

		PVertex<WeilerPoint> ip = null;

		// compute intersections and add those to the two faces, this implementation is rather slow!
		List<VPoint> intersectionPoints = new ArrayList<>();
		do {
			PHalfEdge<WeilerPoint> innerStart = clippingMesh.getEdge(clippingFace);
			PHalfEdge<WeilerPoint> innerNext = innerStart;
			do {
				Optional<VPoint> optIntersectionPoint = equalIntersectionPoints(next, subjectMesh, innerNext, clippingMesh);

				if(!optIntersectionPoint.isPresent()) {
					VLine l1 = subjectMesh.toLine(next);
					VLine l2 = clippingMesh.toLine(innerNext);

					VPoint intersectionPoint = null;

					if(!(l1.getP1().equals(l2.getP1()) || l1.getP1().equals(l2.getP2()) || l1.getP2().equals(l2.getP1()) || l1.getP2().equals(l2.getP2()))) {
						if(GeometryUtils.distanceToLineSegment(new VPoint(l1.getP1()), new VPoint(l1.getP2()), new VPoint(l2.getP1())) <= EPSILON) {
							intersectionPoint = new VPoint(l2.getP1());
						} else if(GeometryUtils.distanceToLineSegment(new VPoint(l1.getP1()), new VPoint(l1.getP2()), new VPoint(l2.getP2())) <= EPSILON) {
							intersectionPoint = new VPoint(l2.getP2());
						} else if(GeometryUtils.distanceToLineSegment(new VPoint(l2.getP1()), new VPoint(l2.getP2()), new VPoint(l1.getP1())) <= EPSILON) {
							intersectionPoint = new VPoint(l1.getP1());
						} else if(GeometryUtils.distanceToLineSegment(new VPoint(l2.getP1()), new VPoint(l2.getP2()), new VPoint(l1.getP2())) <= EPSILON) {
							intersectionPoint = new VPoint(l1.getP2());
						} else if (GeometryUtils.intersectLineSegment(l1.getP1().getX(), l1.getP1().getY(), l1.getP2().getX(), l1.getP2().getY(), l2.getP1().getX(), l2.getP1().getY(), l2.getP2().getX(), l2.getP2().getY())) {
							intersectionPoint = GeometryUtils.intersectionPoint(l1.getP1().getX(), l1.getP1().getY(), l1.getP2().getX(), l1.getP2().getY(), l2.getP1().getX(), l2.getP1().getY(), l2.getP2().getX(), l2.getP2().getY());
						}
					}

					if(intersectionPoint != null) {

						WeilerPoint wp1 = new WeilerPoint(intersectionPoint, true, false);
						WeilerPoint wp2 = new WeilerPoint(intersectionPoint, true, false);

						PHalfEdge<WeilerPoint> prev = clippingMesh.getPrev(next);
						PHalfEdge<WeilerPoint> innerPrev = subjectMesh.getPrev(innerNext);

						PVertex<WeilerPoint> ip1 = IPolyConnectivity.splitEdge(next, wp1, subjectMesh);
						PVertex<WeilerPoint> ip2 = IPolyConnectivity.splitEdge(innerNext, wp2, clippingMesh);

						wp1.setTwinPoint(ip2);
						wp2.setTwinPoint(ip1);

						intersectionPoints.add(intersectionPoint);
						// go one step back
						next = subjectMesh.getNext(start);
						innerNext = clippingMesh.getNext(innerStart);
					}
					else {
						innerNext = clippingMesh.getNext(innerNext);
					}
				}
				else {
					intersectionPoints.add(optIntersectionPoint.get());
					innerNext = clippingMesh.getNext(innerNext);
				}

			} while (!innerStart.equals(innerNext));

			next = subjectMesh.getNext(next);
		} while (!start.equals(next));

		//System.out.println(intersectionPoints);

		return Pair.create(subjectFace, clippingFace);
	}

	private Optional<VPoint> equalIntersectionPoints(PHalfEdge<WeilerPoint> subjectEdge, PMesh<WeilerPoint> subjectMesh, PHalfEdge<WeilerPoint> clippingEdge, PMesh<WeilerPoint> clippingMesh) {
		PVertex<WeilerPoint> v1 = subjectMesh.getVertex(subjectMesh.getPrev(subjectEdge));
		PVertex<WeilerPoint> v2 = subjectMesh.getVertex(subjectEdge);

		PVertex<WeilerPoint> u1 = clippingMesh.getVertex(clippingMesh.getPrev(clippingEdge));
		PVertex<WeilerPoint> u2 = clippingMesh.getVertex(clippingEdge);

		WeilerPoint p1 = subjectMesh.getPoint(v1);
		WeilerPoint p2 = subjectMesh.getPoint(v2);

		WeilerPoint q1 = clippingMesh.getPoint(u1);
		WeilerPoint q2 = clippingMesh.getPoint(u2);


		if(p1.equals(q1) && !p1.isIntersectionPoint()) {
			return Optional.of(connectWeilerPoints(v1, p1, u1, q1));
		} else if(p1.equals(q2) && !p1.isIntersectionPoint()) {
			return Optional.of(connectWeilerPoints(v1, p1, u2, q2));
		} else if(p2.equals(q1) && !p2.isIntersectionPoint()) {
			return Optional.of(connectWeilerPoints(v2, p2, u1, q1));
		} else if(p2.equals(q2) && !p2.isIntersectionPoint()) {
			return Optional.of(connectWeilerPoints(v2, p2, u2, q2));
		}
		else {
			return Optional.empty();
		}
	}

	private VPoint connectWeilerPoints(PVertex<WeilerPoint> v, WeilerPoint p, PVertex<WeilerPoint> u, WeilerPoint q) {
		p.setIntersectionPoint(true);
		q.setIntersectionPoint(true);
		p.setTwinPoint(u);
		q.setTwinPoint(v);
		return new VPoint(p);
	}

	/**
	 * Executes the Weiler-Atherton-Algorithm.
	 *
	 * @return a list of merged polygons
	 */
	public List<VPolygon> execute() {

		boolean merged = true;
		List<VPolygon> newPolygons = new ArrayList<>();
		newPolygons.addAll(polygons);

		while (merged) {
			int ii = -1;
			int jj = -1;
			merged = false;
			Pair<VPolygon, VPolygon> mergeResult = null;
			for(int i = 0; i < newPolygons.size(); i++) {
				VPolygon first = newPolygons.get(i);

				for(int j = i+1; j < newPolygons.size(); j++) {
					VPolygon second = newPolygons.get(j);

					mergeResult = merge(first, second);

					// something got merged
					if(mergeResult.getSecond() == null) {
						merged = true;
						ii = i;
						jj = j;
						break;
					}
				}

				if(merged) {
					break;
				}
			}

			if(merged) {
				newPolygons.remove(ii);
				newPolygons.remove(jj-1);
				newPolygons.add(mergeResult.getFirst());
			}
		}

		return newPolygons;
	}

	/**
	 * Executes the Weiler-Atherton-Algorithm for two polygons.
	 *
	 * @param subjectCandidat   the first polygon
	 * @param clippingCandidat  the second polygon
	 * @return a pair of polygon where the second element is null if the polygons got merged. This is not the case if there do not overlap.
	 */
	public Pair<VPolygon, VPolygon> merge(@NotNull final VPolygon subjectCandidat, @NotNull final VPolygon clippingCandidat) {

		VPolygon subject = GeometryUtils.isCCW(subjectCandidat) ? subjectCandidat.revertOrder() : subjectCandidat;
		VPolygon clipping = GeometryUtils.isCCW(clippingCandidat) ? clippingCandidat.revertOrder() : clippingCandidat;


		PMesh<WeilerPoint> subjectMesh = new PMesh<>((x,y) -> new WeilerPoint(new VPoint(x,y), false, false));
		PMesh<WeilerPoint> clippingMesh = new PMesh<>((x,y) -> new WeilerPoint(new VPoint(x,y), false, false));

		List<VPolygon> result = new ArrayList<>(2);

		Pair<PFace<WeilerPoint>, PFace<WeilerPoint>> pair = constructIntersectionFaces(subject, subjectMesh, clipping, clippingMesh);

		PFace<WeilerPoint> subjectFace = pair.getFirst();
		PFace<WeilerPoint> clippingFace = pair.getSecond();

		// phase (2)
		Optional<PHalfEdge<WeilerPoint>> intersectionEdges = subjectMesh.findAnyEdge(p -> p.isIntersectionPoint());
		Optional<PHalfEdge<WeilerPoint>> optStartPointSub = subjectMesh.findAnyEdge(p -> !p.isIntersectionPoint() && !p.isInside());
		Optional<PHalfEdge<WeilerPoint>> optStartPointClip = clippingMesh.findAnyEdge(p -> !p.isIntersectionPoint() && !p.isInside());

		// no point intersection and no point is contained in the other polygon => polygons do not overlap at all
		if(optStartPointSub.isPresent() && optStartPointClip.isPresent() && !intersectionEdges.isPresent()) {
			result.add(subject);
			result.add(clipping);
			return Pair.create(subject, clipping);
		}

		// no point intersections and there is a point of the subject outside of the clipping => clipping is fully contained in subject
		if(optStartPointSub.isPresent() && !intersectionEdges.isPresent()) {
			result.add(subject);
			return Pair.create(subject, null);
		}

		// no point intersections and there is a point of the clipping outside of the subject => subject is fully contained in clipping
		if(optStartPointClip.isPresent() && !intersectionEdges.isPresent()) {
			result.add(clipping);
			return pair.create(clipping, null);
		}

		PHalfEdge<WeilerPoint> first = null;
		PHalfEdge<WeilerPoint> next = null;
		PMesh<WeilerPoint> mesh1 = null;
		PMesh<WeilerPoint> mesh2 = null;


		if(intersectionEdges.isPresent() && optStartPointClip.isPresent()) {
			first = optStartPointClip.get();
			next = first;
			mesh1 = clippingMesh;
			mesh2 = subjectMesh;
		}
		else if(intersectionEdges.isPresent() && optStartPointSub.isPresent()) {
			first = optStartPointSub.get();
			next = first;
			mesh1 = subjectMesh;
			mesh2 = clippingMesh;
		}

		PMesh<WeilerPoint> mesh = mesh1;
		List<VPoint> points = new ArrayList<>();
		do {
			next = getNext(mesh, next, false);
			WeilerPoint wp = mesh.getPoint(next);

			// walk into the other mesh / polygon
			if(wp.isIntersectionPoint()) {
				// swap meshes
				mesh = mesh == mesh1 ? mesh2 : mesh1;

				// swap edges
				next = mesh.getEdge(wp.twinPoint);

			}

			points.add(new VPoint(wp));
		}
		while (!next.equals(first));

		return Pair.create(GeometryUtils.toPolygon(filterColinearPoints(points)), null);
	}

	private List<VPoint> filterColinearPoints(@NotNull final List<VPoint> points) {
		assert points.size() >= 3;
		List<VPoint> filteredList = new ArrayList<>(points);

		boolean removePoint = false;

		do {
			removePoint = false;
			for(int i = 0; i < filteredList.size(); i++) {

				VPoint p1 = filteredList.get((i + filteredList.size()-1) % filteredList.size());
				VPoint p2 = filteredList.get(i);
				VPoint p3 = filteredList.get((i + 1) % filteredList.size());

				if(p2.equals(p1) || p2.equals(p3) || GeometryUtils.distanceToLineSegment(p1, p3, p2) <= EPSILON) {
					filteredList.remove(i);
					removePoint = true;
					break;
				}
			}
		} while (removePoint);

		return filteredList;

	}

	private PHalfEdge<WeilerPoint> getNext(@NotNull final PMesh<WeilerPoint> mesh, @NotNull final PHalfEdge<WeilerPoint> edge, boolean reverse) {
		if(reverse) {
			return mesh.getPrev(edge);
		}
		else {
			return mesh.getNext(edge);
		}
	}

}
