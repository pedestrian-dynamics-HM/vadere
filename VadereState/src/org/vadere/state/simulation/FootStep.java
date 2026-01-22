package org.vadere.state.simulation;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;

import java.util.Objects;
import java.util.Optional;

/**
 * A foot step is a simple java bean which represents one pedestrian foot step which is defined by
 * <ol>
 *     <li>start time (simulation time in seconds)</li>
 *     <li>end time (simulation time in seconds)</li>
 *     <li>start point</li>
 *     <li>end point</li>
 * </ol>
 *
 * @author Benedikt Zoennchen
 */
public final class FootStep {

	/*
	 * These are not final because of Jackson.
	 */
	private double startTime;
	private double endTime;
	private VPoint start;
	private VPoint end;

	/**
	 * Constructor for Jackson.
	 */
	public FootStep() {}

	/**
	 * Default constructor.
	 *
	 * @param start     start point of the foot step
	 * @param end       end point of the foot step
	 * @param startTime start time of the foot step
	 * @param endTime   end time of the foot step
	 */
	public FootStep(@NotNull final VPoint start, @NotNull final VPoint end, final double startTime, final double endTime) {
        assert startTime <= endTime;

	    this.start = start;
		this.end = end;
		this.startTime = startTime;
		this.endTime = endTime;
	}

	public double getEndTime() {
		return endTime;
	}

	public double getStartTime() {
		return startTime;
	}

	public VPoint getEnd() {
		return end;
	}

	public VPoint getStart() {
		return start;
	}

	public double length() {
		return start.distance(end);
	}

	public double duration() {
		return endTime - startTime;
	}

	/**
	 * @see GeometryUtils#intersectsRectangleBoundary(VRectangle, double, double, double, double)
	 */
	public boolean intersectsBoundary(@NotNull final VRectangle rectangle) {
		return GeometryUtils.intersectsRectangleBoundary(rectangle, getStart().x, getStart().y, getEnd().x, getEnd().y);
	}

	/**
	 * @see GeometryUtils#intersectLineSegment(IPoint, IPoint, IPoint, IPoint)
	 */
	public boolean intersects(@NotNull final VLine line) {
		VPoint start = getStart();
		VPoint end = getEnd();
		return GeometryUtils.intersectLineSegment(new VPoint(line.getP1()), new VPoint(line.getP2()), start, end);
	}

	public double computeIntersectionTime(@NotNull final VLine line) {
		VPoint start = getStart();
		VPoint end = getEnd();
		VPoint intersectionPoint = GeometryUtils.intersectionPoint(line.getX1(), line.getY1(), line.getX2(), line.getY2(), start.getX(), start.getY(), end.getX(), end.getY());
		double dStart = intersectionPoint.distance(start);
		double stepLength = start.distance(end);
		double duration = getEndTime() - getStartTime();
		double intersectionTime = getStartTime() + duration * (dStart / stepLength);
		return intersectionTime;
	}

	public Pair<FootStep, FootStep> cut(final double simTimeInSec) {
		if(simTimeInSec >= endTime || simTimeInSec <= startTime) {
			throw new IllegalArgumentException("invalid time.");
		}

		VPoint vector = end.subtract(start);
		double duration = duration();
		double portion = simTimeInSec - startTime;
		VPoint portionStep = vector.scalarMultiply(portion / duration);

		VPoint middle = start.add(portionStep);

		FootStep first = new FootStep(start, middle, startTime, simTimeInSec);
		FootStep second = new FootStep(middle, end, simTimeInSec, endTime);
		return Pair.of(first, second);
	}

	/**
	 * Computes the intersection of the footstep with a rectangle.
	 *
	 * <p>The result describes the portion of the step that lies inside the rectangle.</p>
	 *
	 * <table border="1">
	 *   <tr><td>Result</td><td> entryPoint </td> <td> exitPoint </td> <td> Reason </td></tr>
	 * 	<tr><td> ✔ </td> <td> ✔ </td> <td> ✔ </td> <td> step crosses rectangle</td></tr>
	 * 	<tr><td> ✔ </td> <td> ✔ </td> <td> x </td> <td> step's end is on boundary or enters rectangle without exiting</td></tr>
	 * 	<tr><td> ✔ </td> <td> x </td> <td> ✔ </td> <td> step starts inside and exits or starts on boundary going outwards</td></tr>
	 * 	<tr><td> ✔ </td> <td> x </td> <td> x </td> <td> step lies/touches inside the rectangle boundary</td></tr>
	 * 	<tr><td> x </td> <td> - </td> <td> - </td> <td> step does not intersect the rectangle</td></tr>
	 * </table>
	 *
	 * @return an {@code Optional} containing a {@code LineRectClippingResult} that describes
	 *         the clipped segment, or {@code Optional.empty()} if there is no intersection
	 */
	public Optional<LineRectClippingResult> computeClipping(@NotNull VRectangle rectangle) {
		VPoint start = getStart();
		VPoint end = getEnd();
		Optional<GeometryUtils.LineRectClippingResult> optionalClippingResult = GeometryUtils.computeClipping(rectangle, start, end);
		if(optionalClippingResult.isEmpty()) {
			return Optional.empty();
		}

		GeometryUtils.LineRectClippingResult clippingResult = optionalClippingResult.get();
		double duration = getEndTime() - getStartTime();

		Optional<IntersectionPointAndTime> enter = Optional.empty();
		IntersectionPointAndTime clippingStart;
		if(clippingResult.entryPoint().isPresent()){
			GeometryUtils.IntersectionPointAndPercentage entryPoint = clippingResult.entryPoint().get();

			double intersectionTime = getStartTime() + duration * entryPoint.linePercentage();
			IntersectionPointAndTime pointAndTime = new IntersectionPointAndTime(entryPoint.point(), intersectionTime);

			enter = Optional.of(pointAndTime);
			clippingStart = pointAndTime;
		}else{
			clippingStart = new IntersectionPointAndTime(start, getStartTime());
		}

		Optional<IntersectionPointAndTime> exit = Optional.empty();
		IntersectionPointAndTime clippingEnd;
		if(clippingResult.exitPoint().isPresent()){
			GeometryUtils.IntersectionPointAndPercentage exitPoint = clippingResult.exitPoint().get();

			double intersectionTime = getStartTime() + duration * exitPoint.linePercentage();
			IntersectionPointAndTime pointAndTime = new IntersectionPointAndTime(exitPoint.point(), intersectionTime);

			exit = Optional.of(pointAndTime);
			clippingEnd = pointAndTime;
		}else{
			clippingEnd = new IntersectionPointAndTime(end, getEndTime());
		}

		return Optional.of(new LineRectClippingResult(enter, exit, clippingStart, clippingEnd));
	}

	public static VPoint interpolateFootStep(final double startX, final double startY, final double endX, final double endY, final double startTime, final double endTime, final double time) {
		return interpolateFootStep(new FootStep(new VPoint(startX, startY), new VPoint(endX, endY), startTime, endTime), time);
	}

	public static VPoint interpolateFootStep(final VPoint start, final VPoint end, final double startTime, final double endTime, final double time) {
		return interpolateFootStep(new FootStep(start, end, startTime, endTime), time);
	}

	public static VPoint interpolateFootStep(final FootStep footStep, final double time){
		final double startTime = footStep.getStartTime();
		final double endTime = footStep.getEndTime();
		final double duration = footStep.duration();

		if(startTime > time || endTime < time || startTime < 0 ){
			throw new IllegalArgumentException("Requested time " + time + " outside of valid time " +
					"region (no extrapolation!). Value outside of FootStep " +
					"[start=" + startTime + ", end=" + endTime + "] time or smaller than zero. " +
					"\n FootStep=" + footStep.toString());
		}

		VPoint interpolationResult;

		if(duration < 1E-14){
			// to avoid problems with division by "very small number", simply return the start point
			interpolationResult = footStep.getStart();
		}else{
			double linearTime = (time - startTime) / duration;

			VPoint diffPoint = footStep.getEnd().subtract(footStep.getStart());

			diffPoint.x = diffPoint.x * linearTime;
			diffPoint.y = diffPoint.y * linearTime;

			interpolationResult = footStep.getStart().add(diffPoint);
		}

		return interpolationResult;
	}


	@Override
	public String toString() {
		return "start=" + start + "(t=" + startTime + ")" + "->" + "end=" + end + "(t=" + endTime + ")";
	}

	@Override
	public FootStep clone(){
		return new FootStep(this.start.clone(), this.end.clone(), this.startTime, this.endTime);
	}

	public String[] getValueString(){
		String[] valueLine = {""+startTime, ""+endTime, ""+start.x, ""+start.y, ""+end.x, ""+end.y};
		return valueLine;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass()) return false;
		FootStep footStep = (FootStep) o;
		return Double.compare(startTime, footStep.startTime) == 0 && Double.compare(endTime, footStep.endTime) == 0
				&& Objects.equals(start, footStep.start) && Objects.equals(end, footStep.end);
	}

	@Override
	public int hashCode() {
		return Objects.hash(startTime, endTime, start, end);
	}

	/**
	 * @param entryPoint    the point where the line intersects the boundary entering the rectangle. Empty when the line is inside or when the line exits but does not enter.
	 * @param exitPoint     the point where the line intersects the boundary exiting the rectangle. Empty when the line is inside or when line enters but does not exit.
	 * @param clippingStart the point of the line that first enters the rectangle. When the line is completely inside, this is set to the start of the line.
	 * @param clippingEnd   the point of the line that exits the rectangle. When the line is completely inside or enters and never exists, this is set to the end of the line.
	 */
	public record LineRectClippingResult(Optional<IntersectionPointAndTime> entryPoint, Optional<IntersectionPointAndTime> exitPoint,
										 IntersectionPointAndTime clippingStart, IntersectionPointAndTime clippingEnd) {
		boolean isCompletelyInside(){
			return entryPoint.isEmpty() && exitPoint.isEmpty();
		}

		public boolean entersBoundary(){
			return entryPoint.isPresent();
		}

		public boolean exitsBoundary(){
			return exitPoint.isPresent();
		}
	}
	public record IntersectionPointAndTime(VPoint point, double time) {}
}
