package org.vadere.meshing.mesh.impl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.WeilerAtherton;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VPolygon;
import org.vadere.util.geometry.shapes.VRectangle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class represents a (segment-bounded) planar straight line graph (PLSG).
 * The PLSG is counter clockwise oriented which means that
 * its segment bound polygon is counter clockwise oriented and its
 * holes are clockwise oriented..
 */
public class PSLG {

	/**
	 * points not part of any segment
	 */
	private final ImmutableSet<VPoint> points;

	/**
	 * all points including points of the segments.
	 */
	private final ImmutableSet<VPoint> allPoints;

	/**
	 * all points including points of the segments.
	 */
	private final ImmutableSet<VLine> allSegments;

	/**
	 * segments not part of a polygon.
	 */
	private final ImmutableList<VLine> segments;

	/**
	 * holes
	 */
	private final ImmutableSet<VPolygon> holes;

	/**
	 * the segment-bound.
	 */
	private final VPolygon segmentBound;
	private final Set<VCircle> protectionDiscs;
	private final double factor = 1.0 / 3.0;
	private final double minAngle = Math.toRadians(95);

	private PSLG(@NotNull final VPolygon segmentBound,
	             @NotNull final Collection<VPolygon> holes,
	             @NotNull final Collection<VLine> segments,
	             @NotNull final Collection<VPoint> points,
	             @NotNull final Set<VCircle> protectionDiscs
	   ){
		Stream<VLine> holeLineStream1 = holes.stream().flatMap(hole -> hole.getLinePath().stream());
		Stream<VLine> holeLineStream2 = holes.stream().flatMap(hole -> hole.getLinePath().stream());
		Stream<VLine> segmentBoundLineStream1 = segmentBound.getLinePath().stream();
		Stream<VLine> segmentBoundLineStream2 = segmentBound.getLinePath().stream();
		Stream<VLine> lineStream1 = Stream.concat(Stream.concat(holeLineStream1, segmentBoundLineStream1), segments.stream());
		Stream<VLine> lineStream2 = Stream.concat(Stream.concat(holeLineStream2, segmentBoundLineStream2), segments.stream());

		Stream<VPoint> linePointStream = lineStream1.flatMap(line -> Stream.of(line.getVPoint1(), line.getVPoint2()));
		Stream<VPoint> pointStream = Stream.concat(points.stream(), linePointStream);


		Stream<VPolygon> ccwPolygons = holes.stream().map(hole -> hole.isCCW() ? hole.revertOrder() : hole);

		this.segmentBound = segmentBound.isCCW() ? segmentBound : segmentBound.revertOrder();
		this.holes = new ImmutableSet.Builder<VPolygon>().addAll(ccwPolygons.collect(Collectors.toList())).build();
		this.segments = new ImmutableList.Builder<VLine>().addAll(segments).build();
		this.allSegments = new ImmutableSet.Builder<VLine>().addAll(lineStream2.collect(Collectors.toList())).build();
		this.points = new ImmutableSet.Builder<VPoint>().addAll(points).build();
		this.allPoints = new ImmutableSet.Builder<VPoint>().addAll(pointStream.collect(Collectors.toList())).build();
		this.protectionDiscs = protectionDiscs;
	}

	public PSLG(@NotNull final VPolygon segmentBound,
	            @NotNull final Collection<VPolygon> holes) {
		this(segmentBound, holes, Collections.EMPTY_LIST, Collections.EMPTY_LIST);
	}

	public PSLG(@NotNull final VPolygon segmentBound,
	            @NotNull final Collection<VPolygon> holes,
	            @NotNull final Collection<VLine> segments,
	            @NotNull final Collection<VPoint> points) {
		this(segmentBound, holes, segments, points, new HashSet<>());
	}

	public PSLG conclose(@NotNull final VRectangle rectangle) {
		if(!rectangle.contains(segmentBound.getBounds2D())) {
			throw new IllegalArgumentException();
		}
		ArrayList<VLine> newLines = new ArrayList<>(segments.size() + segmentBound.getLinePath().size());
		newLines.addAll(segments);
		newLines.addAll(segmentBound.getLinePath());
		PSLG pslg = new PSLG(new VPolygon(rectangle), holes, newLines, points);
		pslg.protectionDiscs.addAll(this.protectionDiscs);
		return pslg;
	}

	public PSLG addLines(@NotNull final Collection<VLine> lines) {
		ArrayList<VLine> newLines = new ArrayList<>(segments.size() + lines.size());
		newLines.addAll(segments);
		newLines.addAll(lines);
		PSLG pslg = new PSLG(segmentBound, holes, newLines, points);
		pslg.protectionDiscs.addAll(this.protectionDiscs);
		return pslg;
	}

	public PSLG toProtectedPSLG(final double lfs) {
		Set<VCircle> protectionDiscs = new HashSet<>();
		List<VPolygon> nHoles = holes.stream().map(poly -> protect(Double.POSITIVE_INFINITY, poly, protectionDiscs, true)).collect(Collectors.toList());
		VPolygon segmentBound = protect(Double.POSITIVE_INFINITY, this.segmentBound, protectionDiscs, false);
		return new PSLG(segmentBound, nHoles, segments, points, protectionDiscs);
	}

	public ImmutableSet<VLine> getAllSegments() {
		return allSegments;
	}

	public Collection<VLine> getSegments() {
		return segments;
	}

	public Set<VPoint> getPoints() {
		return points;
	}

	public VPolygon getSegmentBound() {
		return segmentBound;
	}

	public Collection<VPolygon> getHoles() {
		return holes;
	}

	public Collection<VPolygon> getAllPolygons() {
		List<VPolygon> polygons = new ArrayList<>(holes.size()+1);
		polygons.add(segmentBound);
		polygons.addAll(holes);
		return polygons;
	}

	public VRectangle getBoundingBox() {
		return GeometryUtils.boundRelative(allPoints);
	}

	public ImmutableSet<VPoint> getAllPoints() {
		return allPoints;
	}

	private VPolygon protect(final double lfs, @NotNull final VPolygon polygon, @NotNull final Set<VCircle> protectionDiscs, boolean isHole) {
		List<VPoint> points = polygon.getPath();
		List<VPoint> newpoints = new ArrayList<>(points.size() + 5);

		assert points.size() >= 3;

		VPoint p1 = points.get(points.size()-1);
		VPoint p3 = points.get(1);
		VPoint lastP3 = points.get(0);

		for(int i = 0; i < points.size(); i++) {
			VPoint p2 = points.get(i);
			if(isTooAccute(p1, p2, p3, isHole)) {
				VPoint anchor = p2;
				double radius = factor * Math.min(lfs, Math.min(anchor.distance(p1), anchor.distance(p3)));
				VCircle protectionDisk = new VCircle(anchor, radius);
				VPoint dir1 = p1.subtract(anchor).setMagnitude(radius);
				VPoint dir2 = p3.subtract(anchor).setMagnitude(radius);
				VPoint q1 = anchor.add(dir1);
				VPoint q2 = anchor.add(dir2);
				protectionDiscs.add(protectionDisk);

				newpoints.add(q1);
				newpoints.add(p2);
				newpoints.add(q2);
				p1 = q2;

				if(i == 0) {
					lastP3 = q1;
				}

				if(i == points.size()-2) {
					p3 = lastP3;
				} else {
					p3 = points.get((i+2) % points.size());
				}

			} else {
				newpoints.add(p2);
				p1 = p2;
				p3 = points.get((i+2) % points.size());
			}
		}

		return GeometryUtils.toPolygon(newpoints);

	}

	private boolean isTooAccute(@NotNull final VPoint p1, @NotNull final VPoint p2, @NotNull final VPoint p3, boolean isHole) {
		return (!isHole && GeometryUtils.isCCW(p1, p2, p3)) || (isHole && GeometryUtils.isCW(p1,p2,p3)) && GeometryUtils.angle(p1, p2, p3) < minAngle;
	}

	public static List<VPolygon> constructHoles(@NotNull final List<VPolygon> polygons) {
		WeilerAtherton weilerAtherton = new WeilerAtherton(polygons);
		return weilerAtherton.cup();
	}

	public static List<VPolygon> constructBound(@NotNull final VPolygon bound, @NotNull final Collection<VPolygon> polygons) {
		List<VPolygon> originalList = new ArrayList<>();
		List<VPolygon> leftPolygons = new ArrayList<>();
		originalList.add(bound);
		leftPolygons.add(bound);
		for(VPolygon polygon : polygons) {
			if(!bound.containsShape(polygon)) {
				originalList.add(polygon);
			} else {
				leftPolygons.add(polygon);
			}
		}
		WeilerAtherton weilerAtherton = new WeilerAtherton(originalList);
		VPolygon newBound = weilerAtherton.subtraction().get();
		leftPolygons.set(0, newBound);
		return leftPolygons;
	}
}
