package org.vadere.simulator.models.seating;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.apache.commons.math3.util.Pair;
import org.junit.Before;
import org.junit.Test;
import org.vadere.simulator.models.seating.trainmodel.Compartment;
import org.vadere.simulator.models.seating.trainmodel.SeatGroup;
import org.vadere.simulator.models.seating.trainmodel.TrainModel;
import org.vadere.state.attributes.models.AttributesSeating;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.Pedestrian;

public class TestSeatingModel {
	
	private SeatingModel model;
	private TrainModel trainModel;
	
	@Before
	public void setUp() {
		model = new TestTopographyAndModelBuilder().getSeatingModel();
		trainModel = model.getTrainModel();
	}

	// WARNING: this is a statistical test. in case of failure, just run again.
	@Test
	public void testChooseCompartment() {
		final int entranceAreaCount = trainModel.getEntranceAreaCount();
		
		int nTrials = 1000;
		int leftCounter = 0;
		int rightCounter = 0;
		for (int i = 0; i < nTrials; i++) {
			int enterIndex = 4;
			final Compartment compartment = model.chooseCompartment(null, enterIndex);
			final int compartmentIndex = compartment.getIndex();
			assertTrue(compartmentIndex >= 0);
			assertTrue(compartmentIndex <= entranceAreaCount);
			if (compartmentIndex <= enterIndex) {
				leftCounter++;
			} else {
				rightCounter++;
			}
		}
		
		// because of normal distribution, the percentage of people going left
		// or right respectively should be about 50/50
		assertTrue(Math.abs(leftCounter - rightCounter) / nTrials < 0.06);
	}
	
	// WARNING: this is a statistical test. in case of failure, just run again.
	@Test
	public void testChooseSeatGroupBetween4EmptyOnes() {
		final Compartment compartment = trainModel.getCompartment(11);
		assert compartment.getPersonCount() == 0;
		
		final int nTrials = 1000;
		final TallySheet tallySheet = runChooseSeatGroup(compartment, nTrials);
		
		for (Integer key : tallySheet.getKeys()) {
			assertEquals(0.25, (double) tallySheet.getCount(key) / nTrials, 0.05);
		}
		
	}
	
	// WARNING: this is a statistical test. in case of failure, just run again.
	@Test
	public void testChooseSeatGroupBetween0123() {
		final Compartment compartment = trainModel.getCompartment(11);
		assert compartment.getPersonCount() == 0;
		
		fillCompartment(compartment, 0, 1, 2, 3);
		assert compartment.getPersonCount() == 6;
		
		final int nTrials = 1000;
		final TallySheet tallySheet = runChooseSeatGroup(compartment, nTrials);
		
		List<Integer> sortedKeys = tallySheet.getKeys().stream()
				.sorted().collect(Collectors.toList());

		final List<Pair<Boolean, Double>> probabilities = new AttributesSeating().getSeatGroupChoice();
		final Pair<Boolean, Double> pair = probabilities.get(0);
		assert pair.getFirst() == true;
		final Pair<Boolean, Double> otherPair = probabilities.get(1);
		assert otherPair.getFirst() == false;

		double p1 = pair.getSecond();
		double p2 = otherPair.getSecond();
		final double sum = p1 + p2;
		p1 /= sum;
		p2 /= sum;
		double[] ps = { p1, p2, p2*p2, p2*p2*p2 };

		for (int i = 0; i < 4; i++) {
			final Integer key = sortedKeys.get(i);
//			System.out.println(i + " " + key + " " + ps[i]);
			assertEquals(ps[i], (double) tallySheet.getCount(key) / nTrials, 0.05);
		}
		
	}

	private TallySheet runChooseSeatGroup(final Compartment compartment, final int nTrials) {
		final TallySheet tallySheet = new TallySheet();
		for (int i = 0; i < nTrials; i++) {
			final SeatGroup sg = model.chooseSeatGroup(compartment);
			tallySheet.addOneTo(sg.getIndex());
		}
		return tallySheet;
	}

	private void fillCompartment(Compartment compartment, int... numbers) {
		for (int i = 0; i < 4; i++) {
			sitDownNewPerson(compartment.getSeatGroups().get(i), numbers[i]);
		}
		
	}

	private void sitDownNewPerson(SeatGroup seatGroup, int number) {
		for (int i = 0; i < number; i++) {
			seatGroup.getSeat(i).setSittingPerson(new Pedestrian(new AttributesAgent(), new Random()));
		}
	}
}
