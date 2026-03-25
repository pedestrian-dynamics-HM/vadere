package org.vadere.meshing;

import org.apache.commons.math3.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.gen.mesh.pointerBased.elements.PFace;
import org.vadere.meshing.mesh.gen.mesh.pointerBased.elements.PHalfEdge;
import org.vadere.meshing.mesh.gen.mesh.pointerBased.elements.PVertex;
import org.vadere.meshing.mesh.gen.mesh.pointerBased.triangles.PTriangleMeshBuilder;
import org.vadere.meshing.mesh.inter.mesh.builder.ITriangleMeshBuilder;
import org.vadere.meshing.mesh.inter.meshConnectivity.IPolyConnectivity;
import org.vadere.meshing.mesh.inter.mesh.IMesh;
import org.vadere.meshing.mesh.inter.mesh.data.IMeshDataStorage;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VPolygon;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The Weiler-Atherton-Algorithm (https://en.wikipedia.org/wiki/Weiler%E2%80%93Atherton_clipping_algorithm)
 * enables boolean operations on polygons, i.e.:
 * <ul>
 *     <li>
 *         INTERSECTION of polygon A (subject) and B (clipper).
 *     </li>
 *     <li>
 *         UNION of polygon A (subject) and B (clipper).
 *     </li>
 *     <li>
 *         SUBTRACTION of polygon B (subject) from polygon A (clipper).
 *     </li>
 * </ul>
 * Note that holes are not supported, i.e. if UNION produces a polygon with holes this will be filled.
 * If SUBTRACTION produces a hole an error will be thrown since {@link VPolygon} do not support holes.
 * Furthermore, it is assumed that each {@link VPolygon} which is involved is a simple non-self intersecting
 * polygon. Co-linear and duplicated points, i.e. useless points, will be removed.
 *
 * @author Benedikt Zoennchen
 */
public class WeilerAtherton {

	private List<VPolygon> polygons;
	private final static double  EPSILON = 1.0E-12;
	private static final String propNameIntersection = "intersection";
	private static final String propNameInside = "inside";
	private static final String propNameTwin = "twin";

	private enum Operation {
		INTERSECTION,
		UNION,
		SUBTRACTION
	}


	/**
	 * The default constructor.
	 *
	 * @param polygons a List of {@link VPolygon} which will be merged.
	 */
	public WeilerAtherton(@NotNull final List<VPolygon> polygons) {
		this.polygons = polygons;
	}

	/**
	 * Constructs two faces which represents two polygons including their intersection points.
	 * Each intersection point is connected to his twin (the connection point of the other face
	 * which is geometrically the same point).
	 *
	 * @param subject       the subject polygon
	 * @param subjectMeshBuilder   the subject mesh used to construct the face
	 * @param clipping      the clipping polygon
	 * @param clippingMeshBuilder  the clipping mesh used to constrcut the face
	 *
	 * @return two faces which represents two polygons including their intersection points (in CW order)
	 */
	public Pair<PFace, PFace> constructIntersectionFaces(
			@NotNull final VPolygon subject,
			@NotNull final ITriangleMeshBuilder<PVertex, PHalfEdge, PFace> subjectMeshBuilder,
			@NotNull final VPolygon clipping,
			@NotNull final ITriangleMeshBuilder<PVertex, PHalfEdge, PFace> clippingMeshBuilder) {

		Set<VPoint> clipPointSet = new HashSet<>();
		Set<VPoint> subPointSet = new HashSet<>();

		clipPointSet.addAll(clipping.getPath());
		subPointSet.addAll(subject.getPath());

		List<IPoint> subjectPoints = subject.getPath().stream().map(p->(IPoint)p).collect(Collectors.toList());
		PFace subjectFace = subjectMeshBuilder.faces().createAndInsertFromList(subjectPoints);
		subjectMeshBuilder.getMesh().vertices().stream().forEach(v -> {
			subjectMeshBuilder.getDataStorage().setData(v, propNameIntersection, false);
			subjectMeshBuilder.getDataStorage().setData(v, propNameInside, clipPointSet.contains(v) || clipping.contains(v));
		});

		List<IPoint> clippingPoints = clipping.getPath().stream().map(p->(IPoint)p).collect(Collectors.toList());
		PFace clippingFace = clippingMeshBuilder.faces().createAndInsertFromList(clippingPoints);
		clippingMeshBuilder.getMesh().vertices().stream().forEach(v -> {
			clippingMeshBuilder.getDataStorage().setData(v, propNameIntersection, false);
			clippingMeshBuilder.getDataStorage().setData(v, propNameInside, subPointSet.contains(v) || subject.contains(v));
		});

		List<VPoint> intersectionPoints = new ArrayList<>();
		PVertex ip = null;

		// compute intersections and add those to the two faces, this implementation is rather slow!

		boolean intersectionFound = true;
		int count = 0;
		while (intersectionFound) {

			List<PHalfEdge> clippingEdges = clippingMeshBuilder.getMesh().edges().getAllOf(clippingFace);
			List<PHalfEdge> subjectEdges = subjectMeshBuilder.getMesh().edges().getAllOf(subjectFace);
			intersectionFound = false;

			// TODO: this can be simplified!
			for(PHalfEdge clippingEdge : clippingEdges) {
				for(PHalfEdge subjectEdge : subjectEdges) {
					Optional<VPoint> optIntersectionPoint = equalIntersectionPoints(subjectEdge, subjectMeshBuilder, clippingEdge, clippingMeshBuilder);
					if(!optIntersectionPoint.isPresent()) {
						VLine l1 = subjectMeshBuilder.getMesh().edges().toLine(subjectEdge);
						VLine l2 = clippingMeshBuilder.getMesh().edges().toLine(clippingEdge);

						VPoint intersectionPoint = null;

						if(!(l1.getP1().equals(l2.getP1()) || l1.getP1().equals(l2.getP2()) || l1.getP2().equals(l2.getP1()) || l1.getP2().equals(l2.getP2()))) {
							if(GeometryUtils.distanceToLineSegment(new VPoint(l1.getP1()), new VPoint(l1.getP2()), new VPoint(l2.getP1())) <= EPSILON) {
								intersectionPoint = new VPoint(l2.getP1());
							} else if(GeometryUtils.distanceToLineSegment(new VPoint(l1.getP1()), new VPoint(l1.getP2()), new VPoint(l2.getP2())) <= EPSILON) {
								intersectionPoint = new VPoint(l2.getP2());
							} else if(GeometryUtils.distanceToLineSegment(new VPoint(l2.getP1()), new VPoint(l2.getP2()), new VPoint(l1.getP1())) <= EPSILON) {
								intersectionPoint = new VPoint(l1.getP1());
							} else if(GeometryUtils.distanceToLineSegment(new VPoint(l2.getP1()), new VPoint(l2.getP2()), new VPoint(l1.getP2())) <= EPSILON) {
								intersectionPoint = new VPoint(l1.getP2());
							} else if (GeometryUtils.intersectLineSegment(l1.getP1().getX(), l1.getP1().getY(), l1.getP2().getX(), l1.getP2().getY(), l2.getP1().getX(), l2.getP1().getY(), l2.getP2().getX(), l2.getP2().getY())) {
								intersectionPoint = GeometryUtils.intersectionPoint(l1.getP1().getX(), l1.getP1().getY(), l1.getP2().getX(), l1.getP2().getY(), l2.getP1().getX(), l2.getP1().getY(), l2.getP2().getX(), l2.getP2().getY());
							}
						}

						if(intersectionPoint != null) {

							VPoint wp1 = intersectionPoint;
							VPoint wp2 = intersectionPoint;

							PHalfEdge prev = clippingMeshBuilder.getMesh().edges().getPrev(subjectEdge);
							PHalfEdge innerPrev = subjectMeshBuilder.getMesh().edges().getPrev(clippingEdge);

							PVertex ip1 = splitEdge(subjectEdge, wp1, subjectMeshBuilder);
							subjectMeshBuilder.getDataStorage().setData(ip1, propNameIntersection, true);
							subjectMeshBuilder.getDataStorage().setData(ip1, propNameInside, true);

							PVertex ip2 = splitEdge(clippingEdge, wp2, clippingMeshBuilder);
							clippingMeshBuilder.getDataStorage().setData(ip2, propNameIntersection, true);
							clippingMeshBuilder.getDataStorage().setData(ip2, propNameInside, true);

							clippingMeshBuilder.getDataStorage().setData(ip2, propNameTwin, ip1);
							subjectMeshBuilder.getDataStorage().setData(ip1, propNameTwin, ip2);

							intersectionPoints.add(intersectionPoint);
							intersectionFound = true;
							// go one step back
						}
					}
					else {
						intersectionPoints.add(optIntersectionPoint.get());
					}

					if(intersectionFound) {
						break;
					}
				}

				if(intersectionFound) {
					break;
				}
			}
		}

		return Pair.create(subjectFace, clippingFace);
	}

	private PVertex splitEdge(@NotNull final PHalfEdge edge, @NotNull final IPoint p, @NotNull final ITriangleMeshBuilder<PVertex, PHalfEdge, PFace> meshBuilder) {
		var mesh = meshBuilder.getMesh();
		PVertex v1 = mesh.vertices().getEndOf(edge);
		PVertex v2 = mesh.vertices().getEndOf(mesh.edges().getPrev(edge));
		if(mesh.vertices().toPoint(v1).equals(p)) {
			return v1;
		}

		if(mesh.vertices().toPoint(v2).equals(p)) {
			return v2;
		}

		return IPolyConnectivity.splitEdge(edge, p, meshBuilder);
	}

	/**
	 * Tests if the two edges from different faces have geometrical equal points. If this is the case
	 * the points will be transformed into intersection points and they will be connected i.e. twins.
	 *
	 * @param subjectEdge   edge from the first face containing the first point
	 * @param subjectMeshWithDataStorage   mesh which created the first face
	 * @param clippingEdge  edge from the second face containing the first point
	 * @param clippingMeshWithDataStorage  mesh which created the second face
	 *
	 * @return a point which is the intersection point or null / empty if the two edges did not have an geometrical equal point
	 */
	private Optional<VPoint> equalIntersectionPoints(
			@NotNull final PHalfEdge subjectEdge,
			@NotNull final ITriangleMeshBuilder<PVertex, PHalfEdge, PFace> subjectMeshWithDataStorage,
			@NotNull final PHalfEdge clippingEdge,
			@NotNull final ITriangleMeshBuilder<PVertex, PHalfEdge, PFace>clippingMeshWithDataStorage) {

		IMesh<PVertex, PHalfEdge, PFace> subjectMesh = subjectMeshWithDataStorage.getMesh();
		IMesh<PVertex, PHalfEdge, PFace> clippingMesh = clippingMeshWithDataStorage.getMesh();
		IMeshDataStorage<PVertex, PHalfEdge, PFace> subjectMeshDataStorage = subjectMeshWithDataStorage.getDataStorage();
		IMeshDataStorage<PVertex, PHalfEdge, PFace> clippingMeshDataStorage = clippingMeshWithDataStorage.getDataStorage();

		PVertex v1 = subjectMesh.vertices().getEndOf(subjectMesh.edges().getPrev(subjectEdge));
		PVertex v2 = subjectMesh.vertices().getEndOf(subjectEdge);

		PVertex u1 = clippingMesh.vertices().getEndOf(clippingMesh.edges().getPrev(clippingEdge));
		PVertex u2 = clippingMesh.vertices().getEndOf(clippingEdge);

		VPoint p1 = subjectMesh.vertices().toPoint(v1);
		VPoint p2 = subjectMesh.vertices().toPoint(v2);

		VPoint q1 = clippingMesh.vertices().toPoint(u1);
		VPoint q2 = clippingMesh.vertices().toPoint(u2);

		boolean intersection1 = subjectMeshDataStorage.getData(v1, propNameIntersection, Boolean.class).get();
		boolean intersection2 = subjectMeshDataStorage.getData(v2, propNameIntersection, Boolean.class).get();

		if(p1.equals(q1)) {
			subjectMeshDataStorage.setData(v1, propNameIntersection, true);
			clippingMeshDataStorage.setData(u1, propNameIntersection, true);
			subjectMeshDataStorage.setData(v1, propNameTwin, u1);
			clippingMeshDataStorage.setData(u1, propNameTwin, v1);
			return Optional.of(p1);
		} else if(p1.equals(q2)) {
			subjectMeshDataStorage.setData(v1, propNameIntersection, true);
			clippingMeshDataStorage.setData(u2, propNameIntersection, true);
			subjectMeshDataStorage.setData(v1, propNameTwin, u2);
			clippingMeshDataStorage.setData(u2, propNameTwin, v1);
			return Optional.of(p1);
		} else if(p2.equals(q1)) {
			subjectMeshDataStorage.setData(v2, propNameIntersection, true);
			clippingMeshDataStorage.setData(u1, propNameIntersection, true);
			subjectMeshDataStorage.setData(v2, propNameTwin, u1);
			clippingMeshDataStorage.setData(u1, propNameTwin, v2);
			return Optional.of(p2);
		} else if(p2.equals(q2)) {
			subjectMeshDataStorage.setData(v2, propNameIntersection, true);
			clippingMeshDataStorage.setData(u2, propNameIntersection, true);
			subjectMeshDataStorage.setData(v2, propNameTwin, u2);
			clippingMeshDataStorage.setData(u2, propNameTwin, v2);
			return Optional.of(p2);
		}
		else {
			return Optional.empty();
		}
	}

	/**
	 * Executes the Weiler-Atherton-Algorithm for all of its polygons.
	 *
	 * @return a list of merged polygons
	 */
	public List<VPolygon> cup() {

		boolean merged = true;
		List<VPolygon> newPolygons = new ArrayList<>();
		newPolygons.addAll(polygons);

		while (merged) {
			int ii = -1;
			int jj = -1;
			merged = false;
			List<VPolygon> cupResult = null;
			for(int i = 0; i < newPolygons.size(); i++) {
				VPolygon first = newPolygons.get(i);

				for(int j = i+1; j < newPolygons.size(); j++) {
					VPolygon second = newPolygons.get(j);

					cupResult = cup(first, second);
					assert cupResult.size() <= 2;

					// something got merged
					if(cupResult.size() <= 1) {
						merged = true;
						ii = i;
						jj = j;
						break;
					}
				}

				if(merged) {
					break;
				}
			}

			if(merged) {
				newPolygons.remove(ii);
				newPolygons.remove(jj-1);
				newPolygons.add(cupResult.get(0));
			}
		}

		return newPolygons;
	}

	public Optional<VPolygon> subtraction() {
		List<VPolygon> newPolygons = new ArrayList<>();
		newPolygons.addAll(polygons);

		if(polygons.size() == 0) {
			return Optional.empty();
		}
		else if(polygons.size() == 1) {
			return Optional.of(polygons.get(0));
		}
		else {
			VPolygon subject = polygons.get(0);
			List<VPolygon> clippers = polygons.subList(1, polygons.size());
			for(VPolygon clipper : clippers) {
				Optional<VPolygon> result = subtraction(subject, clipper);
				if(result.isPresent()) {
					subject = result.get();
				} else {
					return Optional.empty();
				}
			}
			return Optional.of(subject);
		}
	}

	/**
	 * Executes the Weiler-Atherton-Algorithm for all of its polygons.
	 *
	 * @return a list of merged polygons
	 */
	public Optional<VPolygon> cap() {
		List<VPolygon> newPolygons = new ArrayList<>();
		newPolygons.addAll(polygons);

		if(polygons.size() == 0) {
			return Optional.empty();
		}
		else if(polygons.size() == 1) {
			return Optional.of(polygons.get(0));
		}
		else {
			LinkedList<VPolygon> toDo = new LinkedList<>();
			toDo.addAll(polygons);

			while (toDo.size() >= 2) {
				VPolygon poly1 = toDo.remove();
				VPolygon poly2 = toDo.remove();
				List<VPolygon> capResult = cap(poly1, poly2);

				if(capResult.isEmpty()) {
					return Optional.empty();
				}

				toDo.addAll(capResult);
			}

			assert toDo.size() == 1;
			return Optional.of(toDo.remove());
		}
	}

	private boolean contains(@NotNull final VPolygon polygon, @NotNull final IPoint point) {
		return polygon.contains(point) || Math.abs(polygon.distance(point)) <= GeometryUtils.DOUBLE_EPS;
	}

	private List<VPolygon> construct(
			@NotNull final VPolygon subjectCandidat,
			@NotNull final VPolygon clippingCandidat,
			final Operation operation) {

		//Predicate<WeilerPoint> startEdgeCondition = cap ? p -> p.isInside() : p -> !p.isInside();
		VPolygon subject = GeometryUtils.isCCW(subjectCandidat) ? subjectCandidat : subjectCandidat.revertOrder();
		VPolygon clipping;
		switch (operation) {
			case SUBTRACTION: clipping = GeometryUtils.isCCW(clippingCandidat) ? clippingCandidat.revertOrder() : clippingCandidat; break;
			case UNION:
			case INTERSECTION:
			default: {
				clipping = GeometryUtils.isCCW(clippingCandidat) ? clippingCandidat : clippingCandidat.revertOrder(); break;
			}
		}
		ITriangleMeshBuilder<PVertex, PHalfEdge, PFace> subjectMeshBuilder = new PTriangleMeshBuilder();
		ITriangleMeshBuilder<PVertex, PHalfEdge, PFace> clippingMeshBuilder = new PTriangleMeshBuilder();

		//List<VPolygon> result = new ArrayList<>(2);

		/**
		 * (1) construct the list connections
		 */
		Pair<PFace, PFace> pair = constructIntersectionFaces(subject, subjectMeshBuilder, clipping, clippingMeshBuilder);

		PFace subjectFace = pair.getFirst();
		PFace clippingFace = pair.getSecond();

		Set<PHalfEdge> subjectExitingEdges = subjectMeshBuilder
				.getMesh()
				.edges()
				.streamEdgesOf(subjectFace)
				.filter(edge -> subjectMeshBuilder.getDataStorage().getData(subjectMeshBuilder.getMesh().vertices().getEndOf(edge), propNameIntersection, Boolean.class).get())
				.filter(edge -> !clipping.contains(subjectMeshBuilder.getMesh().edges().toLine(subjectMeshBuilder.getMesh().edges().getNext(edge)).midPoint()))
				.filter(edge -> clipping.contains(subjectMeshBuilder.getMesh().edges().toLine(edge).midPoint()))
				/*.filter(edge ->
						contains(clipping, subjectMeshBuilder.getPoint(subjectMeshBuilder.getPrev(edge))) &&
						!contains(clipping, subjectMeshBuilder.getPoint(subjectMeshBuilder.getNext(edge))))*/
				.collect(Collectors.toSet());

		Set<PHalfEdge> subjectEnteringEdges = subjectMeshBuilder
				.getMesh()
				.edges()
				.streamEdgesOf(subjectFace)
				.filter(edge -> subjectMeshBuilder.getDataStorage().getData(subjectMeshBuilder.getMesh().vertices().getEndOf(edge), propNameIntersection, Boolean.class).get())
				.filter(edge -> clipping.contains(subjectMeshBuilder.getMesh().edges().toLine(subjectMeshBuilder.getMesh().edges().getNext(edge)).midPoint()))
				.filter(edge -> !clipping.contains(subjectMeshBuilder.getMesh().edges().toLine(edge).midPoint()))
				/*.filter(edge ->
						contains(clipping, subjectMeshBuilder.getPoint(subjectMeshBuilder.getNext(edge))) &&
						!contains(clipping, subjectMeshBuilder.getPoint(subjectMeshBuilder.getPrev(edge)))
				)*/
				.collect(Collectors.toSet());

		List<VPoint> points = new ArrayList<>();
		List<VPolygon> polygons = new ArrayList<>();
		ITriangleMeshBuilder<PVertex, PHalfEdge, PFace> meshBuilder = subjectMeshBuilder;

		Set<PHalfEdge> intersectionSet;
		switch (operation) {
			case INTERSECTION: intersectionSet = subjectEnteringEdges; break;
			case UNION:
			case SUBTRACTION:
			default: intersectionSet = subjectExitingEdges; break;
		};

		// cup will preserve the polyons.
		if(intersectionSet.isEmpty()) {
			boolean subInClip = subjectMeshBuilder
					.getMesh()
					.vertices()
					.streamPoints(subjectFace)
					.allMatch(p -> contains(clipping, p));

			boolean clipInSub = clippingMeshBuilder
					.getMesh()
					.vertices()
					.streamPoints(clippingFace)
					.allMatch(p -> contains(subject, p));

			switch (operation) {
				case INTERSECTION: {
					if(subInClip) {
						polygons.add(subjectCandidat);
					}
					else if(clipInSub) {
						polygons.add(clippingCandidat);
					}
				} break;
				case UNION: {
					if(subInClip) {
						polygons.add(clippingCandidat);
					}
					else if(clipInSub) {
						polygons.add(subjectCandidat);
					}
					else {
						polygons.add(subjectCandidat);
						polygons.add(clippingCandidat);
					}
				} break;
				case SUBTRACTION:
				default: {
					if(clipInSub) {
						throw new IllegalArgumentException("subtracting a polygon which is contained in its counterpart will produce a polygon with a hole which is not supported.");
					}
				} break;
			}
			return polygons;
		}

		while (!intersectionSet.isEmpty()) {
			PHalfEdge subjectEdge = intersectionSet.iterator().next();
			PHalfEdge subjectTwin = clippingMeshBuilder.getMesh().edges().getOf(subjectMeshBuilder.getDataStorage().getData(subjectMeshBuilder.getMesh().vertices().getEndOf(subjectEdge), propNameTwin, PVertex.class).get());
			PHalfEdge next = subjectEdge;
			intersectionSet.remove(subjectEdge);

			do {
				next = meshBuilder.getMesh().edges().getNext(next);
				var v = meshBuilder.getMesh().vertices().getEndOf(next);
				// adaptPath
				if(meshBuilder.getDataStorage().getData(v, propNameIntersection, Boolean.class).get()) {
					/*
					 * Special case!
					 */
					var twinPoint = meshBuilder.getDataStorage().getData(v, propNameTwin, PVertex.class).get();
					ITriangleMeshBuilder<PVertex, PHalfEdge, PFace> twinMeshWithDataStorage = meshBuilder.equals(subjectMeshBuilder) ? clippingMeshBuilder : subjectMeshBuilder;
					PHalfEdge twinPointEdge = twinMeshWithDataStorage.getMesh().edges().getOf(twinPoint);
					VPoint prevTwinPoint = new VPoint(twinMeshWithDataStorage.getMesh().edges().endToPoint(twinMeshWithDataStorage.getMesh().edges().getPrev(twinPointEdge)));
					VPoint prevPoint = new VPoint(meshBuilder.getMesh().edges().endToPoint(meshBuilder.getMesh().edges().getPrev(next)));

					intersectionSet.remove(next);
					intersectionSet.remove(twinMeshWithDataStorage.getMesh().edges().getOf(twinPoint));

					if(!prevTwinPoint.equals(prevPoint)) {
						meshBuilder = meshBuilder.equals(subjectMeshBuilder) ? clippingMeshBuilder : subjectMeshBuilder;
						next = meshBuilder.getMesh().edges().getOf(meshBuilder.getDataStorage().getData(meshBuilder.getMesh().vertices().getEndOf(next), propNameTwin, PVertex.class).get());
					}

				}
				points.add(new VPoint(meshBuilder.getMesh().edges().getMutableEndPoint(next)));

			} while (!next.equals(subjectEdge) && !next.equals(subjectTwin));

			polygons.add(GeometryUtils.toPolygon(GeometryUtils.filterUselessPoints(points, EPSILON)));
		}

		return polygons;
	}

	public List<VPolygon> cap(@NotNull final VPolygon subjectCandidat, @NotNull final VPolygon clippingCandidat) {
		return construct(subjectCandidat, clippingCandidat, Operation.INTERSECTION);
	}

	/**
	 * Executes the Weiler-Atherton-Algorithm for two polygons.
	 *
	 * @param subjectCandidat   the first polygon
	 * @param clippingCandidat  the second polygon
	 * @return a pair of polygon where the second element is null if the polygons got merged. This is not the case if there do not overlap.
	 */
	public List<VPolygon> cup(@NotNull final VPolygon subjectCandidat, @NotNull final VPolygon clippingCandidat) {
		return construct(subjectCandidat, clippingCandidat, Operation.UNION);
	}

	public Optional<VPolygon> subtraction(@NotNull final VPolygon subjectCandidat, @NotNull final VPolygon clippingCandidat) {
		List<VPolygon> result = construct(subjectCandidat, clippingCandidat, Operation.SUBTRACTION);
		assert result.size() >= 1;
		if(result.size() == 1) {
			return Optional.of(result.get(0));
		} else if(result.isEmpty()) {
			return Optional.empty();
		} else{
			throw new IllegalStateException("subtraction of two polygons should never produce more than one polygon.");
		}
	}
}
