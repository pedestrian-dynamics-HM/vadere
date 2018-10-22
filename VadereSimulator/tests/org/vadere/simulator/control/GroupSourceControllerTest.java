package org.vadere.simulator.control;

import org.junit.Test;
import org.mockito.Mockito;
import org.vadere.simulator.control.factory.GroupSourceControllerFactory;
import org.vadere.simulator.control.factory.SourceControllerFactory;
import org.vadere.simulator.models.groups.CentroidGroupFactory;
import org.vadere.simulator.models.groups.CentroidGroupModel;
import org.vadere.simulator.models.groups.GroupModel;
import org.vadere.simulator.models.groups.GroupSizeDeterminatorRandom;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.models.AttributesCGM;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.attributes.scenario.AttributesSource;
import org.vadere.state.attributes.scenario.SourceTestAttributesBuilder;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.geometry.shapes.VPoint;
import org.vadere.geometry.shapes.VRectangle;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;


public class GroupSourceControllerTest extends TestSourceControllerUsingConstantSpawnRate {

	private GroupModel m;

	public SourceControllerFactory getSourceControllerFactory(SourceTestData d) {
		m = new CentroidGroupModel();
		ArrayList<Attributes> attrs = new ArrayList<>();
		attrs.add(new AttributesCGM());
		m.initialize(attrs, d.topography, d.attributesPedestrian, d.random);

		return new GroupSourceControllerFactory(m);
	}

	@Override
	public void initialize(SourceTestAttributesBuilder builder) {
		super.initialize(builder);
		SourceTestData d = sourceTestData.get(sourceTestData.size() - 1);

		if (builder.getGroupSizeDistributionMock().length > 0) {
			Integer[] groupSizeDistributionMock = builder.getGroupSizeDistributionMock();
			GroupSizeDeterminatorRandom gsdRnd =
					Mockito.mock(GroupSizeDeterminatorRandom.class, Mockito.RETURNS_DEEP_STUBS);
			Mockito.when(gsdRnd.nextGroupSize())
					.thenReturn(groupSizeDistributionMock[0],
							Arrays.copyOfRange(groupSizeDistributionMock,
									1, groupSizeDistributionMock.length));
			CentroidGroupFactory cgf = (CentroidGroupFactory) m.getGroupFactory(d.source.getId());
			cgf.setGroupSizeDeterminator(gsdRnd);
		}

	}

	@Test
	public void testUpdateEqualStartAndEndTime() {
		SourceTestAttributesBuilder builder = new SourceTestAttributesBuilder()
				.setOneTimeSpawn(0)
				.setSourceDim(5.0, 5.0)
				.setGroupSizeDistribution(0.0, 0.5, 0.5)
				.setGroupSizeDistributionMock(2, 2, 3, 3);

		initialize(builder);

		first().sourceController.update(0);
		first().sourceController.update(1);
		first().sourceController.update(2);
		first().sourceController.update(3);

		assertEquals("wrong pedestrian number", 2, countPedestrians(0));
	}


	@Test
	public void testUpdateEndTimeLarge() {

		double startTime = 0.0;
		double endTime = 10.0;
		SourceTestAttributesBuilder builder = new SourceTestAttributesBuilder()
				.setStartTime(startTime).setEndTime(endTime)
				.setSpawnIntervalForConstantDistribution(10)
				.setSourceDim(5.0, 5.0)
				.setGroupSizeDistribution(0.0, 0.5, 0.5)
				.setGroupSizeDistributionMock(2, 3, 2, 3);
		initialize(builder);

		first().sourceController.update(startTime);
		// one at the beginning
		assertEquals("wrong pedestrian number.", 2, countPedestrians(0));

		first().sourceController.update(endTime);
		// and one at the end
		assertEquals("wrong pedestrian number.", 5, countPedestrians(0));
	}


	@Test
	public void testUpdateSpawnDelayThreeTimes() {

		double endTime = 10.0;
		SourceTestAttributesBuilder builder = new SourceTestAttributesBuilder()
				.setStartTime(0).setEndTime(endTime)
				.setSpawnIntervalForConstantDistribution(5)
				.setSourceDim(5.0, 5.0)
				.setGroupSizeDistribution(0.0, 0.5, 0.5)
				.setGroupSizeDistributionMock(2, 3, 2, 3);
		initialize(builder);

		for (double simTimeInSec = 0; simTimeInSec < endTime * 2; simTimeInSec += 1.0) {
			first().sourceController.update(simTimeInSec);
		}

		assertEquals("wrong pedestrian number.", 7, countPedestrians(0));
	}


	@Test
	public void testUpdateSmallSpawnDelay() {

		double endTime = 1.0;
		SourceTestAttributesBuilder builder = new SourceTestAttributesBuilder()
				.setStartTime(0).setEndTime(endTime)
				.setSpawnIntervalForConstantDistribution(0.1)
				.setSourceDim(5.0, 5.0)
				.setGroupSizeDistribution(0.0, 0.5, 0.5)
				.setGroupSizeDistributionMock(2, 3, 2, 3, 2, 2, 3, 3, 3, 2, 2, 3, 3, 3, 3);
		initialize(builder);

		for (double simTimeInSec = 0; simTimeInSec < endTime * 2; simTimeInSec += 1.0) {
			first().sourceController.update(simTimeInSec);
		}

		// sum(2, 3, 2, 3, 2, 2, 3, 3, 3, 2, 2) = 24
		assertEquals("wrong pedestrian number.", 27, countPedestrians(0));
	}

	/**
	 * Test method for {@link org.vadere.simulator.control.SourceController#update(double)}.
	 */
	@Test
	public void testUpdateUseFreeSpaceOnly() {

		double d = new AttributesAgent().getRadius() * 2  +  SourceController.SPAWN_BUFFER_SIZE;
		SourceTestAttributesBuilder builder = new SourceTestAttributesBuilder()
				.setOneTimeSpawn(0)
				.setSpawnNumber(100)
				.setUseFreeSpaceOnly(true)
				.setSourceDim(2 * d + 0.1, 4 * d + 0.1)
				.setGroupSizeDistribution(0.0, 0.0, 1);
		initialize(builder);

		for (double simTimeInSec = 0; simTimeInSec < 1000; simTimeInSec += 1.0) {
			first().sourceController.update(simTimeInSec);
		}

		// The source has space for tow groups of three (with Random seed of 0)
		// ist could also be the case that only one group can be placed.
		assertEquals("wrong pedestrian number.", 6, countPedestrians(0));

		// now, move the peds away after creating them
		for (double simTimeInSec = 1000; simTimeInSec < 2000; simTimeInSec += 1.0) {
			first().sourceController.update(simTimeInSec);

			VPoint positionFarAway = new VPoint(1000, 1000);
			for (Pedestrian pedestrian : first().topography.getElements(Pedestrian.class)) {
				pedestrian.setPosition(positionFarAway);
			}
		}

		// now, all pedestrians should have been created
		assertEquals("wrong pedestrian number.", 300, countPedestrians(0));
	}

	// WithDistribution

	@Test
	public void testStartTime() {
		SourceTestAttributesBuilder builder = new SourceTestAttributesBuilder()
				.setDistributionClass(TestSourceControllerUsingDistributions.ConstantTestDistribution.class)
				.setSourceDim(5.0, 5.0)
				.setGroupSizeDistribution(0.0, 0.0, 1)
				.setGroupSizeDistributionMock(3, 3, 3, 3);
		initialize(builder);

		first().sourceController.update(0);
		pedestrianCountEquals(0);
		first().sourceController.update(0.9);
		pedestrianCountEquals(0);

		first().sourceController.update(1);
		pedestrianCountEquals(3);
	}

	@Test
	public void testEndTime() {
		SourceTestAttributesBuilder builder = new SourceTestAttributesBuilder()
				.setDistributionClass(TestSourceControllerUsingDistributions.ConstantTestDistribution.class)
				.setEndTime(2)
				.setSourceDim(5.0, 5.0)
				.setGroupSizeDistribution(0.0, 0.0, 1) // only groups of 3
				.setGroupSizeDistributionMock(3, 3, 3, 3);
		initialize(builder);

		first().sourceController.update(1);
		pedestrianCountEquals(3);
		first().sourceController.update(2);
		pedestrianCountEquals(6);

		first().sourceController.update(3); // end time reached -> no effect
		pedestrianCountEquals(6);
	}

	@Test
	public void testOneTimeSpawn() {
		SourceTestAttributesBuilder builder = new SourceTestAttributesBuilder()
				.setOneTimeSpawn(1)
				.setSourceDim(5.0, 5.0)
				.setGroupSizeDistribution(0.0, 0.0, 1) // only groups of 3
				.setGroupSizeDistributionMock(3, 3, 3, 3);
		initialize(builder);

		first().sourceController.update(0);
		pedestrianCountEquals(0);
		first().sourceController.update(1);
		pedestrianCountEquals(3);
		first().sourceController.update(2);
		pedestrianCountEquals(3);
	}

	@Test(expected = RuntimeException.class)
	public void testSpawnNumber() {
		double d = new AttributesAgent().getRadius() * 2 + SourceController.SPAWN_BUFFER_SIZE;
		SourceTestAttributesBuilder builder = new SourceTestAttributesBuilder()
				.setSpawnNumber(10)
				.setSourceDim(12 * d, 12 * d ) // create source with 12x12 spots
				.setGroupSizeDistribution(0.0, 0.0, 0.0, 1); // only groups of 4
		initialize(builder);

		first().sourceController.update(1);
		pedestrianCountEquals(10 * 4);
		first().sourceController.update(2); 	// use random positioning thus not
															// not optimal and thus not enough
															// space for all
		fail("should not be reached. Exception expected");

	}

	@Test
	public void testSpawnRateGreaterThanUpdateRate() {
		SourceTestAttributesBuilder builder = new SourceTestAttributesBuilder()
				.setStartTime(0).setEndTime(1)
				.setSpawnIntervalForConstantDistribution(0.3)
				.setSourceDim(5.0, 5.0)
				.setGroupSizeDistribution(0.0, 0.0, 0.25, 0.75)
				.setGroupSizeDistributionMock(4, 3, 4, 4);
		initialize(builder);

		// per update only one "spawn action" is performed.
		// if the spawn rate is higher than the update time increment, spawns will get lost.
		first().sourceController.update(0);
		pedestrianCountEquals(4);
		first().sourceController.update(1);
		pedestrianCountEquals(15);
	}

	@Test
	public void testUseFreeSpaceOnly() {
		// expected: not stop spawning before all pedestrians are created (even after end time)
		double d = new AttributesAgent().getRadius() * 2 + SourceController.SPAWN_BUFFER_SIZE;
		double startTime = 0;
		double endTime = 1;
		int spawnNumber = 100;
		SourceTestAttributesBuilder builder = new SourceTestAttributesBuilder()
				.setStartTime(startTime).setEndTime(endTime)
				.setSpawnNumber(100)
				.setUseFreeSpaceOnly(true)
				.setSourceDim(2 * d + 0.1, 4 * d + 0.1)
				.setGroupSizeDistribution(0.0, 0.0, 0.0, 1);

		initialize(builder);

		doUpdates(0, 100, startTime, endTime + 1);

		// despite many updates, only tow groups of four can be spawned
		assertEquals(8, countPedestrians(0));

		doUpdatesBeamingPedsAway(0, 1000);

		// now, all pedestrians should have been created
		assertEquals(2 * spawnNumber * 4, countPedestrians(0));
	}

	@Test
	public void testMaxSpawnNumberTotalSetTo0() {
		SourceTestAttributesBuilder builder = new SourceTestAttributesBuilder()
				.setMaxSpawnNumberTotal(0) // <-- max 0 -> spawn no peds at all
				.setSourceDim(5.0, 5.0)
				.setGroupSizeDistribution(0.0, 0.0, 0.25, 0.75)
				.setGroupSizeDistributionMock(4, 3, 4, 4);
		initialize(builder);

		first().sourceController.update(1);
		first().sourceController.update(2);
		first().sourceController.update(3);

		assertEquals(0, countPedestrians(0));
	}

	@Test
	public void testMaxSpawnNumberTotalNotSet() {
		SourceTestAttributesBuilder builder = new SourceTestAttributesBuilder()
				.setMaxSpawnNumberTotal(AttributesSource.NO_MAX_SPAWN_NUMBER_TOTAL) // <-- maximum not set
				.setEndTime(2)
				.setSourceDim(5.0, 5.0)
				.setGroupSizeDistribution(0.0, 0.0, 0.25, 0.75)
				.setGroupSizeDistributionMock(4, 3, 4, 4);
		initialize(builder);

		first().sourceController.update(1);
		first().sourceController.update(2);
		first().sourceController.update(3);

		assertEquals(7, countPedestrians(0));
	}

	@Test
	public void testMaxSpawnNumberTotalWithSmallEndTime() {
		SourceTestAttributesBuilder builder = new SourceTestAttributesBuilder()
				.setMaxSpawnNumberTotal(4) // <-- not exhausted
				.setEndTime(2)
				.setSourceDim(5.0, 5.0)
				.setGroupSizeDistribution(0.0, 0.0, 0.25, 0.75)
				.setGroupSizeDistributionMock(4, 3, 4, 4);
		initialize(builder);

		first().sourceController.update(1);
		first().sourceController.update(2);
		first().sourceController.update(3);

		assertEquals(7, countPedestrians(0));
	}

	@Test
	public void testMaxSpawnNumberTotalWithLargeEndTime() {
		double endTime = 100;
		int maxSpawnNumberTotal = 4;
		SourceTestAttributesBuilder builder = new SourceTestAttributesBuilder()
				.setEndTime(endTime)
				.setMaxSpawnNumberTotal(maxSpawnNumberTotal) // <-- exhausted!
				.setSourceDim(5.0, 5.0)
				.setGroupSizeDistribution(0.0, 0.0, 0.25, 0.75)
				.setGroupSizeDistributionMock(4, 3, 4, 4);
		initialize(builder);

		doUpdates(0, 50, 0, 200);

		assertEquals(15, countPedestrians(0));
	}



	@Test
	public void testMaxSpawnNumberTotalWithLargeEndTimeAndSpawnNumberGreater1() {
		int maxSpawnNumberTotal = 4; // <-- exhausted!
		SourceTestAttributesBuilder builder = new SourceTestAttributesBuilder()
				.setEndTime(100)
				.setSpawnNumber(5)
				.setMaxSpawnNumberTotal(maxSpawnNumberTotal)
				.setSourceDim(5.0, 5.0)
				.setGroupSizeDistribution(0.0, 0.0, 0.25, 0.75)
				.setGroupSizeDistributionMock(4, 3, 4, 4);
		initialize(builder);

		doUpdates(0, 50, 0, 200);

		assertEquals(15, countPedestrians(0));
	}

	@Test
	public void multipleSources() {
		SourceTestAttributesBuilder builder1 = new SourceTestAttributesBuilder()
				.setDistributionClass(TestSourceControllerUsingDistributions.ConstantTestDistribution.class)
				.setGroupSizeDistribution(0.0, 0.0, 0.25, 0.75)
				.setSourceDim(new VRectangle(0, 0, 3, 4))
				.setEndTime(4)
				.setMaxSpawnNumberTotal(4)
				.setGroupSizeDistributionMock(3, 4, 4, 4, 3);
		SourceTestAttributesBuilder builder2 = new SourceTestAttributesBuilder()
				.setDistributionClass(TestSourceControllerUsingDistributions.ConstantTestDistribution.class)
				.setGroupSizeDistribution(0.0, 1.0)
				.setSourceDim(new VRectangle(20, 20, 3, 2))
				.setEndTime(6)
				.setMaxSpawnNumberTotal(6)
				.setGroupSizeDistributionMock(2, 2, 2, 2, 2, 2);

		initialize(builder1);
		initialize(builder2);

		first().sourceController.update(1);
		assertEquals(3, countPedestrians(0));

		second().sourceController.update(1);
		assertEquals(2, countPedestrians(1));

		first().sourceController.update(2);
		first().sourceController.update(3);
		assertEquals(3 + 4 + 4, countPedestrians(0));

		second().sourceController.update(2);
		second().sourceController.update(3);
		assertEquals(2 + 2 + 2, countPedestrians(1));

	}

}