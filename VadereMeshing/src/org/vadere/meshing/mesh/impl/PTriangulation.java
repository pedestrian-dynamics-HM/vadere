package org.vadere.meshing.mesh.impl;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.gen.IncrementalTriangulation;
import org.vadere.meshing.mesh.gen.PFace;
import org.vadere.meshing.mesh.gen.PHalfEdge;
import org.vadere.meshing.mesh.gen.PMesh;
import org.vadere.meshing.mesh.gen.PVertex;
import org.vadere.meshing.mesh.inter.IPointConstructor;
import org.vadere.meshing.mesh.inter.IPointLocator;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VRectangle;

import java.util.Collection;
import java.util.Set;
import java.util.function.Predicate;

public class PTriangulation<P extends IPoint, CE, CF> extends IncrementalTriangulation<P, CE, CF, PVertex<P, CE, CF>, PHalfEdge<P, CE, CF>, PFace<P, CE, CF>> {

	public PTriangulation(
			@NotNull final Collection<P> points,
			@NotNull final Predicate<PHalfEdge<P, CE, CF>> illegalPredicate,
			@NotNull final IPointConstructor<P> pointConstructor) {
		super(new PMesh<>(pointConstructor), IPointLocator.Type.JUMP_AND_WALK, points, illegalPredicate);
	}

	public PTriangulation(
			@NotNull final Set<P> points,
			@NotNull final IPointConstructor<P> pointConstructor) {
		super(new PMesh<>(pointConstructor), IPointLocator.Type.JUMP_AND_WALK, points);
	}

	public PTriangulation(
			@NotNull final VRectangle bound,
			@NotNull final Predicate<PHalfEdge<P, CE, CF>> illegalPredicate,
			@NotNull final IPointConstructor<P> pointConstructor){
		super(new PMesh<>(pointConstructor), IPointLocator.Type.JUMP_AND_WALK, bound, illegalPredicate);
	}

	public PTriangulation(@NotNull final VRectangle bound,
	                      @NotNull final IPointConstructor<P> pointConstructor) {
		super(new PMesh<>(pointConstructor), IPointLocator.Type.JUMP_AND_WALK, bound);
	}

	public PTriangulation(@NotNull final PMesh<P, CE, CF> mesh) {
		super(mesh);
	}
}
