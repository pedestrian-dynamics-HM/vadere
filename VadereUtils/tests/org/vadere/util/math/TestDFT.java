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
		double[] sample = new double [] {0.0, 0.707, 1.0, 0.707, 0.0, -0.707, -1.0, -0.707};
		Complex[] complexSample = MathUtil.toComplex(sample);

		CLFourierTransformation clFourierTransformation = new CLFourierTransformation();

		Complex[] coeffs = clFourierTransformation.dft(complexSample, true);
		Complex[] reverseCoeffs = clFourierTransformation.dft(coeffs, false);

		for(int i = 0; i < coeffs.length; i++) {
			logger.info(coeffs[i]);
		}

		FourierTransformation fourierTransformation = new FourierTransformation();
		Complex[] coeffs2 = fourierTransformation.transform(complexSample);

		for(int i = 0; i < coeffs2.length; i++) {
			logger.info(coeffs2[i]);
		}

		for(int i = 0; i < coeffs2.length; i++) {
			assertTrue(coeffs[i].subtract(coeffs2[i]).abs() < EPSILON);
		}

		for(int i = 0; i < coeffs2.length; i++) {
			assertTrue(complexSample[i].subtract(reverseCoeffs[i]).abs() < EPSILON);
		}

	}
}
