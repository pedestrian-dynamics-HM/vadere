package org.vadere.simulator.control;

import org.junit.Test;
import org.mockito.Mockito;
import org.vadere.simulator.control.factory.GroupSourceControllerFactory;
import org.vadere.simulator.control.factory.SingleSourceControllerFactory;
import org.vadere.simulator.models.groups.CentroidGroupModel;
import org.vadere.simulator.models.groups.GroupSizeDeterminatorRandom;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.models.AttributesCGM;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.attributes.scenario.SourceTestAttributesBuilder;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.util.StateJsonConverter;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;


public class GroupSourceControllerTest extends TestSourceControllerUsingConstantSpawnRate {

	private Integer[] groupSpawn;
	private double[] groupDist;

	public void initSourceControllerFactory() {
		CentroidGroupModel m = new CentroidGroupModel();
		ArrayList<Attributes> attrs = new ArrayList<>();
		attrs.add(generateCGMAttributesJson(groupDist));
		m.initialize(attrs, topography, attributesPedestrian, random);

		if (groupSpawn.length > 0) {
			GroupSizeDeterminatorRandom gsdRnd = Mockito.mock(GroupSizeDeterminatorRandom.class, Mockito.RETURNS_DEEP_STUBS);
			Mockito.when(gsdRnd.nextGroupSize())
					.thenReturn(groupSpawn[0], Arrays.copyOfRange(groupSpawn, 1, groupSpawn.length));
			m.setGroupSizeDeterminator(gsdRnd);
		}

		sourceControllerFactory = new GroupSourceControllerFactory(m);
	}

	@Test
	public void testUpdateEqualStartAndEndTime() {
		SourceTestAttributesBuilder builder = new SourceTestAttributesBuilder()
				.setOneTimeSpawn(0)
				.setSourceDim(5.0, 5.0);
		groupDist = new double[]{0.0, 0.5, 0.5};
		groupSpawn = new Integer[]{2, 2, 3, 3};

		initialize(builder);

		sourceController.update(0);
		sourceController.update(1);
		sourceController.update(2);
		sourceController.update(3);

		assertEquals("wrong pedestrian number", 2, countPedestrians());
	}


	@Test
	public void testUpdateEndTimeLarge() {

		double startTime = 0.0;
		double endTime = 10.0;
		SourceTestAttributesBuilder builder = new SourceTestAttributesBuilder()
				.setStartTime(startTime).setEndTime(endTime)
				.setSpawnIntervalForConstantDistribution(10)
				.setSourceDim(5.0, 5.0);
		groupDist = new double[]{0.0, 0.5, 0.5};
		groupSpawn = new Integer[]{2, 3, 2, 3};
		initialize(builder);

		sourceController.update(startTime);
		// one at the beginning
		assertEquals("wrong pedestrian number.", 2, countPedestrians());

		sourceController.update(endTime);
		// and one at the end
		assertEquals("wrong pedestrian number.", 5, countPedestrians());
	}


	@Test
	public void testUpdateSpawnDelayThreeTimes() {

		double endTime = 10.0;
		SourceTestAttributesBuilder builder = new SourceTestAttributesBuilder()
				.setStartTime(0).setEndTime(endTime)
				.setSpawnIntervalForConstantDistribution(5)
				.setSourceDim(5.0, 5.0);
		groupDist = new double[]{0.0, 0.5, 0.5};
		groupSpawn = new Integer[]{2, 3, 2, 3};
		initialize(builder);

		for (double simTimeInSec = 0; simTimeInSec < endTime * 2; simTimeInSec += 1.0) {
			sourceController.update(simTimeInSec);
		}

		assertEquals("wrong pedestrian number.", 7, countPedestrians());
	}


	@Test
	public void testUpdateSmallSpawnDelay() {

		double endTime = 1.0;
		SourceTestAttributesBuilder builder = new SourceTestAttributesBuilder()
				.setStartTime(0).setEndTime(endTime)
				.setSpawnIntervalForConstantDistribution(0.1)
				.setSourceDim(5.0, 5.0);
		groupDist = new double[]{0.0, 0.5, 0.5};
		groupSpawn = new Integer[]{2, 3, 2, 3, 2, 2, 3, 3, 3, 2, 2, 3, 3, 3, 3};
		initialize(builder);

		for (double simTimeInSec = 0; simTimeInSec < endTime * 2; simTimeInSec += 1.0) {
			sourceController.update(simTimeInSec);
		}

		// sum(2, 3, 2, 3, 2, 2, 3, 3, 3, 2, 2) = 24
		assertEquals("wrong pedestrian number.", 27, countPedestrians());
	}

	/**
	 * Test method for {@link org.vadere.simulator.control.SourceController#update(double)}.
	 */
	@Test
	public void testUpdateUseFreeSpaceOnly() {

		double d = new AttributesAgent().getRadius() * 2;
		SourceTestAttributesBuilder builder = new SourceTestAttributesBuilder()
				.setOneTimeSpawn(0)
				.setSpawnNumber(100)
				.setUseFreeSpaceOnly(true)
				.setSourceDim(2 * d + 0.1, 4 * d + 0.1);
		groupDist = new double[]{0.0, 0.0, 1}; // only groups of 3
		groupSpawn = new Integer[]{};
		initialize(builder);

		for (double simTimeInSec = 0; simTimeInSec < 1000; simTimeInSec += 1.0) {
			sourceController.update(simTimeInSec);
		}

		// The source has space for tow groups of three (with Random seed of 0)
		// ist could also be the case that only one group can be placed.
		assertEquals("wrong pedestrian number.", 6, countPedestrians());

		// now, move the peds away after creating them
		for (double simTimeInSec = 1000; simTimeInSec < 2000; simTimeInSec += 1.0) {
			sourceController.update(simTimeInSec);

			VPoint positionFarAway = new VPoint(1000, 1000);
			for (Pedestrian pedestrian : topography.getElements(Pedestrian.class)) {
				pedestrian.setPosition(positionFarAway);
			}
		}

		// now, all pedestrians should have been created
		assertEquals("wrong pedestrian number.", 300, countPedestrians());
	}


	private AttributesCGM generateCGMAttributesJson(double[] groups) {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		for (int i = 0; i < groups.length - 1; i++) {
			sb.append(groups[i]).append(", ");
		}
		sb.append(groups[groups.length - 1]).append(" ]");

		String json = "{\n" +
				"      \"groupMemberRepulsionFactor\" : 0.01,\n" +
				"      \"leaderAttractionFactor\" : 0.003,\n" +
				"      \"groupSizeDistribution\" : " + sb.toString() + "\n" +
				"    }";
		return StateJsonConverter.deserializeObjectFromJson(json, AttributesCGM.class);
	}


	private CentroidGroupModel mockGroupSizeSpawn(CentroidGroupModel m, Integer first, Integer... groups) {
		GroupSizeDeterminatorRandom gsdRnd = Mockito.mock(GroupSizeDeterminatorRandom.class, Mockito.RETURNS_DEEP_STUBS);
		Mockito.when(gsdRnd.nextGroupSize()).thenReturn(first, groups);
		m.setGroupSizeDeterminator(gsdRnd);
		return m;
	}


}