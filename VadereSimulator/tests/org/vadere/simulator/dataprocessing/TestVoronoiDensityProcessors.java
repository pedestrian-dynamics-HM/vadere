//package org.vadere.simulator.dataprocessing;
//
//import org.junit.Before;
//import org.junit.Test;
//import org.mockito.internal.stubbing.answers.DoesNothing;
//import org.mockito.invocation.InvocationOnMock;
//import org.mockito.stubbing.Answer;
//import org.vadere.simulator.control.simulation.SimulationState;
//import org.vadere.simulator.projects.dataprocessing.processor.*;
//import org.vadere.state.attributes.AttributesBuilder;
//import org.vadere.state.attributes.processor.AttributesDensityVoronoiProcessor;
//import org.vadere.state.attributes.scenario.AttributesAgent;
//import org.vadere.state.scenario.Agent;
//import org.vadere.state.scenario.Pedestrian;
//import org.vadere.state.scenario.Topography;
//import org.vadere.utils.data.Row;
//import org.vadere.utils.data.Table;
//import org.vadere.utils.geometry.GeometryUtils;
//import org.vadere.utils.geometry.shapes.VPoint;
//import org.vadere.utils.geometry.shapes.VRectangle;
//
//import java.net.URISyntaxException;
//import java.utils.ArrayList;
//import java.utils.List;
//
//import static junit.framework.Assert.assertEquals;
//import static junit.framework.Assert.assertTrue;
//import static org.mockito.Mockito.mock;
//import static org.mockito.Mockito.when;
//
//
///**
// * This class contains tests that test both Voronoi-Implementation (the JTS-Version and our own
// * version).
// */
//public class TestVoronoiDensityProcessors {
//
//	private DensityVoronoiProcessor densityVoronoiProcessor;
//	private DensityVoronoiGeoProcessor densityVoronoiGeoProcessor;
//	private AttributesDensityVoronoiProcessor attributes;
//	private double densityEpsilon;
//	private int stepCounter;
//	private Topography mockedTopography;
//	private SimulationState state;
//	private VRectangle topographyBounds;
//
//	@Before
//	public void setUp() throws URISyntaxException {
//		densityEpsilon = GeometryUtils.DOUBLE_EPS;
//		topographyBounds = new VRectangle(0, 0, 10, 10);
//		stepCounter = 0;
//		mockedTopography = mock(Topography.class);
//		when(mockedTopography.getBounds()).thenReturn(topographyBounds);
//
//		state = mock(SimulationState.class);
//
//		when(state.getStep()).thenAnswer(new Answer<Integer>() {
//			@Override
//			public Integer answer(InvocationOnMock invocationOnMock) throws Throwable {
//				return count();
//			}
//		});
//		when(state.getTopography()).thenReturn(mockedTopography);
//	}
//
//	/**
//	 * This test fills the whole topography with the maximum amount of pedestrians (hexagon pattern)
//	 * and calculates the density at the center of the topography. This density should be the
//	 * maximum density defined in:
//	 * Seitz, Koester 2012 (Natural discretization of pedestrian movement in continuous space).
//	 */
//	@Test
//	public void testHighDensity() {
//		double pedestrianRadius = new AttributesAgent().getRadius();
//		/*
//		 * Based on http://en.wikipedia.org/wiki/Circle_packing we create a high density
//		 * environment.
//		 */
//		VPoint center = new VPoint(topographyBounds.getCenterX(), topographyBounds.getCenterY());
//		List<VPoint> pedestrianPoints = new ArrayList<>();
//		pedestrianPoints.add(center);
//
//		/*
//		 * fills the whole topographyBounds with pedestrian by starting at center and adds in each
//		 * iteration in a
//		 * hexagon-pattern pedestrian around the the existing pedestrian until it reaches the
//		 * bounds.
//		 */
//		double hexagonAmbitRadius = new AttributesAgent().getRadius() * 2;
//		while (CreatePoints.addHexagonPoints(pedestrianPoints, topographyBounds, hexagonAmbitRadius)) {
//		}
//
//		VRectangle voronoiArae = new VRectangle(topographyBounds);
//		setUpProcessors(voronoiArae);
//
//		when(mockedTopography.getElements(Agent.class))
//				.thenReturn(PointToPedestrianConverter.getPedestriansAt(pedestrianPoints));
//		densityVoronoiProcessor.addPosition(center);
//		densityVoronoiGeoProcessor.addPosition(center);
//
//		Table table1 = densityVoronoiGeoProcessor.postUpdate(state);
//		Table table2 = densityVoronoiProcessor.postUpdate(state);
//
//		assertTrue("invalid talbe size " + table2.size() + " !=" + 1, table2.size() == 1);
//		assertEquals(table1.size(), table2.size());
//		compareTables(table1, densityVoronoiGeoProcessor.getDensityType(), table2,
//				densityVoronoiProcessor.getDensityType());
//
//		double voronoiSiteArea = 2 * Math.sqrt(3) * pedestrianRadius * pedestrianRadius;
//		double highestPossibleDensity = 1 / voronoiSiteArea;
//
//		// System.out.println(highestPossibleDensity);
//
//		double geoDensity = (double) table1.getEntry(densityVoronoiGeoProcessor.getDensityType(), 0);
//		double density = (double) table2.getEntry(densityVoronoiProcessor.getDensityType(), 0);
//
//		assertTrue("high density is not equals to the expected highest density",
//				Math.abs(highestPossibleDensity - geoDensity) <= GeometryUtils.DOUBLE_EPS);
//		assertTrue("high density is not equals to the expected highest density",
//				Math.abs(highestPossibleDensity - density) <= GeometryUtils.DOUBLE_EPS);
//	}
//
//	/**
//	 * Tests the equality of the 2 Voronoi-Implementations by adding just 2 Points inside the
//	 * voronoi-area
//	 * and measures the density at one of these points.
//	 */
//	@Test
//	public void testTwoSimple() {
//
//		VRectangle voronoiArea = new VRectangle(3, 3, 12, 12);
//		setUpProcessors(voronoiArea);
//
//		VPoint[] pedestrianPoints = new VPoint[] {new VPoint(5, 5), new VPoint(7.5, 5)};
//		VPoint measurePoint = new VPoint(5, 5);
//
//		when(mockedTopography.getElements(Agent.class))
//				.thenReturn(PointToPedestrianConverter.getPedestriansAt(pedestrianPoints));
//
//		densityVoronoiGeoProcessor.addPosition(measurePoint);
//		densityVoronoiProcessor.addPosition(measurePoint);
//
//		Table table1 = densityVoronoiGeoProcessor.postUpdate(state);
//		Table table2 = densityVoronoiProcessor.postUpdate(state);
//
//		assertTrue("invalid talbe size " + table2.size() + " !=" + 1, table2.size() == 1);
//		assertEquals(table1.size(), table2.size());
//		compareTables(table1, densityVoronoiGeoProcessor.getDensityType(), table2,
//				densityVoronoiProcessor.getDensityType());
//	}
//
//	/**
//	 * Tests the equality of the 2 Voronoi-Implementations by adding just 1 Points inside the
//	 * voronoi-area
//	 * and 1 other point outside of the voronoi-area.
//	 */
//	@Test
//	public void testTwoSimple2() {
//
//		VRectangle voronoiArea = new VRectangle(2, 0, 14, 8);
//		setUpProcessors(voronoiArea);
//
//		VPoint[] pedestrianPoints = new VPoint[] {new VPoint(5, 5), new VPoint(0.5, 5)};
//		VPoint measurePoint = new VPoint(5, 5);
//
//		when(mockedTopography.getElements(Agent.class))
//				.thenReturn(PointToPedestrianConverter.getPedestriansAt(pedestrianPoints));
//
//		densityVoronoiGeoProcessor.addPosition(measurePoint);
//		densityVoronoiProcessor.addPosition(measurePoint);
//
//		Table table1 = densityVoronoiGeoProcessor.postUpdate(state);
//		Table table2 = densityVoronoiProcessor.postUpdate(state);
//
//		assertTrue("invalid talbe size " + table2.size() + " !=" + 1, table2.size() == 1);
//		assertEquals(table1.size(), table2.size());
//		compareTables(table1, densityVoronoiGeoProcessor.getDensityType(), table2,
//				densityVoronoiProcessor.getDensityType());
//	}
//
//	/**
//	 * Randomly position some Pedestrians inside the voronoi-area and measure the density at some
//	 * points for both implementations.
//	 * Check the equality of the both results.
//	 */
//	@Test
//	public void testVoronoiAreaOverlapAllPedestrians() {
//
//		int numberOfPedestrians = 5;
//		int numberOfMeasurePoints = 100;
//
//		VRectangle voronoiArea = new VRectangle(2, 2, 10, 10);
//		setUpProcessors(voronoiArea);
//
//		VPoint[] pedestrianPoints = CreatePoints.generateRandomVPoint(voronoiArea, numberOfPedestrians);
//		VPoint[] measurementPoints = CreatePoints.generateRandomVPoint(voronoiArea, numberOfMeasurePoints);
//
//		when(mockedTopography.getElements(Agent.class))
//				.thenReturn(PointToPedestrianConverter.getPedestriansAt(pedestrianPoints));
//
//		densityVoronoiGeoProcessor.addAllPositions(measurementPoints);
//		densityVoronoiProcessor.addAllPositions(measurementPoints);
//
//		Table table1 = densityVoronoiGeoProcessor.postUpdate(state);
//		Table table2 = densityVoronoiProcessor.postUpdate(state);
//
//		assertTrue("invalid talbe size " + table1.size() + " !=" + numberOfMeasurePoints,
//				table2.size() == numberOfMeasurePoints);
//		assertEquals(table1.size(), table2.size());
//		compareTables(table1, densityVoronoiGeoProcessor.getDensityType(), table2,
//				densityVoronoiProcessor.getDensityType());
//	}
//
//	/**
//	 * Randomly position some Pedestrians inside and outside (at least one outside) the voronoi-area
//	 * and measure the density at some points for both implementations.
//	 * Check the equality of the both results.
//	 */
//	@Test
//	public void testVoronoiAreaOverlapNotAllPedestrians() {
//
//		int numberOfPedestrians = 5;
//		int numberOfMeasurePoints = 100;
//
//		VRectangle voronoiArea = new VRectangle(2, 2, 10, 10);
//		setUpProcessors(voronoiArea);
//
//		VPoint[] pedestrianPoints =
//				CreatePoints.generateRandomVPoint(new VRectangle(0, 0, 20, 20), numberOfPedestrians);
//		VPoint pointOutsideVoronoiArea = pedestrianPoints[0]
//				.add(new VPoint(voronoiArea.getWidth() - voronoiArea.getX() + GeometryUtils.DOUBLE_EPS, 0));
//		pedestrianPoints[0] = pointOutsideVoronoiArea;
//
//		VPoint[] measurementPoints = CreatePoints.generateRandomVPoint(voronoiArea, numberOfMeasurePoints);
//
//		when(mockedTopography.getElements(Agent.class))
//				.thenReturn(PointToPedestrianConverter.getPedestriansAt(pedestrianPoints));
//		SimulationState state = mock(SimulationState.class);
//
//		when(state.getStep()).thenAnswer(new Answer<Integer>() {
//			@Override
//			public Integer answer(InvocationOnMock invocationOnMock) throws Throwable {
//				return count();
//			}
//		});
//
//		when(state.getTopography()).thenReturn(mockedTopography);
//
//		densityVoronoiGeoProcessor.addAllPositions(measurementPoints);
//		densityVoronoiProcessor.addAllPositions(measurementPoints);
//
//		Table table1 = densityVoronoiGeoProcessor.postUpdate(state);
//		Table table2 = densityVoronoiProcessor.postUpdate(state);
//
//		assertTrue("invalid talbe size " + table1.size() + " != " + numberOfMeasurePoints,
//				table2.size() == numberOfMeasurePoints);
//		assertEquals(table1.size(), table2.size());
//
//		compareTables(table1, densityVoronoiGeoProcessor.getDensityType(), table2,
//				densityVoronoiProcessor.getDensityType());
//	}
//
//	/**
//	 * Randomly generate Pedestrian from 2 to maxPedCount and for each number of Pedestrians measure
//	 * the
//	 * density at 1 to maxMeasurePointCount position. Check the equality of the result of both
//	 * implementations.
//	 */
//	@Test
//	public void testIntervalOfPoints() {
//
//		int maxPedCount = 100;
//		int maxMeasurePointCount = 10;
//
//		for (int numberOfPedestrians = 2; numberOfPedestrians <= maxPedCount; numberOfPedestrians++) {
//
//			for (int numberOfMeasurePoints =
//					1; numberOfMeasurePoints <= maxMeasurePointCount; numberOfMeasurePoints++) {
//				VRectangle voronoiArea = new VRectangle(2, 2, 10, 10);
//				setUpProcessors(voronoiArea);
//
//				VPoint[] pedestrianPoints = CreatePoints.generateRandomVPoint(voronoiArea, numberOfPedestrians);
//				VPoint[] measurementPoints = CreatePoints.generateRandomVPoint(voronoiArea, numberOfMeasurePoints);
//
//				when(mockedTopography.getElements(Agent.class))
//						.thenReturn(PointToPedestrianConverter.getPedestriansAt(pedestrianPoints));
//
//				densityVoronoiGeoProcessor.addAllPositions(measurementPoints);
//				densityVoronoiProcessor.addAllPositions(measurementPoints);
//
//				Table table1 = densityVoronoiGeoProcessor.postUpdate(state);
//				Table table2 = densityVoronoiProcessor.postUpdate(state);
//
//				assertTrue("invalid talbe size " + table1.size() + " !=" + numberOfMeasurePoints,
//						table2.size() == numberOfMeasurePoints);
//				assertEquals(table1.size(), table2.size());
//				compareTables(table1, densityVoronoiGeoProcessor.getDensityType(), table2,
//						densityVoronoiProcessor.getDensityType());
//			}
//		}
//	}
//
//	private void setUpProcessors(final VRectangle voronoiArea) {
//		AttributesBuilder<AttributesDensityVoronoiProcessor> attributeBuilder =
//				new AttributesBuilder<>(new AttributesDensityVoronoiProcessor());
//		attributeBuilder.setField("measurementArea", voronoiArea);
//		attributeBuilder.setField("voronoiArea", voronoiArea);
//		attributes = attributeBuilder.build();
//
//		densityVoronoiProcessor = new DensityVoronoiProcessor(attributes);
//		densityVoronoiProcessor.addColumnNames(densityVoronoiProcessor.getDensityType());
//		densityVoronoiGeoProcessor = new DensityVoronoiGeoProcessor(attributes);
//		densityVoronoiGeoProcessor.addColumnNames(densityVoronoiGeoProcessor.getDensityType());
//	}
//
//	private void compareTables(final Table table1, final String densityName1, final Table table2,
//			final String densityName2) {
//		List<Double> densities = new ArrayList<>();
//		for (Row row : table1) {
//			double density = (double) row.getEntry(densityName1);
//			assertTrue("invalid density value ", Double.isNaN(density) || density > 0);
//			densities.add(density);
//		}
//
//		int i = 0;
//		for (Row row : table2) {
//			double density = (double) row.getEntry(densityName2);
//			assertTrue("invalid density value " + density, Double.isNaN(density) || density > 0);
//
//			if (Double.isNaN(density)) {
//				assertTrue("not both NaN ", Double.isNaN(densities.get(i)));
//			} else if (!Double.isNaN(density) && !Double.isNaN(densities.get(i))) {
//				double densityDifference = Math.abs(density - densities.get(i));
//				double absolutDifference = 0.0;
//
//				if (densities.get(i) > 0) {
//					absolutDifference = densityDifference / densities.get(i);
//				}
//
//				// System.out.println("("+(row.getEntry("x") + "," + row.getEntry("y") + "): " +
//				// densities.get(i) + ", " + density) + " computeGodunovDifference:"
//				// +densityDifference);
//
//				assertTrue("the absolut difference is too large (" +
//						row.getEntry("x") + "," + row.getEntry("y") + "): " +
//						densityDifference + " > " + densityEpsilon, densityDifference <= densityEpsilon);
//
//				assertTrue("the relative difference is too large (" +
//						row.getEntry("x") + "," + row.getEntry("y") + "): " +
//						absolutDifference + " > " + densityEpsilon, absolutDifference <= densityEpsilon);
//			}
//			i++;
//		}
//	}
//
//	private int count() {
//		stepCounter++;
//		return stepCounter;
//	}
//}
