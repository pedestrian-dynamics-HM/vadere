package org.vadere.util.triangulation.adaptive;

import org.apache.commons.lang3.tuple.Pair;
import org.vadere.util.geometry.shapes.VLine;

public class IndexedVLine extends VLine {

	private final IndexedVPoint p1;
	private final IndexedVPoint p2;

	public IndexedVLine(IndexedVPoint p1, IndexedVPoint p2) {
		super(p1, p2);
		this.p1 = p1;
		this.p2 = p2;
	}

	@Override
	public boolean equals(Object obj) {
		if(obj == null) return false;
		if(obj == this) return true;

		if(obj.getClass() != getClass()) return false;

		IndexedVLine line = (IndexedVLine) obj;
		return line.getP1().equals(getP1()) && line.getP2().equals(getP2()) || line.getP2().equals(getP1()) && line.getP1().equals(getP2());
	}

	@Override
	public int hashCode() {
		return p1.hashCode() * p2.hashCode();
	}

	public Pair<Integer, Integer> getId() {
		return Pair.of(p1.getId(), p2.getId());
	}
}
