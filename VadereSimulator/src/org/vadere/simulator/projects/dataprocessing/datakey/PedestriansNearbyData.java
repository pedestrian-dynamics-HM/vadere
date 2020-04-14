package org.vadere.simulator.projects.dataprocessing.datakey;

import org.vadere.state.scenario.DynamicElement;

public class PedestriansNearbyData {

	public static final PedestriansNearbyData noOverLap = new PedestriansNearbyData();

	private final int ped1Id;
	private final int ped2Id;
	private final int durationTimesteps;


	private PedestriansNearbyData() {
		this.ped1Id = -1;
		this.ped2Id = -1;
		this.durationTimesteps = 1;
	}


	public PedestriansNearbyData(final DynamicElement ped1, final DynamicElement ped2, final int durationTimesteps){
		this.ped1Id = ped1.getId();
		this.ped2Id = ped2.getId();
		this.durationTimesteps = durationTimesteps;
	}

	/*public boolean isOverlap(){
		return overlap > 0;
	}*/

	/*public boolean isNotSelfOverlap(){
		return  !(ped1Id == ped2Id);
	}*/

	public int getPed1Id() {
		return ped1Id;
	}

	public int getPed2Id() {
		return ped2Id;
	}

	/*public Double getOverlap() {
		return overlap;
	}*/

	public String[] toStrings(){
		return new String[]{Integer.toString(durationTimesteps)};
	}

	/*public int maxDist(@NotNull PedestiansNearbyData o) {
		return Double.compare(overlap, o.getOverlap());
	}*/
}
