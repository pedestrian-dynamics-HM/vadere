package org.vadere.state.simulation;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vadere.util.geometry.shapes.VRectangle;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Optional;
import java.util.stream.Stream;

public class VTrajectory implements Iterable<FootStep> {

	// Variables
	private LinkedList<FootStep> footSteps;

	// Constructors
	public VTrajectory() {
		footSteps = new LinkedList<>();
	}

	// Getters
	public LinkedList<FootStep> getFootSteps() {
		return new LinkedList<>(footSteps);
	}

	public boolean adjustEndTime(@NotNull final double endTime) {
		if(!isEmpty()) {
			while (!isEmpty() && footSteps.peekLast().getStartTime() >= endTime) {
				footSteps.removeLast();
			}

			if(footSteps.isEmpty()) {
				return false;
			}
			FootStep footStep = footSteps.removeLast();
			footSteps.addLast(new FootStep(footStep.getStart(), footStep.getEnd(), footStep.getStartTime(), endTime));
		} else {
			throw new IllegalStateException("cant adjust the last footstep of an empty trajectory.");
		}
		return true;
	}

	// Methods
	public int size() {
		return footSteps.size();
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

	public Optional<Double> duration() {
		if(footSteps.isEmpty()) {
			return Optional.empty();
		}
		else {
			double duration = footSteps.peekLast().getEndTime() - footSteps.peekFirst().getStartTime();
			return Optional.of(duration);
		}
	}

	public Optional<Double> speed() {
		if(footSteps.isEmpty()) {
			return Optional.empty();
		}
		else {
			return Optional.of(length() / duration().get());
		}
	}

	public VTrajectory add(@NotNull final FootStep footStep) {

		assert footSteps.isEmpty() ||
				(footSteps.peekLast().getEndTime() <= footStep.getStartTime() &&  // make sure it is in order
						footSteps.peekLast().getStartTime() < footStep.getStartTime());

		footSteps.add(footStep);
		return this;
	}

	public FootStep removeLast() {
		assert !footSteps.isEmpty();
		return footSteps.removeLast();
	}

	public VTrajectory cut(@NotNull final VRectangle rectangle) {
		LinkedList<FootStep> newFootSteps = new LinkedList<>();
		boolean inside = !footSteps.isEmpty() ? rectangle.contains(footSteps.peekFirst().getStart()) : false;

		for(FootStep footStep : footSteps) {
			if(footStep.intersects(rectangle)) {
				Pair<FootStep, FootStep> splitStep = footStep.cut(footStep.computeIntersectionTime(rectangle));

				if(!inside) {
					newFootSteps.clear();
					newFootSteps.add(splitStep.getRight());
				}
				else {
					newFootSteps.add(splitStep.getLeft());
				}

				inside = !inside;

			}
			else if(inside) {
				newFootSteps.add(footStep);
			}
		}

		VTrajectory copy = new VTrajectory();
		copy.footSteps = newFootSteps;
		return copy;
	}

	private boolean isEntering(@NotNull final VRectangle rectangle, @NotNull FootStep intersectionStep) {
		return rectangle.contains(intersectionStep.getEnd());
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
		VTrajectory newTrajectory = new VTrajectory();

		LinkedList<FootStep> footStepCopy = new LinkedList<>(footSteps);
		newTrajectory.footSteps = footStepCopy;

		return newTrajectory;
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
