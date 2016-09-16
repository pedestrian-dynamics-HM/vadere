package org.vadere.simulator.models.seating.trainmodel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Collection;
import java.util.Scanner;

import org.junit.Before;
import org.junit.Test;
import org.vadere.simulator.projects.io.JsonConverter;
import org.vadere.simulator.projects.io.TextOutOfNodeException;
import org.vadere.state.scenario.Et423Geometry;
import org.vadere.state.scenario.Topography;

public class TestTrainModel {
	
	private static final int nEntranceAreas = 12;
	private static final int nCompartments = nEntranceAreas + 1; // includes 2 half-compartments
	private static final int nInterimDestinations = nCompartments * 3 - 4; // includes 2 targets from half-compartments
	private static final int nSeatGroups = nCompartments * 4 - 4;
	private static final int nSeats = nSeatGroups * 4;
	private static final int nPersons = 10;

	private TrainModel trainModel;

	@Before
	public void setUp() {
		try {
			@SuppressWarnings("resource")
			final String json = new Scanner(TestTrainModel.class.getResourceAsStream("/data/test-train-topography.json"), "UTF-8").useDelimiter("\\A").next();
			final Topography topography = JsonConverter.deserializeTopography(json);
			trainModel = new TrainModel(topography, new Et423Geometry());
		} catch (IOException | TextOutOfNodeException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	@Test
	public void testBasicModelProperties() {
		assertEquals(nEntranceAreas, trainModel.getNumberOfEntranceAreas());
		checkSize(nInterimDestinations, trainModel.getInterimDestinations());
		checkSize(nSeatGroups, trainModel.getSeatGroups());
		checkSize(nSeats, trainModel.getSeats());
		assertEquals(nPersons, trainModel.getPedestrians().size());
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

	private void checkSize(int expected, Collection<?> actualCollection) {
		assertEquals(expected, actualCollection.size());
	}

}
