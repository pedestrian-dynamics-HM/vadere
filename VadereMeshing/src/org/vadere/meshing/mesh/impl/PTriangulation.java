package org.vadere.meshing.mesh.impl;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.gen.IncrementalTriangulation;
import org.vadere.meshing.mesh.gen.PFace;
import org.vadere.meshing.mesh.gen.PHalfEdge;
import org.vadere.meshing.mesh.gen.PMesh;
import org.vadere.meshing.mesh.gen.PVertex;
import org.vadere.meshing.mesh.inter.IPointLocator;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VRectangle;

import java.util.Collection;
import java.util.Set;
import java.util.function.Predicate;

public class PTriangulation extends IncrementalTriangulation<PVertex, PHalfEdge, PFace> {

	public PTriangulation(
			@NotNull final Collection<IPoint> points,
			@NotNull final Predicate<PHalfEdge> illegalPredicate) {
		super(new PMesh(), IPointLocator.Type.JUMP_AND_WALK, points, illegalPredicate);
	}

	public PTriangulation(
			@NotNull final Set<IPoint> points) {
		super(new PMesh(), IPointLocator.Type.JUMP_AND_WALK, points);
	}

	public PTriangulation(
			@NotNull final VRectangle bound,
			@NotNull final Predicate<PHalfEdge> illegalPredicate){
		super(new PMesh(), IPointLocator.Type.JUMP_AND_WALK, bound, illegalPredicate);
	}

	public PTriangulation(@NotNull final VRectangle bound) {
		super(new PMesh(), IPointLocator.Type.JUMP_AND_WALK, bound);
	}

	public PTriangulation(@NotNull final PMesh mesh) {
		super(mesh);
	}
}
