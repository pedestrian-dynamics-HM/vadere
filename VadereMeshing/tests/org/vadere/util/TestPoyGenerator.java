package org.vadere.util;

import org.junit.Test;
import org.vadere.meshing.utils.io.poly.PolyGenerator;
import org.vadere.meshing.utils.io.tex.TexGraphGenerator;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPolygon;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

public class TestPoyGenerator {

	@Test
	public void testRead2DPolyFile() {
		final InputStream inputStream1 = getClass().getResourceAsStream("/poly/a.poly");
		final InputStream inputStream2 = getClass().getResourceAsStream("/poly/a.poly");
		try {
			var vgeometry1 = PolyGenerator.toPSLGtoVShapes(inputStream1, false);
			var vgeometry2 = PolyGenerator.toPSLGtoVShapes(inputStream2, true);
			List<VLine> lines1 = vgeometry1.getRight();
			List<VPolygon> polygons1 = vgeometry1.getLeft();
			List<VLine> lines2 = vgeometry2.getRight();
			List<VPolygon> polygons2 = vgeometry2.getLeft();

			List<VLine> allLines = polygons1.stream().flatMap(polygon -> polygon.getLinePath().stream()).collect(Collectors.toList());
			assertEquals(2, polygons1.size());
			assertEquals(2, polygons2.size());
			assertEquals(allLines.size(), lines2.size());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testRead2DPolyToTikZ() {
		final InputStream inputStream1 = getClass().getResourceAsStream("/poly/chicken.poly");
		try {
			var vgeometry = PolyGenerator.toPSLGtoVShapes(inputStream1, true);
			System.out.println(TexGraphGenerator.toTikz(vgeometry.getRight()));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
