package org.vadere.simulator.models.seating.trainmodel;

import static org.junit.Assert.*;

import java.util.Random;

import org.junit.Test;
import org.vadere.simulator.models.seating.TestTopographyAndModelBuilder;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.Pedestrian;

public class TestCompartment {

	@Test
	public void testGetPersonCount() {
		final TrainModel trainModel = new TestTopographyAndModelBuilder().getTrainModel();
		final Compartment c = trainModel.getCompartment(11);

		assertEquals(0, c.getPersonCount());

		sitDownPerson(c, 1, 3);
		assertEquals(1, c.getPersonCount());

		sitDownPerson(c, 0, 2);
		sitDownPerson(c, 2, 1);
		sitDownPerson(c, 3, 0);
		assertEquals(4, c.getPersonCount());

	}
	
	@Test
	public void testIsFull() {
		final TrainModel trainModel = new TestTopographyAndModelBuilder().getTrainModel();
		final Compartment c = trainModel.getCompartment(11);

		for (int i = 0; i < 4; i++)
			for (int j = 0; j < 4; j++) {
				assertFalse(c.isFull());
				sitDownPerson(c, i, j);
			}

		assertTrue(c.isFull());
	}

	private void sitDownPerson(Compartment c, int seatGroupIndex, int seatIndex) {
		// not for half-compartments!
		c.getSeat(seatGroupIndex, seatIndex)
				.setSittingPerson(new Pedestrian(new AttributesAgent(), new Random()));
		
	}

}
