package org.vadere.util.geometry;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VTriangle;

/**
 * Incomplete tests of the {@link VTriangle} class.
 * 
 * 
 */
public class TestTriangle {

	public static VTriangle testTriangle;

	@Before
	public void setUp() throws Exception {

		testTriangle = new VTriangle(new VPoint(40, 40), new VPoint(30, 50),
				new VPoint(80, 0));
	}

	@Test
	public void testIsLine() {
		assertEquals("Triangle should be a line.", true, testTriangle.isLine());
	}

}
