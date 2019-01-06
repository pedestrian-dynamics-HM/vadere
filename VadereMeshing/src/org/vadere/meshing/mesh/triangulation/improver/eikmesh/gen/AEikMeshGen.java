package org.vadere.meshing.mesh.triangulation.improver.eikmesh.gen;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.gen.AFace;
import org.vadere.meshing.mesh.gen.AHalfEdge;
import org.vadere.meshing.mesh.gen.AMesh;
import org.vadere.meshing.mesh.gen.AVertex;
import org.vadere.meshing.mesh.inter.IPointConstructor;
import org.vadere.meshing.mesh.triangulation.IEdgeLengthFunction;
import org.vadere.meshing.mesh.triangulation.improver.eikmesh.EikMeshPoint;
import org.vadere.util.geometry.shapes.VPolygon;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.math.IDistanceFunction;

import java.util.Collection;

public class AEikMeshGen<P extends EikMeshPoint, CE, CF> extends EikMesh<P, CE, CF, AVertex<P>, AHalfEdge<CE>, AFace<CF>> {

	public AEikMeshGen(
			@NotNull IDistanceFunction distanceFunc,
			@NotNull IEdgeLengthFunction edgeLengthFunc,
			double initialEdgeLen,
			@NotNull VRectangle bound,
			@NotNull Collection<? extends VShape> obstacleShapes,
			@NotNull IPointConstructor<P> pointConstructor) {

		super(distanceFunc, edgeLengthFunc, initialEdgeLen, bound, obstacleShapes,
				() -> new AMesh<>((x, y) -> pointConstructor.create(x, y)));
	}

	public AEikMeshGen(
			@NotNull IDistanceFunction distanceFunc,
			@NotNull IEdgeLengthFunction edgeLengthFunc,
			double initialEdgeLen,
			@NotNull VRectangle bound,
			@NotNull IPointConstructor<P> pointConstructor) {

		super(distanceFunc, edgeLengthFunc, initialEdgeLen, bound,
				() -> new AMesh<>((x, y) -> pointConstructor.create(x, y)));
	}

	public AEikMeshGen(
			@NotNull IDistanceFunction distanceFunc,
			double initialEdgeLen,
			@NotNull VRectangle bound,
			@NotNull IPointConstructor<P> pointConstructor) {

		super(distanceFunc, e -> 1.0, initialEdgeLen, bound,
				() -> new AMesh<>((x, y) -> pointConstructor.create(x, y)));
	}

	public AEikMeshGen(
			@NotNull VPolygon polygon,
			double initialEdgeLen,
			@NotNull Collection<? extends VShape> obstacleShapes,
			@NotNull IPointConstructor<P> pointConstructor) {
		super(polygon, initialEdgeLen, obstacleShapes,
				() -> new AMesh<>((x, y) -> pointConstructor.create(x, y)));
	}
}