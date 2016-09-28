package org.vadere.simulator.models.seating.trainmodel;

import java.util.ArrayList;
import java.util.List;
import org.vadere.state.scenario.Target;

/**
 * The first and the last compartments are half-compartments.
 */
public class Compartment {

	private TrainModel trainModel;
	private int index;
	
	/**
	 * For half-compartments, the first and the last seat groups respectively are set to null.
	 */
	private List<SeatGroup> seatGroups;

	public Compartment(TrainModel trainModel, int index) {
		this.trainModel = trainModel;
		this.index = index;

		seatGroups = new ArrayList<>(4);
		if (isFirstHalfCompartment()) {
			addFirstHalfCompartment();
		} else if (isLastHalfCompartment()) {
			addLastHalfCompartment();
		} else {
			addNormalCompartment();
		}
	}


	public SeatGroup getSeatGroup(int seatGroupIndex) {
		return seatGroups.get(seatGroupIndex);
	}

	public Seat getSeat(int seatGroupIndex, int seatIndex) {
		return getSeatGroup(seatGroupIndex).getSeat(seatIndex);
	}

	private void addNormalCompartment() {
		final int startSeatGroupIndex = index * 4 - 2;
		seatGroups.add(trainModel.getSeatGroup(startSeatGroupIndex));
		seatGroups.add(trainModel.getSeatGroup(startSeatGroupIndex + 1));
		seatGroups.add(trainModel.getSeatGroup(startSeatGroupIndex + 2));
		seatGroups.add(trainModel.getSeatGroup(startSeatGroupIndex + 3));
	}
	
	private void addFirstHalfCompartment() {
		seatGroups.add(trainModel.getSeatGroup(0));
		seatGroups.add(trainModel.getSeatGroup(1));
	}

	private void addLastHalfCompartment() {
		final int n = trainModel.getSeatGroups().size();
		seatGroups.add(trainModel.getSeatGroup(n - 2));
		seatGroups.add(trainModel.getSeatGroup(n - 1));
	}

	/**
	 * A list of the compartment's seat groups. For half-compartments, the list
	 * contains only two seat groups.
	 */
	public List<SeatGroup> getSeatGroups() {
		return seatGroups;
	}

	public Target getCompartmentTarget() {
		// entrance areas:         0   1   2   3
		// compartments:         0   1   2   3   4
		// compartment targets:  0   1   2   3   4
		return trainModel.getCompartmentTargets().get(index);
	}

	private boolean isLastHalfCompartment() {
		return index == trainModel.getEntranceAreaCount();
	}

	private boolean isFirstHalfCompartment() {
		return index == 0;
	}

	public int getIndex() {
		return index;
	}

	public int getPersonCount() {
		return getSeatGroups().stream().mapToInt(SeatGroup::getPersonCount).sum();
	}

}
