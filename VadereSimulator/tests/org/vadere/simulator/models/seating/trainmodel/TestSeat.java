package org.vadere.simulator.models.seating.trainmodel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.vadere.state.attributes.scenario.AttributesTarget;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Target;

public class TestSeat {

	@Test
	public void test() {
		final Target target = new Target(new AttributesTarget());
		final Pedestrian pedestrian = TestTrainModel.createTestPedestrian();
		final Seat seat = new Seat(null, target, 0);
		
		assertEquals(null, seat.getSeatGroup());

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
