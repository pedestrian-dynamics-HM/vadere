package org.vadere.state.scenario;

import static org.junit.Assert.*;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Random;

import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.distribution.RealDistribution;
import org.junit.Before;
import org.junit.Test;
import tech.tablesaw.api.DoubleColumn;

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
			int spawnNumber = 10;


			DistributionFactory factory = new DistributionFactory(ConstantDistribution.class.getCanonicalName());
			ConstantDistribution actual = (ConstantDistribution) factory.createDistribution(random, spawnNumber, new LinkedList<Double>(Arrays.asList(param)));

			assertEquals(spawnNumber, actual.getSpawnNumber(0));
			assertEquals(param, actual.getUpdateFrequency(), 1E-16);
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void testFromDistributionWithNonDistribution() throws Exception {
		DistributionFactory f = new DistributionFactory(Double.class.getName());
		f.createDistribution(random, 10, new LinkedList<Double>(Arrays.asList(2.0)));
	}

	@Test(expected = ClassNotFoundException.class)
	public void testFromDistributionWithNonExistingClass() throws Exception {
		new DistributionFactory("MyNonExistingClass");
	}

}
