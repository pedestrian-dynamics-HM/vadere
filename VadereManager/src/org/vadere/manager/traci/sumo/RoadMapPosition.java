package org.vadere.manager.traci.sumo;

import java.util.Objects;

public class RoadMapPosition {

	private String roadId;
	private double pos;
	private int laneId;

	public RoadMapPosition(String roadId, double pos, int laneId) {
		this.roadId = roadId;
		this.pos = pos;
		this.laneId = laneId;
	}

	public String getRoadId() {
		return roadId;
	}

	public void setRoadId(String roadId) {
		this.roadId = roadId;
	}

	public double getPos() {
		return pos;
	}

	public void setPos(double pos) {
		this.pos = pos;
	}

	public int getLaneId() {
		return laneId;
	}

	public void setLaneId(int laneId) {
		this.laneId = laneId;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		RoadMapPosition that = (RoadMapPosition) o;
		return Double.compare(that.pos, pos) == 0 &&
				laneId == that.laneId &&
				roadId.equals(that.roadId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(roadId, pos, laneId);
	}

	@Override
	public String toString() {
		return "RoadMapPosition{" +
				"roadId='" + roadId + '\'' +
				", pos=" + pos +
				", laneId=" + laneId +
				'}';
	}
}
