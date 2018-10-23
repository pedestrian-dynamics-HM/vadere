package org.vadere.state.scenario;

import org.junit.Before;
import org.junit.Test;
import org.vadere.state.attributes.scenario.AttributesObstacle;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.VPolygon;
import org.vadere.util.geometry.shapes.VRectangle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
/**
 * @author Benedikt Zoennchen
 */
public class TestObstacle {

	private Obstacle obstacle1;
	private Obstacle obstacle2;
	private Obstacle obstacle3;
	private Obstacle obstacle4;
	private Obstacle obstacle5;

	@Before
	public void setUp() {
		VRectangle rectangle1 = new VRectangle(0, 0, 10, 10);
		VRectangle rectangle2 = new VRectangle(0, 0, 10, 10);
		VPolygon polygon1 = GeometryUtils.polygonFromPoints2D(rectangle1.getCornerPoints());
		VPolygon polygon2 = GeometryUtils.polygonFromPoints2D(rectangle2.getCornerPoints());

		AttributesObstacle attributesObstacle1 = new AttributesObstacle(-1, rectangle1);
		AttributesObstacle attributesObstacle2 = new AttributesObstacle(-1, rectangle2);
		AttributesObstacle attributesObstacle3 = new AttributesObstacle(-1, polygon1);
		AttributesObstacle attributesObstacle4 = new AttributesObstacle(-1, polygon2);

		obstacle1 = new Obstacle(attributesObstacle1);
		obstacle2 = new Obstacle((AttributesObstacle)attributesObstacle1.clone());
		obstacle3 = new Obstacle(attributesObstacle2);
		obstacle4 = new Obstacle(attributesObstacle3);
		obstacle5 = new Obstacle(attributesObstacle4);
	}

	@Test
	public void testEquals() {
		assertEquals("equals() does not work properly.", obstacle1, obstacle2);
		assertEquals("equals() does not work properly.", obstacle1, obstacle3);
		assertNotEquals("equals() does not work properly.", obstacle1, obstacle4);
		assertEquals("equals() does not work properly.", obstacle4, obstacle5);
	}
}
