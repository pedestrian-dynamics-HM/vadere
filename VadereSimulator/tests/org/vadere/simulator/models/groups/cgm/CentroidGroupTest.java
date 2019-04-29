package org.vadere.simulator.models.groups.cgm;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.vadere.simulator.models.potential.fields.IPotentialFieldTarget;
import org.vadere.simulator.utils.CentroidGroupListBuilder;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.attributes.scenario.AttributesObstacle;
import org.vadere.state.scenario.Obstacle;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.PedestrianPair;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.vadere.simulator.models.groups.cgm.PedestrianIdMatcher.containsPedIds;

public class CentroidGroupTest {

	private CentroidGroupListBuilder groupBuilder;

	@Before
	public void init() {
		groupBuilder = new CentroidGroupListBuilder();
	}

	@Test
	public void getMemberPairsTest(){
		groupBuilder.setNextGroupId(1).setNextPedestrianId(1).add(3);
		CentroidGroup group = groupBuilder.getList().get(0);

		ArrayList<PedestrianPair> memberPair = group.getMemberPairs();
		assertThat(memberPair.size(), comparesEqualTo((3*3 - 3) / 2));
		assertThat(memberPair.get(0), containsPedIds(1,2));
		assertThat(memberPair.get(1), containsPedIds(1,3));
		assertThat(memberPair.get(2), containsPedIds(2,3));

		groupBuilder.clear();
		groupBuilder.setNextGroupId(1).setNextPedestrianId(1).add(10);
		group = groupBuilder.getList().get(0);
		memberPair = group.getMemberPairs();
		assertThat(memberPair.size(), comparesEqualTo((10*10 -10) / 2));
	}

	@Test
	public void getEuclidDistTest(){
		groupBuilder.setNextGroupId(1).setNextPedestrianId(1)
				.add(p(1.0, 1.0), p(2.0, 2.0), p(2.0, 1.0));
		CentroidGroup group = groupBuilder.getList().get(0);
		assertThat(group.getSize(), comparesEqualTo(3));

		ArrayList<Pair<PedestrianPair, Double>> pairs = group.getEuclidDist();
		assertThat(pairs.size(), comparesEqualTo(3));

		Pair<PedestrianPair, Double> pair0 = pairs.get(0);
		assertThat(pair0.getLeft(), containsPedIds(1, 2));
		assertThat(pair0.getRight(), closeTo(p(1.0, 1.0).distance(p(2.0, 2.0)), 0.001));

		Pair<PedestrianPair, Double> pair1 = pairs.get(1);
		assertThat(pair1.getLeft(), containsPedIds(1, 3));
		assertThat(pair1.getRight(), closeTo(p(1.0, 1.0).distance(p(2.0, 1.0)), 0.001));


		Pair<PedestrianPair, Double> pair2 = pairs.get(2);
		assertThat(pair2.getLeft(), containsPedIds(2, 3));
		assertThat(pair2.getRight(), closeTo(p(2.0, 2.0).distance(p(2.0, 1.0)), 0.001));
	}


	@Test
	public void getPairIntersectObstacleTest(){
		List<Obstacle> obs = new ArrayList<>();
		obs.add(new Obstacle(new AttributesObstacle(1, new VRectangle(1.0,5.0, 10.0, 1.0))));
		CentroidGroupModel model = mock(CentroidGroupModel.class, Mockito.RETURNS_DEEP_STUBS);
		when(model.getTopography().getObstacles()).thenReturn(obs);

		groupBuilder.setGroupModel(model)
				.add(p(1.5, 7.0), p(3.0, 8.1), p(2.0, 2.0), p(7.0, 3.1));

		CentroidGroup group = groupBuilder.getList().get(0);
		assertThat(group.getSize(), comparesEqualTo(4));

		ArrayList<Pair<PedestrianPair, Boolean>> pairs = group.getPairIntersectObstacle();
		assertThat(pairs.size(), comparesEqualTo((4*4 -4) / 2));

		assertThat(pairs.get(0).getLeft(), containsPedIds(1, 2));
		assertThat(pairs.get(0).getRight(), equalTo(false));

		assertThat(pairs.get(1).getLeft(), containsPedIds(1, 3));
		assertThat(pairs.get(1).getRight(), equalTo(true));

		assertThat(pairs.get(2).getLeft(), containsPedIds(1, 4));
		assertThat(pairs.get(2).getRight(), equalTo(true));

		assertThat(pairs.get(3).getLeft(), containsPedIds(2, 3));
		assertThat(pairs.get(3).getRight(), equalTo(true));

		assertThat(pairs.get(4).getLeft(), containsPedIds(2, 4));
		assertThat(pairs.get(4).getRight(), equalTo(true));

		assertThat(pairs.get(5).getLeft(), containsPedIds(3, 4));
		assertThat(pairs.get(5).getRight(), equalTo(false));
	}

	@Test
	public void getPotentialDistTest(){
		IPotentialFieldTarget field = mock(IPotentialFieldTarget.class, Mockito.RETURNS_DEEP_STUBS);
		groupBuilder
				.add(p(1.5, 7.0), p(3.0, 8.1), p(2.0, 2.0));
		CentroidGroup group = groupBuilder.getList().get(0);
		LinkedList<Double> potential = new LinkedList<>(Arrays.asList(23.1, 20.1, 17.3));
		group.members
				.forEach(m -> when(field.getPotential(m.getPosition(),m ))
								.thenReturn(potential.pollFirst()));
		group.setPotentialFieldTarget(field);

		ArrayList<Pair<PedestrianPair, Double>> pairs = group.getPotentialDist();
		assertThat(group.getSize(), comparesEqualTo(3));

		assertThat(pairs.get(0).getLeft(), containsPedIds(1, 2));
		assertThat(pairs.get(0).getRight(), closeTo(Math.abs(23.1 - 20.1), 0.0001));

		assertThat(pairs.get(1).getLeft(), containsPedIds(1, 3));
		assertThat(pairs.get(1).getRight(), closeTo(Math.abs(23.1 - 17.3), 0.0001));

		assertThat(pairs.get(2).getLeft(), containsPedIds(2, 3));
		assertThat(pairs.get(2).getRight(), closeTo(Math.abs(20.1 - 17.3), 0.0001));
	}

	@Test
	public void checkGroupSpeed() {

		Pedestrian p1 = new Pedestrian(new AttributesAgent(1), new Random());
		Pedestrian p2 = new Pedestrian(new AttributesAgent(2), new Random());
		Pedestrian p3 = new Pedestrian(new AttributesAgent(3), new Random());
		p1.setFreeFlowSpeed(1.3);
		p2.setFreeFlowSpeed(1.1);
		p3.setFreeFlowSpeed(1.4);
		CentroidGroupModel model = new CentroidGroupModel();
		CentroidGroup group = new CentroidGroup(1, 3, model);
		group.addMember(p1);
		group.addMember(p2);
		group.addMember(p3);
		assertThat(group.getGroupVelocity(), equalTo(1.1));
		assertThat(p1.getFreeFlowSpeed(), equalTo(1.1));
		assertThat(p2.getFreeFlowSpeed(), equalTo(1.1));
		assertThat(p3.getFreeFlowSpeed(), equalTo(1.1));


	}


	private VPoint p(double x, double y){
		return new VPoint(x, y);
	}




}