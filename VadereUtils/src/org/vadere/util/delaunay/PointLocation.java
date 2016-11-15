package org.vadere.util.delaunay;

import com.sun.javafx.UnmodifiableArrayList;

import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class PointLocation {

	private final Collection<Face> faces;
	private final List<VPoint> orderedPointList;
	private final List<List<HalfEdge>> halfeEdgesSegments;
	private final List<List<VPoint>> intersectionPointsInSegment;

	private Comparator<VPoint> pointComparatorX = (p1, p2) -> {
		double dx = p1.getX() - p2.getX();
		if(dx < 0) return -1;
		else if(dx > 0) return 1;
		else return 0;
	};

	private Comparator<VPoint> pointComparatorY = (p1, p2) -> {
		double dy = p1.getY() - p2.getY();
		if(dy < 0) return -1;
		else if(dy > 0) return 1;
		else return 0;
	};

	private class BetweenTwoPoints implements Predicate<HalfEdge> {

		private VPoint p1;
		private VPoint p2;

		private BetweenTwoPoints(final VPoint p1, final VPoint p2) {
			this.p1 = p1;
			this.p2 = p2;
		}

		@Override
		public boolean test(final HalfEdge halfEdge) {
			return (halfEdge.getEnd().getX() > p1.getX() && halfEdge.getPrevious().getEnd().getX() < p2.getX()) ||
					(halfEdge.getEnd().getX() > p2.getX() && halfEdge.getPrevious().getEnd().getX() < p1.getX());
		}
	}

	private class HalfEdgeComparator implements Comparator<HalfEdge> {

		private double x1;
		private double x2;

		private HalfEdgeComparator(final double x1, final double x2) {
			this.x1 = x1;
			this.x2 = x2;
		}

		@Override
		public int compare(final HalfEdge edge1, final HalfEdge edge2) {
			VLine line1 = edge1.toLine();
			VLine line2 = edge2.toLine();
			double slope1 = line1.slope();
			double slope2 = line2.slope();

			double yIntersection1 = line1.getY1() + (x1-line1.getX1()) * slope1;
			double yIntersection2 = line2.getY1() + (x1 - line2.getX1()) * slope2;

			if(yIntersection1 < yIntersection2) return -1;
			else if(yIntersection1 > yIntersection2) return 1;
			else return 0;
		}
	}

	public PointLocation(final Collection<Face> faces) {
		this.faces = faces;
		//TODO distinct is maybe slow here
		Set<VPoint> pointSet = faces.stream()
				.flatMap(face -> face.stream()).map(edge -> edge.getEnd())
				.sorted(pointComparatorX).collect(Collectors.toSet());

		orderedPointList = pointSet.stream().sorted(pointComparatorX).collect(Collectors.toList());
		halfeEdgesSegments = new ArrayList<>(orderedPointList.size()-1);
		intersectionPointsInSegment = new ArrayList<>(orderedPointList.size()-1);

		for(int i = 0; i < orderedPointList.size() - 1; i++) {
			VPoint p1 = orderedPointList.get(i);
			VPoint p2 = orderedPointList.get(i+1);
			List<HalfEdge> halfEdges = faces.stream().flatMap(face -> face.stream()).filter(new BetweenTwoPoints(p1, p2))
					.sorted(new HalfEdgeComparator(p1.getX(), p2.getX())).collect(Collectors.toList());
			List<VPoint> intersectionPoints = halfEdges.stream()
					.map(hf -> hf.toLine())
					.map(line -> intersectionWithX(p1.getX(), line)).collect(Collectors.toList());
			halfeEdgesSegments.add(halfEdges);
			intersectionPointsInSegment.add(intersectionPoints);
		}
	}

	private static VPoint intersectionWithX(double x, VLine line) {
		return new VPoint(x, (line.getY1() + (line.getX1()-x) * line.slope()));
	}

	public Optional<Face> getFace(final VPoint point) {
		int index = Collections.binarySearch(orderedPointList, point, pointComparatorX);
		int xSegmentIndex = (index >= 0) ? index : -index - 2;
		if(xSegmentIndex < 0 || xSegmentIndex >= intersectionPointsInSegment.size()) {
			return Optional.empty();
		}

		index = Collections.binarySearch(intersectionPointsInSegment.get(xSegmentIndex), point, pointComparatorY);
		int ySegmentIndex = (index >= 0) ? index : -index - 2;
		if(ySegmentIndex < 0 || ySegmentIndex >= halfeEdgesSegments.get(xSegmentIndex).size()) {
			return Optional.empty();
		}

		HalfEdge edge = halfeEdgesSegments.get(xSegmentIndex).get(ySegmentIndex);

		if(edge.getFace().contains(point)) {
			return Optional.of(edge.getFace());
		}
		else {
			if(edge.getTwin().isPresent()) {
				return Optional.of(edge.getTwin().get().getFace());
			}
			else {
				return Optional.empty();
			}
        }
	}
}
