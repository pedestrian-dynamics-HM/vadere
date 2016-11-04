package org.vadere.simulator.models.seating.trainmodel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.List;
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
import org.vadere.state.scenario.TrainGeometry;
import org.vadere.util.geometry.shapes.VPoint;

import static org.vadere.simulator.models.seating.TestTopographyAndModelBuilder.*;

public class TestTrainModel {
	
	private TrainGeometry trainGeometry;
	private TrainModel trainModel;

	@Before
	public void setUp() {
		TestTopographyAndModelBuilder builder = new TestTopographyAndModelBuilder();
		trainModel = builder.getTrainModel();
		trainGeometry = builder.getTrainGeometry();
	}

	@Test
	public void testBasicModelProperties() {
		assertEquals(nEntranceAreas, trainModel.getEntranceAreaCount());
		assertEquals(nCompartments, trainModel.getCompartmentCount());
		checkSize(nCompartmentTargets, trainModel.getCompartmentTargets());
		assertEquals(nSeatGroups, trainModel.getSeatGroupCount());
		checkSize(nSeats, trainModel.getSeats());

		// Peds are created by the main model when the simulation starts!
		// They don't really exist before that.
		checkSize(0, trainModel.getPedestrians());
	}
	
	@Test
	public void testAllSeatTargetsAreDistinct() {
		final Collection<Seat> seats = trainModel.getSeats();
		final int distinctTargetCount = (int) seats.stream()
				.map(s -> s.getAssociatedTarget())
				.distinct()
				.count();
		assertEquals(seats.size(), distinctTargetCount);
	}

	@Test
	public void testAllCompartmentTargetsAreDistinct() {
		final List<Target> compartmentTargets = trainModel.getCompartmentTargets();
		final int distinctTargetCount = (int) compartmentTargets.stream()
				.distinct()
				.count();
		assertEquals(compartmentTargets.size(), distinctTargetCount);
	}

	@Test
	public void testDoorSources() {
		checkSize(nSources, trainModel.getAllDoorSources());
		checkSize(nSourcesLeft, trainModel.getLeftDoorSources());
		checkSize(nSourcesRight, trainModel.getRightDoorSources());
	}
	
	@Test
	public void testCertainCompartmentTarget() {
		for (Target t : trainModel.getCompartmentTargets())
			if (t.getId() == 205) // an example for an compartment target
				return;
		fail("Compartment target with id not found.");
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
			assertEquals(trainModel.getEntranceAreaIndexOfSource(s), trainModel.getEntranceAreaIndexForPerson(p));
		}
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

		assertEquals(trainModel.getSeatGroup(0, 0), c.getSeatGroups().get(0));
		assertEquals(trainModel.getSeatGroup(0, 1), c.getSeatGroups().get(1));

		assertEquals(trainModel.getSeat(0, 0, 0), c.getSeatGroups().get(0).getSeat(0));
		
		assertEquals(trainModel.getCompartmentTargets().get(0),
				c.getCompartmentTarget());
	}

	@Test
	public void testFirstNormalCompartment() {
		final Compartment c = trainModel.getCompartment(1);
		checkSize(4, c.getSeatGroups());

		assertEquals(trainModel.getSeatGroup(1, 0), c.getSeatGroups().get(0));
		assertEquals(trainModel.getSeatGroup(1, 1), c.getSeatGroups().get(1));
		assertEquals(trainModel.getSeatGroup(1, 2), c.getSeatGroups().get(2));
		assertEquals(trainModel.getSeatGroup(1, 3), c.getSeatGroups().get(3));

		assertTrue(c.getSeatGroups().get(0).getSeat(0) == trainModel.getSeat(1, 0, 0));
		
		assertEquals(trainModel.getCompartmentTargets().get(1), c.getCompartmentTarget());
	}

	@Test
	public void testLastHalfCompartment() {
		final Compartment c = trainModel.getCompartment(nCompartments - 1);
		checkSize(2, c.getSeatGroups());

		assertEquals(trainModel.getSeatGroup(nCompartments - 1, 0), c.getSeatGroups().get(0));
		assertEquals(trainModel.getSeatGroup(nCompartments - 1, 1), c.getSeatGroups().get(1));
		
		assertTrue(c.getSeatGroups().get(1).getSeat(3) == trainModel.getSeat(nCompartments - 1, 1, 3));
		
		assertEquals(trainModel.getCompartmentTargets().get(nCompartmentTargets - 1),
				c.getCompartmentTarget());
	}
	
	@Test
	public void testSeatLocationFallInCompartment() {
		assertIsInCompartment(trainModel.getSeat(0, 0, 0).getAssociatedTarget(), 0);
		assertIsInCompartment(trainModel.getSeat(0, 1, 1).getAssociatedTarget(), 0);

		assertIsInCompartment(trainModel.getSeat(1, 0, 2).getAssociatedTarget(), 1);
		assertIsInCompartment(trainModel.getSeat(1, 3, 3).getAssociatedTarget(), 1);
	}
	
	@Test
	public void testCompartmentTargetLocationFallInCompartment() {
		assertIsInCompartment(trainModel.getCompartmentTargets().get(0), 0);

		assertIsInCompartment(trainModel.getCompartmentTargets().get(nCompartmentTargets - 1), nCompartments - 1);
	}
	
	private void assertIsInCompartment(Target target, int compartmentIndex) {
		final VPoint point = target.getShape().getCentroid();
		final Point2D targetPoint = new Point2D.Double(point.getX(), point.getY());
		assertTrue(trainGeometry.getCompartmentRect(compartmentIndex).contains(targetPoint));
	}

	@Test(expected=IllegalArgumentException.class)
	public void testGetCompartmentWithInvalidTarget() {
		trainModel.getCompartment(new Target(new AttributesTarget()));
	}

	@Test
	public void testGetCompartmentByTarget() {
		assertEquals(trainModel.getCompartment(0), trainModel.getCompartment(0));
		assertEquals(trainModel.getCompartment(0), getCompartmentByCompartmentTargetIndex(0));

		assertEquals(trainModel.getCompartment(1), getCompartmentByCompartmentTargetIndex(1));
		assertEquals(trainModel.getCompartment(2), getCompartmentByCompartmentTargetIndex(2));
		assertEquals(trainModel.getCompartment(3), getCompartmentByCompartmentTargetIndex(3));

		assertEquals(trainModel.getCompartment(nCompartments - 4), getCompartmentByCompartmentTargetIndex(nCompartmentTargets - 4));
		assertEquals(trainModel.getCompartment(nCompartments - 3), getCompartmentByCompartmentTargetIndex(nCompartmentTargets - 3));
		assertEquals(trainModel.getCompartment(nCompartments - 2), getCompartmentByCompartmentTargetIndex(nCompartmentTargets - 2));

		assertEquals(trainModel.getCompartment(nCompartments - 1), getCompartmentByCompartmentTargetIndex(nCompartmentTargets - 1));
	}

	@Test
	public void testGetCompartmentByPedestrian() {
		Pedestrian p = createTestPedestrian();
		p.addTarget(trainModel.getCompartmentTargets().get(0));
		assertEquals(trainModel.getCompartment(p), getCompartmentByCompartmentTargetIndex(0));
	}

	@Test(expected=IllegalStateException.class)
	public void testGetCompartmentByPedestrianNoTargets() {
		Pedestrian p = createTestPedestrian();
		trainModel.getCompartment(p);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testGetCompartmentByPedestrianWrongLastTarget() {
		Pedestrian p = createTestPedestrian();
		p.addTarget(trainModel.getSeatGroup(0, 0).getSeat(0).getAssociatedTarget());
		trainModel.getCompartment(p);
	}

	@Test
	public void testCalcSeatNumberWithinCompartmentFirstHalfCompartment() {
		assertEquals(1, TrainModel.calculateSeatNumberWithinCompartment(0, 0));
		assertEquals(2, TrainModel.calculateSeatNumberWithinCompartment(1, 0));
		assertEquals(3, TrainModel.calculateSeatNumberWithinCompartment(2, 0));
		assertEquals(4, TrainModel.calculateSeatNumberWithinCompartment(3, 0));
		assertEquals(5, TrainModel.calculateSeatNumberWithinCompartment(0, 1));
		assertEquals(6, TrainModel.calculateSeatNumberWithinCompartment(1, 1));
		assertEquals(7, TrainModel.calculateSeatNumberWithinCompartment(2, 1));
		assertEquals(8, TrainModel.calculateSeatNumberWithinCompartment(3, 1));
	}

	@Test
	public void testCalcSeatNumberWithinCompartmentFirstNormalCompartment() {
		assertEquals(1, TrainModel.calculateSeatNumberWithinCompartment(0, 2));
		assertEquals(2, TrainModel.calculateSeatNumberWithinCompartment(1, 2));
		assertEquals(3, TrainModel.calculateSeatNumberWithinCompartment(2, 2));
		assertEquals(4, TrainModel.calculateSeatNumberWithinCompartment(3, 2));
		assertEquals(5, TrainModel.calculateSeatNumberWithinCompartment(0, 3));
		assertEquals(6, TrainModel.calculateSeatNumberWithinCompartment(1, 3));
		assertEquals(7, TrainModel.calculateSeatNumberWithinCompartment(2, 3));
		assertEquals(8, TrainModel.calculateSeatNumberWithinCompartment(3, 3));
		assertEquals(9,  TrainModel.calculateSeatNumberWithinCompartment(0, 4));
		assertEquals(10, TrainModel.calculateSeatNumberWithinCompartment(1, 4));
		assertEquals(11, TrainModel.calculateSeatNumberWithinCompartment(2, 4));
		assertEquals(12, TrainModel.calculateSeatNumberWithinCompartment(3, 4));
		assertEquals(13, TrainModel.calculateSeatNumberWithinCompartment(0, 5));
		assertEquals(14, TrainModel.calculateSeatNumberWithinCompartment(1, 5));
		assertEquals(15, TrainModel.calculateSeatNumberWithinCompartment(2, 5));
		assertEquals(16, TrainModel.calculateSeatNumberWithinCompartment(3, 5));
	}

	@Test
	public void testCalcSeatNumberWithinCompartmentLastHalfCompartment() {
		assertEquals(1, TrainModel.calculateSeatNumberWithinCompartment(0, nSeatRows - 2));
		assertEquals(2, TrainModel.calculateSeatNumberWithinCompartment(1, nSeatRows - 2));
		assertEquals(3, TrainModel.calculateSeatNumberWithinCompartment(2, nSeatRows - 2));
		assertEquals(4, TrainModel.calculateSeatNumberWithinCompartment(3, nSeatRows - 2));
		assertEquals(5, TrainModel.calculateSeatNumberWithinCompartment(0, nSeatRows - 1));
		assertEquals(6, TrainModel.calculateSeatNumberWithinCompartment(1, nSeatRows - 1));
		assertEquals(7, TrainModel.calculateSeatNumberWithinCompartment(2, nSeatRows - 1));
		assertEquals(8, TrainModel.calculateSeatNumberWithinCompartment(3, nSeatRows - 1));
	}
	
	@Test
	public void testGetSeatForTargetForNonSeatTargets() {
		for (Target t : trainModel.getCompartmentTargets()) {
			assertTrue(trainModel.getSeatForTarget(t) == null);
		}
	}

	@Test
	public void testGetSeatNumberWithinCompartment() {
		// params: expectedSeatNumber, compartmentIndex, seatGroupIndex, seatIndex
		assertEqualsSeatNumber(1, 0, 0, 0);
		assertEqualsSeatNumber(2, 0, 0, 1);
		assertEqualsSeatNumber(3, 0, 1, 0);
		assertEqualsSeatNumber(5, 0, 0, 2);
		assertEqualsSeatNumber(8, 0, 1, 3);

		assertEqualsSeatNumber(1, 1, 0, 0);
		assertEqualsSeatNumber(7, 1, 1, 2);
		assertEqualsSeatNumber(9, 1, 2, 0);
		assertEqualsSeatNumber(15, 1, 3, 2);
	}

	private void assertEqualsSeatNumber(int expectedSeatNumber, int compartmentIndex, int seatGroupIndex, int seatIndex) {
		assertEquals(expectedSeatNumber,
				trainModel.getSeat(compartmentIndex, seatGroupIndex, seatIndex)
						.getSeatNumberWithinCompartment());
	}

	private Compartment getCompartmentByCompartmentTargetIndex(int index) {
		return trainModel.getCompartment(trainModel.getCompartmentTargets().get(index));
	}

	private void checkSize(int expected, Collection<?> actualCollection) {
		assertEquals(expected, actualCollection.size());
	}

	public static Pedestrian createTestPedestrian() {
		return new Pedestrian(new AttributesAgent(), new Random());
	}
	
}
