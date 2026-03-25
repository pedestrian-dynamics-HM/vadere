package org.vadere.meshing.mesh.triangulation.edgeLengthFunctions;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.inter.mesh.IFace;
import org.vadere.meshing.mesh.inter.mesh.IHalfEdge;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.meshing.mesh.inter.mesh.IVertex;
import org.vadere.meshing.mesh.inter.mesh.triangle.ITriangleMeshEdges;
import org.vadere.meshing.mesh.inter.mesh.triangle.ITriangleMeshFaces;
import org.vadere.meshing.mesh.inter.mesh.triangle.ITriangleMeshVertices;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.math.InterpolationUtil;

public class EdgeLengthFunctionTri<V extends IVertex, E extends IHalfEdge, F extends IFace> implements IEdgeLengthFunction {
	private final ITriangleMeshEdges<V, E, F> edges;
	private final ITriangleMeshFaces<V, E, F> faces;
	private final ITriangleMeshVertices<V, E, F> vertices;
	private IIncrementalTriangulation<V, E, F> triangulation;

	public EdgeLengthFunctionTri(
			@NotNull final IIncrementalTriangulation<V, E, F> triangulation) {

		this.triangulation = triangulation;
		this.edges = this.triangulation.getMesh().edges();
		this.faces = this.triangulation.getMesh().faces();
		this.vertices = this.triangulation.getMesh().vertices();

		// compute and set the local feature size
		var vertices = triangulation.getMesh().vertices().getAll();
		var meshWithDatastorage = triangulation.getMeshBuilder();
		for(var v : vertices) {
			double minEdgeLen = Double.MAX_VALUE;
			for(var e : meshWithDatastorage.getMesh().edges().getAllOf(v)) {
				if(!meshWithDatastorage.getDataStorage().getBooleanData(meshWithDatastorage.getMesh().faces().getOf(e), "boundary")
						|| !meshWithDatastorage.getDataStorage().getBooleanData(meshWithDatastorage.getMesh().faces().getTwin(e), "boundary")) {
					var u = meshWithDatastorage.getMesh().vertices().getTwin(e);
					double len = v.distance(u);
					if(len < minEdgeLen) {
						minEdgeLen = len;
					}
				}
			}

			triangulation.getMeshBuilder().getDataStorage().setDoubleData(v, propName, minEdgeLen);
		}
	}

	public void smooth(double g) {
		assert g > 0;
		smooth(g, triangulation);
	}

	@Override
	public Double apply(IPoint p) {
		var face = triangulation.locateFace(new VPoint(p.getX(), p.getY())).get();

		if(faces.isBoundary(face)) {
			double dist = Double.MAX_VALUE;
			E edge = null;
			for (E e : edges.iterableFor(face)) {
				V v1 = vertices.getEndOf(e);
				V v2 = vertices.getTwin(e);

				double d = GeometryUtils.distanceToLineSegment(
						vertices.getX(v1), vertices.getY(v1),
						vertices.getX(v2), vertices.getY(v2),
						p.getX(), p.getY()
						);
				if(edge == null || d < dist) {
					edge = e;
					dist = d;
				}
			}

			V v1 = vertices.getEndOf(edge);
			V v2 = vertices.getTwin(edge);

			return (triangulation.getMeshDataStorage().getDoubleData(v1, propName) + triangulation.getMeshDataStorage().getDoubleData(v2, propName)) / 2.0;
		}
		else {
			double x[] = new double[3];
			double y[] = new double[3];
			double z[] = new double[3];

			triangulation.getMesh().readConnectivity().getTriPoints(face, x, y, z, propName, triangulation.getMeshDataStorage());

			double totalArea = GeometryUtils.areaOfPolygon(x, y);

			return InterpolationUtil.barycentricInterpolation(x, y, z, totalArea, p.getX(), p.getY());
		}
	}
}
