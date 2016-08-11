package org.vadere.util.io.filewatcher;

import java.awt.geom.Point2D;


public class TransitumPedestrianState {

	public TransitumPedestrianState(int id, double x, double y, int originId, int destinationId) {

		this.id = id;
		this.x = x;
		this.y = y;
		this.originId = originId;
		this.destinationId = destinationId;
	}

	private int id;

	private double x;

	private double y;

	private int originId;

	private int destinationId;

	public int getOriginId() {
		return originId;
	}

	public int getDestinationId() {
		return destinationId;
	}

	public int getId() {

		return id;
	}

	public Point2D.Double getPosition() {

		return new Point2D.Double(x, y);
	}
}
