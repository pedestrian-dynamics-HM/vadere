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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The Weiler-Atherton-Algorithm (https://en.wikipedia.org/wiki/Weiler%E2%80%93Atherton_clipping_algorithm)
 * merges a set of polygons. Note this merging does not support holes that is if there is a hole it will be
 * filled by the merging algorithm. Two polygons will be merged if they overlap. Co-linear and duplicated points,
 * i.e. useless points, will be removed. If polygon A contains polygon B, the result will be equals to polygon A.
 *
 * @author Benedikt Zoennchen
 */
public class WeilerAtherton {

	private List<VPolygon> polygons;
	private final static double  EPSILON = 1.0E-12;

	/**
	 * A WeilerPoint is used to connect the intersection points of two polygons each represented by
	 * a face such that the path consistent of a subset of both half-edges of the two faces can be build.
	 *
	 */
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
	 * Each intersection point is connected to his twin (the connection point of the other face
	 * which is geometrically the same point).
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

		List<VPoint> intersectionPoints = new ArrayList<>();
		PVertex<WeilerPoint> ip = null;

		// compute intersections and add those to the two faces, this implementation is rather slow!

		boolean intersectionFound = true;
		while (intersectionFound) {
			List<PHalfEdge<WeilerPoint>> clippingEdges = clippingMesh.getEdges(clippingFace);
			List<PHalfEdge<WeilerPoint>> subjectEdges = subjectMesh.getEdges(subjectFace);
			intersectionFound = false;

			for(PHalfEdge<WeilerPoint> clippingEdge : clippingEdges) {
				for(PHalfEdge<WeilerPoint> subjectEdge : subjectEdges) {
					Optional<VPoint> optIntersectionPoint = equalIntersectionPoints(subjectEdge, subjectMesh, clippingEdge, clippingMesh);

					if(!optIntersectionPoint.isPresent()) {
						VLine l1 = subjectMesh.toLine(subjectEdge);
						VLine l2 = clippingMesh.toLine(clippingEdge);

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

							PHalfEdge<WeilerPoint> prev = clippingMesh.getPrev(subjectEdge);
							PHalfEdge<WeilerPoint> innerPrev = subjectMesh.getPrev(clippingEdge);

							PVertex<WeilerPoint> ip1 = IPolyConnectivity.splitEdge(subjectEdge, wp1, subjectMesh);
							PVertex<WeilerPoint> ip2 = IPolyConnectivity.splitEdge(clippingEdge, wp2, clippingMesh);

							wp1.setTwinPoint(ip2);
							wp2.setTwinPoint(ip1);

							intersectionPoints.add(intersectionPoint);
							intersectionFound = true;
							// go one step back
						}
					}
					else {
						intersectionPoints.add(optIntersectionPoint.get());
					}

					if(intersectionFound) {
						break;
					}
				}

				if(intersectionFound) {
					break;
				}
			}
		}

		return Pair.create(subjectFace, clippingFace);
	}

	/**
	 * Tests if the two edges from different faces have are geometrical equal points. If this is the case
	 * the points will be transformed into intersection points and they will be connected i.e. twins.
	 *
	 * @param subjectEdge   edge from the first face containing the first point
	 * @param subjectMesh   mesh which created the first face
	 * @param clippingEdge  edge from the second face containing the first point
	 * @param clippingMesh  mesh which created the second face
	 *
	 * @return a point which is the intersection point or null / empty if the two edges did not have an geometrical equal point
	 */
	private Optional<VPoint> equalIntersectionPoints(
			@NotNull final PHalfEdge<WeilerPoint> subjectEdge,
			@NotNull final PMesh<WeilerPoint> subjectMesh,
			@NotNull final PHalfEdge<WeilerPoint> clippingEdge,
			@NotNull final PMesh<WeilerPoint> clippingMesh) {
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

	/**
	 * Sets the twin of two points. This is necessary if there are two points of the original polygons
	 * which are geometrically the same. If this is the case those two points are intersection points but
	 * they already exists.
	 *
	 * @param v the vertex of the first point
	 * @param p the weiler point of the first point
	 * @param u the vertex of the second point
	 * @param q the weilper point of the second point
	 *
	 * @return a new intersection point
	 */
	private VPoint connectWeilerPoints(
			@NotNull final PVertex<WeilerPoint> v,
			@NotNull final WeilerPoint p,
			@NotNull final PVertex<WeilerPoint> u,
			@NotNull final WeilerPoint q) {
		p.setIntersectionPoint(true);
		q.setIntersectionPoint(true);
		p.setTwinPoint(u);
		q.setTwinPoint(v);
		return new VPoint(p);
	}

	/**
	 * Executes the Weiler-Atherton-Algorithm for all of its polygons.
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
			next = mesh.getNext(next);
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

		return Pair.create(GeometryUtils.toPolygon(GeometryUtils.filterUselessPoints(points, EPSILON)), null);
	}
}
