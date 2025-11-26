package org.vadere.meshing.mesh.inter.meshConnectivity;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vadere.meshing.mesh.gen.pointLocator.DelaunayHierarchyPointLocator;
import org.vadere.meshing.mesh.gen.GenEar;
import org.vadere.meshing.mesh.inter.mesh.*;
import org.vadere.meshing.mesh.inter.mesh.builder.IMeshBuilder;
import org.vadere.meshing.mesh.inter.mesh.builder.IMeshBuilderEdges;
import org.vadere.meshing.mesh.inter.mesh.builder.IMeshBuilderFaces;
import org.vadere.meshing.mesh.inter.mesh.builder.IMeshBuilderVertices;
import org.vadere.meshing.mesh.inter.mesh.triangle.ITriangleMesh;
import org.vadere.util.data.Node;
import org.vadere.util.data.NodeLinkedList;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VTriangle;
import org.vadere.util.logging.Logger;
import org.vadere.util.math.IDistanceFunction;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * <p>A tri-connectivity {@link ITriConnectivity} is the connectivity of a mesh of non-intersecting connected triangles including holes.
 * A hole can be an arbitrary simple polygon. So it is more concrete than a poly-connectivity {@link IPolyConnectivity}.
 * The mesh {@link IMesh} stores all the date of the base elements (vertices {@link V}, half-edges {@link E}
 * and faces {@link F}) and offers factory method to create new base elements.
 * The connectivities, i.e. {@link IPolyConnectivity} and {@link ITriConnectivity} offers all the operations manipulating
 * the connectivity of the mesh. The connectivity is the relation between vertices and edges which define faces which therefore define the mesh structure.</p>
 *
 * <p>We say a mesh represents a valid triangulation or a triangulation is valid if and only if all triangle-faces are counter-clockwise oriented.</p>
 *
 * <p>We say a mesh represents a feasible triangulation or a triangulation is feasible if and only if all triangle-faces are legal, i.e. all half-edges are legal.
 * The certificate for an edge to be legal relies on the concrete implementation of the triangulation. E. g. for a strict Delaunay-Triangulation the Delaunay-Criterion
 * has to be fulfilled.</p>
 *
 * @param <V> the type of the vertices
 * @param <E> the type of the half-edges
 * @param <F> the type of the faces
 *
 * @author Benedikt Zoennchen
 */
public interface ITriConnectivity<V extends IVertex, E extends IHalfEdge, F extends IFace> extends IPolyConnectivity<V, E, F> {
	/**
	 * A Random number generator to randomly walk through the trinagulation.
	 */
	Random random = new Random();

	/**
	 * A flag to activate and deactivate the debug mode.
	 */
	boolean debug = false;

	/**
	 * A logger to debug some code.
	 */
	Logger log = Logger.getLogger(IPolyConnectivity.class);

	/**
	 * <p>This will replace the point of a vertex. If the point has other coordinates than
	 * the old point of the vertex this will reposition the vertex without any checks, i.e.
	 * the user has to know what he does and has to make sure that the mesh is valid and feasible
	 * afterwards and all listeners e.g. the point locators such as the Delaunay-Hierarchy
	 * {@link DelaunayHierarchyPointLocator} can handle this
	 * repositioning!</p>
	 *
	 * <p>Does not change the connectivity but may change the position of a vertex and therefore requires
	 * connectivity changes which has to be made manually!</p>
	 *
	 * @param vertex    the vertex
	 * @param point     the new point of the vertex
	 */
	void replacePoint(@NotNull final V vertex, @NotNull final IPoint point);

	/**
	 * Set predicates to be evaluated when isIllegal is called
	 * @param isIllegalPredicate
	 * @param isIllegalPredicateWithEps
	 */
	void setIsIllegalPredicate(Predicate<Pair<E, V>> isIllegalPredicate, Predicate<Triple<E, V, Double>> isIllegalPredicateWithEps);

	/**
	 * <p>Tests whether an edge is illegal and should be flipped.</p>
	 *
	 * <p>Does not change the connectivity.</p>
	 *
	 * @param edge  the edge which will be tested
	 * @param p     the point, i.e. point(next(edge))
	 * @return true if the edge is illega, false otherwise
	 */
	boolean isIllegal(@NotNull final E edge, @NotNull final V p);

	boolean isIllegal(@NotNull final E edge, @NotNull final V p, final double eps);

	/**
	 * <p>Tests whether an edge is illegal and should be flipped.</p>
	 *
	 * <p>Does not change the connectivity.</p>
	 *
	 * @param edge  the edge which will be tested
	 * @return true if the edge is illega, false otherwise
	 */
	boolean isIllegal(@NotNull final E edge);

	boolean isIllegal(@NotNull final E edge, final double eps);

	boolean isDelaunayIllegal(@NotNull final E edge);

	boolean isDelaunayIllegal(@NotNull final E edge, @NotNull final V p, final double eps);

	default boolean isDelaunayIllegal(@NotNull final E edge, @NotNull final V p) {
		return isDelaunayIllegal(edge, p, 0.0);
	}

	Pair<E, E> splitEdge(@NotNull V v, @NotNull E halfEdge, boolean legalize);

	/**
	 * <p>Splits the half-edge at point p, which means two triangles will be split into four if
	 * the edge is not a boundary edge otherwise only one triangle will be split into two.</p>
	 *
	 * <p>Assumption: p is located on the edge!</p>
	 *
	 * <p>Mesh changing method.</p>
	 *
	 * @param p         the split point
	 * @param halfEdge  the half-edge which will be split
	 * @param legalize  if true the split will be legalized i.e. the mesh will be locally changed until it is legal
	 * @return one (the halfEdge is a boundary edge) or two halfEdges such that the set of faces of these
     *         edges and their twins are the faces which took part / where modified / added by the split.
     */
	Pair<E, E> splitEdge(@NotNull IPoint p, @NotNull E halfEdge, boolean legalize);

	List<E> splitEdgeAndReturn(@NotNull final V v, @NotNull E halfEdge, boolean legalize);

	/**
	 * <p>Splits the half-edge at point p, which means two triangles will be split into four if
	 * the edge is not a boundary edge otherwise only one triangle will be split into two.
	 * Afterwards the mesh is legalized locally, to preserve a feasible triangulation.</p>
	 *
	 * <p>Assumption: p is located on the edge!</p>
	 *
	 * <p>Mesh changing method.</p>
	 *
	 * @param p         the split point
	 * @param halfEdge  the half-edge which will be split
	 * @return one (the halfEdge is a boundary edge) or two halfEdges such that the set of faces of these
	 *         edges and their twins are the faces which took part / where modified / added by the split.
	 */
	default Pair<E, E> splitEdge(@NotNull final IPoint p, @NotNull final E halfEdge) {
		return splitEdge(p, halfEdge, true);
	}

	/**
	 * <p>Splits the half-edge at the mid point of its full-edge, which means two triangles will be split into four if
	 * the edge is not a boundary edge otherwise only one triangle will be split into two.</p>
	 *
	 * <p>Assumption: p is located on the edge!</p>
	 *
	 * <p>Mesh changing method.</p>
	 *
	 * @param halfEdge  the half-edge which will be split
	 * @param legalize  if true the split will be legalized i.e. the mesh will be locally changed until it is legal
	 * @return one (the halfEdge is a boundary edge) or two halfEdges such that the set of faces of these
	 *         edges and their twins are the faces which took part / where modified / added by the split.
	 */
	default Pair<E, E> splitEdge(@NotNull final E halfEdge, final boolean legalize) {
		return splitEdge(halfEdge, legalize, p -> {});
	}

	/**
	 * <p>Splits the half-edge at the mid point of its full-edge, which means two triangles will be split into four if
	 * the edge is not a boundary edge otherwise only one triangle will be split into two.</p>
	 *
	 * <p>Assumption: p is located on the edge!</p>
	 *
	 * <p>Mesh changing method.</p>
	 *
	 * @param halfEdge          the half-edge which will be split
	 * @param legalize          if true the split will be legalized i.e. the mesh will be locally changed until it is legal
	 * @return one (the halfEdge is a boundary edge) or two halfEdges such that the set of faces of these
	 *         edges and their twins are the faces which took part / where modified / added by the split.
	 *         Both edges ending in the inserted vertex i.e. getVertex(edge) returns the inserted vertex.
	 */
	Pair<E, E> splitEdge(@NotNull final E halfEdge, final boolean legalize, @NotNull final Consumer<V> action);

	/**
	 * <p>A synchronized version of {@link ITriConnectivity#flip(IHalfEdge)}, i.e. the method acquires every
	 * involved vertex (four vertices) before it flips the edge.</p>
	 *
	 * <p>Mesh changing method.</p>
	 *
	 * @param edge the edge which will be flipped.
	 */
	void flipSync(@NotNull final E edge);

	/**
	 * <p>Flips an edge in the triangulation assuming the egdge which will be created is not jet there.</p>
	 *
	 * <p>Mesh changing method.</p>
	 *
	 * @param edge the edge which will be flipped.
	 */
	void flip(@NotNull final E edge) ;

	E splitTriangle(@NotNull final F face, final boolean legalize);

	//TODO test it
	void removeBoundaryVertex(@NotNull final V vertex);

	/**
	 * Removes a non-boundary vertex from the triangulation by removing the point and re-triangulating the hole
	 * using the algorithm described in: On Deletion in Delaunay Triangulations.
	 *
	 * Assumption: the vertex is not at a boundary
	 *
	 * @param vertex the vertex which will be removed
	 */
	void removeNonBoundaryVertex(@NotNull final V vertex);

	/**
	 * Removes a vertex from the triangulation by removing the point and re-triangulating the hole
	 * using the algorithm described in: On Deletion in Delaunay Triangulations.
	 *
	 * @param vertex the vertex which will be removed
	 */
	void remove(@NotNull final V vertex);

	/**
	 * <p>Splits the triangle xyz into three new triangles xyp, yzp and zxp. This requires amortized O(1) time.</p>
	 *
	 * <p>Assumption: p is inside the face.</p>
	 *
	 * <p>Mesh changing method.</p>
	 *
	 * @param face      the triangle face we split
	 * @param point     the point which splits the triangle
	 * @param legalize  if true the triangulation will be legalized locally at the split to preserve a feasible triangulation
	 *
	 * @return an half-edge which has point as its end-point
	 */
	E splitTriangle(@NotNull F face, @NotNull final IPoint point, boolean legalize);

	E splitTriangle(@NotNull F face, @NotNull final V p, boolean legalize);

	/**
	 * <p>Splits the triangle xyz into three new triangles xyp, yzp and zxp and legalizes all possibly illegal edges locally,
	 * which preserves a legal triangulation. This requires amortized O(1) time.</p>
	 *
	 * <p>Assumption: p is contained in the face.</p>
	 *
	 * <p>Mesh changing method.</p>
	 *
	 * @param face      the triangle face we split
	 *
	 * @param p         the point which splits the triangle
	 * @return a list of all newly created face.
	 */
	default E splitTriangle(@NotNull final F face, @NotNull final IPoint p) {
		return splitTriangle(face, p, true);
	}

	V collapseEdge(@NotNull final E edge, final boolean deleteIsolatededVertex);

	/**
	 * <p>This method collapses a three degree vertex which is at the boundary by removing the
	 * one edge (a simple link) which is not a boundary edge and by merging the two other boundary edges,
	 * i.e. two triangles will become one and the vertex will be deleted. This requires O(1) time
	 * since we assume a triangulation.</p>
	 *
	 * <p>Assumption: vertex is a three degree vertex, the edge ends in vertex, the edge is a non-boundary edge,
	 * the two other half-edges ending in the vertex are boundary edges</p>
	 *
	 * <p>Mesh changing method.</p>
	 *
	 * @param vertex                    the vertex where the edge ends
	 * @param deleteIsolatededVertex    if true the vertex will be removed from the mesh data structure.
	 */
	 void collapse3DVertex(@NotNull final V vertex, final boolean deleteIsolatededVertex);

	/**
	 * <p>This method collapses a four degree vertex which is not at the boundary</p>
	 *
	 * @param vertex
	 * @param deleteIsolatededVertex
	 */
	void collapse4DVertex(@NotNull final V vertex, final boolean deleteIsolatededVertex);

	/**
	 * <p>Removes a two degree vertex by removing its two collapsing its two neighbouring edges which
	 * will remove two half-edges which is one full-edge in O(1)</p>
	 *
	 * <p>Assumption: the veterx is of degree equals two.</p>
	 *
	 * <p>Mesh changing method.</p>
	 *
	 * @param vertex                    the 2-degree vertex which will be removed
	 * @param deleteIsolatededVertex    if true the vertex will be removed from the mesh data structure
	 */
	E remove2DVertex(@NotNull final V vertex, final boolean deleteIsolatededVertex);

	/**
	 * <p>Creates a new face by connecting two boundary vertices v1, v3 of a boundary path v1 to v2 to v3 such that
	 * v1 to v2 to v3 becomes a new face. This requires O(1) time.</p>
	 *
	 * Assumption:
	 * <ul>
	 *     <li>there is an counter clockwise angle3D smaller than 180 (PI) at v2 of the triangle (v1,v2,v3)</li>
	 *     <li>the boundaryEdge is a boundary edge</li>
	 * </ul>
	 *
	 * <p>Mesh changing method.</p>
	 *
	 * @param boundaryEdge an edge of the boundary (i.e. part of the border or a hole).
	 */
	F createFaceAtBoundary(@NotNull final E boundaryEdge);

	/**
	 * <p>Legalizes an edge xy of a triangle xyz if it is illegal / not feasible by flipping it.
	 * This requires amortized O(1) time.</p>
	 *
	 * <p>Mesh changing method.</p>
	 *
	 * @param edge  an edge zx of a triangle xyz
	 * @param p     point(next(edge))
	 */
	void legalizeNonRecursive(@NotNull final E edge, @NotNull final V p);

	/**
	 * <p>Legalizes an edge xy of a triangle xyz if it is illegal / not feasible by flipping it.
	 * This requires amortized O(1) time.</p>
	 *
	 * <p>Mesh changing method.</p>
	 *
	 * @param edge  an edge zx of a triangle xyz
	 * @param p     point(next(edge))
	 */
	default void legalize(@NotNull  final E edge, @NotNull final V p) {
		legalizeNonRecursive(edge, p);
	}

	void legalize(@NotNull  final E edge);

	/**
	 * This method is a plausibility assertion-test. It tests if
	 * <ol>
	 *  <li>the edge is not a boundary edge</li>
	 *  <li>the vertex of the next of its twin is not equal to any vertex of the neighbouring edges of its next.</li>
	 * </ol>
	 * This method requires O(d) time where d is the degree of the involved vertices and should only be used for assertions.
	 *
	 * <p>Does not change the connectivity.</p>
	 *
	 * @param halfEdge the half-edge that might be flipped
	 * @return true if the plausibility assertion-test is true.
	 */
	boolean isFlipOkAssertion(@NotNull final E halfEdge);

	/**
	 * <p>Connects each (current) two consecutive border edge if they form an acute angle3D
	 * which smoothes the border of the mesh overall. This requires O(n) time, where n
	 * is the number of border edges.</p>
	 *
	 * <p>Mesh changing method.</p>
	 *
	 */
	void smoothBorder();

	void smoothHoles(@NotNull final IDistanceFunction distanceFunction, Predicate<V> isBoundary);

	/**
	 * <p>Connects each (current) two consecutive border edge if they form an acute angle3D
	 * which smoothes the border of the mesh overall. This requires O(n) time, where n
	 * is the number of border edges.</p>
	 *
	 * <p>Mesh changing method.</p>
	 *
	 */
	default void smoothHoles(@NotNull final IDistanceFunction distanceFunction) {
		smoothBorder(distanceFunction, v -> false);
	}

	default void smoothBoundary(@NotNull final IDistanceFunction distanceFunction, @NotNull final Predicate<V> predicate) {
		smoothBorder(distanceFunction, predicate);
		smoothHoles(distanceFunction, predicate);
	}

	default void smoothBoundary(@NotNull final IDistanceFunction distanceFunction) {
		smoothBorder(distanceFunction);
		smoothHoles(distanceFunction);
	}

	default void smoothBoundary() {
		smoothBorder(null);
		smoothHoles(null);
	}

	default void collapseBoundaryFaces(@NotNull final Predicate<F> collapsePredicate, @NotNull final Predicate<E> edgeCollapsePredicate, @NotNull final Consumer<V> action) {
		collapseBorderFaces(collapsePredicate, edgeCollapsePredicate, action);
		collapseHoleFaces(collapsePredicate, edgeCollapsePredicate, action);
	}

	void collapseHoleFaces(@NotNull final Predicate<F> collapsePredicate, @NotNull final Predicate<E> edgeCollapsePredicate, @NotNull final Consumer<V> action);

	void collapseBorderFaces(@NotNull final Predicate<F> collapsePredicate, @NotNull final Predicate<E> edgeCollapsePredicate, @NotNull final Consumer<V> action);

	void smoothBorder(@Nullable final IDistanceFunction distanceFunction, @NotNull final Predicate<V> isBoundary);

	void smoothBorder(@NotNull final IDistanceFunction distanceFunction);
}
