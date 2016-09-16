package org.vadere.simulator.models.seating.trainmodel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Random;

import org.junit.Test;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.attributes.scenario.AttributesTarget;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Target;

public class TestSeat {

	@Test
	public void test() {
		final Target target = new Target(new AttributesTarget());
		final Pedestrian pedestrian = new Pedestrian(new AttributesAgent(), new Random());
		final Seat seat = new Seat(target);

		assertEquals(target, seat.getAssociatedTarget());

		assertEquals(null, seat.getSittingPerson());
		assertTrue(seat.isAvailable());
		assertFalse(seat.isOccupied());

		seat.setSittingPerson(pedestrian);
		
		assertEquals(pedestrian, seat.getSittingPerson());
		assertTrue(seat.isOccupied());
		assertFalse(seat.isAvailable());

		seat.setSittingPerson(null);

		assertEquals(null, seat.getSittingPerson());
		assertTrue(seat.isAvailable());
		assertFalse(seat.isOccupied());
	}

}
