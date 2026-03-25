package org.vadere.meshing.mesh.triangulation.triangulator.gen;

import org.vadere.meshing.mesh.inter.ITriangleMeshPointLocator;
import org.vadere.meshing.mesh.inter.mesh.*;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.meshing.mesh.inter.mesh.builder.ITriangleMeshBuilder;
import org.vadere.meshing.mesh.inter.mesh.data.IMeshDataStorage;
import org.vadere.meshing.mesh.triangulation.triangulator.inter.ITriangulator;
import org.vadere.util.geometry.shapes.IPoint;

import java.util.Collection;

/**
 * <p>A default triangulator: This triangulator takes a set of points P and constructs the Delaunay-Triangulation of P.</p>
 *
 * @author Benedikt Zoennchen
 *
 * @param <V> the type of the vertices
 * @param <E> the type of the half-edges
 * @param <F> the type of the faces
 */
public class GenPointSetTriangulator<V extends IVertex, E extends IHalfEdge, F extends IFace> implements ITriangulator<V, E, F> {

	/**
	 * the triangulation which determines how points will be inserted.
	 */
    private final IIncrementalTriangulation<V, E, F> triangulation;

	/**
	 * the collection of points P.
	 */
	private final Collection<IPoint> points;

	private boolean generated;

	/**
	 * <p>The default constructor.</p>
	 *  @param points        the collection of points P
	 * @param triangulation a triangulation which determines how points will be inserted
	 */
    public GenPointSetTriangulator(final Collection<IPoint> points, final IIncrementalTriangulation<V, E, F> triangulation) {
        this.triangulation = triangulation;
        this.points = points;
        this.generated = false;
    }

	/**
	 * <p>The default constructor.</p>
	 *  @param points        the collection of points P
	 * @param meshBuilder          an empty mesh
	 */
	public GenPointSetTriangulator(final Collection<IPoint> points, final ITriangleMeshBuilder<V, E, F> meshBuilder) {
		this.triangulation = IIncrementalTriangulation.createTriangulation(ITriangleMeshPointLocator.Type.JUMP_AND_WALK, meshBuilder);
		this.points = points;
		this.generated = false;
	}


	@Override
    public IIncrementalTriangulation<V, E, F> generate() {
		return generate(true);
    }

	@Override
	public IIncrementalTriangulation<V, E, F> generate(boolean finalize) {
		if(!generated) {
			triangulation.init();
			triangulation.insert(points);

			if(finalize) {
				triangulation.finish();
			}

			generated = true;
		}
		return triangulation;
	}

	@Override
	public IIncrementalTriangulation<V, E, F> getTriangulation() {
		return triangulation;
	}

	@Override
	public IMeshDataStorage<V, E, F> getMeshDataStorage() {
		return triangulation.getMeshDataStorage();
	}

	@Override
	public ITriangleMeshBuilder<V, E, F> getMeshBuilder() { return triangulation.getMeshBuilder(); }
}
