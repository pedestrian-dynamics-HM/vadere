package org.vadere.util.data.cellgrid;

import java.util.Objects;

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
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		CellState cellState = (CellState) o;
		return potential.equals(cellState.potential) &&	tag == cellState.tag;
	}

	@Override
	public int hashCode() {
		return Objects.hash(potential, tag);
	}

	@Override
	public CellState clone() {
		return new CellState(Double.valueOf(potential), tag);
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
