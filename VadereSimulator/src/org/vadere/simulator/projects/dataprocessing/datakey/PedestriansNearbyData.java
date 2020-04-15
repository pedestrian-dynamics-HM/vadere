package org.vadere.simulator.projects.dataprocessing.datakey;

import org.vadere.state.scenario.DynamicElement;

public class PedestriansNearbyData {

	public static final PedestriansNearbyData noOverLap = new PedestriansNearbyData();

	private final int pedId1;
	private final int pedId2;
	private int durationTimesteps;
	private int startTimestep;



	private PedestriansNearbyData() {
		this.pedId1 = -1;
		this.pedId2 = -1;
		this.durationTimesteps = 1;
		this.startTimestep = 1;
	}

	public int getStartTimestep() {
		return startTimestep;
	}

	public PedestriansNearbyData(int ped1, int ped2, final int durationTimesteps, int startTimestep){
		this.pedId1 = Math.min(ped1, ped2);
		this.pedId2 = Math.max(ped1, ped2);
		this.durationTimesteps = durationTimesteps;
		this.startTimestep = startTimestep;
	}

	public int getPedId1() {
		return pedId1;
	}

	public int getPedId2() {
		return pedId2;
	}

	public int getDurationTimesteps() {
		return durationTimesteps;
	}

	public PedestriansNearbyData getDataWithIncrementedDuration() {
		return new PedestriansNearbyData(getPedId1(), getPedId2(), getDurationTimesteps() + 1, getStartTimestep());
	}


	public String[] toStrings(){
		return new String[]{Integer.toString(durationTimesteps)};
	}

}
