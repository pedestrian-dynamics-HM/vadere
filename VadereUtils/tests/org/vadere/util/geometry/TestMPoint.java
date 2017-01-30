package org.vadere.util.geometry;

import org.junit.Before;
import org.junit.Test;
import org.vadere.util.triangulation.adaptive.MeshPoint;

import java.util.HashSet;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestMPoint {

	@Before
	public void setUp() {

	}

	@Test
	public void testHashSet() {
		MeshPoint mPoint1 = new MeshPoint(3.0, 1.43545, false);
		MeshPoint mPoint2 = new MeshPoint(3.0, 1.43545, false);
		MeshPoint mPoint3 = new MeshPoint(3.0, 1.43545, true);
		HashSet<MeshPoint> set = new HashSet<>();
		assertTrue(set.add(mPoint1));
		assertFalse(set.add(mPoint2));
		assertFalse(set.add(mPoint3));
		assertTrue(set.size() == 1);
	}
}
