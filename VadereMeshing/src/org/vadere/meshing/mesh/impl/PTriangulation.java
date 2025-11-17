package org.vadere.meshing.mesh.impl;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.gen.IncrementalTriangulation;
import org.vadere.meshing.mesh.gen.mesh.pointerBased.*;
import org.vadere.meshing.mesh.inter.IPointLocator;
import org.vadere.meshing.mesh.inter.mesh.IMesh;
import org.vadere.meshing.mesh.inter.mesh.data.IMeshDataStorage;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VRectangle;

import java.util.Collection;
import java.util.Set;
import java.util.function.Predicate;

public class PTriangulation extends IncrementalTriangulation<PVertex, PHalfEdge, PFace> {

	private PTriangulation(
			@NotNull final Collection<IPoint> points,
			@NotNull final Predicate<PHalfEdge> illegalPredicate) {
		super(PMeshWithDataStorage.constructEmpty(), IPointLocator.Type.JUMP_AND_WALK, points, illegalPredicate);
	}

	public static PTriangulation fromEmptyMesh(@NotNull final Collection<IPoint> points,
											   @NotNull final Predicate<PHalfEdge> illegalPredicate){
		return new PTriangulation(points, illegalPredicate);
	}

	private PTriangulation(
			@NotNull final Collection<IPoint> points) {
		super(PMeshWithDataStorage.constructEmpty(), IPointLocator.Type.JUMP_AND_WALK, points, pHalfEdge ->  true);
	}

	public static PTriangulation fromEmptyMesh(@NotNull final Collection<IPoint> points){
		return new PTriangulation(points);
	}

	public static PTriangulation fromEmptyMesh(
			@NotNull final VRectangle bound,
			@NotNull final Predicate<PHalfEdge> illegalPredicate){
		return new PTriangulation(bound, illegalPredicate);
	}

	private PTriangulation(
			@NotNull final VRectangle bound,
			@NotNull final Predicate<PHalfEdge> illegalPredicate){
		super(PMeshWithDataStorage.constructEmpty(), IPointLocator.Type.JUMP_AND_WALK, bound, illegalPredicate);
	}

	public static PTriangulation fromEmptyMesh(
			@NotNull final IPointLocator.Type type,
			@NotNull final VRectangle bound){
		return new PTriangulation(type, bound);
	}

	public static PTriangulation fromEmptyMesh(
			@NotNull final VRectangle bound){
		return new PTriangulation(IPointLocator.Type.JUMP_AND_WALK, bound);
	}

	private PTriangulation(
									@NotNull final IPointLocator.Type type,
									@NotNull final VRectangle bound) {
		super(PMeshWithDataStorage.constructEmpty(), type, bound, halfEdge -> true);
	}
}
