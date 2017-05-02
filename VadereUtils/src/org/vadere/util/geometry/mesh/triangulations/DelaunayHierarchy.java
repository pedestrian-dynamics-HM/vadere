package org.vadere.util.geometry.mesh.triangulations;

import org.jetbrains.annotations.NotNull;
import org.vadere.util.geometry.mesh.inter.IFace;
import org.vadere.util.geometry.mesh.inter.IHalfEdge;
import org.vadere.util.geometry.mesh.inter.IMesh;
import org.vadere.util.geometry.mesh.inter.IPointLocator;
import org.vadere.util.geometry.mesh.inter.ITriConnectivity;
import org.vadere.util.geometry.mesh.inter.ITriangulation;
import org.vadere.util.geometry.shapes.IPoint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.function.Supplier;

/**
 * @author Benedikt Zoennchen
 *
 * @param <P>
 * @param <E>
 * @param <F>
 */
public class DelaunayHierarchy<P extends IPoint, E extends IHalfEdge<P>, F extends IFace<P>> implements IPointLocator<P, E, F>  {

	private List<ITriangulation<P, E, F>> hierarchySets;

	private List<Map<E, E>> hierarchyConnector;

	private ITriangulation<P, E, F> base;

	private Supplier<ITriangulation<P, E, F>> triangulationSupplier;

	// see delaunay-hierarchy paper!
	private double alpha = 40;

	private double epsilon = 0.00001;

	private Random random;

	public DelaunayHierarchy(final ITriangulation<P, E, F> base, final Supplier<ITriangulation<P, E, F>> triangulationSupplier) {
		this.hierarchySets = new ArrayList<>();
		this.hierarchyConnector = new ArrayList<>();
		this.random = new Random();
		this.triangulationSupplier = triangulationSupplier;
		this.base = base;
		this.base.init();

		hierarchySets.add(base);
		hierarchyConnector.add(new HashMap<>());
	}

	@Override
	public void splitFaceEvent(F original, F[] faces) {}

	@Override
	public void flipEdgeEvent(F f1, F f2) {}

	@Override
	public void insertEvent(@NotNull final E halfEdge) {
		P vertex = base.getMesh().getVertex(halfEdge);
		E lastEdge = halfEdge;
		int limit = hierarchySets.size();
		for(int i = 1; i <= limit; ++i) {

			if(random.nextDouble() < alpha) {

				if(hierarchySets.size() <= i) {
					ITriangulation<P, E, F> triangulation = triangulationSupplier.get();
					triangulation.init();
					hierarchySets.add(triangulation);
				}

				E edge = hierarchySets.get(i).insert(vertex);

				if(hierarchyConnector.size() < i) {
					hierarchyConnector.add(new HashMap<>());
				}

				hierarchyConnector.get(i-1).put(edge, lastEdge);
				lastEdge = edge;

			}
			else {
				break;
			}
		}
	}

	@Override
	public void deleteBoundaryFace(F face) {

	}

	@Override
	public Collection<F> locatePoint(P point, boolean insertion) {
		Optional<F> optFace = locate(point);

		if(optFace.isPresent()) {
			F face = optFace.get();

			if(!insertion) {
				return Collections.singleton(face);
			}
			else {
				Optional<E> optEdge = base.getMesh().getMemberEdge(face, point.getX(), point.getY(), epsilon);

				// ignore point
				if(optEdge.isPresent()) {
					return Collections.emptyList();
				}
				else {
					optEdge = base.getMesh().getEdgeCloseToVertex(face, point.getX(), point.getY(), epsilon);
					if(optEdge.isPresent()) {
						return Arrays.asList(base.getMesh().getFace(optEdge.get()), base.getMesh().getTwinFace(optEdge.get()));
					}
					else {
						return Collections.singleton(face);
					}
				}
			}
		}
		else {
			return Collections.emptyList();
		}
	}

	@Override
	public Optional<F> locate(final P point) {
		Optional<F> optStartFace = Optional.empty();
		for(int i = hierarchySets.size()-1; i >= 0; --i) {
			if(i == hierarchySets.size()-1) {
				optStartFace = hierarchySets.get(i).locate(point.getX(), point.getY());
			}
			else {
				if(i > 0) {
					E edge = getNearestPoint(hierarchySets.get(i+1), optStartFace.get(), point);
					E newEdge = hierarchyConnector.get(i).get(edge);
					optStartFace = hierarchySets.get(i).locate(point.getX(), point.getY(), hierarchySets.get(i).getMesh().getFace(newEdge));
				}
				else {
					E edge = getNearestPoint(hierarchySets.get(i+1), optStartFace.get(), point);
					E newEdge = hierarchyConnector.get(i).get(edge);
					return base.locate(point.getX(), point.getY(), base.getMesh().getFace(newEdge));
				}

			}
		}

		return optStartFace;
	}

	public E getNearestPoint(final ITriangulation<P, E, F> triangulation, final F face, final P point) {
		IMesh<P, E, F> mesh = triangulation.getMesh();
		return triangulation.getMesh().streamEdges(face).reduce((p1, p2) -> mesh.getVertex(p1).distance(point) > mesh.getVertex(p2).distance(point) ? p2 : p1).get();
	}
}
