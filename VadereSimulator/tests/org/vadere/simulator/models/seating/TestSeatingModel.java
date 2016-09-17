package org.vadere.simulator.models.seating;

import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.vadere.simulator.models.seating.trainmodel.Compartment;

public class TestSeatingModel {
	
	private SeatingModel model;
	
	@Before
	public void setUp() {
		model = new TestTopographyAndModelBuilder().getSeatingModel();

	}

	// WARNING: this is a statistical test. in case of failure, just run again.
	@Test
	public void testChooseCompartment() {
		final int entranceAreaCount = 12; // from test topography
		
		double nTrials = 1000;
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
	
	@Test
	public void testChooseSeatGroup() {
	}
	
}
