package org.vadere.meshing.mesh.inter;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.gen.PMeshSuppliert;
import org.vadere.meshing.mesh.impl.DataPoint;
import org.vadere.meshing.mesh.impl.PMeshPanel;
import org.vadere.meshing.mesh.impl.PSLG;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VPolygon;
import org.vadere.util.geometry.shapes.VTriangle;
import org.vadere.util.math.IDistanceFunction;
import org.vadere.util.math.InterpolationUtil;

import java.util.Collection;

public interface IMeshDistanceFunction extends IDistanceFunction {

	static IDistanceFunction createDistanceFunction(@NotNull final PSLG pslg) {

		// (1) construct the exact distance function
		IDistanceFunction distanceFunction = IDistanceFunction.create(pslg.getSegmentBound(), pslg.getHoles());

		// (2) construct a background mesh
		PMeshSuppliert<DoubleDataPoint, Double, MarkedTriangle> meshSuppliert = new PMeshSuppliert<>((x, y) -> new DoubleDataPoint(x ,y));

		final var backgroundGrid = IIncrementalTriangulation.createBackGroundMesh(meshSuppliert, pslg, false);
		final var mesh = backgroundGrid.getMesh();

		// (3) set distance values for each background vertex
		for(var point : mesh.getPoints()) {
			point.setData(distanceFunction.apply(point));
		}

		// (4) pre-compute triangles to accelerate interpolation
		for(var face : mesh.getFaces()) {
			if(!mesh.isBoundary(face)) {
				VTriangle triangle = mesh.toTriangle(face);
				boolean inside = pslg.getHoles().stream().allMatch(polygon -> !polygon.contains(triangle.midPoint()));
				mesh.setData(face, new MarkedTriangle(triangle, inside));
			}
		}

		// TOODO: remove this
		var panel = new PMeshPanel<>(mesh, 1000, 1000);
		panel.display("dist func.");

		// (4) construct a distance function based on the background mesh
		IDistanceFunction approxDistance = p -> {

			// (4.1) locate the face containing the point
			var optFace = backgroundGrid.locateFace(p.getX(), p.getY());
			var face = optFace.get();

			// point lies outside the boundary
			if(mesh.isBoundary(face)) {
				return pslg.getSegmentBound().distance(p);
			} else {
				MarkedTriangle markedTriangle = mesh.getData(face).get();
				VTriangle triangle = markedTriangle.triangle;
				DoubleDataPoint[] points = backgroundGrid.getPoints(face, DoubleDataPoint.class);
				double distance = InterpolationUtil.barycentricInterpolation(points[0], points[1], points[2], dataPoint -> dataPoint.getData(), triangle.getArea(), p.getX(), p.getY());
				if(markedTriangle.inside) {

				}
				return InterpolationUtil.barycentricInterpolation(points[0], points[1], points[2], dataPoint -> dataPoint.getData(), triangle.getArea(), p.getX(), p.getY());
			}
		};
		return approxDistance;
	}

	class MarkedTriangle {
		public VTriangle triangle;
		public boolean inside;

		public MarkedTriangle(VTriangle triangle, boolean inside) {
			this.triangle = triangle;
			this.inside = inside;
		}
	}

	class DoubleDataPoint extends DataPoint<Double> {
		public DoubleDataPoint(final double x, final double y) {
			super(x, y);
		}
		public DoubleDataPoint(@NotNull final IPoint point) {
			super(point);
		}

		@Override
		public String toString() {
			return super.toString();
		}

		@Override
		public boolean equals(Object obj) {
			return super.equals(obj);
		}
	}

}
