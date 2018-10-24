package org.vadere.simulator.imageprocessing;

import org.junit.Before;
import org.vadere.simulator.dataprocessing.CreatePoints;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestPedestrianGaussianFilter {

	private List<Pedestrian> pedestriansMaxPacked;
	private VRectangle topographyBounds;
	private VPoint center;

	@Before
	public void setUp() throws Exception {
		topographyBounds = new VRectangle(0, 0, 10, 10);
		List<VPoint> pedestrianPositions = new ArrayList<>();
		pedestriansMaxPacked = new ArrayList<>();

		double pedestrianRadius = new AttributesAgent().getRadius();
		/*
		 * Based on http://en.wikipedia.org/wiki/Circle_packing we create a high density
		 * environment.
		 */
		center = new VPoint(topographyBounds.getCenterX(), topographyBounds.getCenterY());
		pedestrianPositions.add(center);

		/*
		 * fills the whole topographyBounds with pedestrian by starting at center and adds in each
		 * iteration in a
		 * hexagon-pattern pedestrian around the the existing pedestrian until it reaches the
		 * bounds.
		 */
		double hexagonAmbitRadius = new AttributesAgent().getRadius() * 2;
		while (CreatePoints.addHexagonPoints(pedestrianPositions, topographyBounds, hexagonAmbitRadius)) {
		}

		for (int i = 0; i < pedestrianPositions.size(); i++) {
			Pedestrian ped = mock(Pedestrian.class);
			when(ped.getPosition()).thenReturn(pedestrianPositions.get(i));
			pedestriansMaxPacked.add(ped);
		}


	}

	/**
	 * Fill the topography with the maximum amount of pedestrian and compare the
	 * exact density with the approximated (cl gaussian filter) at each grid point.
	 *
	 * Note: This test requires a huge amount of running time (> 5 minutes)
	 */
	/*@Test
	public void testMaxDensity() {
		IGaussianFilter clFilter = IGaussianFilter.create(topographyBounds, pedestriansMaxPacked, 10, 0.7,
				new AttributesAgent(-1), (ped) -> 1.0, IGaussianFilter.Type.OpenCL);
		clFilter.filterImage();

		double maxDensity = clFilter.getFilteredValue(center.x, center.y);
		double dx = 0.1;
		double dy = 0.1;
		double maxRelErrorCL = 0.0;
		double maxRelErrorCV = 0.0;
		double maxAbsErrorCV = 0.0;
		double maxAbsErrorCL = 0.0;
		for (double x = 0; x <= topographyBounds.getWidth() - 1; x += dx) {
			for (double y = 0; y <= topographyBounds.getHeight() - 1; y += dy) {
				assertTrue(maxDensity >= clFilter.getFilteredValue(x, y) - 0.1);
				double exactDensity = calculateExactDensity(new VPoint(x, y), pedestriansMaxPacked, 0.7);
				maxAbsErrorCL = Math.bound(maxAbsErrorCL, Math.abs((clFilter.getFilteredValue(x, y) - exactDensity)));
				maxRelErrorCL = Math.bound(maxRelErrorCL,
						Math.abs((clFilter.getFilteredValue(x, y) - exactDensity)) / exactDensity);
				// System.out.println("abs. cl-topographyError:" + (clFilter.getFilteredValue(x, y) -
				// exactDensity));
				// System.out.println("abs. cv-topographyError:" + (cvFilter.getFilteredValue(x, y) -
				// exactDensity));
			}
		}
		assertTrue(maxAbsErrorCL <= 0.1);
		assertTrue(maxAbsErrorCV <= 0.1);
		assertTrue(maxRelErrorCL <= 0.1);
		assertTrue(maxRelErrorCV <= 0.1);

		/*
		 * System.out.println("abs. cl-topographyError:" + maxAbsErrorCL);
		 * System.out.println("abs. cv-topographyError:" + maxAbsErrorCV);
		 * 
		 * System.out.println("rel. cl-topographyError:" + maxRelErrorCL);
		 * System.out.println("rel. cv-topographyError:" + maxRelErrorCV);
		 */
//	}

	private static double calculateExactDensity(final VPoint point, final Collection<Pedestrian> pedestrians,
			final double standardDerivation) {
		return pedestrians.stream().map(ped -> ped.getPosition())
				.map(p -> calculatePartialDensity(p, point, standardDerivation, new AttributesAgent(-1)))
				.reduce(0.0, (d1, d2) -> d1 + d2);
	}

	private static double calculatePartialDensity(final VPoint p1, final VPoint p2, final double standardDerivation,
			final AttributesAgent attributesPedestrian) {
		double varianz = standardDerivation * standardDerivation;

		double scaleFactor = attributesPedestrian.getRadius() * 2
				* attributesPedestrian.getRadius() * 2
				* Math.sqrt(3)
				* 0.5
				/ (2 * Math.PI * standardDerivation * standardDerivation);

		double dx = p1.x - p2.x;
		double dy = p1.y - p2.y;
		return (float) scaleFactor * Math.exp(-(dx * dx + dy * dy) / (2 * varianz));
	}

}
