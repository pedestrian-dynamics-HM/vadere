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
import org.vadere.state.attributes.scenario.AttributesSource;
import org.vadere.state.attributes.scenario.AttributesTarget;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Source;
import org.vadere.state.scenario.Target;

import static org.vadere.simulator.models.seating.TestTopographyAndModelBuilder.*;

public class TestTrainModel {
	
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
		checkSize(0, trainModel.getPedestrians());
	}
	
	@Test
	public void testDoorSources() {
		checkSize(nSources, trainModel.getAllDoorSources());
		checkSize(nSourcesLeft, trainModel.getLeftDoorSources());
		checkSize(nSourcesRight, trainModel.getRightDoorSources());

		boolean interimDestsContain133 = trainModel.getInterimDestinations().stream()
				.mapToInt(d -> d.getId())
				.filter(id -> id == 133)
				.count() > 0;
		assertTrue(interimDestsContain133);
	}
	
	@Test(expected=RuntimeException.class)
	public void testGetEntranceAreaIndexForPersonFail1() {
		Pedestrian p = createTestPedestrian();
		// no source assigned
		trainModel.getEntranceAreaIndexForPerson(p);
	}
	
	@Test(expected=RuntimeException.class)
	public void testGetEntranceAreaIndexForPersonFail2() {
		Pedestrian p = createTestPedestrian();
		// assign source that is not one of the door sources
		p.setSource(new Source(new AttributesSource(0)));
		trainModel.getEntranceAreaIndexForPerson(p);
	}
	
	@Test
	public void testGetEntranceAreaIndexForPerson() {
		Pedestrian p = createTestPedestrian();
		for (Source s : trainModel.getAllDoorSources()) {
			p.setSource(s);
			assertEquals(trainModel.entranceAreaIndexOfSource(s), trainModel.getEntranceAreaIndexForPerson(p));
		}
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
		checkSize(2, c.getSeatGroups());

		assertEquals(trainModel.getSeatGroup(0), c.getSeatGroups().get(0));
		assertEquals(trainModel.getSeatGroup(1), c.getSeatGroups().get(1));

		assertEquals(trainModel.getSeats().get(0), c.getSeatGroups().get(0).getSeat(0));
		
		for (int i = 0; i < nEntranceAreas; i++) {
			assertEquals(trainModel.getInterimDestinations().get(0),
					c.getInterimTargetCloserTo(i));
		}
	}

	@Test
	public void testFirstNormalCompartment() {
		final Compartment c = trainModel.getCompartment(1);
		checkSize(4, c.getSeatGroups());

		assertEquals(trainModel.getSeatGroup(2), c.getSeatGroups().get(0));
		assertEquals(trainModel.getSeatGroup(3), c.getSeatGroups().get(1));
		assertEquals(trainModel.getSeatGroup(4), c.getSeatGroups().get(2));
		assertEquals(trainModel.getSeatGroup(5), c.getSeatGroups().get(3));

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
		checkSize(2, c.getSeatGroups());

		assertEquals(trainModel.getSeatGroup(nSeatGroups - 2), c.getSeatGroups().get(0));
		assertEquals(trainModel.getSeatGroup(nSeatGroups - 1), c.getSeatGroups().get(1));
		
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
	
}
