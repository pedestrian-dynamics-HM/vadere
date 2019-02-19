package org.vadere.meshing.mesh.triangulation.improver.eikmesh.gen;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.gen.PFace;
import org.vadere.meshing.mesh.gen.PHalfEdge;
import org.vadere.meshing.mesh.gen.PVertex;
import org.vadere.meshing.mesh.impl.APMesh;
import org.vadere.meshing.mesh.inter.IPointConstructor;
import org.vadere.meshing.mesh.triangulation.improver.eikmesh.EikMeshPoint;
import org.vadere.util.geometry.shapes.VPolygon;
import org.vadere.util.geometry.shapes.VShape;

import java.util.Collection;

public class APEikMeshGen<P extends EikMeshPoint> extends GenEikMesh<P, Object, Object,
		PVertex<P, Object, Object>, PHalfEdge<P, Object, Object>, PFace<P, Object, Object>> {

	public APEikMeshGen(
			@NotNull VPolygon polygon,
			double initialEdgeLen,
			@NotNull Collection<? extends VShape> obstacleShapes,
			@NotNull IPointConstructor<P> pointConstructor) {
		super(polygon, initialEdgeLen, obstacleShapes, () -> new APMesh<>(pointConstructor));
	}

	@Override
	public APMesh<P> getMesh() {
		return (APMesh<P>)super.getMesh();
	}
}
