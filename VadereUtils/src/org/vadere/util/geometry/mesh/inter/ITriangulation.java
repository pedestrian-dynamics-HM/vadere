package org.vadere.util.geometry.mesh.inter;

import org.apache.commons.lang3.RandomUtils;
import org.vadere.util.geometry.mesh.impl.PFace;
import org.vadere.util.geometry.mesh.impl.PHalfEdge;
import org.vadere.util.geometry.mesh.impl.PMesh;
import org.vadere.util.geometry.mesh.triangulations.IncrementalTriangulation;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.triangulation.IPointConstructor;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

public interface ITriangulation<P extends IPoint, E extends IHalfEdge<P>, F extends IFace<P>> extends Iterable<F>, ITriConnectivity<P, E, F> {
	void compute();
	Stream<F> streamFaces();
	Set<F> getFaces();
	E insert(final P point);
	void remove(final P point);

	static ITriangulation<VPoint, PHalfEdge<VPoint>, PFace<VPoint>> generateRandomTriangulation(final long numberOfPoints) {

		double min = 0;
		double max = 100;

		Set<VPoint> points = new HashSet<>();

		for(int i = 0; i < numberOfPoints; ++i) {
			double x = RandomUtils.nextDouble(min, max);
			double y = RandomUtils.nextDouble(min, max);

			points.add(new VPoint(x, y));
		}

		IMesh<VPoint, PHalfEdge<VPoint>, PFace<VPoint>> mesh = new PMesh<>(IPointConstructor.pointConstructorVPoint);
		return new IncrementalTriangulation<>(mesh, points);
	}
}
