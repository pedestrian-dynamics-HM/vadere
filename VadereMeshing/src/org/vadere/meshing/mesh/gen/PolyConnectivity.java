package org.vadere.meshing.mesh.gen;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.IllegalMeshException;
import org.vadere.meshing.mesh.inter.mesh.*;
import org.vadere.meshing.mesh.inter.mesh.builder.*;
import org.vadere.meshing.mesh.inter.meshConnectivity.IPolyConnectivity;
import org.vadere.meshing.mesh.inter.meshConnectivity.IReadOnlyPolyConnectivity;
import org.vadere.meshing.mesh.iterators.EdgeIterator;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.logging.Logger;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class PolyConnectivity<V extends IVertex, E extends IHalfEdge, F extends IFace> implements IPolyConnectivity<V, E, F> {
	Logger log = Logger.getLogger(PolyConnectivity.class);

	private final IMeshBuilder<V, E, F> meshBuilder;
	private final IReadOnlyPolyConnectivity<V, E, F> readonlyConnectivity;

	private final IMeshVertices<V, E, F> vertices;
	private final IMeshFaces<V, E, F> faces;
	private final IMeshEdges<V, E, F> edges;
	private final IMeshBuilderEdges<V, E, F> edgeBuilder;
	private final IMeshBuilderFaces<V, E, F> faceBuilder;
	private final IMeshBuilderVertices<V, E, F> vertexBuilder;


	public PolyConnectivity(@NotNull final IMeshBuilder<V, E, F> meshBuilder) {
        this.meshBuilder = meshBuilder;
		readonlyConnectivity = meshBuilder.getMesh().readConnectivity();

		edgeBuilder = meshBuilder.edges();
		faceBuilder = meshBuilder.faces();
		vertexBuilder = meshBuilder.vertices();

		vertices = meshBuilder.getMesh().vertices();
		faces = meshBuilder.getMesh().faces();
		edges = meshBuilder.getMesh().edges();
	}

	@Override
	public IMeshBuilder<V, E, F> getMeshBuilder() {
		return meshBuilder;
	}

	public void adjustVertex(@NotNull final V vertex) {
		edges.streamEdgesOf(vertex).filter(edge -> edges.isBoundary(edge)).findAny().ifPresent(edge -> getMeshBuilder().vertices().setEdge(vertex, edge));
	}

	@Override
	public void split(@NotNull final V vertex) {
		Optional<F> optFace = readonlyConnectivity.locateNonBoundaryByFullScan(vertices.toPoint(vertex));
		if(!optFace.isPresent()) {
			throw new IllegalArgumentException(vertex + " is not contained in any face. Therefore, no face found to split into faces.");
		} else {
			split(optFace.get(), vertex);
		}
	}

	@Override
	public void split(@NotNull final F face, @NotNull final V vertex) {
		assert readonlyConnectivity.locateNonBoundaryByFullScan(vertices.toPoint(vertex)).get().equals(face);

		E hend = edges.getAnyOf(face);
		E hh = edges.getNext(hend);
		E hold = edgeBuilder.createAndInsert(vertex);
		E twin = edgeBuilder.createAndInsert(vertices.getEndOf(hend));

		edgeBuilder.setTwin(hold, twin);
		edgeBuilder.setNext(hend, hold);
		edgeBuilder.setFace(hold, face);

		hold = edges.getTwin(hold);
		while (!hh.equals(hend)) {
			E hnext = edges.getNext(hh);
			F newFace = faceBuilder.createAndInsert();
			faceBuilder.setEdge(newFace, hh);

			// update the edge of the vertex such that the last new created edge will be its edge
			E hnew = edgeBuilder.createAndInsert(vertex);
			vertexBuilder.setEdge(vertex, hnew);

			edgeBuilder.setNext(hnew, hold);
			edgeBuilder.setNext(hold, hh);
			edgeBuilder.setNext(hh, hnew);

			edgeBuilder.setFace(hnew, newFace);
			edgeBuilder.setFace(hold, newFace);
			edgeBuilder.setFace(hh, newFace);

			E hnewTwin = edgeBuilder.createAndInsert(vertices.getEndOf(hh));
			edgeBuilder.setTwin(hnew, hnewTwin);

			hold = hnewTwin;
			hh = hnext;
		}

		edgeBuilder.setNext(hold, hend);
		edgeBuilder.setNext(edges.getNext(hend), hold);
		edgeBuilder.setFace(hold, face);
	}

	@Override
	public Optional<F> removeEdges(@NotNull final F face, @NotNull F otherFace, final boolean deleteIsolatedVertices) {
		// TODO: test it!
		assert !faces.isDestroyed(face) && !faces.isDestroyed(otherFace);

		F delFace = otherFace;
		F remFace = face;

		if(faces.isBoundary(delFace)) {
			F tmp = delFace;
			delFace = remFace;
			remFace = tmp;
		}

		if(faces.isOuterBorder(delFace)) {
			F tmp = delFace;
			delFace = remFace;
			remFace = tmp;
		}

		final F finalFace = delFace;
		List<E> toDeleteEdges = edges.getAllOf(remFace).stream().filter(e -> faces.getTwin(e).equals(finalFace)).collect(Collectors.toList());

		assert edges.getAllOf(remFace).size() > toDeleteEdges.size() : "can not remove all of the edges, since this could lead to an invalid mesh";

		// face and otherFace share no common edge.
		if(toDeleteEdges.isEmpty()) {
			return Optional.empty();
		}

		for(E edge : toDeleteEdges) {
			E twin = edges.getTwin(edge);

			assert !faces.isDestroyed(delFace);

			E prevEdge = edges.getPrev(edge);
			E prevTwin = edges.getPrev(twin);

			E nextEdge = edges.getNext(edge);
			E nextTwin = edges.getNext(twin);

			// = prevEdge == twin
			boolean tDangling = nextTwin.equals(edge);

			// = prevTwin == edge
			boolean eDangling = nextEdge.equals(twin);

			// adjust vertices, mb later
			V eVertex = vertices.getEndOf(edge);
			V tVertex = vertices.getEndOf(twin);

			// twin vertex has to be deleted
			if(deleteIsolatedVertices && edges.getNext(twin).equals(edge)) {
				vertexBuilder.destroy(tVertex);
			}

			// edge vertex has to be deleted
			if(deleteIsolatedVertices && edges.getNext(edge).equals(twin)) {
				vertexBuilder.destroy(eVertex);
			}

			edgeBuilder.setNext(prevEdge, nextTwin);
			edgeBuilder.setNext(prevTwin, nextEdge);

			vertexBuilder.setEdge(eVertex, prevTwin);
			vertexBuilder.setEdge(tVertex, prevEdge);

			if(edges.getAnyOf(remFace).equals(edge)) {
				if(!eDangling) {
					faceBuilder.setEdge(remFace, nextEdge);
				}
				else {
					faceBuilder.setEdge(remFace, prevEdge);
				}
			}

			//getMesh().setEdge(remFace, survivalEdges.get(0));

			edgeBuilder.destroy(edge);
			edgeBuilder.destroy(twin);
		}

		for(E halfEdge : edges.iterableFor(remFace)) {
			edgeBuilder.setFace(halfEdge, remFace);
			faceBuilder.setEdge(remFace, halfEdge);
		}

		faceBuilder.destroy(delFace);
		return Optional.of(remFace);
	}

	@Override
	public Optional<F> mergeFaces(
			@NotNull final F face,
			@NotNull final Predicate<F> mergeCondition,
			@NotNull final Predicate<F> errorCondition,
			final boolean deleteIsolatedVertices,
			final int maxDept) throws IllegalMeshException {
		boolean modified = true;
		F currentFace = face;
		int dept = 0;

		while (modified) {
			modified = false;
			dept++;
			List<F> neighbouringFaces = faces.getSurroundingOf(currentFace);

			assert neighbouringFaces.isEmpty() || neighbouringFaces.stream().anyMatch(f -> !f.equals(neighbouringFaces.get(0))) : "each edge of both faces is a link to the other face";

			for(F neighbouringFace : neighbouringFaces) {
				if(errorCondition.test(neighbouringFace)) {
					throw new IllegalMeshException("the errorCondition is satisfied.");
				}
				// the face might be destroyed by an operation before
				if(!faces.isDestroyed(neighbouringFace) && mergeCondition.test(neighbouringFace)) {
					Optional<F> optionalMergeResult = removeEdges(currentFace, neighbouringFace, deleteIsolatedVertices);

					if(optionalMergeResult.isPresent()) {
						modified = true;
						currentFace = optionalMergeResult.get();
					}
					else {
						if(faces.isDestroyed(currentFace)) {
							return Optional.empty();
						}
					}
				}
			}

			if(maxDept > 0 && dept >= maxDept) {
				if(faces.isDestroyed(currentFace)) {
					return Optional.empty();
				}
				else {
					return Optional.of(currentFace);
				}
			}
		}

		return Optional.of(currentFace);
	}

	@Override
	public Optional<F> createHole(@NotNull final F face, @NotNull final Predicate<F> mergeCondition, final boolean deleteIsoletedVertices, final boolean vertexAdjust) {

		if(mergeCondition.test(face)) {
			getMeshBuilder().faces().convertToHole(face);
			shrinkBoundary(face, mergeCondition, deleteIsoletedVertices, vertexAdjust);
			return Optional.of(face);
		}
		else {
			//	if(!getMesh().isDestroyed(face) && !mergeCondition.test(face)) {
			//		System.out.println("could not delete it!");
			//	}
			return Optional.empty();
		}
	}

	@Override
	public void shrinkBorder(final Predicate<F> removeCondition, final boolean deleteIsolatedVertices, final boolean vertexAdjust) {
		shrinkBoundary(faces.getOuterBorder(), removeCondition, deleteIsolatedVertices, vertexAdjust);
	}

	@Override
	public void shrinkBorder(final Predicate<F> removeCondition, final boolean deleteIsolatedVertices) {
		shrinkBorder(removeCondition, deleteIsolatedVertices, true);
	}

	@Override
	public void shrinkBoundary(final Predicate<F> removeCondition, final boolean deleteIsolatedVertices) {
		shrinkBoundary(faces.getOuterBorder(), removeCondition, deleteIsolatedVertices);
		for(F hole : faces.getHoles()) {
			shrinkBoundary(hole, removeCondition, deleteIsolatedVertices);
		}
	}

	@Override
	public void shrinkBoundary(@NotNull final F boundary, final Predicate<F> removeCondition, final boolean deleteIsolatedVertices, final boolean adjustVertices) {
		assert faces.isBoundary(boundary);

		List<F> boundaryFaces = faces.getSurroundingOf(boundary);
		List<F> neighbouringFaces = boundaryFaces;

		do {
			List<F> nextNeighbouringFaces = new ArrayList<>();
			for(F neighbouringFace : neighbouringFaces) {
				// the face might be destroyed by an operation before
				if(!faces.isDestroyed(neighbouringFace) && removeCondition.test(neighbouringFace)) {
					for(F face : faces.surroundingIterableFor(neighbouringFace)) {
						assert face.equals(boundary) || !faces.isBoundary(face);
						if(!face.equals(boundary)) {
							nextNeighbouringFaces.add(face);
						}
					}
					removeFaceAtBoundary(neighbouringFace, boundary, deleteIsolatedVertices, adjustVertices);
				}
			}
			neighbouringFaces = nextNeighbouringFaces;
		} while (!neighbouringFaces.isEmpty());
	}

	@Override
	public void shrinkBoundary(@NotNull final F boundary, final Predicate<F> removeCondition, final boolean deleteIsolatedVertices) {
		shrinkBoundary(boundary, removeCondition, deleteIsolatedVertices, true);
	}

	@Override
	public void removeFacesAtBoundary(@NotNull final Predicate<F> mergePredicate, @NotNull final Predicate<F> errorPredicate) throws IllegalMeshException {
		mergeFaces(faces.getOuterBorder(), mergePredicate, errorPredicate, true, 1);
		for(F face : faces.getHoles()) {
			mergeFaces(face, mergePredicate, errorPredicate, true, 1);
		}
	}

	@Override
	public F removeEdgeSafely(@NotNull final E edge) {
		if (readonlyConnectivity.isSimpleLink(edge) && !edges.isDestroyed(edge)) {
			return removeSimpleLink(edge);
		}
		else {
			return faces.getOf(edge);
		}
	}

	@Override
	public F removeSimpleLink(@NotNull final E edge) {
		assert readonlyConnectivity.isSimpleLink(edge) && !edges.isDestroyed(edge);
		E twin = edges.getTwin(edge);
		F delFace = faces.getOf(edge);
		F remFace = faces.getOf(twin);

		if(faces.isBoundary(delFace)) {
			F tmp = delFace;
			delFace = remFace;
			remFace = tmp;
		}

		assert !faces.isDestroyed(delFace);

		E prevEdge = edges.getPrev(edge);
		E prevTwin = edges.getPrev(twin);

		E nextEdge = edges.getNext(edge);
		E nextTwin = edges.getNext(twin);

		edgeBuilder.setNext(prevEdge, nextTwin);
		edgeBuilder.setNext(prevTwin, nextEdge);

		// adjust vertices, mb later
		V eVertex = vertices.getEndOf(edge);
		V tVertex = vertices.getEndOf(twin);

		vertexBuilder.setEdge(eVertex, prevTwin);
		vertexBuilder.setEdge(tVertex, prevEdge);

		if(edges.getAnyOf(remFace).equals(edge)) {
			faceBuilder.setEdge(remFace, prevTwin);
		}
		else if(edges.getAnyOf(remFace).equals(twin)) {
			faceBuilder.setEdge(remFace, prevEdge);
		}

		for(E halfEdge : edges.getAllOf(remFace)) {
			edgeBuilder.setFace(halfEdge, remFace);
			faceBuilder.setEdge(remFace, halfEdge);
		}

		edgeBuilder.destroy(edge);
		edgeBuilder.destroy(twin);
		faceBuilder.destroy(delFace);
		//System.out.println("after = " + getMesh().degree(vertex));
		return remFace;
	}

	@Override
	public void removeFaceAtBoundary(@NotNull final F face, @NotNull final F boundary, final boolean deleteIsolatedVertices, final boolean adjustVertices) {
		if(!faces.isDestroyed(face)) {

			assert faces.streamOf(face).filter(neighbour -> neighbour.equals(boundary)).count() > 0;

			List<E> delEdges = new ArrayList<>();
			List<V> vertices = new ArrayList<>();

			// number of edges of the face
			int nEdges = 0;
			boolean boundaryEdgeDeleted = false;
			E survivingEdge = null;
			E boundaryEdge = edges.getAnyOf(boundary);

			for(E edge : edges.getAllOf(face)) {
				E twin = edges.getTwin(edge);
				F twinFace = faces.getOf(twin);

				assert twinFace.equals(boundary) || !faces.isBoundary(twinFace);

				nEdges++;
				if(twinFace.equals(boundary)) {
					delEdges.add(edge);

					// adjust the boundary edge if it will be deleted
					if(boundaryEdge.equals(twin)) {
						boundaryEdgeDeleted = true;
					}
				}
				else {
					// remember an edge that will not be deleted. This edge can be used as the edge of the boundary.
					survivingEdge = edge;

					// if the edge will not be deleted it becomes an boundary edge
					edgeBuilder.setFace(edge, boundary);
				}
				vertices.add(this.vertices.getEndOf(edge));
			}


			//TODO: this might be computational expensive!
			// special case: all edges will be deleted && the edge of the border will be deleted as well! => adjust the border edge
			if(faces.getTwin(boundaryEdge).equals(face) && delEdges.size() == nEdges) {
				assert survivingEdge == null;

				// all edges are border edges!
				EdgeIterator<V, E, F> edgeIterator = new EdgeIterator<>(meshBuilder.getMesh(), boundaryEdge);

				F twinFace = faces.getTwin(boundaryEdge);

				// walk along the border away from this faces to get another edge which won't be deleted
				while (edgeIterator.hasNext() && twinFace.equals(face)) {
					boundaryEdge = edgeIterator.next();
					twinFace = faces.getTwin(boundaryEdge);
				}

				// no such candidate was found. This can happen if an island will be deleted.
				if(twinFace.equals(face)) {
					log.warn("no boundary candidate was found, we search through all edges of the mesh.");
					Optional<E> optBoundaryEdge = edges.stream()
							.filter(e -> faces.getOf(e).equals(boundary))
							.filter(e -> !faces.getTwin(e).equals(face))
							.findAny();

					if(!optBoundaryEdge.isPresent()) {
						if(edges.getAllOf(boundary).size() == delEdges.size()) {
							log.warn(face + " is the last remaining face which will be deletes as well, therefore the mesh will be emoty!");
							assert faces.count() == 1;
							faceBuilder.destroy(face);
							faceBuilder.destroy(faces.getOuterBorder());
							meshBuilder.clear();
							return;
						}
					} else {
						boundaryEdge = optBoundaryEdge.get();
					}
				}

				edgeBuilder.setFace(boundaryEdge, boundary);
				faceBuilder.setEdge(boundary, boundaryEdge);
			}
			else if(boundaryEdgeDeleted) {
				assert survivingEdge != null;
				faceBuilder.setEdge(boundary, survivingEdge);
			}

			if(!delEdges.isEmpty()) {
				E h0, h1, next0, next1, prev0, prev1;
				V v0, v1;

				for(E delEdge : delEdges) {
					h0 = delEdge;
					v0 = this.vertices.getEndOf(delEdge);
					next0 = edges.getNext(h0);
					prev0 = edges.getPrev(h0);

					h1    = edges.getTwin(delEdge);
					v1    = this.vertices.getEndOf(h1);
					next1 = edges.getNext(h1);
					prev1 = edges.getPrev(h1);

					boolean isolated0 = readonlyConnectivity.isSimpleConnected(v0);
					boolean isolated1 = readonlyConnectivity.isSimpleConnected(v1);

					//getMesh().setEdge(hole, prev1);

					// adjust next and prev half-edges
					edgeBuilder.setNext(prev0, next1);
					edgeBuilder.setNext(prev1, next0);

					// adjust vertices
					if(edges.getOf(v0) == h0 && !isolated0) {
						vertexBuilder.setEdge(v0, prev1);
					}

					if(deleteIsolatedVertices && isolated0) {
						vertexBuilder.destroy(v0);
					}

					if(edges.getOf(v1) == h1 && !isolated1) {
						vertexBuilder.setEdge(v1, prev0);
					}

					if(deleteIsolatedVertices && isolated1) {
						vertexBuilder.destroy(v1);
					}

					// mark edge deleted if the mesh has a edge status
					edgeBuilder.destroy(h0);
					edgeBuilder.destroy(h1);

					// adjust vertices such that we speed up the querry isBoundary(vertex).
					if(adjustVertices) {
						vertices.stream().filter(this.vertices::isAlive).forEach(v -> adjustVertex(v));
					}
				}
			}

			if(nEdges > 0) {
				faceBuilder.destroy(face);
			}
			else {
				log.warn("could not delete face " + face + ". It is not at the border!");
			}

		}
	}

	@Override
	public void removeFaceAtBorder(@NotNull final F face, final boolean deleteIsolatedVertices) {
		removeFaceAtBoundary(face, faces.getOuterBorder(), deleteIsolatedVertices);
	}

	@Override
	public F insertOutsidePoint(@NotNull final IPoint point, @NotNull final E boundaryEdge, @NotNull final F boundary) {
		assert edges.isBoundary(boundaryEdge) &&
				faces.isBoundary(boundary) &&
				faces.getOf(boundaryEdge).equals(boundary) &&
				(!readonlyConnectivity.locateNonBoundaryByFullScan(point.getX(), point.getY()).isPresent()
						|| readonlyConnectivity.locateNonBoundaryByFullScan(point.getX(), point.getY()).get().equals(boundary));

		IMeshBuilder<V, E, F> meshBuilder = getMeshBuilder();
		IMeshBuilderEdges<V, E, F> edgeBuilder = meshBuilder.edges();
		IMeshBuilderVertices<V, E, F> verticesBuilder = meshBuilder.vertices();
		IMeshBuilderFaces<V, E, F> faceBuilder = meshBuilder.faces();

		V vertex = verticesBuilder.create(point);
		F face = faceBuilder.createAndInsert();
		F borderFace = faces.getOf(boundaryEdge);

		E prev = edges.getPrev(boundaryEdge);
		E next = edges.getNext(boundaryEdge);

		E e1 = edgeBuilder.createAndInsert(vertex);
		edgeBuilder.setFace(e1, face);
		E e2 = edgeBuilder.createAndInsert(vertices.getEndOf(prev));
		edgeBuilder.setFace(e2, face);

		E b1 = edgeBuilder.createAndInsert(vertex);
		edgeBuilder.setFace(b1, borderFace);
		E b2 = edgeBuilder.createAndInsert(vertices.getEndOf(boundaryEdge));
		edgeBuilder.setFace(b2, borderFace);

		edgeBuilder.setNext(prev, b1);
		edgeBuilder.setNext(b1, b2);
		edgeBuilder.setNext(b2, next);

		edgeBuilder.setNext(boundaryEdge, e1);
		edgeBuilder.setNext(e1, e2);
		edgeBuilder.setNext(e2, boundaryEdge);

		edgeBuilder.setTwin(b1, e2);
		edgeBuilder.setTwin(b2, e1);

		verticesBuilder.setEdge(vertex, e1);
		faceBuilder.setEdge(borderFace, b1);
		faceBuilder.setEdge(face, e1);

		edgeBuilder.setFace(boundaryEdge, face);

		return face;
	}
}
