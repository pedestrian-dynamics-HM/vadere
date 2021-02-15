package org.vadere.state.scenario;

import org.junit.Test;
import org.vadere.util.geometry.shapes.VPoint;

import static org.junit.Assert.*;

public class ReferenceCoordinateSystemTest {



	//EPSG:25832 ETRS89 / UTM zone 32N
	@Test
	public void convertToGeo1() {
		ReferenceCoordinateSystem c = new ReferenceCoordinateSystem("EPSG:25832", "", new VPoint(0.0,0.0));
		VPoint p = c.convertToCartesian(0.9029408, 5.4072291);
		assertEquals(100000, p.x, 0.01);
		assertEquals(100000, p.y, 0.01);
	}

	@Test
	public void convertToCartesian1() {
		ReferenceCoordinateSystem c = new ReferenceCoordinateSystem("EPSG:25832", "", new VPoint(0.0,0.0));
		VPoint p = c.convertToGeo(new VPoint(100000, 100000));
		assertEquals(0.9029408, p.x, 0.00001);
		assertEquals(5.4072291, p.y, 0.00001);
	}

	// WGS84 (OpenStreetMaps)
	@Test
	public void convertToGeo() {
		ReferenceCoordinateSystem c = new ReferenceCoordinateSystem("EPSG:32632", "", new VPoint(692000.0,5337000.0));
		VPoint p = c.convertToCartesian(48.16219, 11.58645);
		assertEquals(324.886638718, p.x, 0.00001);
		assertEquals(562.448443838, p.y, 0.00001);
	}

	@Test
	public void convertToCartesian() {
		ReferenceCoordinateSystem c = new ReferenceCoordinateSystem("EPSG:32632", "", new VPoint(692000.0,5337000.0));
		VPoint p = c.convertToGeo(new VPoint(324.886638718, 562.448443838));
		assertEquals(48.16219, p.x, 0.00001);
		assertEquals(11.58645, p.y, 0.00001);
	}
}