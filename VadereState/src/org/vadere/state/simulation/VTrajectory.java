package org.vadere.state.simulation;


import org.jetbrains.annotations.NotNull;
import org.vadere.util.geometry.shapes.VRectangle;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class VTrajectory implements Iterable<FootStep> {

	private LinkedList<FootStep> footSteps;

	public VTrajectory(){
		footSteps = new LinkedList<>();
	}

	public Optional<Double> speed(@NotNull final VRectangle rectangle) {
		VTrajectory cutting = clone();
		cutting.cut(rectangle);
		return cutting.speed();
	}

	public boolean isInBetween(double startTime, double endTime) {
		if(isEmpty()) {
			return false;
		}
		return footSteps.peekFirst().getStartTime() <= startTime && footSteps.peekLast().getEndTime() >= endTime;
	}

	public boolean isInBetween(@NotNull final VTrajectory other) {
		if(isEmpty() || other.isEmpty()) {
			return false;
		}
		return isInBetween(other.footSteps.peekFirst().getStartTime(), other.footSteps.peekLast().getEndTime());

	}

	public Optional<Double> getStartTime() {
		if(isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(footSteps.peekFirst().getStartTime());
	}

	public Optional<Double> getEndTime() {
		if(isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(footSteps.peekLast().getEndTime());
	}

	public boolean isEmpty() {
		return footSteps.isEmpty();
	}

	public double length() {
		return footSteps.stream().mapToDouble(footStep -> footStep.length()).sum();
	}

	public double length(final double startSimTime, final double endSimTime) {
		return clone().cut(startSimTime, endSimTime).length();
	}

	public double duration() {
		return footSteps.peekLast().getEndTime() - footSteps.peekFirst().getStartTime();
	}

	public Optional<Double> speed() {
		if(footSteps.isEmpty()) {
			return Optional.empty();
		}
		else {
			return Optional.of(length() / duration());
		}
	}

	public void add(@NotNull final FootStep footStep) {
		assert footSteps.isEmpty() || footSteps.peekLast().getEndTime() <= footStep.getStartTime();
		footSteps.add(footStep);
	}

	public VTrajectory cut(@NotNull final VRectangle rectangle) {
		List<FootStep> intersectionSteps = footSteps.stream().filter(footStep -> footStep.intersects(rectangle)).collect(Collectors.toList());
		if(intersectionSteps.size() == 2) {
			double startSimTime = intersectionSteps.get(0).computeIntersectionTime(rectangle);
			double endSimTime = intersectionSteps.get(1).computeIntersectionTime(rectangle);
			return cut(startSimTime, endSimTime);
		}
		else if(intersectionSteps.size() == 1) {
			double simTime = intersectionSteps.get(0).computeIntersectionTime(rectangle);
			VTrajectory clone = clone();

			if(rectangle.contains(footSteps.peekLast().getEnd())) {
				clone.cutHead(simTime);
			}
			else {
				clone.cutTail(simTime);
			}
			return clone;
		}
		else if(intersectionSteps.size() > 0) {
			throw new IllegalArgumentException("the number of intersection points is not zero or 2.");
		}
		return this;
	}

	public void cutTail(final double simStartTime) {
		while (!footSteps.isEmpty() && footSteps.peekFirst().getEndTime() <= simStartTime) {
			footSteps.removeFirst();
		}

		if(!footSteps.isEmpty() && footSteps.peekFirst().getStartTime() < simStartTime) {
			FootStep footStep = footSteps.removeFirst();
			footSteps.addFirst(footStep.cut(simStartTime).getRight());
		}

	}

	public void cutHead(final double simEndTime) {
		while (!footSteps.isEmpty() && footSteps.peekLast().getStartTime() >= simEndTime) {
			footSteps.removeLast();
		}

		if(!footSteps.isEmpty() && footSteps.peekLast().getEndTime() > simEndTime) {
			FootStep footStep = footSteps.removeLast();
			footSteps.addLast(footStep.cut(simEndTime).getLeft());
		}
	}

	public void concat(@NotNull final VTrajectory trajectory) {
		for (FootStep footStep : trajectory) {
			if(isEmpty() || (footStep.getStartTime() >= footSteps.peekLast().getEndTime())) {
				footSteps.addLast(footStep);
			}
		}
	}

	public void clear() {
		footSteps.clear();
	}

	public VTrajectory clone() {
		LinkedList<FootStep> copy = new LinkedList<>(footSteps);
		VTrajectory clone = new VTrajectory();
		clone.footSteps = copy;
		return clone;
	}

	public LinkedList<FootStep> getFootSteps() {
		return new LinkedList<>(footSteps);
	}

	public VTrajectory cut(final double startTime, final double endTime) {
		LinkedList<FootStep> copy = new LinkedList<>(footSteps);
		VTrajectory subTrajectory = new VTrajectory();
		subTrajectory.footSteps = copy;
		subTrajectory.cutHead(endTime);
		subTrajectory.cutTail(startTime);
		return subTrajectory;
	}

	@NotNull
	@Override
	public Iterator<FootStep> iterator() {
		return footSteps.iterator();
	}

	public Iterator<FootStep> descendingIterator() {
		return footSteps.descendingIterator();
	}

	public Stream<FootStep> stream() {
		return footSteps.stream();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		footSteps.stream().forEach(footStep -> builder.append(footStep));
		return builder.toString();
	}
}
