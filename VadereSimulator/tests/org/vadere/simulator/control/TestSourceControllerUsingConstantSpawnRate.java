package org.vadere.simulator.control;

import org.junit.Test;
import org.vadere.simulator.control.scenarioelements.SourceController;
import org.vadere.simulator.control.scenarioelements.listener.ControllerEventListener;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.attributes.scenario.SourceTestAttributesBuilder;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class TestSourceControllerUsingConstantSpawnRate extends TestSourceController {




	/**
	 * Test method for {@link SourceController#update(double)}.
	 */
	@Test
	public void testUpdateEqualStartAndEndTime() throws IOException {

		SourceTestAttributesBuilder builder = new SourceTestAttributesBuilder()
				.setOneTimeSpawn(0);
		initialize(builder);

		first().sourceController.update(0);
		first().sourceController.update(1);
		first().sourceController.update(2);

		assertEquals("wrong pedestrian number", 1, countPedestrians(0));
	}

	@Test
	public void testListener() throws IOException {
		SourceTestAttributesBuilder builder = new SourceTestAttributesBuilder()
				.setStartTime(0)
				.setEndTime(10)
				.setSpawnNumber(1)
				.setMaxSpawnNumberTotal(2)
				.setId(33);
		initialize(builder);

		ControllerEventListener<Agent, SourceController> listener = new ControllerEventListener<Agent, SourceController>() {
			@Override
			public Agent notify(SourceController controller, double simTimeInSec, Agent scenarioElement) {
				scenarioElement.setSingleTarget(99, true);
				return null;
			}
		};
		first().sourceController.register(listener);

		// expect first pedestrians target list will be updated
		first().sourceController.update(0);
		Pedestrian p = first().topography.getPedestrianDynamicElements().getElements().toArray(Pedestrian[]::new)[0];
		int target = p.getTargets().getFirst();
		assertEquals("", 99, target);

		// after unregister listener must not change target
		first().sourceController.unregister(listener);
		first().sourceController.update(1);
		int pedCount = first().topography.getPedestrianDynamicElements().getElements().size();
		assertEquals("should be 2", 2, pedCount);
		p = first().topography.getPedestrianDynamicElements().getElements().toArray(Pedestrian[]::new)[1];
		target = p.getTargets().getFirst();
		assertNotEquals(99, target);
	}

	/**
	 * Test method for {@link SourceController#update(double)}.
	 */
	@Test
	public void testUpdateEndTimeLarge() throws IOException {

		double startTime = 0.0;
		double endTime = 10.0;
		SourceTestAttributesBuilder builder = new SourceTestAttributesBuilder()
				.setStartTime(startTime).setEndTime(endTime)
				.setDistributionParams(10);
		initialize(builder);

		first().sourceController.update(startTime);
		// one at the beginning
		assertEquals("wrong pedestrian number.", 1, countPedestrians(0));

		first().sourceController.update(endTime);
		// and one at the end
		assertEquals("wrong pedestrian number.", 2, countPedestrians(0));
	}

	/**
	 * Test method for {@link SourceController#update(double)}.
	 */
	@Test
	public void testUpdateSpawnDelayThreeTimes() throws IOException {

		double endTime = 10.0;
		SourceTestAttributesBuilder builder = new SourceTestAttributesBuilder()
				.setStartTime(0).setEndTime(endTime)
				.setDistributionParams(5);
		initialize(builder);

		for (double simTimeInSec = 0; simTimeInSec < endTime * 2; simTimeInSec += 1.0) {
			first().sourceController.update(simTimeInSec);
		}

		assertEquals("wrong pedestrian number.", 3, countPedestrians(0));
	}

	/**
	 * Test method for {@link SourceController#update(double)}.
	 */
	@Test
	public void testUpdateSmallSpawnDelay() throws IOException {

		double endTime = 1.0;
		SourceTestAttributesBuilder builder = new SourceTestAttributesBuilder()
				.setStartTime(0).setEndTime(endTime)
				.setDistributionParams(0.1);
		initialize(builder);

		for (double simTimeInSec = 0; simTimeInSec < endTime * 2; simTimeInSec += 1.0) {
			first().sourceController.update(simTimeInSec);
		}

		assertEquals("wrong pedestrian number.", 11, countPedestrians(0));
	}

	/**
	 * Test method for {@link SourceController#update(double)}.
	 */
	@Test
	public void testUpdateUseFreeSpaceOnly() throws IOException {

		AttributesAgent attributesAgent = new AttributesAgent();
		SourceTestAttributesBuilder builder = new SourceTestAttributesBuilder()
				.setOneTimeSpawn(0)
				.setSpawnNumber(100)
				.setUseFreeSpaceOnly(true)
				.setSourceDim(new VRectangle(0, 0, attributesAgent.getRadius()*2 + 0.05, attributesAgent.getRadius()*2 + 0.05)); // small source
		initialize(builder);

		for (double simTimeInSec = 0; simTimeInSec < 1000; simTimeInSec += 1.0) {
			first().sourceController.update(simTimeInSec);
		}

		// if the first ped does not move away, there should no more pedestrians
		// be created
		assertEquals("wrong pedestrian number.", 1, countPedestrians(0));

		// now, move the peds away after creating them
		for (double simTimeInSec = 1000; simTimeInSec < 2000; simTimeInSec += 1.0) {
			first().sourceController.update(simTimeInSec);

			VPoint positionFarAway = new VPoint(1000, 1000);
			for (Pedestrian pedestrian : first().topography.getElements(Pedestrian.class)) {
				pedestrian.setPosition(positionFarAway);
			}
		}

		// now, all pedestrians should have been created
		assertEquals("wrong pedestrian number.", 100, countPedestrians(0));
	}



	protected void doUpdates(int source, int number, double startTime, double endTimeExclusive) {
		double timeStep = (endTimeExclusive - startTime) / number;
		for (double t = startTime; t < endTimeExclusive + 1; t += timeStep) {
			sourceTestData.get(source).sourceController.update(t);
		}
	}

	protected void doUpdatesBeamingPedsAway(int source, int number) {
		double start = 10;
		for (double t = start; t < start + number; t += 1) {
			sourceTestData.get(source).sourceController.update(t);
			beamPedsAway(source);
		}
	}

	protected void beamPedsAway(int source) {
		final VPoint positionFarAway = new VPoint(1000, 1000);
		for (Pedestrian pedestrian : sourceTestData.get(source).topography.getElements(Pedestrian.class)) {
			pedestrian.setPosition(positionFarAway);
		}
	}

}
