package org.vadere.manager.traci.sumo;

import java.util.Objects;

public class TrafficLightPhase {

	private String PrecRoad;
	private String SuccRoad;
	private LightPhase phase;


	public TrafficLightPhase(String precRoad, String succRoad, LightPhase phase) {
		PrecRoad = precRoad;
		SuccRoad = succRoad;
		this.phase = phase;
	}

	public String getPrecRoad() {
		return PrecRoad;
	}

	public void setPrecRoad(String precRoad) {
		PrecRoad = precRoad;
	}

	public String getSuccRoad() {
		return SuccRoad;
	}

	public void setSuccRoad(String succRoad) {
		SuccRoad = succRoad;
	}

	public LightPhase getPhase() {
		return phase;
	}

	public void setPhase(LightPhase phase) {
		this.phase = phase;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		TrafficLightPhase that = (TrafficLightPhase) o;
		return PrecRoad.equals(that.PrecRoad) &&
				SuccRoad.equals(that.SuccRoad) &&
				phase == that.phase;
	}

	@Override
	public int hashCode() {
		return Objects.hash(PrecRoad, SuccRoad, phase);
	}

	@Override
	public String toString() {
		return "TrafficLightPhase{" +
				"PrecRoad='" + PrecRoad + '\'' +
				", SuccRoad='" + SuccRoad + '\'' +
				", phase=" + phase +
				'}';
	}
}
