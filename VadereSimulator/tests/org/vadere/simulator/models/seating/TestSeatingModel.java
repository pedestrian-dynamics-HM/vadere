package org.vadere.simulator.models.seating;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;
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
	
	@Before
	public void setUp() {
		random = new Random();
		topography = new TestTopographyAndModelBuilder().getTopography();
		attributes = new ArrayList<>();
		attributes.add(new AttributesSeating());
		attributesPedestrian = new AttributesAgent();
	}

	@Test(expected=IllegalStateException.class)
	public void testInitializeWithWrongTopography() {
		new SeatingModel().initialize(attributes, new Topography(), attributesPedestrian, random);
	}

	@Test(expected=AttributesNotFoundException.class)
	public void testInitializeWithNoAttributes() {
		new SeatingModel().initialize(new ArrayList<>(0), topography, attributesPedestrian, random);
	}

}
