package org.vadere.geometry;

import org.junit.Before;
import org.junit.Test;
import org.vadere.meshing.mesh.triangulation.improver.eikmesh.EikMeshPoint;

import java.util.HashSet;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestMPoint {

	@Before
	public void setUp() {

	}

	@Test
	public void testHashSet() {
		EikMeshPoint mPoint1 = new EikMeshPoint(3.0, 1.43545, false);
		EikMeshPoint mPoint2 = new EikMeshPoint(3.0, 1.43545, false);
		EikMeshPoint mPoint3 = new EikMeshPoint(3.0, 1.43545, true);
		HashSet<EikMeshPoint> set = new HashSet<>();
		assertTrue(set.add(mPoint1));
		assertFalse(set.add(mPoint2));
		assertFalse(set.add(mPoint3));
		assertTrue(set.size() == 1);
	}
}
