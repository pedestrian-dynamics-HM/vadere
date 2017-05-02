package org.vadere.util.geometry.mesh.triangulations;

import org.vadere.util.geometry.mesh.inter.IFace;
import org.vadere.util.geometry.mesh.inter.IHalfEdge;
import org.vadere.util.geometry.mesh.inter.IMesh;
import org.vadere.util.geometry.mesh.inter.IPointLocator;
import org.vadere.util.geometry.mesh.inter.ITriConnectivity;
import org.vadere.util.geometry.mesh.inter.ITriangulation;
import org.vadere.util.geometry.shapes.IPoint;

import java.util.ArrayList;
import java.util.Collection;
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

	private ITriConnectivity<P, E, F> base;

	private Supplier<ITriangulation<P, E, F>> triangulationSupplier;

	private double alpha;

	private Random random;

	public DelaunayHierarchy(final ITriangulation<P, E, F> base, final Supplier<ITriangulation<P, E, F>> triangulationSupplier) {
		this.hierarchySets = new ArrayList<>();
		this.hierarchyConnector = new ArrayList<>();
		this.random = new Random();
		this.triangulationSupplier = triangulationSupplier;

		hierarchySets.add(base);
		hierarchyConnector.add(new HashMap<>());
	}

	@Override
	public void splitFaceEvent(F original, F[] faces) {}

	@Override
	public void flipEdgeEvent(F f1, F f2) {}

	@Override
	public void insertEvent(final E halfEdge) {
		P vertex = base.getMesh().getVertex(halfEdge);
		E lastEdge = halfEdge;
		for(int i = 1; i < hierarchySets.size(); ++i) {

			if(random.nextDouble() < alpha) {

				if(hierarchySets.size() <= i) {
					hierarchySets.add(triangulationSupplier.get());
				}

				E edge = hierarchySets.get(i).insert(vertex);

				if(hierarchyConnector.size() < i) {
					hierarchyConnector.add(new HashMap<>());
				}

				hierarchyConnector.get(i-1).put(lastEdge, edge);
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
		return null;
	}

	@Override
	public Optional<F> locate(final P point) {
		Optional<F> optStartFace = Optional.empty();
		for(int i = hierarchySets.size()-1; i >= 0; --i) {
			if(!optStartFace.isPresent()) {
				optStartFace = hierarchySets.get(i).locate(point.getX(), point.getY());
			}
			else {
				E edge = getNearestPoint(hierarchySets.get(i-1), optStartFace.get(), point);
				E newEdge = hierarchyConnector.get(i-1).get(edge);
				optStartFace = hierarchySets.get(i).locate(point.getX(), point.getY(), hierarchySets.get(i).getMesh().getFace(newEdge));

				if(!optStartFace.isPresent()) {
					return Optional.empty();
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
