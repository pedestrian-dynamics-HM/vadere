package org.vadere.util.voronoi;

abstract class Event implements Comparable<Event> {

	private final double yCoordinate;
	private final double xCoordinate;

	private final double valueY;
	private final double valueX;

	Event(double xCoordinate, double yCoordinate, double valueY, double valueX) {
		this.xCoordinate = xCoordinate;
		this.yCoordinate = yCoordinate;
		this.valueY = valueY;
		this.valueX = valueX;
	}

	@Override
	public int compareTo(Event event) {
		int result;
		double otherValueY = event.getValueY();

		if (valueY < otherValueY) {
			result = 1;
		} else if (valueY > otherValueY) {
			result = -1;
		} else {
			if (valueX < event.getValueX()) {
				result = -1;
			} else if (valueX > event.getValueX()) {
				result = 1;
			} else {
				result = 0;
			}
		}

		return result;
	}

	private double getValueY() {
		return valueY;
	}

	private double getValueX() {
		return valueX;
	}

	public double getXCoordinate() {
		return xCoordinate;
	}

	public double getYCoordinate() {
		return yCoordinate;
	}
}
