package org.vadere.simulator.control;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.mockito.Mockito;
import org.vadere.simulator.control.factory.GroupSourceControllerFactory;
import org.vadere.simulator.control.factory.SourceControllerFactory;
import org.vadere.simulator.control.scenarioelements.GroupSourceController;
import org.vadere.simulator.control.scenarioelements.SourceController;
import org.vadere.simulator.models.DynamicElementFactory;
import org.vadere.simulator.models.groups.GroupModel;
import org.vadere.simulator.models.groups.GroupSizeDeterminator;
import org.vadere.simulator.models.groups.GroupSizeDeterminatorRandom;
import org.vadere.simulator.models.groups.cgm.CentroidGroupModel;
import org.vadere.simulator.projects.Domain;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.models.AttributesCGM;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.attributes.scenario.AttributesDynamicElement;
import org.vadere.state.attributes.scenario.AttributesSource;
import org.vadere.state.attributes.scenario.SourceTestAttributesBuilder;
import org.vadere.state.attributes.spawner.AttributesSpawner;
import org.vadere.state.scenario.*;
import org.vadere.state.util.StateJsonConverter;
import org.vadere.util.geometry.LinkedCellsGrid;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class GroupSourceControllerTest extends TestSourceControllerUsingConstantSpawnRate {

	private GroupModel m;


	private SourceControllerFactory getTestGroupFactory(GroupModel groupModel, GroupSizeDeterminator gsd) {
		return new SourceControllerFactory() {
			@Override
			public SourceController create(Topography scenario, Source source,
										   DynamicElementFactory dynamicElementFactory,
										   AttributesDynamicElement attributesDynamicElement,
										   Random random) {
				return new GroupSourceController(scenario, source, dynamicElementFactory,
						attributesDynamicElement, random, groupModel, gsd);
			}
		};
	}

	public SourceControllerFactory getSourceControllerFactory(SourceTestData d, GroupSizeDeterminatorRandom gsd) {
		m = new CentroidGroupModel();
		ArrayList<Attributes> attrs = new ArrayList<>();
		attrs.add(new AttributesCGM());
		m.initialize(attrs, new Domain(d.topography), d.attributesPedestrian, d.random);

		return getTestGroupFactory(m, gsd);
	}

	public SourceControllerFactory getSourceControllerFactory(SourceTestData d) {
		m = new CentroidGroupModel();
		ArrayList<Attributes> attrs = new ArrayList<>();
		attrs.add(new AttributesCGM());
		m.initialize(attrs, new Domain(d.topography), d.attributesPedestrian, d.random);

		return new GroupSourceControllerFactory(m);
	}


	@Override
	public void initialize(SourceTestAttributesBuilder builder) {
		SourceTestData d = new SourceTestData();

		try{
			d.attributesSource = builder.getResult();
		} catch(IOException e){
			throw new RuntimeException(e.getMessage());
		}

		d.attributesPedestrian = new AttributesAgent();

		d.random = new Random(builder.getRandomSeed());

		d.source = new Source(d.attributesSource);
		d.pedestrianFactory = new TestDynamicElementFactory(d) {
			private int pedestrianIdCounter = 0;

			@Override
			public <T extends DynamicElement> DynamicElement createElement(VPoint position, int id, Class<T> type) {

				return createElement(position, id, d.attributesPedestrian, type);
			}

			@Override
			public <T extends DynamicElement> DynamicElement createElement(VPoint position, int id, Attributes attr, Class<T> type) {

				AttributesAgent aAttr = (AttributesAgent) attr;

				AttributesAgent att = new AttributesAgent(
						aAttr, registerDynamicElementId(null, id));
				Pedestrian ped = new Pedestrian(att, d.random);
				ped.setPosition(position);
				return ped;
			}

			@Override
			public int registerDynamicElementId(Topography topography, int id) {
				return id > 0 ? id : ++pedestrianIdCounter;
			}

			@Override
			public int getNewDynamicElementId(Topography topography) {
				return registerDynamicElementId(topography, AttributesAgent.ID_NOT_SET);
			}

			@Override
			public VShape getDynamicElementRequiredPlace(@NotNull VPoint position) {
				return createElement(position, AttributesAgent.ID_NOT_SET, Pedestrian.class).getShape();
			}


		};


		if (builder.getGroupSizeDistributionMock().length > 0) {
			Integer[] groupSizeDistributionMock = builder.getGroupSizeDistributionMock();
			GroupSizeDeterminatorRandom gsdRnd =
					Mockito.mock(GroupSizeDeterminatorRandom.class, Mockito.RETURNS_DEEP_STUBS);
			Mockito.when(gsdRnd.nextGroupSize())
					.thenReturn(groupSizeDistributionMock[0],
							Arrays.copyOfRange(groupSizeDistributionMock,
									1, groupSizeDistributionMock.length));
			d.sourceControllerFactory = getSourceControllerFactory(d, gsdRnd);
		} else {
			d.sourceControllerFactory = getSourceControllerFactory(d);
		}


		d.sourceController = d.sourceControllerFactory.create(d.topography, d.source,
				d.pedestrianFactory, d.attributesPedestrian, d.random);

		sourceTestData.add(d);


	}

	@Test
	public void testUpdateEqualStartAndEndTime() {
		SourceTestAttributesBuilder builder = new SourceTestAttributesBuilder()
				.setOneTimeSpawn(0)
				.setUseFreeSpaceOnly(true)
				.setSourceDim(15.0, 15.0)
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
				.setDistributionParams(10)
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
				.setDistributionParams(5)
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
				.setDistributionParams(0.1)
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
	 * Test method for {@link SourceController#update(double)}.
	 */
	@Test
	public void testUpdateUseFreeSpaceOnly() {

		double d = new AttributesAgent().getRadius() * 2;
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

		// now, move the pedestrian away after creating them
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

	@Test
	public void testSpawnRateGreaterThanUpdateRate() {
		SourceTestAttributesBuilder builder = new SourceTestAttributesBuilder()
				.setStartTime(0).setEndTime(1)
				.setDistributionParams(0.3)
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
		double d = new AttributesAgent().getRadius() * 2;
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
				.setMaxSpawnNumberTotal(0) // <-- max 0 -> spawn no pedestrian at all
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
				.setMaxSpawnNumberTotal(AttributesSpawner.NO_MAX_SPAWN_NUMBER_TOTAL) // <-- maximum not set
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
		int maxSpawnNumberTotal = 4;
		SourceTestAttributesBuilder builder = new SourceTestAttributesBuilder()
				.setMaxSpawnNumberTotal(maxSpawnNumberTotal) // <-- not exhausted
				.setEndTime(2)
				.setSourceDim(5.0, 5.0)
				.setGroupSizeDistribution(0.0, 0.0, 0.25, 0.75)
				.setGroupSizeDistributionMock(4, 3, 4, 4);
		initialize(builder);

		first().sourceController.update(1);
		first().sourceController.update(2);
		first().sourceController.update(3);

		assertEquals(maxSpawnNumberTotal, countPedestrians(0));
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
				.setGroupSizeDistributionMock(4, 3, 4, 4)
				.setUseFreeSpaceOnly(false);
		initialize(builder);

		doUpdates(0, 50, 0, 200);

		assertEquals(maxSpawnNumberTotal, countPedestrians(0));
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

		assertEquals(maxSpawnNumberTotal, countPedestrians(0));
	}

	@Test
	public void multipleSources() {
		SourceTestAttributesBuilder builder1 = new SourceTestAttributesBuilder()
				.setGroupSizeDistribution(0.0, 0.0, 0.25, 0.75)
				.setSourceDim(new VRectangle(0, 0, 3, 4))
				.setEndTime(4)
				.setMaxSpawnNumberTotal(6)
				.setUseFreeSpaceOnly(false)
				.setGroupSizeDistributionMock(3, 4, 4, 4, 3);

		SourceTestAttributesBuilder builder2 = new SourceTestAttributesBuilder()
				.setGroupSizeDistribution(0.0, 1.0)
				.setSourceDim(new VRectangle(20, 20, 3, 2))
				.setEndTime(6)
				.setMaxSpawnNumberTotal(20)
				.setUseFreeSpaceOnly(false)
				.setGroupSizeDistributionMock(2, 2, 2, 2, 2, 2);

		initialize(builder1);
		initialize(builder2);

		first().sourceController.update(1);
		assertEquals(3, countPedestrians(0));

		second().sourceController.update(1);
		assertEquals(2, countPedestrians(1));

		first().sourceController.update(2);
		first().sourceController.update(3);
		assertEquals(3 + 4, countPedestrians(0));

		second().sourceController.update(2);
		second().sourceController.update(3);
		assertEquals(2 + 2 + 2, countPedestrians(1));

	}

	private static final String sourceJson = "{\n" +
			"  \"id\" : 2,\n" +
			"  \"shape\" : {\n" +
			"    \"type\" : \"POLYGON\",\n" +
			"    \"points\" : [ {\n" +
			"      \"x\" : 506.39999999999964,\n" +
			"      \"y\" : 509.40000000000146\n" +
			"    }, {\n" +
			"      \"x\" : 502.10000000000036,\n" +
			"      \"y\" : 507.59999999999854\n" +
			"    }, {\n" +
			"      \"x\" : 501.60000000000036,\n" +
			"      \"y\" : 503.2999999999993\n" +
			"    }, {\n" +
			"      \"x\" : 503.89999999999964,\n" +
			"      \"y\" : 501.59999999999854\n" +
			"    }, {\n" +
			"      \"x\" : 508.7999999999993,\n" +
			"      \"y\" : 503.2999999999993\n" +
			"    }, {\n" +
			"      \"x\" : 510.39999999999964,\n" +
			"      \"y\" : 506.7000000000007\n" +
			"    }, {\n" +
			"      \"x\" : 506.2999999999993,\n" +
			"      \"y\" : 508.7000000000007\n" +
			"    } ]\n" +
			"  }\n" +
			"}";


	@Test
	public void testCentroid() throws IOException {
		AttributesSource attributesSource =
				StateJsonConverter.deserializeObjectFromJson(sourceJson, AttributesSource.class);
		Source source = new Source(attributesSource);

		System.out.println(source.getShape().getBounds2D());
		System.out.println(source.getShape().getCentroid());
		System.out.println(source.getShape().getCircumCircle());
		VRectangle bound = new VRectangle(source.getShape().getBounds2D());
		System.out.println(bound.getCentroid());
	}

	@Test
	public void testSource() throws  IOException {
		AttributesSource attributesSource =
				StateJsonConverter.deserializeObjectFromJson(sourceJson, AttributesSource.class);

		VShape a = new VCircle(new VPoint(503.9265351385102, 506.9174145081969), 0.195);
		Pedestrian pedA = new Pedestrian(new AttributesAgent(1), new Random(1));
		pedA.setPosition(((VCircle) a).getCenter());

		VShape b = new VCircle(new VPoint(504.19098333791044, 506.8493305279853), 0.195);
		Pedestrian pedB = new Pedestrian(new AttributesAgent(2), new Random(1));
		pedB.setPosition(((VCircle) b).getCenter());

		assertTrue(a.intersects(b));

		Source source = new Source(attributesSource);
		Topography topography = new Topography();
		topography.addElement(pedA);
		topography.addElement(pedB);

		LinkedCellsGrid grid = topography.getSpatialMap(DynamicElement.class);

		VCircle center = source.getShape().getCircumCircle();
		List<VPoint> inSource = grid.getObjects(center.getCenter(), center.getRadius());
		assertEquals(2, inSource.size());
	}
}