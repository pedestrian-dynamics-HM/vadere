package org.vadere.state.scenario;

import org.junit.Before;
import org.junit.Test;
import org.vadere.state.attributes.scenario.AttributesStairs;
import org.vadere.state.scenario.Stairs;
import org.vadere.state.scenario.Stairs.Tread;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.io.IOUtils;

import static org.junit.Assert.assertEquals;

public class TestStairs {

	private String attributesStore1;
	private AttributesStairs attributes1;
	private Stairs stairs1;
	private String attributesStore2;
	private AttributesStairs attributes2;
	private Stairs stairs2;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		attributesStore1 =
				"{shape:{x: 2.0,    y: 0.0,    width: 10.0,    height: 5.0,    type: RECTANGLE }, id:1,treadCount:5,upwardDirection:{x:1.0,y:0.0}}";
		attributes1 = IOUtils.getGson().fromJson(attributesStore1, AttributesStairs.class);

		this.stairs1 = new Stairs(attributes1);

		attributesStore2 =
				"{shape:{x: 0.0,    y: 1.0,    width: 10.0,    height: 5.0,    type: RECTANGLE }, id:1,treadCount:5,upwardDirection:{x:0.0,y:1.0}}";
		attributes2 = IOUtils.getGson().fromJson(attributesStore2, AttributesStairs.class);

		this.stairs2 = new Stairs(attributes2);
	}

	/**
	 * Test method for {@link org.vadere.state.scenario.Stairs#getTreads()}.
	 */
	@Test
	public void testGetTreadCount1() {
		assertEquals(7, stairs1.getTreads().length);
	}

	/**
	 * Test method for {@link org.vadere.state.scenario.Stairs#getTreads()}.
	 */
	@Test
	public void testGetTreadsLines1() {
		Tread[] treads = stairs1.getTreads();

		for (int i = 0; i < treads.length; i++) {
			assertEquals(0.0, treads[i].treadline.y1, GeometryUtils.DOUBLE_EPS);
			assertEquals(5.0, treads[i].treadline.y2, GeometryUtils.DOUBLE_EPS);
			assertEquals(i * 2.0, treads[i].treadline.x1, GeometryUtils.DOUBLE_EPS);
			assertEquals(i * 2.0, treads[i].treadline.x2, GeometryUtils.DOUBLE_EPS);
		}
	}

	/**
	 * Test method for {@link org.vadere.state.scenario.Stairs#getTreads()}.
	 */
	@Test
	public void testGetTreadsLines2() {
		Tread[] treads = stairs2.getTreads();

		for (int i = 0; i < treads.length; i++) {
			assertEquals(10.0, treads[i].treadline.x1, GeometryUtils.DOUBLE_EPS);
			assertEquals(0.0, treads[i].treadline.x2, GeometryUtils.DOUBLE_EPS);
			assertEquals(i * 1.0, treads[i].treadline.y1, GeometryUtils.DOUBLE_EPS);
			assertEquals(i * 1.0, treads[i].treadline.y2, GeometryUtils.DOUBLE_EPS);
		}
	}


	/**
	 * Test method for {@link org.vadere.state.scenario.Stairs#getAttributes()}.
	 */
	@Test
	public void testGetAttributes() {
		assertEquals(attributes1, stairs1.getAttributes());
	}

}
