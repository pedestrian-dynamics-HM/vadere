package org.vadere.simulator.models.seating;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;
import org.vadere.simulator.projects.Domain;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.exceptions.AttributesNotFoundException;
import org.vadere.state.attributes.models.AttributesSeating;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.Topography;

public class TestSeatingModel {
	
	private Random random;
	private Topography topography;
	private List<Attributes> attributes;
	private AttributesAgent attributesPedestrian;
	private SeatingModel seatingModel;
	
	@Before
	public void setUp() {
		random = new Random();
		topography = new TestTopographyAndModelBuilder().getTopography();
		attributes = new ArrayList<>();
		attributes.add(new AttributesSeating());
		attributesPedestrian = new AttributesAgent();
		seatingModel = new SeatingModel();
		seatingModel.initialize(attributes, new Domain(topography), attributesPedestrian, random);
	}

	@Test(expected=IllegalStateException.class)
	public void testInitializeWithWrongTopography() {
		new SeatingModel().initialize(attributes, new Domain(new Topography()), attributesPedestrian, random);
	}

	@Test(expected=AttributesNotFoundException.class)
	public void testInitializeWithNoAttributes() {
		new SeatingModel().initialize(new ArrayList<>(0), new Domain(topography), attributesPedestrian, random);
	}

	@Test
	public void testIsInnerCompartment() {
		assertFalse(seatingModel.isInnerCompartment(0));
		assertTrue(seatingModel.isInnerCompartment(1));
		assertTrue(seatingModel.isInnerCompartment(TestTopographyAndModelBuilder.nCompartments - 2));
		assertFalse(seatingModel.isInnerCompartment(TestTopographyAndModelBuilder.nCompartments - 1));
	}

	@Test
	public void testGetDirectionFromEntranceAreaToCompartment() {
		// entrance areas:    0   1   2   3
		// compartments:    0   1   2   3   4
		assertEquals(-1, seatingModel.getDirectionFromEntranceAreaToCompartment(0, 0));
		assertEquals(-1, seatingModel.getDirectionFromEntranceAreaToCompartment(1, 0));
		assertEquals(-1, seatingModel.getDirectionFromEntranceAreaToCompartment(2, 0));
		assertEquals(-1, seatingModel.getDirectionFromEntranceAreaToCompartment(2, 1));
		assertEquals(-1, seatingModel.getDirectionFromEntranceAreaToCompartment(2, 2));

		assertEquals(1, seatingModel.getDirectionFromEntranceAreaToCompartment(0, 1));
		assertEquals(1, seatingModel.getDirectionFromEntranceAreaToCompartment(1, 2));
		assertEquals(1, seatingModel.getDirectionFromEntranceAreaToCompartment(1, 3));
		assertEquals(1, seatingModel.getDirectionFromEntranceAreaToCompartment(1, 4));
		assertEquals(1, seatingModel.getDirectionFromEntranceAreaToCompartment(3, 4));
	}

}
