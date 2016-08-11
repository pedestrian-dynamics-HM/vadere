package org.vadere.util.voronoi;

public class EventCircle extends Event {

	private final BeachLineLeaf beachLineLeaf;

	EventCircle(double xCoordinate, double yCoordinate,
			BeachLineLeaf beachLineLeaf, double valueY, double valueX) {
		super(xCoordinate, yCoordinate, valueY, valueX);

		this.beachLineLeaf = beachLineLeaf;
	}

	BeachLineLeaf getBeachLineLeaf() {
		return beachLineLeaf;
	}
}
