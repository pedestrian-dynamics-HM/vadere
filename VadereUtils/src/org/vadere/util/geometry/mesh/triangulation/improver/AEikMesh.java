package org.vadere.util.geometry.mesh.triangulation.improver;

import org.jetbrains.annotations.NotNull;
import org.vadere.util.geometry.mesh.gen.AFace;
import org.vadere.util.geometry.mesh.gen.AHalfEdge;
import org.vadere.util.geometry.mesh.gen.AMesh;
import org.vadere.util.geometry.mesh.gen.AVertex;
import org.vadere.util.geometry.shapes.VPolygon;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.math.IDistanceFunction;
import org.vadere.util.geometry.mesh.triangulation.adaptive.IEdgeLengthFunction;

import java.util.Collection;

/**
 * @author Benedikt Zoennchen
 */
public class AEikMesh extends EikMesh<EikMeshPoint, AVertex<EikMeshPoint>, AHalfEdge<EikMeshPoint>, AFace<EikMeshPoint>> {
    public AEikMesh(
            @NotNull IDistanceFunction distanceFunc,
            @NotNull IEdgeLengthFunction edgeLengthFunc,
            double initialEdgeLen,
            @NotNull VRectangle bound,
            @NotNull Collection<? extends VShape> obstacleShapes) {
        super(distanceFunc, edgeLengthFunc, initialEdgeLen, bound, obstacleShapes,
		        () -> new AMesh<>((x, y) -> new EikMeshPoint(x, y, false)));
    }

	public AEikMesh(
			@NotNull VPolygon polygon,
			double initialEdgeLen,
			@NotNull Collection<? extends VShape> obstacleShapes) {
		super(polygon, initialEdgeLen, obstacleShapes,
				() -> new AMesh<>((x, y) -> new EikMeshPoint(x, y, false)));
	}
}
