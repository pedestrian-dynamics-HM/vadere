package org.vadere.simulator.control;

import org.apache.commons.math3.distribution.ConstantRealDistribution;
import org.apache.commons.math3.random.RandomGenerator;
import org.junit.Test;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.attributes.scenario.AttributesSource;
import org.vadere.state.attributes.scenario.SourceTestAttributesBuilder;
import org.vadere.state.attributes.spawner.AttributesSpawner;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;

import java.io.IOException;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestSourceControllerUsingDistributions extends TestSourceControllerUsingConstantSpawnRate {

	@Test
	public void testStartTime() {
		SourceTestAttributesBuilder builder = new SourceTestAttributesBuilder();

		try{
			initialize(builder);
		}catch (IOException e){
			throw new RuntimeException(e.getMessage());
		}

		first().sourceController.update(0);
		pedestrianCountEquals(0);
		first().sourceController.update(0.9);
		pedestrianCountEquals(0);

		first().sourceController.update(1);
		pedestrianCountEquals(1);
	}

	@Test
	public void testEndTime() throws IOException {
		SourceTestAttributesBuilder builder = new SourceTestAttributesBuilder();
		initialize(builder);

		first().sourceController.update(1);
		pedestrianCountEquals(1);
		first().sourceController.update(2);
		pedestrianCountEquals(2);

		first().sourceController.update(3); // end time reached -> no effect
		pedestrianCountEquals(2);
	}

	@Test
	public void testOneTimeSpawn() throws IOException {
		SourceTestAttributesBuilder builder = new SourceTestAttributesBuilder()
				.setOneTimeSpawn(1);
		initialize(builder);

		first().sourceController.update(0);
		pedestrianCountEquals(0);
		first().sourceController.update(1);
		pedestrianCountEquals(1);
		first().sourceController.update(2);
		pedestrianCountEquals(1);
	}

	@Test
	public void testSpawnNumber() throws IOException{
		SourceTestAttributesBuilder builder = new SourceTestAttributesBuilder()
				.setSpawnNumber(10);
		initialize(builder);

		first().sourceController.update(1);
		pedestrianCountEquals(10);
		first().sourceController.update(2);
		pedestrianCountEquals(20);
	}

	@Test
	public void testSpawnRateGreaterThanUpdateRate() throws IOException {
		SourceTestAttributesBuilder builder = new SourceTestAttributesBuilder()
				.setStartTime(0).setEndTime(1)
				.setDistributionParams(0.3);
		initialize(builder);

		// per update only one "spawn action" is performed.
		// if the spawn rate is higher than the update time increment, spawns will get lost.
		first().sourceController.update(0);
		pedestrianCountEquals(1);
		first().sourceController.update(1);
		pedestrianCountEquals(4);
	}

	@Test
	public void testUseFreeSpaceOnly() throws IOException {
		// expected: not stop spawning before all pedestrians are created (even after end time)
		double startTime = 0;
		double endTime = 1;
		int spawnNumber = 100;
		AttributesAgent attributesAgent = new AttributesAgent();
		SourceTestAttributesBuilder builder = new SourceTestAttributesBuilder()
				.setStartTime(startTime).setEndTime(endTime)
				.setSpawnNumber(100)
				.setUseFreeSpaceOnly(true)
				.setSourceDim(new VRectangle(0, 0, attributesAgent.getRadius()*2 + 0.05, attributesAgent.getRadius()*2 + 0.05));
		initialize(builder);

		doUpdates(0, 100, startTime, endTime + 1);

		// despite many updates, only one ped can be spawned
		assertEquals(1, countPedestrians(0));

		doUpdatesBeamingPedsAway(0, 1000);

		// now, all pedestrians should have been created
		assertEquals(2 * spawnNumber, countPedestrians(0));
	}

	@Test
	public void testUseFreeSpaceOnlyWithSingleSpawnEvent() throws IOException {
		// works also with sources that have startTime == endTime?
		// expected: not stop spawning before all pedestrians are created (even after end time)
		double startTime = 1;
		int spawnNumber = 100;
		AttributesAgent attributesAgent = new AttributesAgent();
		SourceTestAttributesBuilder builder = new SourceTestAttributesBuilder()
				.setOneTimeSpawn(startTime)
				.setSpawnNumber(100)
				.setUseFreeSpaceOnly(true)
				.setSourceDim(new VRectangle(0, 0, attributesAgent.getRadius()*2 + 0.05, attributesAgent.getRadius()*2 + 0.05));
		initialize(builder);

		doUpdates(0, 100, 0, startTime + 1);

		// despite many updates, only one ped can be spawned
		assertEquals(1, countPedestrians(0));

		// now, move the peds away after updates
		doUpdatesBeamingPedsAway(0, 1000);

		// now, all pedestrians should have been created
		assertEquals(spawnNumber, countPedestrians(0));

	}

	@Test
	public void testMaxSpawnNumberTotalSetTo0() throws IOException {
		SourceTestAttributesBuilder builder = new SourceTestAttributesBuilder()
				.setMaxSpawnNumberTotal(0); // <-- max 0 -> spawn no peds at all
		initialize(builder);

		first().sourceController.update(1);
		first().sourceController.update(2);
		first().sourceController.update(3);

		assertEquals(0, countPedestrians(0));
	}

	@Test
	public void testMaxSpawnNumberTotalNotSet() throws IOException {
		SourceTestAttributesBuilder builder = new SourceTestAttributesBuilder()
				.setMaxSpawnNumberTotal(AttributesSpawner.NO_MAX_SPAWN_NUMBER_TOTAL); // <-- maximum not set
		initialize(builder);

		first().sourceController.update(1);
		first().sourceController.update(2);
		first().sourceController.update(3);

		assertEquals(2, countPedestrians(0));
	}

	@Test
	public void testMaxSpawnNumberTotalWithSmallEndTime() throws IOException {
		SourceTestAttributesBuilder builder = new SourceTestAttributesBuilder()
				.setMaxSpawnNumberTotal(4); // <-- not exhausted
		initialize(builder);

		first().sourceController.update(1);
		first().sourceController.update(2);
		first().sourceController.update(3);

		assertEquals(2, countPedestrians(0));
	}

	@Test
	public void testMaxSpawnNumberTotalWithLargeEndTime() throws IOException {
		double endTime = 100;
		int maxSpawnNumberTotal = 4;
		SourceTestAttributesBuilder builder = new SourceTestAttributesBuilder()
				.setEndTime(endTime)
				.setMaxSpawnNumberTotal(maxSpawnNumberTotal); // <-- exhausted!
		initialize(builder);

		doUpdates(0, 50, 0, 200);

		assertEquals(maxSpawnNumberTotal, countPedestrians(0));
	}

	@Test
	public void testMaxSpawnNumberTotalWithLargeEndTimeAndSpawnNumberGreater1() throws IOException {
		int maxSpawnNumberTotal = 4; // <-- exhausted!
		SourceTestAttributesBuilder builder = new SourceTestAttributesBuilder()
				.setEndTime(100)
				.setSpawnNumber(5)
				.setMaxSpawnNumberTotal(maxSpawnNumberTotal);
		initialize(builder);

		doUpdates(0, 50, 0, 200);

		assertEquals(maxSpawnNumberTotal, countPedestrians(0));
	}

	/**
	 * Test if in a polygon shape (diamond in this case) the pedestrians are placed within the
	 * source and not within its bound and that no overlap occurs.
	 */
	@Test
	public void testPolygonShapedSourceNoRandom() throws IOException {
		int maxSpawnNumberTotal = 5;
		SourceTestAttributesBuilder builder = new SourceTestAttributesBuilder()
				.setEndTime(100)
				.setSpawnNumber(5)
				.setDiamondShapeSource()
				.setUseFreeSpaceOnly(true)
				.setSpawnAtRandomPositions(false)
				.setMaxSpawnNumberTotal(5);
		initialize(builder);

		VShape sourceShape = builder.getSourceShape();

		doUpdates(0, 10, 0, 10);
		assertEquals(maxSpawnNumberTotal, countPedestrians(0));

		SourceTestData testData = sourceTestData.get(0);
		testData.topography.getPedestrianDynamicElements()
				.getElements().forEach(p -> assertTrue(sourceShape.containsShape(p.getShape())));

		Collection<Pedestrian> peds = testData.topography.getPedestrianDynamicElements().getElements();
		for (Pedestrian p : peds) {
			assertTrue(peds.stream()
					.filter(ped -> !ped.equals(p))
					.map(Pedestrian::getShape)
					.noneMatch(s -> s.intersects(p.getShape())));
		}
	}

	/**
	 * Test if in a polygon shape (diamond in this case) the pedestrians are placed within the
	 * source and not within its bound and that no overlap occurs.
	 */
	@Test
	public void testPolygonShapedSourceWithRandom() throws IOException {
		int maxSpawnNumberTotal = 5;
		SourceTestAttributesBuilder builder = new SourceTestAttributesBuilder()
				.setEndTime(100)
				.setSpawnNumber(5)
				.setDiamondShapeSource()
				.setUseFreeSpaceOnly(true)
				.setSpawnAtRandomPositions(true)
				.setRandomSeed(13)
				.setMaxSpawnNumberTotal(5);
		initialize(builder);

		VShape sourceShape = builder.getSourceShape();

		doUpdates(0, 10, 0, 10);
		assertEquals(maxSpawnNumberTotal, countPedestrians(0));

		SourceTestData testData = sourceTestData.get(0);
		testData.topography.getPedestrianDynamicElements()
				.getElements().forEach(p -> assertTrue(sourceShape.containsShape(p.getShape())));

		Collection<Pedestrian> peds = testData.topography.getPedestrianDynamicElements().getElements();
		for (Pedestrian p : peds) {
			assertTrue(peds.stream()
					.filter(ped -> !ped.equals(p))
					.map(Pedestrian::getShape)
					.noneMatch(s -> s.intersects(p.getShape())));
		}
	}
}
