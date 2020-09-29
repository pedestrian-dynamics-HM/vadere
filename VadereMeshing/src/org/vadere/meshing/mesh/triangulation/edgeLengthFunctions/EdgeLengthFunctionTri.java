package org.vadere.meshing.mesh.triangulation.edgeLengthFunctions;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.inter.IFace;
import org.vadere.meshing.mesh.inter.IHalfEdge;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.meshing.mesh.inter.IVertex;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.math.InterpolationUtil;

public class EdgeLengthFunctionTri<V extends IVertex, E extends IHalfEdge, F extends IFace> implements IEdgeLengthFunction {
	private IIncrementalTriangulation<V, E, F> triangulation;

	public EdgeLengthFunctionTri(
			@NotNull final IIncrementalTriangulation<V, E, F> triangulation) {

		this.triangulation = triangulation;

		// compute and set the local feature size
		var vertices = triangulation.getMesh().getVertices();
		var mesh = triangulation.getMesh();
		for(var v : vertices) {
			double minEdgeLen = Double.MAX_VALUE;
			for(var e : triangulation.getMesh().getEdges(v)) {
				if(!mesh.getBooleanData(mesh.getFace(e), "boundary")
						|| !mesh.getBooleanData(mesh.getTwinFace(e), "boundary")) {
					var u = triangulation.getMesh().getTwinVertex(e);
					double len = v.distance(u);
					if(len < minEdgeLen) {
						minEdgeLen = len;
					}
				}
			}

			triangulation.getMesh().setDoubleData(v, propName, minEdgeLen);
		}
	}

	public void smooth(double g) {
		assert g > 0;
		smooth(g, triangulation);
	}

	@Override
	public Double apply(IPoint p) {
		var face = triangulation.locateFace(new VPoint(p.getX(), p.getY())).get();
		var mesh = triangulation.getMesh();

		if(mesh.isBoundary(face)) {
			double dist = Double.MAX_VALUE;
			E edge = null;
			for (E e : triangulation.getMesh().getEdgeIt(face)) {
				V v1 = triangulation.getMesh().getVertex(e);
				V v2 = triangulation.getMesh().getTwinVertex(e);

				double d = GeometryUtils.distanceToLineSegment(
						triangulation.getMesh().getX(v1), triangulation.getMesh().getY(v1),
						triangulation.getMesh().getX(v2), triangulation.getMesh().getY(v2),
						p.getX(), p.getY()
						);
				if(edge == null || d < dist) {
					edge = e;
					dist = d;
				}
			}

			V v1 = triangulation.getMesh().getVertex(edge);
			V v2 = triangulation.getMesh().getTwinVertex(edge);

			return (triangulation.getMesh().getDoubleData(v1, propName) + triangulation.getMesh().getDoubleData(v2, propName)) / 2.0;
		}
		else {
			double x[] = new double[3];
			double y[] = new double[3];
			double z[] = new double[3];

			triangulation.getTriPoints(face, x, y, z, propName);

			double totalArea = GeometryUtils.areaOfPolygon(x, y);

			return InterpolationUtil.barycentricInterpolation(x, y, z, totalArea, p.getX(), p.getY());
		}
	}
}
