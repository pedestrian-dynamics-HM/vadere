package org.vadere.simulator.models.osm.opencl;

import org.junit.Test;
import org.vadere.util.logging.Logger;
import org.vadere.util.opencl.OpenCLException;

import java.util.Random;

import static org.junit.Assert.assertArrayEquals;

public class TestCLScanNegatives {
	private static Logger logger = Logger.getLogger(TestCLScanNegatives.class);
	private static Random random = new Random();

	static {
		logger.setDebug();
	}

	@Test
	public void testSmallArrayScan() throws OpenCLException {
		int[] data = new int[]{-3,1,2,-5,6,1,3,12};
		int[] expectedResult = new int[]{0,-3,-3,-3,-8,-8,-8,-8,-8};
		CLScanDirectional clScan = new CLScanDirectional();
		int[] result = clScan.scan(data, -1);
		assertArrayEquals(expectedResult, result);
	}

	@Test
	public void testLargeArrayScanNeg() throws OpenCLException {
		int n = 100_000;
		int[] data = new int[n];

		for(int i = 0; i < n; i++) {
			// this works only for small numbers such that there is no overflow!
			data[i] = 1 * (random.nextBoolean() ? -1 : 1);
		}

		int[] expectedResult = scan(data, -1);
		CLScanDirectional clScan = new CLScanDirectional();
		int[] result = clScan.scan(data, -1);
		assertArrayEquals(expectedResult, result);
	}

	@Test
	public void testLargeArrayScanPos() throws OpenCLException {
		int n = 100_000;
		int[] data = new int[n];

		for(int i = 0; i < n; i++) {
			// this works only for small numbers such that there is no overflow!
			data[i] = 1 * (random.nextBoolean() ? -1 : 1);
		}

		int[] expectedResult = scan(data, 1);
		CLScanDirectional clScan = new CLScanDirectional();
		int[] result = clScan.scan(data, 1);
		assertArrayEquals(expectedResult, result);
	}

	private int[] scan(int[] data, int dir){
		int[] result = new int[data.length+1];
		result[0] = 0;

		for(int i = 1; i < result.length; i++) {
			result[i] = result[i-1] + (data[i-1] * dir > 0 ? data[i-1] : 0);
		}

		return result;
	}
}
