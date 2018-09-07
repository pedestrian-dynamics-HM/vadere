package org.vadere.util.potential;

public class CellState implements Cloneable {
	public Double potential;
	public PathFindingTag tag;

	public CellState(Double potential, PathFindingTag tag) {
		this.potential = potential;
		this.tag = tag;
	}

	public CellState() {
		this.potential = Double.MAX_VALUE;
		this.tag = PathFindingTag.Undefined;
	}

	@Override
	public CellState clone() {
		return new CellState(new Double(potential), tag);
	}

	@Override
	public String toString() {
		String s = "MAX   ";
		if (potential != Double.MAX_VALUE) {
			s = String.format("%2.4f", potential);
		}

		switch (tag) {
			case Undefined:
				s += "/U ";
				break;
			case Reachable:
				s += "/R ";
				break;
			case Reached:
				s += "/E ";
				break;
			case Target:
				s += "/T ";
				break;
			case Obstacle:
				s += "/O ";
				break;
			case Margin:
				s += "/M ";
				break;
		}

		return s;
	}
}
