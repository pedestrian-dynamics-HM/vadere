package org.vadere.util.triangulation.improver;

import org.jetbrains.annotations.NotNull;
import org.vadere.util.geometry.mesh.gen.*;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.math.IDistanceFunction;
import org.vadere.util.triangulation.adaptive.IEdgeLengthFunction;
import org.vadere.util.triangulation.adaptive.MeshPoint;

import java.util.Collection;

/**
 * @author Benedikt Zoennchen
 */
public class PPSMeshing extends PSMeshing<MeshPoint, PVertex<MeshPoint>, PHalfEdge<MeshPoint>, PFace<MeshPoint>> {

    public PPSMeshing(
            @NotNull IDistanceFunction distanceFunc,
            @NotNull IEdgeLengthFunction edgeLengthFunc,
            double initialEdgeLen,
            @NotNull VRectangle bound,
            @NotNull Collection<? extends VShape> obstacleShapes) {
        super(distanceFunc, edgeLengthFunc, initialEdgeLen, bound, obstacleShapes,
		        () -> new PMesh<>((x, y) -> new MeshPoint(x, y, false)));
    }
}
