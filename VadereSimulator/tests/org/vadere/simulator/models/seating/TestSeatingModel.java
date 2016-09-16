package org.vadere.simulator.models.seating;

import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.junit.Test;
import org.vadere.simulator.models.seating.trainmodel.Compartment;
import org.vadere.simulator.models.seating.trainmodel.TestTrainModel;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.models.AttributesSeating;
import org.vadere.state.scenario.Topography;

public class TestSeatingModel {

	// WARNING: this is a statistical test. in case of failure, just run again.
	@Test
	public void testChooseCompartment() {
		final Topography topography = TestTrainModel.createTestTopography();
		final int entranceAreaCount = 12;
		final SeatingModel model = new SeatingModel();
		final List<Attributes> attributes = Collections.singletonList(new AttributesSeating());
		model.initialize(attributes, topography, null, new Random());
		
		double nTrials = 1000;
		int leftCounter = 0;
		int rightCounter = 0;
		for (int i = 0; i < nTrials; i++) {
			int enterIndex = 2;
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
		assertTrue(Math.abs(leftCounter - rightCounter) / nTrials < 0.05);
	}
	
}
