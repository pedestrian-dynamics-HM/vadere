package org.vadere.util;

import org.junit.Test;
import org.vadere.meshing.mesh.impl.PSLG;
import org.vadere.meshing.utils.io.poly.PSLGGenerator;
import org.vadere.meshing.utils.io.tex.TexGraphGenerator;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPolygon;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

public class TestPoyGenerator {

	@Test
	public void testRead2DPolyFile() {
		final InputStream inputStream1 = getClass().getResourceAsStream("/poly/a.poly");
		final InputStream inputStream2 = getClass().getResourceAsStream("/poly/a.poly");
		try {
			PSLG pslg = PSLGGenerator.toPSLG(inputStream1);
			Collection<VLine> segments = pslg.getAllSegments();
			Collection<VPolygon> polygons = pslg.getAllPolygons();
			Collection<VLine> allLines = polygons.stream().flatMap(polygon -> polygon.getLinePath().stream()).collect(Collectors.toList());
			assertEquals(allLines.size(), segments.size());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testRead2DPolyToTikZ() {
		final InputStream inputStream = getClass().getResourceAsStream("/poly/greenland.poly");
		try {
			PSLG pslg = PSLGGenerator.toPSLG(inputStream).toProtectedPSLG(Double.POSITIVE_INFINITY);
			System.out.println(TexGraphGenerator.toTikz(pslg.getAllSegments()));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
