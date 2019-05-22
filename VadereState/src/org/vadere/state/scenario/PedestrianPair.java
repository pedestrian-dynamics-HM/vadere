package org.vadere.state.scenario;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.vadere.util.geometry.shapes.VPoint;

/**
 * Describes an order pair of pedestrian objects.
 */
public class PedestrianPair extends Pair<Pedestrian, Pedestrian> {

	private ImmutablePair<Pedestrian, Pedestrian> pair;

	public static PedestrianPair of (Pedestrian left, Pedestrian right) {
		return new PedestrianPair(left, right);
	}

	private PedestrianPair(Pedestrian left, Pedestrian right) {
		this.pair = new ImmutablePair<>(left, right);
	}

	@Override
	public Pedestrian getValue() {
		return pair.getValue();
	}

	@Override
	public int compareTo(Pair<Pedestrian, Pedestrian> other) {
		return pair.compareTo(other);
	}

	@Override
	public boolean equals(Object obj) {
		return pair.equals(obj);
	}

	@Override
	public int hashCode() {
		return pair.hashCode();
	}

	@Override
	public String toString() {
		return String.format("PedestrianPair: {left: %d, right: %d}",
				pair.getLeft().getId(),
				pair.getRight().getId());
	}

	@Override
	public String toString(String format) {
		return super.toString(format);
	}

	@Override
	public Pedestrian getLeft() {
		return pair.getLeft();
	}

	public int getLeftId(){
		return pair.getLeft().getId();
	}

	public VPoint getLeftPosition(){
		return pair.getLeft().getPosition().clone();
	}

	@Override
	public Pedestrian getRight() {
		return pair.getRight();
	}

	public int getRightId(){
		return pair.getRight().getId();
	}

	public VPoint getRightPosition(){
		return pair.getRight().getPosition().clone();
	}

	@Override
	public Pedestrian setValue(Pedestrian value) {
		pair = new ImmutablePair<>(pair.getLeft(), value);
		return pair.getValue();
	}
}
