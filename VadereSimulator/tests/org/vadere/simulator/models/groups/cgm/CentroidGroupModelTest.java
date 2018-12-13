package org.vadere.simulator.models.groups.cgm;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.vadere.simulator.models.groups.Group;
import org.vadere.simulator.models.groups.GroupFactory;
import org.vadere.simulator.models.potential.fields.PotentialFieldTarget;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.models.AttributesCGM;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.attributes.scenario.AttributesSource;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Source;
import org.vadere.state.scenario.Topography;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class CentroidGroupModelTest {

	private CentroidGroupModel cgm;
	private Random rnd;
	private List<Attributes> attributesList;
	private Topography topography;
	private AttributesAgent attributesAgent;

	@Before
	public void init(){
		cgm = new CentroidGroupModel();
		rnd = new Random();
		attributesAgent = new AttributesAgent();
		attributesList = new ArrayList<>();
		attributesList.add(new AttributesCGM());
		topography = new Topography();
	}

	@After
	public void cleanup(){
		cgm = null;
	}

	@Test
	public void initializeGroupFactoryTest(){
		simpleInitialize();
		List<Double> dist = new LinkedList<>();
		dist.add(0.2);
		dist.add(0.8);
		cgm.initializeGroupFactory(1, dist);
		GroupFactory cgf = cgm.getGroupFactory(1);
		assertNotNull(cgf);
	}

	@Test(expected = IllegalArgumentException.class)
	public void initializeGroupFactoryWrongIDTest(){
		simpleInitialize();
		List<Double> dist = new LinkedList<>();
		dist.add(0.2);
		dist.add(0.8);
		cgm.initializeGroupFactory(2, dist);
		cgm.getGroupFactory(1);
	}

	@Test
	public void setPotentialFieldTest(){
		simpleInitialize();
		PotentialFieldTarget potential = mock(PotentialFieldTarget.class);
		List<Double> dist = new LinkedList<>();
		dist.add(0.0);
		dist.add(0.0);
		dist.add(1.0); // 3 groups
		cgm.initializeGroupFactory(2, dist);
		int groupSize = cgm.getGroupFactory(2).createNewGroup();
		getDummies(groupSize, 2).forEach(p -> cgm.elementAdded(p));
		assertEquals(groupSize, cgm.getPedestrianGroupMap().size());

		cgm.setPotentialFieldTarget(potential);
		cgm.getPedestrianGroupMap().forEach((key, value) -> assertEquals(value.getPotentialFieldTarget(), potential));
	}

	@Test
	public void getNewGroupTest(){
		simpleInitialize();
		Group group = cgm.getNewGroup(5);
		assertEquals(5, group.getSize());
	}

	@Test
	public void removeMemberTest() {
		simpleInitialize();
		PotentialFieldTarget potential = mock(PotentialFieldTarget.class);
		List<Double> dist = new LinkedList<>();
		dist.add(0.0);
		dist.add(0.0);
		dist.add(1.0); // 3 groups
		cgm.initializeGroupFactory(2, dist);
		int groupSize1 = cgm.getGroupFactory(2).createNewGroup();
		List<Pedestrian> peds1 = getDummies(groupSize1, 2);
		peds1.forEach(p -> cgm.elementAdded(p));

		int groupSize2 = cgm.getGroupFactory(2).createNewGroup();
		List<Pedestrian> peds2 = getDummies(groupSize2, 2);
		peds2.forEach(p -> cgm.elementAdded(p));

		assertEquals("6 Pedestrian should be inside the simulation",6, cgm.getPedestrianGroupMap().size());

	}

	private void simpleInitialize(){
		cgm.initialize(attributesList, topography,attributesAgent, rnd);
	}

	private List<Pedestrian> getDummies(int n, int sourceId){
		Source source = new Source(new AttributesSource(sourceId));
		List<Pedestrian> peds = new ArrayList<>();
		for (int i = 1; i <= n; i++){
			Pedestrian ped = new Pedestrian(new AttributesAgent(i), rnd);
			ped.setSource(source);
			peds.add(ped);
		}
		return peds;
	}
}