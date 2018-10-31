package org.vadere.util.geometry;

import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VPolygon;

/**
 * Utility class for tests of geometry classes. Generates several test
 * geometries.
 * 
 */
public class CreateGeometry {

	public CreateGeometry() {}

	/**
	 * Setup a sample room with no obstacles.
	 * 
	 * @param roomSideLen
	 * @return
	 */
	public static Geometry createRoomWithoutObstacles(double roomSideLen) {
		Geometry roomWithoutObstacles = new Geometry();

		// setup borders
		VPoint p1 = new VPoint(0, 0);
		VPoint p2 = new VPoint(roomSideLen, 0);
		VPoint p3 = new VPoint(roomSideLen, roomSideLen);
		VPoint p4 = new VPoint(0, roomSideLen);

		VPolygon polygon = GeometryUtils.polygonFromPoints2D(p1, p2, p3, p4);
		roomWithoutObstacles.addPolygon(polygon);

		return roomWithoutObstacles;
	}

	/**
	 * Setup a sample room with one square shaped obstacle.
	 * 
	 * @param roomSideLen
	 * @param obstacleSideLen
	 * @return
	 */
	public static Geometry createRoomWithObstacle(double roomSideLen,
			double obstacleSideLen) {
		Geometry roomWithObstacle = new Geometry();

		// setup borders
		VPoint p1 = new VPoint(0, 0);
		VPoint p2 = new VPoint(roomSideLen, 0);
		VPoint p3 = new VPoint(roomSideLen, roomSideLen);
		VPoint p4 = new VPoint(0, roomSideLen);

		VPolygon polygon = GeometryUtils.polygonFromPoints2D(p1, p2, p3, p4);
		roomWithObstacle.addPolygon(polygon);

		// setup obstacle points
		VPoint o_p1 = new VPoint(roomSideLen / 2 - obstacleSideLen / 2,
				roomSideLen / 2 - obstacleSideLen / 2);
		VPoint o_p2 = new VPoint(roomSideLen / 2 + obstacleSideLen / 2,
				roomSideLen / 2 - obstacleSideLen / 2);
		VPoint o_p3 = new VPoint(roomSideLen / 2 + obstacleSideLen / 2,
				roomSideLen / 2 + obstacleSideLen / 2);
		VPoint o_p4 = new VPoint(roomSideLen / 2 - obstacleSideLen / 2,
				roomSideLen / 2 + obstacleSideLen / 2);

		// create obstacle
		VPolygon obstacle = GeometryUtils.polygonFromPoints2D(o_p1, o_p2, o_p3,
				o_p4);

		roomWithObstacle.addPolygon(obstacle);

		return roomWithObstacle;
	}

	/**
	 * Setup a sample room with two square shaped obstacles.
	 * 
	 * @param roomSideLen
	 * @param obstacleSideLen
	 * @return
	 */
	public static Geometry createRoomWith2Obstacles(double roomSideLen,
			double obstacleSideLen) {
		Geometry roomWithObstacle = new Geometry();

		// setup borders
		VPoint p1 = new VPoint(0, 0);
		VPoint p2 = new VPoint(roomSideLen, 0);
		VPoint p3 = new VPoint(roomSideLen, roomSideLen);
		VPoint p4 = new VPoint(0, roomSideLen);

		VPolygon poly = GeometryUtils.polygonFromPoints2D(p1, p2, p3, p4);

		// setup obstacle points
		VPoint o_p1 = new VPoint(roomSideLen / 2 - obstacleSideLen / 2,
				roomSideLen / 2 - obstacleSideLen / 2);
		VPoint o_p2 = new VPoint(roomSideLen / 2 + obstacleSideLen / 2,
				roomSideLen / 2 - obstacleSideLen / 2);
		VPoint o_p3 = new VPoint(roomSideLen / 2 + obstacleSideLen / 2,
				roomSideLen / 2 + obstacleSideLen / 2);
		VPoint o_p4 = new VPoint(roomSideLen / 2 - obstacleSideLen / 2,
				roomSideLen / 2 + obstacleSideLen / 2);

		// create obstacle
		VPolygon obstacle = GeometryUtils.polygonFromPoints2D(o_p1, o_p2, o_p3,
				p4);

		roomWithObstacle.addPolygon(obstacle);

		// create obstacle
		VPolygon obstacle2 = GeometryUtils.polygonFromPoints2D(new VPoint(
				roomSideLen / 2 - obstacleSideLen / 2, roomSideLen / 2
						- obstacleSideLen / 2 + 30),
				new VPoint(roomSideLen / 2
						+ obstacleSideLen / 2, roomSideLen / 2 - obstacleSideLen / 2
								+ 30),
				new VPoint(roomSideLen / 2 + obstacleSideLen / 2,
						roomSideLen / 2 + obstacleSideLen / 2 + 30),
				new VPoint(
						roomSideLen / 2 - obstacleSideLen / 2, roomSideLen / 2
								+ obstacleSideLen / 2 + 30));

		roomWithObstacle.addPolygon(obstacle2);

		return roomWithObstacle;
	}

	/**
	 * Create a U-shape.
	 * 
	 * @param roomSideLen
	 * @return
	 */
	public static Geometry createComplexGeometry1(double roomSideLen) {
		Geometry geometry1 = CreateGeometry
				.createRoomWithoutObstacles(roomSideLen);

		// create u shape
		VPolygon p1 = GeometryUtils.polygonFromPoints2D(new VPoint(0, 20),
				new VPoint(70, 20), new VPoint(70, 25), new VPoint(0, 25));

		VPolygon p2 = GeometryUtils.polygonFromPoints2D(new VPoint(40, 25),
				new VPoint(35, 25), new VPoint(35, 75), new VPoint(40, 75));

		VPolygon p3 = GeometryUtils.polygonFromPoints2D(new VPoint(30, 75),
				new VPoint(70, 75), new VPoint(70, 80), new VPoint(30, 80));

		VPolygon p4 = GeometryUtils.polygonFromPoints2D(new VPoint(60, 25),
				new VPoint(80, 60), new VPoint(75, 60), new VPoint(55, 25));

		geometry1.addPolygon(p1);
		geometry1.addPolygon(p2);
		geometry1.addPolygon(p3);
		geometry1.addPolygon(p4);

		return geometry1;
	}

	/**
	 * Create a room with a target after a smaller pathway
	 * 
	 * @param roomSideLen
	 * @return
	 */
	public static Geometry createComplexGeometry2(double roomSideLen) {
		Geometry geometry2 = CreateGeometry
				.createRoomWithoutObstacles(roomSideLen);

		// create shapes
		VPolygon p1 = GeometryUtils
				.polygonFromPoints2D(new VPoint(38, 0), new VPoint(49.75, 0),
						new VPoint(49.75, 50), new VPoint(38, 50));

		VPolygon p2 = GeometryUtils
				.polygonFromPoints2D(new VPoint(60, 0), new VPoint(50.25, 0),
						new VPoint(50.25, 50), new VPoint(60, 50));

		VPolygon p3 = GeometryUtils.polygonFromPoints2D(new VPoint(49.75, 50),
				new VPoint(49.75, 52), new VPoint(10, 52), new VPoint(10, 50));

		VPolygon p4 = GeometryUtils
				.polygonFromPoints2D(new VPoint(50.25, 50), new VPoint(50.25,
						52), new VPoint(100, 52), new VPoint(100, 50));

		geometry2.addPolygon(p1);
		geometry2.addPolygon(p2);
		geometry2.addPolygon(p3);
		geometry2.addPolygon(p4);

		return geometry2;
	}

	/**
	 * Create a room with a target after a pathway
	 * 
	 * @param roomSideLen
	 * @return
	 */
	public static Geometry createComplexGeometry3(double roomSideLen) {
		Geometry geometry3 = CreateGeometry
				.createRoomWithoutObstacles(roomSideLen);

		// create shapes
		VPolygon p1 = GeometryUtils.polygonFromPoints2D(new VPoint(38, 0),
				new VPoint(45, 0), new VPoint(45, 50), new VPoint(38, 50));

		VPolygon p2 = GeometryUtils.polygonFromPoints2D(new VPoint(60, 0),
				new VPoint(55, 0), new VPoint(55, 50), new VPoint(60, 50));

		VPolygon p3 = GeometryUtils.polygonFromPoints2D(new VPoint(45, 50),
				new VPoint(45, 60), new VPoint(10, 60), new VPoint(10, 50));

		VPolygon p4 = GeometryUtils.polygonFromPoints2D(new VPoint(55, 50),
				new VPoint(55, 60), new VPoint(100, 60), new VPoint(100, 50));

		geometry3.addPolygon(p1);
		geometry3.addPolygon(p2);
		geometry3.addPolygon(p3);
		geometry3.addPolygon(p4);

		return geometry3;
	}

	/**
	 * Create a room with a target after a small pathway
	 * 
	 * @param roomSideLen
	 * @return
	 */
	public static Geometry createComplexGeometry4(double roomSideLen) {
		Geometry geometry3 = CreateGeometry
				.createRoomWithoutObstacles(roomSideLen);

		// create shapes
		VPolygon p1 = GeometryUtils.polygonFromPoints2D(new VPoint(0, 0),
				new VPoint(10, 0), new VPoint(10, 20), new VPoint(0, 20));

		VPolygon p2 = GeometryUtils.polygonFromPoints2D(new VPoint(10, 0),
				new VPoint(14, 0), new VPoint(14, 11.5), new VPoint(10, 11.5));

		VPolygon p3 = GeometryUtils.polygonFromPoints2D(new VPoint(14, 0),
				new VPoint(18, 0), new VPoint(18, 20), new VPoint(14, 20));

		double w = 4;
		double b = 4;

		VPolygon p4 = GeometryUtils.polygonFromPoints2D(new VPoint(18, 20),
				new VPoint(18, 60), new VPoint(14 - b / 2 + w / 2, 60),
				new VPoint(14 - b / 2 + w / 2, 20));

		VPolygon p5 = GeometryUtils.polygonFromPoints2D(new VPoint(0, 20),
				new VPoint(14 - b / 2 - w / 2, 20), new VPoint(14 - b / 2 - w
						/ 2, 60),
				new VPoint(0, 60));

		geometry3.addPolygon(p1);
		geometry3.addPolygon(p2);
		geometry3.addPolygon(p3);
		geometry3.addPolygon(p4);
		geometry3.addPolygon(p5);

		return geometry3;
	}
}
