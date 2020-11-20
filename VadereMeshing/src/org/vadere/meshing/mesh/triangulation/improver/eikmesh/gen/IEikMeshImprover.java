package org.vadere.meshing.mesh.triangulation.improver.eikmesh.gen;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.IllegalMeshException;
import org.vadere.meshing.mesh.inter.IFace;
import org.vadere.meshing.mesh.inter.IHalfEdge;
import org.vadere.meshing.mesh.inter.IMesh;
import org.vadere.meshing.mesh.inter.IVertex;
import org.vadere.meshing.mesh.triangulation.improver.IMeshImprover;
import org.vadere.meshing.mesh.triangulation.improver.distmesh.Parameters;
import org.vadere.meshing.mesh.triangulation.improver.eikmesh.EikMeshPoint;
import org.vadere.meshing.mesh.triangulation.triangulator.inter.ITriangulator;
import org.vadere.util.math.IDistanceFunction;

import java.util.function.Predicate;

public interface IEikMeshImprover<V extends IVertex, E extends IHalfEdge, F extends IFace> extends IMeshImprover<V, E, F>, ITriangulator<V, E, F> {

	/**
	 * Returns the mesh the improver is working on.
	 *
	 * @return the mesh the improver is working on.
	 */
	default IMesh<V,E,F> getMesh() {
		return getTriangulation().getMesh();
	}

	default Predicate<F> outsidePredicate(@NotNull final IDistanceFunction distanceFunc) {
		return f -> distanceFunc.apply(getMesh().toMidpoint(f)) > 0;
	}

	default Predicate<F> lowQualityPredicate() {
		return f -> faceToQuality(f) < Parameters.MIN_TRIANGLE_QUALITY;
	}

	/**
	 * Removes / merges {@link F} faces of the mesh by a virus algorithm which starts at each hole and consumes faces as long as
	 * the face is outside which is defined by the <tt>distanceFunc</tt>. The virus consumes faces as long as they are outside
	 * and therefore expanding each hole. The method should not change the number of holes, i.e. should not merge two holes
	 * or the border and a hole.
	 *
	 * This takes O(n) time where n is the number of faces which will be consumed.
	 *
	 * Assumptions: Each pair of holes and each hole and the border are well separated by enough faces such that none pair will be merged together.
	 *
	 * @param distanceFunc          the distance function which defines inside and outside
	 * @throws IllegalMeshException if two holes or the border and a hole collide, i.e. there are no more faces between them and
	 *                              therefore the mesh becomes illegal
	 */
	default void removeFacesInsideHoles(@NotNull final IDistanceFunction distanceFunc) throws IllegalMeshException {
		Predicate<F> isBoundary = f -> !getMesh().isBoundary(f);
		Predicate<F> isOutside = outsidePredicate(distanceFunc);

		for(F face : getMesh().getHoles()) {
			getTriangulation().mergeFaces(face, isOutside, isBoundary, true);
		}
	}

	/**
	 * Removes / merges {@link F} faces of the mesh by a virus algorithm which starts at the border and consumes faces as long as
	 * the face is outside which is defined by the <tt>distanceFunc</tt>. The virus consumes faces as long as they are outside
	 * and therefore expanding the border, that is shrinking the meshed area.
	 *
	 * @param distanceFunc the distance function which defines inside and outside
	 */
	default void removeFacesOutsideBBox(@NotNull final IDistanceFunction distanceFunc) {
		getTriangulation().shrinkBorder(outsidePredicate(distanceFunc), true);
	}

	/**
	 * Removes / merges {@link F} faces of the mesh by a virus algorithm which starts at each boundary and consumes faces as long as
	 * the face is outside which is defined by the <tt>distanceFunc</tt>. The virus consumes faces as long as they are outside
	 * and therefore expanding each hole and shrinking the boundary. Overall the meshed area will be reduced.
	 *
	 * Assumption: For each face which is outside there is a hole (or the border) and a chain of faces starting from that hole (or border)
	 *             and leading to that face. All those faces of the chain have to be outside as well. Otherwise a face which is outside
	 *             might not be removed!
	 *
	 * @param distanceFunc          the distance function which defines inside and outside
	 * @throws IllegalMeshException if two holes or the border and a hole collide, i.e. there are no more faces between them and
	 *                              therefore the mesh becomes illegal
	 */
	default void removeFacesOutside(@NotNull final IDistanceFunction distanceFunc) throws IllegalMeshException {
		removeFacesInsideHoles(distanceFunc);
		removeFacesOutsideBBox(distanceFunc);
	}

	/**
	 * Removes / merges {@link F} faces of the mesh by a virus algorithm which consumes faces as long as
	 * the face is outside which is defined by the <tt>distanceFunc</tt>. Since all faces of the mesh
	 * are considered this is more expansive than {@link IEikMeshImprover#removeFacesOutside}.
	 *
	 * This takes O(n) time where n is the number of faces which will be consumed.
	 *
	 * @param distanceFunc the distance function which defines inside and outside
	 */
	default void removeAllFacesOutside(@NotNull final IDistanceFunction distanceFunc) {
		for(F face : getMesh().getFaces()) {
			if(!getTriangulation().getMesh().isDestroyed(face) && !getTriangulation().getMesh().isHole(face)) {
				getTriangulation().createHole(face, outsidePredicate(distanceFunc), true);
			}
		}
	}

	/**
	 * Removes / merges {@link F} faces which are directly neighbouring a hole and are of low quality.
	 *
	 * @throws IllegalMeshException if two holes or the border and a hole collide, i.e. there are no more faces between them and
	 *                              therefore the mesh becomes illegal
	 */
	default void removeLowQualityFacesAtHoles() throws IllegalMeshException {
		Predicate<F> isBoundary = f -> !getMesh().isBoundary(f);
		Predicate<F> isOfLowQuality = lowQualityPredicate();

		for(F face : getMesh().getHoles()) {
			getTriangulation().mergeFaces(face, isOfLowQuality, isBoundary, true, 1);
		}
	}

	/**
	 * Removes / merges {@link F} faces which are directly neighbouring the border and are of low quality.
	 *
	 * @throws IllegalMeshException if two holes or the border and a hole collide, i.e. there are no more faces between them and
	 *                              therefore the mesh becomes illegal
	 */
	default void removeLowQualityFacesAtBorder() throws IllegalMeshException {
		Predicate<F> isBoundary = f -> !getMesh().isBoundary(f);
		Predicate<F> isOfLowQuality = lowQualityPredicate();
		getTriangulation().mergeFaces(getMesh().getBorder(), isOfLowQuality, isBoundary, true, 1);
	}

	/**
	 * Removes / merges {@link F} faces which are directly neighbouring the border or a hole and are of low quality.
	 *
	 * @throws IllegalMeshException if two holes or the border and a hole collide, i.e. there are no more faces between them and
	 *                              therefore the mesh becomes illegal
	 */
	default void removeLowQualityFaces() throws IllegalMeshException {
		removeLowQualityFacesAtBorder();
		removeLowQualityFacesAtHoles();
	}
}
