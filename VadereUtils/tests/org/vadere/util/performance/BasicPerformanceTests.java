package org.vadere.util.performance;


import org.junit.Before;
import org.junit.Test;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.logging.Logger;

import java.util.ArrayList;

/**
 * Created by bzoennchen on 06.09.17.
 */
public class BasicPerformanceTests {
	private static Logger log = Logger.getLogger(BasicPerformanceTests.class);
	private ArrayList<VPoint> integers;
	private int n = 40000;

	@Before
	public void setUp() throws Exception {
		integers = new ArrayList<>();
		for(int i = 0; i < n; i++) {
			integers.add(new VPoint(i, i));
		}
	}

	@Test
	public void testIteration(){
		long ms = System.currentTimeMillis();
		int sum = 0;

		for(int i = 0; i < n; i++) {
			sum += Math.sqrt(integers.get(i).distanceToOrigin() * 0.5) - i*5 + 3.3 / Math.min(0.4, i/10.0);
		}

		ms = System.currentTimeMillis() - ms;
		log.info(ms);
		log.info(sum);
	}
}
