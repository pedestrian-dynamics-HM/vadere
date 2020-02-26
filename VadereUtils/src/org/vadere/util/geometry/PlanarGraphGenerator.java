package org.vadere.util.geometry;

import com.github.davidmoten.rtree.RTree;
import com.github.davidmoten.rtree.geometry.Rectangle;
import com.github.davidmoten.rtree.geometry.internal.RectangleDouble;

import org.jetbrains.annotations.NotNull;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * This class generates a planar graph based on a list of possibly intersecting
 * and co-linear lines. By line we mean line-segment. There are multiple cases for line intersection:
 *
 * 1. two lines intersect at one point which is not an endpoint of the line (general case)
 * 2. two lines are identical (the have the same end point)
 * 3. two lines share infinitely many points but are not identical
 * 4. two lines share exactly one endpoint
 *
 * @author Benedikt Zoennchen
 */
public class PlanarGraphGenerator {

	private RTree<String, VLine> lineRTree;
	private RTree<String, VLine> unresolvedLines;
	private ArrayList<VLine> allLines;
	private double tol;
	private double ccwTol;

	/**
	 * Default constructor.
	 *
	 * @param lines collection of intersecting lines
	 * @param tol   a distance tolerance at which two points are seen as identical
	 */
	public PlanarGraphGenerator(@NotNull final Collection<VLine> lines, final double tol) {
		this.allLines = new ArrayList<>();
		this.tol = tol;
		this.ccwTol = tol;
		this.allLines.addAll(lines);
	}

	private void reset() {
		this.lineRTree = RTree.create();
		this.unresolvedLines = RTree.create();

		for(VLine line : allLines) {
			lineRTree = lineRTree.add(line.toString(), line);
			unresolvedLines = unresolvedLines.add(line.toString(), line);
		}
	}

	/**
	 * Computes and returns non-intersecting lines.
	 * The computation is supported by a R-Tree data structure such that
	 * we avoid to test all pairs of lines.
	 *
	 * @return non-intersecting lines.
	 */
	public Collection<VLine> generate() {
		reset();
		boolean hasChanged;

		do {
			hasChanged = false;
			ArrayList<VLine> allLines = new ArrayList<>();
			unresolvedLines.entries().map(e -> e.geometry()).forEach(line -> allLines.add(line));
			//int size = unresolvedLines.size();
			//System.out.println(size);
			for(VLine line1 : allLines) {
				Rectangle mbr = line1.mbr();
				RectangleDouble bufferedMbr = RectangleDouble.create(mbr.x1() - tol, mbr.y1() - tol, mbr.x2() + tol, mbr.y2() + tol);
				ArrayList<VLine> otherLines = new ArrayList<>();

				unresolvedLines.search(bufferedMbr).map(e -> e.geometry()).forEach(l -> otherLines.add(l));
				//lineRTree.entries().map(e -> e.geometry()).forEach(line -> otherLines.add(line));

				for(VLine line2 : otherLines) {
					if(line1 != line2) {
						deleteLine(line1);
						deleteLine(line2);
						List<VLine> result = new ArrayList<>();
						hasChanged = adjustLines(line1, line2, result);
						for(VLine line : result) {
							addLine(line);
						}

						if(hasChanged) {
							break;
						}
					}
				}

				if(hasChanged) {
					break;
				} else {
					unresolvedLines = unresolvedLines.delete(line1.toString(), line1);
				}
			}

		} while (hasChanged);


		ArrayList<VLine> result = new ArrayList<>();
		lineRTree.entries().map(e -> e.geometry()).forEach(line -> result.add(line));
		return result;
	}

	private void addLine(@NotNull final VLine line) {
		unresolvedLines = unresolvedLines.add(line.toString(), line);
		lineRTree = lineRTree.add(line.toString(), line);
	}

	private void deleteLine(@NotNull final VLine line) {
		int ssize = unresolvedLines.size();
		unresolvedLines = unresolvedLines.delete(line.toString(), line);
		lineRTree = lineRTree.delete(line.toString(), line);
		assert unresolvedLines.size() == ssize - 1 : line.toString();
	}

	/**
	 * Computes and returns non-intersecting lines.
	 * The computation is done via a bruteforce method by testing each pair of lines which can be computational expensive.
	 *
	 * @return non-intersecting lines.
	 */
	public Collection<VLine> generateBruteForce() {
		reset();
		boolean hasChanged;

		do {
			hasChanged = false;
			for(int i = 0; i < allLines.size(); i++) {

				for(int j = 0; j < allLines.size(); j++) {
					if(j != i) {
						List<VLine> result = new ArrayList<>();
						boolean adjust = adjustLines(allLines.get(i), allLines.get(j), result);
						if(adjust) {
							if(i < j) {
								allLines.remove(j);
								allLines.remove(i);
							}
							else {
								allLines.remove(i);
								allLines.remove(j);
							}
							allLines.addAll(result);
							hasChanged = true;
							break;
						}
					}
				}
				if(hasChanged) {
					break;
				}
			}
		} while (hasChanged);

		return allLines;
	}

	/**
	 * Resolves the possible intersection of two lines and writes the resulting lines (at most 4)
	 * back into the result list.
	 *
	 * @param line1     the first line
	 * @param line2     the second line
	 * @param result    an empty list of lines
	 * @return true if some lines changed (a line got deleted, split, moved), otherwise (no treatment required) false
	 */
	private boolean adjustLines(@NotNull final VLine line1, @NotNull final VLine line2, @NotNull final List<VLine> result) {

		assert result.isEmpty();

		// two lines can only intersect if their bounding box intersect
		//if(line1.mbr().intersects(line2.mbr())) {

			// case 1: both lines are identical
			if(equals(line1, line2)) {
				result.add(line1);
				return true;
			} // case 2: some end point of a line is very close to the other line
			else if (closeEndPoints(line1, line2)) {
				double ccw11 = GeometryUtils.ccw(line1.x1, line1.y1, line1.x2, line1.y2, line2.x1, line2.y1);
				double ccw12 = GeometryUtils.ccw(line1.x1, line1.y1, line1.x2, line1.y2, line2.x2, line2.y2);
				double ccw21 = GeometryUtils.ccw(line2.x1, line2.y1, line2.x2, line2.y2, line1.x1, line1.y1);
				double ccw22 = GeometryUtils.ccw(line2.x1, line2.y1, line2.x2, line2.y2, line1.x2, line1.y2);

				if(Math.abs(ccw11) < ccwTol && Math.abs(ccw12) < ccwTol) {
					return handleCoLinearLines(line1, line2, result);
				}   // case 3 only 1 point of some segment is co-linear to the other segment
				else if(Math.abs(ccw11) < ccwTol) {
					return handleCoLinearPoint(line1, line2.getVPoint1(), line2, result);
				} else if(Math.abs(ccw12) < ccwTol) {
					return handleCoLinearPoint(line1, line2.getVPoint2(), line2, result);
				} else if(Math.abs(ccw21) < ccwTol) {
					return handleCoLinearPoint(line2, line1.getVPoint1(), line1, result);
				} else if(Math.abs(ccw22) < ccwTol) {
					return handleCoLinearPoint(line2, line1.getVPoint2(), line1, result);
				} else {
					// something is wrong!
					result.add(line1);
					result.add(line2);
					return false;
				}

			}// case 3: lines are co-linear
			 else if(GeometryUtils.intersectLineSegment(line1, line2.getVPoint1(), line2.getVPoint2())) {
				return handleIntersection(line1, line2, result);
			} else {
				result.add(line1);
				result.add(line2);
				return false;
			}
		//}
	}


	/**
	 * Lines intersect (general case)
	 *
	 * @param line1 line1 which intersects line2
	 * @param line2 line2 which intersects line1
	 * @param lines an empty list of lines
	 *
	 * @return true since both lines changes
	 */
	private boolean handleIntersection(@NotNull final VLine line1, @NotNull final VLine line2, @NotNull final List<VLine> lines) {
		assert lines.isEmpty();
		VPoint iPoint = GeometryUtils.intersectionPoint(line1, line2);
		lines.add(new VLine(line1.getVPoint1(), iPoint));
		lines.add(new VLine(line1.getVPoint2(), iPoint));
		lines.add(new VLine(line2.getVPoint1(), iPoint));
		lines.add(new VLine(line2.getVPoint2(), iPoint));
		return true;
	}

	/**
	 * the point of line2 is co-linear to line1, the other point of line2 is not co-linear
	 *
	 * @param line1 line1
	 * @param point the point which is co-linear to line1 and part of line2
	 * @param line2 line2
	 * @param lines an empty list of lines
	 *
	 * @return true if the point is not an endpoint of line1, otherwise false because no treatment is required
	 */
	private boolean handleCoLinearPoint(@NotNull final VLine line1, @NotNull final VPoint point, @NotNull final VLine line2, @NotNull final List<VLine> lines) {
		assert lines.isEmpty();
		if(equals(line1.getVPoint1(), point) || equals(line1.getVPoint2(), point)) {
			lines.add(line1);
			lines.add(line2);
			return false;
		} else {
			//VPoint pPoint = GeometryUtils.projectOntoLine(point.getX(), point.getY(), line1.x1, line1.y1, line1.x2, line1.y2);
			VPoint pPoint = point;
			lines.add(new VLine(line1.getVPoint1(), pPoint));
			lines.add(new VLine(line1.getVPoint2(), pPoint));
			lines.add(line2);
			return true;
		}
	}

	/**
	 * the two lines are co-linear
	 *
	 * @param line1 first line
	 * @param line2 second line
	 * @param lines an empty list of lines
	 *
	 * @return false if the two lines share exactly one endpoint (no treatment required), true otherwise
	 */
	private boolean handleCoLinearLines(@NotNull final VLine line1, @NotNull final VLine line2, @NotNull final List<VLine> lines)  {
		assert lines.isEmpty();

		boolean hasChanged = false;
		ArrayList<VPoint> list = new ArrayList<>(4);
		list.add(new VPoint(line1.x1, line1.y1));
		list.add(new VPoint(line1.x2, line1.y2));
		list.add(new VPoint(line2.x1, line2.y1));
		list.add(new VPoint(line2.x2, line2.y2));
		PointComparator<VPoint> comparator = new PointComparator<>();
		list.sort(comparator);

		VPoint point = list.get(0);
		for(int i = 1; i < list.size(); i++) {
			if(!equals(point, list.get(i))) {
				VLine newLine = new VLine(point, list.get(i));
				point = list.get(i);
				lines.add(newLine);
			} else {
				if(!point.equals(list.get(i))) {
					hasChanged = true;
				}
			}
		}

		if(!hasChanged && lines.size() == 2) {
			lines.clear();
			lines.add(line1);
			lines.add(line2);
			return false;
		}

		return true;
	}

	/**
	 * Returns true if there the two lines have close end points, false otherwise.
	 *
	 * @param line1 first line
	 * @param line2 second line
	 *
	 * @return true if there the two lines have close end points, false otherwise.
	 */
	private boolean closeEndPoints(@NotNull final VLine line1, @NotNull final VLine line2) {
		return GeometryUtils.distanceToLineSegment(line1.getVPoint1(), line1.getVPoint2(), line2.getVPoint1()) < tol ||
				GeometryUtils.distanceToLineSegment(line1.getVPoint1(), line1.getVPoint2(), line2.getVPoint2()) < tol ||
				GeometryUtils.distanceToLineSegment(line2.getVPoint1(), line2.getVPoint2(), line1.getVPoint1()) < tol ||
				GeometryUtils.distanceToLineSegment(line2.getVPoint1(), line2.getVPoint2(), line1.getVPoint2()) < tol;
	}

	/**
	 * Returns true if both lines are approximately equal, false otherwise.
	 *
	 * @param line1 first line
	 * @param line2 second line
	 *
	 * @return true if both lines are approximately equal, false otherwise.
	 */
	private boolean equals(@NotNull final VLine line1, @NotNull final VLine line2) {
		return equals(line1.getVPoint1(), line2.getVPoint1()) && equals(line1.getVPoint2(), line2.getVPoint2()) ||
				equals(line1.getVPoint1(), line2.getVPoint2()) && equals(line1.getVPoint2(), line2.getVPoint1());
	}

	/**
	 * Returns true if two points are approximately equals, false otherwise.
	 *
	 * @param point1 first point
	 * @param point2 second point
	 *
	 * @return true if two points are approximately equals, false otherwise
	 */
	private boolean equals(@NotNull final VPoint point1, @NotNull final VPoint point2) {
		return point1.distanceSq(point2) < tol * tol;
	}
}
