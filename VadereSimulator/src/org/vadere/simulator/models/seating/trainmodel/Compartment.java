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
		seatGroups.add(null);
		seatGroups.add(null);
		seatGroups.add(trainModel.getSeatGroup(0));
		seatGroups.add(trainModel.getSeatGroup(1));
	}

	private void addLastHalfCompartment() {
		final int n = trainModel.getSeatGroups().size();
		seatGroups.add(trainModel.getSeatGroup(n - 2));
		seatGroups.add(trainModel.getSeatGroup(n - 1));
		seatGroups.add(null);
		seatGroups.add(null);
	}

	/**
	 * For half-compartments, the first and the last seat groups respectively are set to null.
	 */
	public List<SeatGroup> getSeatGroups() {
		return seatGroups;
	}

	public Target getInterimTargetCloserTo(int entranceAreaIndex) {
		// entrance areas:     0   1   2   3
		// compartments:     0   1   2   3   4
		// interim targets:  0  123 456 489  .
		
		trainModel.checkEntranceAreaIndexRange(entranceAreaIndex);
		
		final List<Target> interimTargets = trainModel.getInterimDestinations();
		
		if (isFirstHalfCompartment()) {
			return interimTargets.get(0);
		} else if (isLastHalfCompartment()) {
			return interimTargets.get(interimTargets.size() - 1);
		}
		
		final int interimTargetStartIndex = index * 3 - 2;
		if (index <= entranceAreaIndex) {
			// use interim target with higher number
			return interimTargets.get(interimTargetStartIndex + 2);
		} else {
			// use interim target with smaller number
			return interimTargets.get(interimTargetStartIndex);
		}
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