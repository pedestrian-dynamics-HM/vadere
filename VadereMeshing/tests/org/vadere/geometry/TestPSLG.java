package org.vadere.geometry;

import org.junit.Test;
import org.vadere.meshing.examples.MeshExamples;
import org.vadere.meshing.mesh.impl.PSLG;
import org.vadere.meshing.utils.io.poly.PSLGGenerator;
import org.vadere.meshing.utils.io.tex.TexGraphGenerator;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VPolygon;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class TestPSLG {

	@Test
	public void testProtection() {
		VPoint p1 = new VPoint(0,0);
		VPoint p2 = new VPoint(1, 0);
		VPoint p3 = new VPoint(0.5, 1.0 * Math.sqrt(3) / 2.0);

		VPolygon polygon = GeometryUtils.toPolygon(p3, p2, p1);
		PSLG pslg = new PSLG(polygon, Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
		PSLG protectedPSLG = pslg.toProtectedPSLG(Double.POSITIVE_INFINITY);
		assertEquals(3 + 3 * 2, protectedPSLG.getSegmentBound().getPath().size());
	}


	@Test
	public void testdProtection() throws IOException {
		final InputStream inputStream = MeshExamples.class.getResourceAsStream("/poly/kaiserslautern.poly");
		PSLG pslg = PSLGGenerator.toPSLG(inputStream);
		pslg = pslg.toProtectedPSLG(Double.POSITIVE_INFINITY);
		System.out.println(TexGraphGenerator.toTikz(pslg.getAllSegments()));
	}

}
