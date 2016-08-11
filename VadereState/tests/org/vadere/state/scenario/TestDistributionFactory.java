package org.vadere.state.scenario;

import static org.junit.Assert.*;

import java.util.Random;

import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.distribution.RealDistribution;
import org.junit.Before;
import org.junit.Test;
import org.vadere.state.scenario.ConstantDistribution;
import org.vadere.state.scenario.DistributionFactory;

public class TestDistributionFactory {

	private Random random;

	@Before
	public void setUp() {
		random = new Random(0);
	}

	@Test
	public void testCreateConstantDistribution() {
		try {
			double param = 1;
			RealDistribution expected = new ConstantDistribution(null, param);
			DistributionFactory factory = new DistributionFactory(ConstantDistribution.class);
			RealDistribution actual = factory.createDistribution(random, param);

			assertEquals(expected.getNumericalMean(), actual.getNumericalMean(), 0.000001);
			assertEquals(expected.getNumericalVariance(), actual.getNumericalVariance(), 0.000001);
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testCreateExponentialDistribution() {
		try {
			double param1 = 1;
			double param2 = 2;
			RealDistribution expected = new ExponentialDistribution(param1, param2);
			DistributionFactory factory = new DistributionFactory(ExponentialDistribution.class);
			RealDistribution actual = factory.createDistribution(random, param1, param2);

			assertEquals(expected.getNumericalMean(), actual.getNumericalMean(), 0.000001);
			assertEquals(expected.getNumericalVariance(), actual.getNumericalVariance(), 0.000001);
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

	@Test(expected = NoSuchMethodException.class)
	public void testFromDistributionWithNonDistribution() throws Exception {
		DistributionFactory f = DistributionFactory.fromDistributionClassName(Double.class.getName());
		f.createDistribution(random, 1.0);
	}

	@Test(expected = ClassNotFoundException.class)
	public void testFromDistributionWithNonExistingClass() throws Exception {
		DistributionFactory.fromDistributionClassName("MyNonExistingClass");
	}

}
