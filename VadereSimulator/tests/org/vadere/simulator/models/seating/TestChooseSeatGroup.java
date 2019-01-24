package org.vadere.simulator.models.seating;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.vadere.simulator.models.seating.trainmodel.Compartment;
import org.vadere.simulator.models.seating.trainmodel.SeatGroup;
import org.vadere.simulator.models.seating.trainmodel.TrainModel;
import org.vadere.state.attributes.models.AttributesSeating;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.util.data.TallySheet;
import org.vadere.util.test.FractionProbabilityNormalization;
import org.vadere.util.test.StatisticalTestCase;

public class TestChooseSeatGroup {
	
	private SeatingModel model;
	private TrainModel trainModel;
	
	@Before
	public void setUp() {
		model = new TestTopographyAndModelBuilder().getSeatingModel();
		trainModel = model.getTrainModel();
	}

	@Test(expected=IllegalStateException.class)
	public void testChooseSeatGroupBetween4FullOnes() {
		final Compartment compartment = trainModel.getCompartment(11);
		fillCompartment(compartment, 4, 4, 4, 4);
		runChooseSeatGroup(compartment, 1);
	}
	
	@StatisticalTestCase
	@Test
	public void testChooseSeatGroupBetween4WithEqualCounts() {
		for (int n = 0; n < 4; n++) {
			testChooseSeatGroupBetween4(n);
		}
	}

	@StatisticalTestCase
	@Test
	public void testChooseSeatGroupBetween0123() {
		final Compartment compartment = trainModel.getCompartment(11);
		assert compartment.getPersonCount() == 0;
		
		fillCompartment(compartment, 0, 1, 2, 3);
		assert compartment.getPersonCount() == 6;
		
		final int nTrials = 10000;
		final TallySheet<Integer> tallySheet = runChooseSeatGroup(compartment, nTrials);
		
		List<Integer> sortedKeys = tallySheet.getKeys().stream()
				.sorted().collect(Collectors.toList());

		double[] ps = getSeatGroupPersonCountProbabilities();

		for (int i = 0; i < sortedKeys.size(); i++) {
			final Integer key = sortedKeys.get(i);
//			System.out.println(i + " " + key + " " + ps[i]);
			assertEquals(ps[i], (double) tallySheet.getCount(key) / nTrials, 0.05);
		}
		
	}

	@StatisticalTestCase
	@Test
	public void testChooseSeatGroupBetween1234() {
		final Compartment compartment = trainModel.getCompartment(11);
		assert compartment.getPersonCount() == 0;
		
		fillCompartment(compartment, 1, 2, 3, 4);
		assert compartment.getPersonCount() == 10;
		
		final int nTrials = 10000;
		final TallySheet<Integer> tallySheet = runChooseSeatGroup(compartment, nTrials);
		
		List<Integer> sortedKeys = tallySheet.getKeys().stream()
				.sorted().collect(Collectors.toList());

		double[] ps = getSeatGroupPersonCountProbabilities();

		for (int i = 0; i < sortedKeys.size(); i++) {
			final Integer key = sortedKeys.get(i);
//			System.out.println(i + " " + key + " " + ps[i]);
			assertEquals(ps[i], (double) tallySheet.getCount(key) / nTrials, 0.05);
		}
		
	}

	private void testChooseSeatGroupBetween4(int personCountForEachSeatGroup) {
		final Compartment compartment = trainModel.getCompartment(11);
		clearCompartment(compartment);
		fillCompartment(compartment, personCountForEachSeatGroup, personCountForEachSeatGroup,
				personCountForEachSeatGroup, personCountForEachSeatGroup);
		
		final int nTrials = 10000;
		final TallySheet<Integer> tallySheet = runChooseSeatGroup(compartment, nTrials);
		
		for (Integer key : tallySheet.getKeys()) {
			assertEquals(0.25, (double) tallySheet.getCount(key) / nTrials, 0.06);
		}
	}

	private void clearCompartment(Compartment compartment) {
		for (int i = 0; i < 4; i++) {
			for (int j = 0; j < 4; j++) {
				compartment.getSeat(i, j).setSittingPerson(null);
			}
		}
	}

	private double[] getSeatGroupPersonCountProbabilities() {
		final Map<Boolean, Double> probabilities = FractionProbabilityNormalization
				.normalize(new AttributesSeating().getSeatGroupChoice());

		double p1 = probabilities.get(true);
		double p2 = probabilities.get(false);
		double[] ps = { p1, p2, p2*p2, p2*p2*p2 };
		return ps;
	}

	private TallySheet<Integer> runChooseSeatGroup(final Compartment compartment, final int nTrials) {
		final TallySheet<Integer> tallySheet = new TallySheet<>();
		for (int i = 0; i < nTrials; i++) {
			final SeatGroup sg = model.chooseSeatGroup(compartment);
			tallySheet.addOneTo(sg.getIndex());
		}
		return tallySheet;
	}

	private void fillCompartment(Compartment compartment, int... numbers) {
		for (int i = 0; i < 4; i++) {
			sitDownNewPerson(compartment.getSeatGroup(i), numbers[i]);
		}
	}

	private void sitDownNewPerson(SeatGroup seatGroup, int number) {
		for (int i = 0; i < number; i++) {
			seatGroup.getSeat(i).setSittingPerson(new Pedestrian(new AttributesAgent(), new Random()));
		}
	}
}
