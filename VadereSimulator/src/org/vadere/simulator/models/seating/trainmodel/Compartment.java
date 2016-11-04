package org.vadere.simulator.models.seating.trainmodel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.vadere.state.scenario.Target;

/**
 * The first and the last compartments are half-compartments.
 */
public class Compartment {

	private TrainModel trainModel;
	private int index;
	private Target compartmentTarget;
	private List<SeatGroup> seatGroups;

	// only for building:
	private List<List<Target>> longSeatRows;
	private Map<Target, Seat> targetSeatMap;

	public Compartment(TrainModel trainModel, int index, List<Target> compartmentTargets, List<List<Target>> seatRows, Map<Target, Seat> targetSeatMap) {
		this.trainModel = trainModel;
		this.index = index;
		this.compartmentTarget = compartmentTargets.get(index);
		this.longSeatRows = seatRows;
		this.targetSeatMap = targetSeatMap;

		seatGroups = new ArrayList<>(4);
		final int nSeatGroups = isHalfCompartment() ? 2 : 4;
		addCompartment(nSeatGroups);
	}

	private void addCompartment(int nSeatGroups) {
		for (int i = 0; i < nSeatGroups; i++)
			addNewSeatGroup(i);
	}

	public SeatGroup getSeatGroup(int seatGroupIndex) {
		return seatGroups.get(seatGroupIndex);
	}

	public Seat getSeat(int seatGroupIndex, int seatIndex) {
		return getSeatGroup(seatGroupIndex).getSeat(seatIndex);
	}

	private void addNewSeatGroup(int index) {
		seatGroups.add(new SeatGroup(this, index, longSeatRows, targetSeatMap));
	}

	/**
	 * A list of the compartment's seat groups. For half-compartments, the list
	 * contains only two seat groups.
	 */
	public List<SeatGroup> getSeatGroups() {
		return seatGroups;
	}

	public Target getCompartmentTarget() {
		return compartmentTarget;
	}
	
	public boolean isHalfCompartment() {
		return isFirstHalfCompartment() || isLastHalfCompartment();
	}

	public boolean isLastHalfCompartment() {
		return index == trainModel.getCompartmentCount() - 1;
	}

	boolean isFirstHalfCompartment() {
		return index == 0;
	}

	public int getIndex() {
		return index;
	}

	public int getPersonCount() {
		return getSeatGroups().stream().mapToInt(SeatGroup::getPersonCount).sum();
	}

	public boolean isFull() {
		for (SeatGroup sg : seatGroups) {
			if (!sg.isFull())
				return false;
		}
		return true;
	}

}
