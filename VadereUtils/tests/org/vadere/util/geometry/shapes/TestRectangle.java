package org.vadere.util.geometry.shapes;

import org.junit.Before;
import org.junit.Test;
import org.vadere.util.geometry.shapes.VRectangle;

import static org.junit.Assert.assertEquals;

/**
 * @author Benedikt Zoennchen
 */
public class TestRectangle {

	private VRectangle rect1;
	private VRectangle rect2;

	@Before
	public void setUp() {
		rect1 = new VRectangle(1.0, 2.0, 10.123, 22.3123);
		rect2 = new VRectangle(1.0, 2.0, 10.123, 22.3123);
	}

	@Test
	public void testEquals() {
		assertEquals("equals() does not work properly.", rect1, rect2);
	}
}
