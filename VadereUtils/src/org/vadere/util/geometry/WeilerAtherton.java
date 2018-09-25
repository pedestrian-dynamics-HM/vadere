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
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class WeilerAtherton {

	private VPolygon subject;
	private VPolygon clipping;
	private boolean directionRevert = true;
	private int i = 0;
	private int j = 1;
	private List<VPolygon> polygons;

	@Nullable private WeilerPoint wp;

	public static class WeilerPoint extends VPoint {
		private boolean isIntersectionPoint;
		private boolean isInside;
		@Nullable private PVertex<WeilerPoint> twinPoint;

		public WeilerPoint(@NotNull final VPoint point, final boolean isIntersectionPoint, final boolean inside) {
			super(point.x, point.y);
			this.isIntersectionPoint = isIntersectionPoint;
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

	public WeilerAtherton(@NotNull final List<VPolygon> polygons) {
		this.polygons = polygons;
	}

	public Pair<PFace<WeilerPoint>, PFace<WeilerPoint>> constructIntersectionFaces(
			@NotNull final VPolygon subject,
			@NotNull final PMesh<WeilerPoint> subjectMesh,
			@NotNull final VPolygon clipping,
			@NotNull final PMesh<WeilerPoint> clippingMesh) {

		PFace<WeilerPoint> subjectFace = subjectMesh.toFace(subject.getPath()
				.stream()
				.map(p -> new WeilerPoint(p, false, clipping.contains(p)))
				.collect(Collectors.toList()));

		PFace<WeilerPoint> clippingFace = clippingMesh.toFace(clipping.getPath()
				.stream()
				.map(p -> new WeilerPoint(p, false, subject.contains(p)))
				.collect(Collectors.toList()));

		PHalfEdge<WeilerPoint> start = subjectMesh.getEdge(subjectFace);
		PHalfEdge<WeilerPoint> next = start;

		PVertex<WeilerPoint> ip = null;

		// compute intersections and add those to the two faces, this implementation is rather slow!
		do {
			PHalfEdge<WeilerPoint> innerStart = clippingMesh.getEdge(clippingFace);
			PHalfEdge<WeilerPoint> innerNext = innerStart;
			do {
				VLine l1 = subjectMesh.toLine(next);
				VLine l2 = clippingMesh.toLine(innerNext);

				if (GeometryUtils.intersectLineSegment(l1.getP1().getX(), l1.getP1().getY(), l1.getP2().getX(), l1.getP2().getY(), l2.getP1().getX(), l2.getP1().getY(), l2.getP2().getX(), l2.getP2().getY())) {
					VPoint intersectionPoint = GeometryUtils.intersectionPoint(l1.getP1().getX(), l1.getP1().getY(), l1.getP2().getX(), l1.getP2().getY(), l2.getP1().getX(), l2.getP1().getY(), l2.getP2().getX(), l2.getP2().getY());
					WeilerPoint wp1 = new WeilerPoint(intersectionPoint, true, false);
					WeilerPoint wp2 = new WeilerPoint(intersectionPoint, true, false);

					PHalfEdge<WeilerPoint> prev = clippingMesh.getPrev(next);
					PHalfEdge<WeilerPoint> innerPrev = subjectMesh.getPrev(innerNext);

					PVertex<WeilerPoint> ip1 = IPolyConnectivity.splitEdge(next, wp1, subjectMesh);
					PVertex<WeilerPoint> ip2 = IPolyConnectivity.splitEdge(innerNext, wp2, clippingMesh);

					wp1.setTwinPoint(ip2);
					wp2.setTwinPoint(ip1);
					wp = wp1;

					// go one step back
					next = subjectMesh.getNext(start);
					innerNext = clippingMesh.getNext(innerStart);
				} else {
					innerNext = clippingMesh.getNext(innerNext);
				}
			} while (!innerStart.equals(innerNext));

			next = subjectMesh.getNext(next);
		} while (!start.equals(next));

		return Pair.create(subjectFace, clippingFace);
	}

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

	public Pair<VPolygon, VPolygon> merge(@NotNull final VPolygon subject, @NotNull final VPolygon clipping) {
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
		directionRevert = wp.isInside && clippingMesh.getPoint(wp.twinPoint).isInside;


		List<VPoint> points = new ArrayList<>();
		do {
			next = getNext(mesh, next, directionRevert && mesh == mesh2);
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

		return Pair.create(GeometryUtils.toPolygon(points), null);
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
