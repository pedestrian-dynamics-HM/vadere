package org.vadere.simulator.models.seating;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.vadere.simulator.models.seating.trainmodel.Compartment;
import org.vadere.simulator.models.seating.trainmodel.TrainModel;
import org.vadere.util.test.StatisticalTestCase;

public class TestChooseCompartment {
	
	private SeatingModel model;
	private TrainModel trainModel;
	
	@Before
	public void setUp() {
		model = new TestTopographyAndModelBuilder().getSeatingModel();
		trainModel = model.getTrainModel();
	}

	@StatisticalTestCase
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

}
