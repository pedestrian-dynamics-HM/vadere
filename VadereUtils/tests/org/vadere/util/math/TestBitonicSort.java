package org.vadere.util.math;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.vadere.util.opencl.CLBitonicSort;
import org.vadere.util.opencl.OpenCLException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * @author Benedikt Zoennchen
 */
public class TestBitonicSort {

	private static Logger logger = LogManager.getLogger(TestConvolution.class);

	@Before
	public void setUp() throws Exception {}

	@Test
	public void testLocalSort() throws IOException, OpenCLException {
		int[] keys = randomArray(32);
		int[] values = randomArray(32);

		CLBitonicSort clBitonicSort = new CLBitonicSort();
		clBitonicSort.sort(keys, values);

		int[] resultKeys = clBitonicSort.getResultKeys();
		int[] resultValues = clBitonicSort.getResultValues();

		Arrays.sort(keys);
		assertTrue(Arrays.equals(keys, resultKeys));
	}

	@Test
	public void testGlobalSort() throws IOException, OpenCLException {
		int[] keys = randomArray(2048*2*2*2*2);
		int[] values = randomArray(2048*2*2*2*2);

		CLBitonicSort clBitonicSort = new CLBitonicSort();
		clBitonicSort.sort(keys, values);

		int[] resultKeys = clBitonicSort.getResultKeys();
		int[] resultValues = clBitonicSort.getResultValues();

		Arrays.sort(keys);
		assertTrue(Arrays.equals(keys, resultKeys));
	}

	public static int[] randomArray(final int size) {
		List<Integer> list = new ArrayList<>(size);
		int[] array = new int[size];

		for(int i = 0; i < size; i++) {
			list.add(i);
		}

		Collections.shuffle(list);

		for(int i = 0; i < size; i++) {
			array[i] = list.get(i);
		}

		return array;
	}
}
