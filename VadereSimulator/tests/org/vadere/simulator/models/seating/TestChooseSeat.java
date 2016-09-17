package org.vadere.simulator.models.seating;

import static org.junit.Assert.*;

import java.util.Random;
import org.junit.Before;
import org.junit.Test;
import org.vadere.simulator.models.seating.trainmodel.Seat;
import org.vadere.simulator.models.seating.trainmodel.SeatGroup;
import org.vadere.simulator.models.seating.trainmodel.TrainModel;
import org.vadere.state.attributes.models.AttributesSeating;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.util.data.TallySheet;
import org.vadere.util.test.StatisticalTestCase;

public class TestChooseSeat {
	
	private SeatingModel model;
	private TrainModel trainModel;
	private SeatGroup seatGroup;
	
	@Before
	public void setUp() {
		model = new TestTopographyAndModelBuilder().getSeatingModel();
		trainModel = model.getTrainModel();
		seatGroup = trainModel.getSeatGroup(0);
		assert seatGroup.getPersonCount() == 0;
	}

	@Test(expected=IllegalStateException.class)
	public void testChooseSeatInFullSeatGroup() {
		fillSeatGroup(seatGroup, true, true, true, true);
		model.chooseSeat(seatGroup);
	}
	
	@StatisticalTestCase
	@Test
	public void testChooseSeat0() {
		final int nTrials = 1000;
		TallySheet<Seat> tallySheet = new TallySheet<>();
		for (int i = 0; i < nTrials; i++) {
			tallySheet.addOneTo(model.chooseSeat(seatGroup));
		}

		final double[] fractions = new AttributesSeating().getSeatChoice0();
		double sum = 0;
		for (double d : fractions) {
			sum += d;
		}
		
		for (int i = 0; i < tallySheet.getKeys().size(); i++) {
			Seat s = seatGroup.getSeat(i);
			assertEquals(fractions[i] / sum, (double) tallySheet.getCount(s) / nTrials, 0.05);
		}
	}

	private void clearSeatGroup(SeatGroup sg) {
		for (int i = 0; i < 4; i++) {
			sg.getSeat(i).setSittingPerson(null);
		}
	}

	private void fillSeatGroup(SeatGroup seatGroup, boolean... seats) {
		for (int i = 0; i < seats.length; i++) {
			if (seats[i]) {
				seatGroup.getSeat(i).setSittingPerson(new Pedestrian(new AttributesAgent(), new Random()));
			}
		}
	}
}
