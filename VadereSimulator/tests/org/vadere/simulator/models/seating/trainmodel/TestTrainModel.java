package org.vadere.simulator.models.seating.trainmodel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.Random;
import org.junit.Before;
import org.junit.Test;
import org.vadere.simulator.models.seating.TestTopographyAndModelBuilder;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.attributes.scenario.AttributesTarget;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Target;

public class TestTrainModel {
	
	private static final int nEntranceAreas = 12;
	private static final int nCompartments = nEntranceAreas + 1; // includes 2 half-compartments
	private static final int nInterimDestinations = nCompartments * 3 - 4; // includes 2 targets from half-compartments
	private static final int nSeatGroups = nCompartments * 4 - 4;
	private static final int nSeats = nSeatGroups * 4;

	private TrainModel trainModel;

	@Before
	public void setUp() {
		trainModel = new TestTopographyAndModelBuilder().getTrainModel();
	}

		@Test
	public void testBasicModelProperties() {
		assertEquals(nEntranceAreas, trainModel.getEntranceAreaCount());
		checkSize(nInterimDestinations, trainModel.getInterimDestinations());
		checkSize(nSeatGroups, trainModel.getSeatGroups());
		checkSize(nSeats, trainModel.getSeats());
		
		// Peds are created by the main model when the simulation starts!
		// They don't really exist before that.
		assertEquals(0, trainModel.getPedestrians().size());
	}
	
	@Test
	public void testGetSeatGroupLimits() {
		assertTrue(trainModel.getSeatGroup(0) != null);
		try {
			trainModel.getSeatGroup(-1);
			fail("exception expected");
		} catch (IndexOutOfBoundsException e) { }
		
		assertTrue(trainModel.getSeatGroup(nSeatGroups - 1) != null);
		try {
			trainModel.getSeatGroup(nSeatGroups);
			fail("exception expected");
		} catch (IndexOutOfBoundsException e) { }
	}
	
	@Test
	public void testGetCompartmentLimits() {
		assertTrue(trainModel.getCompartment(0) != null); // first half-compartment
		assertTrue(trainModel.getCompartment(1) != null); // first normal compartment
		try {
			trainModel.getCompartment(-1);
			fail("exception expected");
		} catch (IndexOutOfBoundsException e) { }
		
		assertTrue(trainModel.getCompartment(nCompartments - 2) != null); // last normal compartment
		assertTrue(trainModel.getCompartment(nCompartments - 1) != null); // last half-compartment
		try {
			trainModel.getCompartment(nCompartments);
			fail("exception expected");
		} catch (IndexOutOfBoundsException e) { }
	}
	
	@Test
	public void testFirstHalfCompartment() {
		final Compartment c = trainModel.getCompartment(0);
		assertTrue(c.getSeatGroups().get(0) == null);
		assertTrue(c.getSeatGroups().get(1) == null);
		assertTrue(c.getSeatGroups().get(2) == trainModel.getSeatGroup(0));
		assertTrue(c.getSeatGroups().get(3) == trainModel.getSeatGroup(1));

		assertTrue(c.getSeatGroups().get(2).getSeat(0) == trainModel.getSeats().get(0));
		
		for (int i = 0; i < nEntranceAreas; i++) {
			assertEquals(trainModel.getInterimDestinations().get(0),
					c.getInterimTargetCloserTo(i));
		}
	}

	@Test
	public void testFirstNormalCompartment() {
		final Compartment c = trainModel.getCompartment(1);
		assertTrue(c.getSeatGroups().get(0) == trainModel.getSeatGroup(2));
		assertTrue(c.getSeatGroups().get(1) == trainModel.getSeatGroup(3));
		assertTrue(c.getSeatGroups().get(2) == trainModel.getSeatGroup(4));
		assertTrue(c.getSeatGroups().get(3) == trainModel.getSeatGroup(5));

		assertTrue(c.getSeatGroups().get(0).getSeat(0) == trainModel.getSeats().get(8));
		
		assertEquals(trainModel.getInterimDestinations().get(1), c.getInterimTargetCloserTo(0));
		for (int i = 1; i < nEntranceAreas; i++) {
			assertEquals(trainModel.getInterimDestinations().get(3),
					c.getInterimTargetCloserTo(i));
		}
	}

	@Test
	public void testLastHalfCompartment() {
		final Compartment c = trainModel.getCompartment(nCompartments - 1);
		assertTrue(c.getSeatGroups().get(0) == trainModel.getSeatGroup(nSeatGroups - 2));
		assertTrue(c.getSeatGroups().get(1) == trainModel.getSeatGroup(nSeatGroups - 1));
		assertTrue(c.getSeatGroups().get(2) == null);
		assertTrue(c.getSeatGroups().get(3) == null);
		
		assertTrue(c.getSeatGroups().get(1).getSeat(3) == trainModel.getSeats().get(nSeats - 1));
		
		for (int i = 0; i < nEntranceAreas; i++) {
			assertEquals(trainModel.getInterimDestinations().get(nInterimDestinations - 1),
					c.getInterimTargetCloserTo(i));
		}
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testGetCompartmentWithInvalidTarget() {
		trainModel.getCompartment(new Target(new AttributesTarget()));
	}

	@Test
	public void testGetCompartmentByTarget() {
		assertEquals(trainModel.getCompartment(0), trainModel.getCompartment(0));
		assertEquals(trainModel.getCompartment(0), getCompartmentByInterimTargetIndex(0));

		assertEquals(trainModel.getCompartment(1), getCompartmentByInterimTargetIndex(1));
		assertEquals(trainModel.getCompartment(1), getCompartmentByInterimTargetIndex(3));
		assertEquals(trainModel.getCompartment(2), getCompartmentByInterimTargetIndex(4));

		assertEquals(trainModel.getCompartment(nCompartments - 3), getCompartmentByInterimTargetIndex(nInterimDestinations - 5));
		assertEquals(trainModel.getCompartment(nCompartments - 2), getCompartmentByInterimTargetIndex(nInterimDestinations - 4));
		assertEquals(trainModel.getCompartment(nCompartments - 2), getCompartmentByInterimTargetIndex(nInterimDestinations - 2));

		assertEquals(trainModel.getCompartment(nCompartments - 1), getCompartmentByInterimTargetIndex(nInterimDestinations - 1));
	}

	@Test
	public void testGetCompartmentByPedestrian() {
		Pedestrian p = createTestPedestrian();
		p.addTarget(trainModel.getInterimDestinations().get(0));
		assertEquals(trainModel.getCompartment(p), getCompartmentByInterimTargetIndex(0));
	}

	@Test(expected=IllegalStateException.class)
	public void testGetCompartmentByPedestrianNoTargets() {
		Pedestrian p = createTestPedestrian();
		trainModel.getCompartment(p);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testGetCompartmentByPedestrianWrongLastTarget() {
		Pedestrian p = createTestPedestrian();
		p.addTarget(trainModel.getSeatGroup(0).getSeat(0).getAssociatedTarget());
		trainModel.getCompartment(p);
	}

	private Compartment getCompartmentByInterimTargetIndex(int index) {
		return trainModel.getCompartment(trainModel.getInterimDestinations().get(index));
	}

	private void checkSize(int expected, Collection<?> actualCollection) {
		assertEquals(expected, actualCollection.size());
	}

	public static Pedestrian createTestPedestrian() {
		return new Pedestrian(new AttributesAgent(), new Random());
	}
	
	// TODO test getCompartment(Person) getCompartment(Target)

}
