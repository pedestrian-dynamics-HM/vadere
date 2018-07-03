package org.vadere.simulator.models.seating;

import static org.junit.Assert.*;

import java.util.Map;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;
import org.vadere.simulator.models.seating.trainmodel.Seat;
import org.vadere.simulator.models.seating.trainmodel.SeatGroup;
import org.vadere.simulator.models.seating.trainmodel.TrainModel;
import org.vadere.state.attributes.models.AttributesSeating;
import org.vadere.state.attributes.models.seating.SeatRelativePosition;
import org.vadere.state.attributes.models.seating.model.SeatPosition;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.util.data.TallySheet;
import org.vadere.util.test.FractionProbabilityNormalization;
import org.vadere.util.test.StatisticalTestCase;

public class TestChooseSeat {
	
	private SeatingModel model;
	private TrainModel trainModel;
	private SeatGroup seatGroup;
	
	@Before
	public void setUp() {
		model = new TestTopographyAndModelBuilder().getSeatingModel();
		trainModel = model.getTrainModel();
		seatGroup = trainModel.getSeatGroup(0, 0);
		assert seatGroup.getPersonCount() == 0;
	}

	@Test(expected=IllegalStateException.class)
	public void testChooseSeatInFullSeatGroup() {
		fillSeatGroup(seatGroup, 0, 1, 2, 3);
		model.chooseSeat(seatGroup);
	}
	
	@StatisticalTestCase
	@Test
	public void testChooseSeat0() {
		final int nTrials = 10000;
		TallySheet<Seat> tallySheet = runChooseSeat(nTrials);

		final Map<SeatPosition, Double> probabilities = FractionProbabilityNormalization.normalize(new AttributesSeating().getSeatChoice0());
		
		for (int i = 0; i < tallySheet.getKeys().size(); i++) {
			Seat s = seatGroup.getSeat(i);
			assertEquals(probabilities.get(getSeatPosition(s)), (double) tallySheet.getCount(s) / nTrials, 0.05);
		}
	}

	private SeatPosition getSeatPosition(Seat seat) {
		for (SeatPosition pos : SeatPosition.values())
			if (seatGroup.getSeatByPosition(pos) == seat)
				return pos;
		throw new IllegalArgumentException("cannot get position for seat");
	}

	@StatisticalTestCase
	@Test
	public void testChooseSeat1() {
		final int[] diagonallyOppositeSeatIndexes = { 3, 2, 1, 0 };
		for (int i = 0; i < 4; i++) {
			final int nTrials = 10000;
			clearSeatGroup(seatGroup);
			fillSeatGroup(seatGroup, i);

			final TallySheet<Seat> tallySheet = runChooseSeat(nTrials);

			final Seat diagonallyOppositeSeat = seatGroup.getSeat(diagonallyOppositeSeatIndexes[i]);
			Map<SeatRelativePosition, Double> map = FractionProbabilityNormalization.normalize(new AttributesSeating().getSeatChoice1());
			assertEquals(map.get(SeatRelativePosition.DIAGONAL),
					(double) tallySheet.getCount(diagonallyOppositeSeat) / nTrials, 0.05);
		}
	}

	@StatisticalTestCase
	@Test
	public void testChooseSeat2Both() {
		final int nTrials = 10000;
		fillSeatGroup(seatGroup, 0, 3);

		final TallySheet<Seat> tallySheet = runChooseSeat(nTrials);

		final Seat seat = seatGroup.getSeat(1); // forward facing seat
		assertEquals(0.5, (double) tallySheet.getCount(seat) / nTrials, 0.05);
	}

	@Test
	public void testChooseSeat3() {
		clearSeatGroup(seatGroup);
		fillSeatGroup(seatGroup, 1, 2, 3);
		assertEquals(seatGroup.getSeat(0), model.chooseSeat(seatGroup));
		
		clearSeatGroup(seatGroup);
		fillSeatGroup(seatGroup, 0, 2, 3);
		assertEquals(seatGroup.getSeat(1), model.chooseSeat(seatGroup));

		clearSeatGroup(seatGroup);
		fillSeatGroup(seatGroup, 0, 2, 1);
		assertEquals(seatGroup.getSeat(3), model.chooseSeat(seatGroup));
	}

	@Test(expected=IllegalStateException.class)
	public void testChooseSeat4() {
		fillSeatGroup(seatGroup, 0, 1, 2, 3);
		model.chooseSeat(seatGroup);
	}

	private TallySheet<Seat> runChooseSeat(final int nTrials) {
		TallySheet<Seat> tallySheet = new TallySheet<>();
		for (int i = 0; i < nTrials; i++) {
			tallySheet.addOneTo(model.chooseSeat(seatGroup));
		}
		return tallySheet;
	}
	
	private void clearSeatGroup(SeatGroup sg) {
		for (int i = 0; i < 4; i++) {
			sg.getSeat(i).setSittingPerson(null);
		}
	}

	private void fillSeatGroup(SeatGroup seatGroup, int... seatsWithPersons) {
		for (int i : seatsWithPersons) {
			seatGroup.getSeat(i).setSittingPerson(new Pedestrian(new AttributesAgent(), new Random()));
		}
	}
	
}
