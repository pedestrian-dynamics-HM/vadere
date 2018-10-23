package org.vadere.geometry.triangulation;

import org.vadere.meshing.utils.debug.DebugGui;
import org.vadere.meshing.utils.debug.SimpleTriCanvas;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.meshing.mesh.inter.IPointConstructor;
import org.vadere.meshing.mesh.triangulation.improver.eikmesh.EikMeshPoint;

import java.util.HashSet;
import java.util.Set;

public class TestUniform {

	VRectangle bbox;
	double initialEdgeLen;

	public TestUniform(VRectangle bbox, double initialEdgeLen) {
		this.bbox = bbox;
		this.initialEdgeLen = initialEdgeLen;
	}

	public static void main(String[] args) {
		new TestUniform(new VRectangle(-11, -11, 11, 11), 0.8).tri();
	}


	private void tri() {
		Set<EikMeshPoint> meshPoints = generateGridPoints(bbox, initialEdgeLen);
		IPointConstructor<VPoint> pointConstructor = (x, y) -> new VPoint(x, y);
		IIncrementalTriangulation tri = IIncrementalTriangulation.createVPTriangulation(bbox);
		System.out.print(meshPoints.size());
		System.out.println("Start inserting");
		DebugGui.setDebugOn(true);
		tri.insert(meshPoints);
		tri.finish();
		System.out.println("finish");
		DebugGui.forceShowAndWait(SimpleTriCanvas.simpleCanvas(tri.getMesh(), bbox));

	}

	/**
	 * Generates the starting points of the algorithm.
	 */
	private Set<EikMeshPoint> generateGridPoints(VRectangle bound, double initialEdgeLen) {
		int elementsInCol = (int) Math.ceil((bound.getWidth()) / initialEdgeLen + 1);
		int elementsInRow = (int) Math.ceil((bound.getHeight()) / (initialEdgeLen * Math.sqrt(3) / 2));
		double startX = bound.getX();
		double startY = bound.getY();
		Set<EikMeshPoint> generatedPoints = new HashSet<>(elementsInRow * elementsInCol);
		double sqrt3 = Math.sqrt(3);

		for (int j = 0; j < elementsInRow; j++) {
			for (int i = 0; i < elementsInCol; i++) {
				EikMeshPoint point;
				if (j != 0 && j % 2 != 0) {
					point = new EikMeshPoint(startX + i * initialEdgeLen + initialEdgeLen / 2, startY + j * initialEdgeLen * sqrt3 / 2, false);
				} else {
					point = new EikMeshPoint(startX + i * initialEdgeLen, startY + j * initialEdgeLen * sqrt3 / 2, false);
				}
				generatedPoints.add(point);
			}
		}

		return generatedPoints;
	}
}
