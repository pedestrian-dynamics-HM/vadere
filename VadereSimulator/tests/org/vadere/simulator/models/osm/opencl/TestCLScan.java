package org.vadere.simulator.models.osm.opencl;

import org.junit.Test;
import org.vadere.util.logging.Logger;
import org.vadere.util.opencl.OpenCLException;

import java.util.Random;

import static org.junit.Assert.assertArrayEquals;

public class TestCLScan {
	private static Logger logger = Logger.getLogger(TestCLScan.class);
	private static Random random = new Random();

	static {
		logger.setDebug();
	}

	@Test
	public void testSmallArrayScan() throws OpenCLException {
		int[] data = new int[]{3,1,2,5,6,1,3,12};
		int[] expectedResult = new int[]{0,3,4,6,11,17,18,21,33};
		CLScan clScan = new CLScan();
		int[] result = clScan.scan(data);
		assertArrayEquals(expectedResult, result);
	}

	@Test
	public void testLargeArrayScan() throws OpenCLException {
		int n = 100_000;
		int[] data = new int[n];

		for(int i = 0; i < n; i++) {
			data[i] = random.nextInt();
		}

		int[] expectedResult = scan(data);
		CLScan clScan = new CLScan();
		int[] result = clScan.scan(data);
		assertArrayEquals(expectedResult, result);
	}

	private int[] scan(int[] data){
		int[] result = new int[data.length+1];
		result[0] = 0;

		for(int i = 1; i < result.length; i++) {
			result[i] = result[i-1] + data[i-1];
		}

		return result;
	}
}
