package org.vadere.util.triangulation;

import org.vadere.util.geometry.shapes.IPoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class UniformTriangulation<P extends IPoint> extends IncrementalTriangulation<P> {

	private double left;
	private double top;
	private double width;
	private double height;
	private double minTriangleSideLength;
	private IPointConstructor<P> pointConstructor;

	public UniformTriangulation(final double minX,
	                            final double minY,
	                            final double width,
	                            final double height,
	                            final double minTriangleSideLength,
	                            final IPointConstructor<P> pointConstructor) {
		super(minX, minY, width, height, pointConstructor);
		this.left = minX;
		this.top = minY;
		this.width = width;
		this.height = height;
		this.minTriangleSideLength = minTriangleSideLength;
		this.pointConstructor = pointConstructor;
	}

	@Override
	public void compute() {
		List<P> pointList = new ArrayList<>(generatePointSet());
		Collections.shuffle(pointList);

		for(P point : pointList) {
			insert(point);
		}

		super.compute();
	}

	private Set<P> generatePointSet() {
		// height of a triangle with 60 deg everywhere
		Set<P> pointSet = new HashSet<>();
		double s = minTriangleSideLength;
		double h = minTriangleSideLength * Math.sqrt(3) / 2.0;
		// create stencil with four triangle which can be used to triangulate
		// the whole rectangle seamlessly
		P add1 = pointConstructor.create(-s / 2, h);
		P add2 = pointConstructor.create(s / 2, h);
		P add3 = pointConstructor.create(s, 0);
		P add4 = pointConstructor.create(0, 2 * h);
		P add5 = pointConstructor.create(s, 2 * h);

		for (int row = 0; row < (int) Math.ceil(height / h) + 1; row += 2) {
			for (int col = 0; col < (int) Math.ceil(width
					/ minTriangleSideLength); col++) {
				P p1 = pointConstructor.create(left + col * minTriangleSideLength, top + row * h);

				P p2 = pointConstructor.create(p1.getX() + add1.getX(), p1.getY() + add1.getY());
				P p3 = pointConstructor.create(p1.getX() + add2.getX(), p1.getY() + add2.getY());
				P p4 = pointConstructor.create(p1.getX() + add3.getX(), p1.getY() + add3.getY());
				P p5 = pointConstructor.create(p1.getX() + add4.getX(), p1.getY() + add4.getY());
				P p6 = pointConstructor.create(p1.getX() + add5.getX(), p1.getY() + add5.getY());

				pointSet.add(p1);
				pointSet.add(p2);
				pointSet.add(p3);
				pointSet.add(p4);
				pointSet.add(p5);
				pointSet.add(p6);
			}
		}
		return pointSet;
	}
}
