package org.vadere.util.math;


import org.junit.Before;
import org.junit.Test;
import org.vadere.util.logging.Logger;
import org.vadere.util.opencl.CLBitonicSort;
import org.vadere.util.opencl.OpenCLException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertTrue;

/**
 * @author Benedikt Zoennchen
 */
public class TestBitonicSort {

	private static Logger logger = Logger.getLogger(TestConvolution.class);
	private static Random random = new Random();

	@Before
	public void setUp() throws Exception {
		logger.setDebug();
	}

	@Test
	public void testLocalSort() throws IOException, OpenCLException {
		int size = 123;
		int[] keys = randomArray(size);
		int[] values = randomArray(size);

		CLBitonicSort clBitonicSort = new CLBitonicSort();
		long ms = System.currentTimeMillis();
		clBitonicSort.sort(keys, values);
		long diff = System.currentTimeMillis() - ms;
		logger.debug("Sort (GPU):" + diff + "[ms], size:" + size);
		clBitonicSort.clear();

		int[] resultKeys = clBitonicSort.getResultKeys();
		int[] resultValues = clBitonicSort.getResultValues();

		ms = System.currentTimeMillis();
		Arrays.sort(keys);
		diff = System.currentTimeMillis() - ms;
		logger.debug("Sort (CPU):" + diff + "[ms], size:" + size);
		assertTrue(Arrays.equals(keys, resultKeys));
	}

	@Test
	public void testGlobalSort() throws IOException, OpenCLException {
		int size = Integer.MAX_VALUE / 1024;
		int[] keys = randomArray(size);
		int[] values = randomArray(size);

		CLBitonicSort clBitonicSort = new CLBitonicSort();
		long ms = System.currentTimeMillis();
		clBitonicSort.sort(keys, values);
		long diff = System.currentTimeMillis() - ms;
		logger.debug("Sort (GPU):" + diff + "[ms], size:" + size);
		clBitonicSort.clear();

		int[] resultKeys = clBitonicSort.getResultKeys();
		int[] resultValues = clBitonicSort.getResultValues();

		ms = System.currentTimeMillis();
		Arrays.sort(keys);
		diff = System.currentTimeMillis() - ms;
		logger.debug("Sort (CPU):" + diff + "[ms], size:" + size);

		assertTrue(Arrays.equals(keys, resultKeys));
	}

	public static int[] randomArray(final int size) {
		//List<Integer> list = new ArrayList<>(size);
		int[] array = new int[size];


		for(int i = 0; i < size; i++) {
			array[i] = i;
		}

		// shuffle
		for(int i = 0; i < size; i++) {
			int j = random.nextInt(size);
			int tmp = array[j];
			array[j] = array[i];
			array[i] = tmp;
		}

		return array;
	}
}
