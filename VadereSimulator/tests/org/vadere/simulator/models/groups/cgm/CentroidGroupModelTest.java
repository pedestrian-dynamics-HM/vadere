package org.vadere.simulator.models.groups.cgm;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.vadere.simulator.models.groups.Group;
import org.vadere.simulator.projects.Domain;
import org.vadere.simulator.utils.CentroidGroupListBuilder;
import org.vadere.simulator.utils.PedestrianListBuilder;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.models.AttributesCGM;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.attributes.scenario.AttributesSource;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Source;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;

public class CentroidGroupModelTest {

	private CentroidGroupModel cgm;
	private Random rnd;
	private List<Attributes> attributesList;
	private Topography topography;
	private AttributesAgent attributesAgent;


	@Before
	public void init() {
		cgm = new CentroidGroupModel();
		rnd = new Random();
		attributesAgent = new AttributesAgent();
		attributesList = new ArrayList<>();
		attributesList.add(new AttributesCGM());
		topography = new Topography();
	}

	@After
	public void cleanup() {
		cgm = null;
	}


	@Test
	public void getNewGroupTest() {
		simpleInitialize();
		Group group = cgm.getNewGroup(5);
		assertEquals(5, group.getSize());
	}


	private void simpleInitialize() {
		cgm.initialize(attributesList, new Domain(topography), attributesAgent, rnd);
	}

	private List<Pedestrian> getDummies(int n, int sourceId) {
		Source source = new Source(new AttributesSource(sourceId));
		List<Pedestrian> peds = new ArrayList<>();
		for (int i = 1; i <= n; i++) {
			Pedestrian ped = new Pedestrian(new AttributesAgent(i), rnd);
			ped.setSource(source);
			peds.add(ped);
		}
		return peds;
	}




}