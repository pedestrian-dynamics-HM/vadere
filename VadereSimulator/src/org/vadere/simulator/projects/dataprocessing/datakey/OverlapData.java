package org.vadere.simulator.projects.dataprocessing.datakey;

import org.jetbrains.annotations.NotNull;
import org.vadere.state.scenario.DynamicElement;

public class OverlapData {

	public static final OverlapData noOverLap = new OverlapData();

	private final int ped1Id;
	private final int ped2Id;
	private final Double dist;
	private final Double overlap;

	private OverlapData() {
		this.ped1Id = -1;
		this.ped2Id = -1;
		this.dist = Double.POSITIVE_INFINITY;
		this.overlap = 0.0;
	}


	public OverlapData(final DynamicElement ped1, final DynamicElement ped2, final double minDist){
		this.ped1Id = ped1.getId();
		this.ped2Id = ped2.getId();
		this.dist = ped1.getPosition().distance(ped2.getPosition());
		this.overlap = minDist - dist;
	}

	public boolean isOverlap(){
		return overlap > 0;
	}

	public boolean isNotSelfOverlap(){
		return  !(ped1Id == ped2Id);
	}

	public int getPed1Id() {
		return ped1Id;
	}

	public int getPed2Id() {
		return ped2Id;
	}

	public Double getOverlap() {
		return overlap;
	}

	public String[] toStrings(){
		return new String[]{Double.toString(dist), Double.toString(overlap)};
	}

	public int maxDist(@NotNull OverlapData o) {
		return Double.compare(overlap, o.getOverlap());
	}
}
