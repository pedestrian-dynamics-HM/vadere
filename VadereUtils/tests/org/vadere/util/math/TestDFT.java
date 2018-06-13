package org.vadere.util.math;

import org.apache.commons.math3.complex.Complex;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertTrue;

public class TestDFT {
	private static Logger logger = LogManager.getLogger(TestDFT.class);
	private static double EPSILON = 1.0E-5;

	@Before
	public void setUp() throws Exception {}

	@Test
	public void testDFT() throws IOException {
		//TODO: reimplement!
	}

	public void testSimpleDFT() {
		float[] kernel = new float[] {
				1, 2, 3,
				4, 5, 6,
				7, 8, 9
		};

		float[] inMatrix = new float[] {
				2, 1, 1, 1,
				1, 1, 1, -1,
				1, 1, 1, 1,
				1, 1, 1, 0
		};

	}


}
