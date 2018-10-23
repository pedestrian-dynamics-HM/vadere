package org.vadere.geometry;

import org.junit.Before;
import org.junit.Test;
import org.vadere.meshing.ConstantLineIterator;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;

import static org.junit.Assert.assertTrue;

public class TestLineIterator {

	@Before
	public void setUp() throws Exception {

	}

	@Test
	public void testFaceIterator() {
		double x1 = 0.0;
		double y1 = 5.0;
		double x2 = 12.0;
		double y2 = -3.0;
		VLine line = new VLine(x1, y1, x2, y2);
		ConstantLineIterator lineIterator = new ConstantLineIterator(line, 0.5);
		double originalSlope = line.slope();
		VPoint startPoint = new VPoint(x1, y1);
		int count = 0;

		while (lineIterator.hasNext()) {
			IPoint nextPoint = lineIterator.next();
			VPoint nextVPoint = new VPoint(nextPoint.getX(), nextPoint.getY());

			if(count == 0) {
				assertTrue(startPoint.equals(nextVPoint));
			}
			else {
				double slope = new VLine(startPoint, nextVPoint).slope();

				if(!startPoint.equals(nextVPoint)) {
					assertTrue(Math.abs(slope - originalSlope) < 0.0001);
				}
			}
			count++;


		}
	}
}
