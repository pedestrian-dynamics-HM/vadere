package org.vadere.geometry.mesh.triangulation.improver;

import org.jetbrains.annotations.NotNull;
import org.vadere.geometry.IDistanceFunction;
import org.vadere.geometry.mesh.gen.*;
import org.vadere.geometry.shapes.VPolygon;
import org.vadere.geometry.shapes.VRectangle;
import org.vadere.geometry.shapes.VShape;
import org.vadere.geometry.mesh.triangulation.adaptive.IEdgeLengthFunction;

import java.util.Collection;

/**
 * @author Benedikt Zoennchen
 */
public class PEikMesh extends EikMesh<EikMeshPoint, PVertex<EikMeshPoint>, PHalfEdge<EikMeshPoint>, PFace<EikMeshPoint>> {

    public PEikMesh(
            @NotNull IDistanceFunction distanceFunc,
            @NotNull IEdgeLengthFunction edgeLengthFunc,
            double initialEdgeLen,
            @NotNull VRectangle bound,
            @NotNull Collection<? extends VShape> obstacleShapes) {

    	super(distanceFunc, edgeLengthFunc, initialEdgeLen, bound, obstacleShapes,
		        () -> new PMesh<>((x, y) -> new EikMeshPoint(x, y, false)));
    }

	public PEikMesh(
			@NotNull VPolygon polygon,
			double initialEdgeLen,
			@NotNull Collection<? extends VShape> obstacleShapes) {
		super(polygon, initialEdgeLen, obstacleShapes,
				() -> new PMesh<>((x, y) -> new EikMeshPoint(x, y, false)));
	}

}
