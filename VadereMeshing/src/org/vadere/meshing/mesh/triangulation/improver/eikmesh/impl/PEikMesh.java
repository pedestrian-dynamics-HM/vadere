package org.vadere.meshing.mesh.triangulation.improver.eikmesh.impl;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.triangulation.improver.eikmesh.gen.PEikMeshGen;
import org.vadere.util.math.IDistanceFunction;
import org.vadere.util.geometry.shapes.VPolygon;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.meshing.mesh.triangulation.IEdgeLengthFunction;

import java.util.Collection;

/**
 * @author Benedikt Zoennchen
 */
public class PEikMesh extends PEikMeshGen {

    public PEikMesh(
            @NotNull IDistanceFunction distanceFunc,
            @NotNull IEdgeLengthFunction edgeLengthFunc,
            double initialEdgeLen,
            @NotNull VRectangle bound,
            @NotNull Collection<? extends VShape> obstacleShapes) {

    	super(distanceFunc, edgeLengthFunc, initialEdgeLen, bound, obstacleShapes
	    );
    }

	public PEikMesh(
			@NotNull IDistanceFunction distanceFunc,
			@NotNull IEdgeLengthFunction edgeLengthFunc,
			double initialEdgeLen,
			@NotNull VRectangle bound) {

		super(distanceFunc, edgeLengthFunc, initialEdgeLen, bound
		);
	}

	public PEikMesh(
			@NotNull IDistanceFunction distanceFunc,
			double initialEdgeLen,
			@NotNull VRectangle bound) {

		super(distanceFunc, e -> initialEdgeLen, initialEdgeLen, bound
		);
	}

	public PEikMesh(
			@NotNull VPolygon polygon,
			double initialEdgeLen,
			@NotNull Collection<? extends VShape> obstacleShapes) {
		super(polygon, initialEdgeLen, obstacleShapes
		);
	}
}
