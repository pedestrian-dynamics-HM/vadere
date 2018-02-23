package org.vadere.util.geometry.mesh.gen;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.vadere.util.geometry.mesh.inter.IFace;
import org.vadere.util.geometry.mesh.inter.IHalfEdge;
import org.vadere.util.geometry.mesh.inter.IMesh;
import org.vadere.util.geometry.mesh.inter.IPointLocator;
import org.vadere.util.geometry.mesh.inter.ITriangulation;
import org.vadere.util.geometry.mesh.inter.IVertex;
import org.vadere.util.geometry.shapes.IPoint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.function.Supplier;

/**
 * @author Benedikt Zoennchen
 *
 * The Delaunay-Hierarchy is a data structure which accelerates the point location problem i.e.
 * given a point p=(x,y) find the triangle which contains p. Let P be the set of all points of the
 * triangulation. The Delaunay-Hierarchy is a hierachy of Delaunay-Triangulations T_0, T_1, ..., T_k
 * of point sets P_0, P_1, ..., P_k such that P_0 = P, P_k is a subset of P_{k-1}.
 *
 * To find the triangle t which contains p the algorithm starts at the hierarchy k and
 * finds the triangle t_k which contains p (by starting from some t_k' triangle of T_k and walking towards t_k).
 * By using some point p_k of t_k the next search (walk) starts from triangle t_{k-1}' which has p_k as its point
 * towards t_{k-1}. This will be repeated until the hierachy 0 and t is reached.
 *
 * For more informations see devillers-2002 (The Delaunay Hierarchy).
 *
 * The {@link DelaunayHierarchy} is also a {@link org.vadere.util.geometry.mesh.inter.ITriEventListener}. It listens to
 * vertex insert events to update itself.
 *
 * Note that any insertion / deletion of a point into / from the triangulation has to be propagated to its Delaunay-Hierarchy.
 *
 * In the current state the Delaunay-Hierarchy does only support triangulations without holes.
 *
 * @param <P>   the type of the points
 * @param <V>   the type of the vertices
 * @param <E>   the type of the half-edges
 * @param <F>   the type of the faces
 */
public class DelaunayHierarchy<P extends IPoint, V extends IVertex<P>, E extends IHalfEdge<P>, F extends IFace<P>> implements IPointLocator<P, V, E, F>  {
	private static Logger log = LogManager.getLogger(DelaunayHierarchy.class);

	/**
	 * Contains T_0, T_1, ..., T_k
	 */
	private List<ITriangulation<P, V, E, F>> hierarchySets;

	/**
	 * Connects t_{k} and t_{k-1}' by connecting its vertices.
	 */
	private List<Map<V, V>> hierarchyConnector;

	/**
	 * T_0, which might contain holes i.e. polygons which are not triangulated.
	 */
	private ITriangulation<P, V, E, F> base;

	/**
	 * T_1, which contains all the points of T_0 but no holes. Therefore we can
	 * walk through T_1 to find the face of T_0.
	 */
	private ITriangulation<P, V, E, F> preBase;

	/**
	 * A supplier to create T_1, ..., T_k.
	 */
	private Supplier<ITriangulation<P, V, E, F>> triangulationSupplier;

	/**
	 * Parameters choosen from devillers-2002 (The Delaunay Hierarchy)
	 */
	private double alpha = 30;
    private int maxLevel = 6; // 6 instead of 5 since we have one extra preBase level
    private int minSize = 20;

	/**
	 * Caches the last search result to start from for consecutive searches.
	 */
	private LinkedList<F> prevLocationResult;

	/**
	 * Caches the last search result (only the point).
	 */
    private P prevLocationPoint;

    private double epsilon = 0.00001;

    private Random random;

	/**
	 * Default constructor of the Delaunay-Hierarchy. The base might be a
	 * triangulation which already contains points.
	 *
	 * @param base                  T_0
	 * @param triangulationSupplier a supplier to construct T_k, k > 0.
	 */
	public DelaunayHierarchy(
    		@NotNull final ITriangulation<P, V, E, F> base,
		    @NotNull final Supplier<ITriangulation<P, V, E, F>> triangulationSupplier) {
        this.hierarchySets = new ArrayList<>(maxLevel);
        this.hierarchyConnector = new ArrayList<>(maxLevel);
        this.random = new Random();
        this.triangulationSupplier = triangulationSupplier;
        this.base = base;
        this.prevLocationPoint = null;
        this.prevLocationResult = null;
        this.init();
    }

	/**
	 * Initializes the Delaunay-Hierarchy by connecting all virtual vertices of the
	 * triangulations (connecting T_k -> T_{k-1}). And inserting all vertices of the
	 * base triangulation which might already contain some points.
	 */
    private void init() {
        base.init();
        hierarchySets.add(base);
        hierarchyConnector.add(new HashMap<>());

        for(int i = 1; i <= maxLevel; i++) {
            ITriangulation<P, V, E, F> triangulation = triangulationSupplier.get();
            triangulation.init();
            hierarchySets.add(triangulation);

            if(i == 0) {
            	preBase = triangulation;
            }

            List<V> superTriangleVertices = triangulation.getVirtualVertices();
            List<V> superTrianglesLastVertices = getLevel(i-1).getVirtualVertices();

            for(int j = 0; j < superTriangleVertices.size(); j++) {
                //i -> i -1
                setDown(superTriangleVertices.get(j), superTrianglesLastVertices.get(j), i);
            }
            hierarchyConnector.add(new HashMap<>());
        }

        // if the triangulation does already contain points => insert them!
        for(V v : base.getMesh().getVertices()) {
			postInsertEvent(v);
        }
    }

    @Override
    public void postSplitTriangleEvent(F original, F f1, F f2, F f3) {}

    @Override
    public void postSplitHalfEdgeEvent(F original, F f1, F f2) {}

    @Override
    public void postFlipEdgeEvent(F f1, F f2) {}

    @Override
    public void postInsertEvent(@NotNull final V vertex) {
        P p = base.getMesh().getPoint(vertex);
        V prev = vertex;

        if(!p.equals(prevLocationPoint)) {
            prevLocationResult = locateAll(p);
            prevLocationPoint = p;
        }

	    /**
	     * Get the level at which the point will be inserted.
	     */
	    int vertexLevel = randomLevel();

	    /**
	     * We insert the point at least into T_1 to support holes in T_0.
	     */
	    assert vertexLevel <= 1;

        Iterator<F> locatedFaces = prevLocationResult.iterator();

        /**
         * Skip the 0 level, since the point is already inserted
         */
        locatedFaces.next();

        for(int i = 1; i <= vertexLevel; i++) {
            V v;
            ITriangulation<P, V, E, F> tri = getLevel(i);
            if(locatedFaces.hasNext()) {
                v = tri.getMesh().getVertex(tri.insert(p, locatedFaces.next()));
            }
            else {
                v = tri.getMesh().getVertex(tri.insert(p));
            }

            setDown(v, prev, i);
            prev = v;
        }
    }

    private ITriangulation<P, V, E, F> getLevel(final int level) {
        if(level > maxLevel) {
            throw new IllegalArgumentException("level is greater than the maximum level.");
        }

        return hierarchySets.get(level);
    }

    private void setDown(V src, V dest, int srcLevel) {
        getLevel(srcLevel).getMesh().setDown(src, dest);
    }

    private V getDown(V src, int srcLevel) {
        // srcLevel-1 since the resulting vertex is contained in the mesh one level below the src vertex!
        return getLevel(srcLevel-1).getMesh().getDown(src);
    }

    private int randomLevel() {
        int level = 1;
        while (random.nextDouble() < 1/alpha && level < maxLevel) {
            level++;
        }
        return level;
    }

    @Override
    public F locatePoint(P point, boolean insertion) {
        Optional<F> optFace = locate(point);
        if(optFace.isPresent()) {
            return optFace.get();
        }
        else {
            throw new IllegalArgumentException(point + " is invalid, it can not be located by " + this);
        }
    }

	/**
	 * Locates for some level k >= 1 all triangles t_k, t_{k-1}, ..., t_0 which are part of
	 * T_k, T_{k-1}, ..., T_k. The location of some t_j uses the result of locating t_{j+1}.
	 *
	 * @param point the point we want to locate.
	 * @return      returns a list of faces [t_k, t_{k-1}, ..., t_0] for which p is contained in any of these faces. Only t_0 might be not a triangle.
	 */
	private LinkedList<F> locateAll(final P point) {
        // find the highest level with enough vertices
        int level = maxLevel;

        while (level > 1 && getLevel(level).getMesh().getNumberOfVertices() < minSize) {
            level--;
        }

        LinkedList<F> faces = new LinkedList<>();
        V v = null;
        F face;
        while (level >= 1) {
            ITriangulation<P, V, E, F> tri = getLevel(level);

            //TODO: SE-Architecture dirty here!
            if(v == null) {
                if(level == 1) {
                    face = tri.straightWalk2D(point.getX(), point.getY(), tri.getMesh().getFace());
                }
                else {
                    face = tri.locateFace(point).get();
                }

                v = tri.getMesh().closestVertex(face, point.getX(), point.getY());
                //tri.getMesh().getVertex(tri.getMesh().getEdge(face));
                //v = tri.locateNearestNeighbour(point, face);
            }
            else {
                // level+1 -> level
                v = getDown(v, level+1);

                //TODO: SE-Architecture dirty here!
                if(level == 1) {
                    face = tri.straightWalk2D(point.getX(), point.getY(), tri.getMesh().getFace(v));
                }
                else {
                    face = tri.locateFace(point, tri.getMesh().getFace(v)).get();
                }

                //v = tri.locateNearestNeighbour(point, face);
                v = tri.getMesh().closestVertex(face, point.getX(), point.getY());
            }
	        faces.addFirst(face);
	        level--;
        }

	    /**
	     * At this point we have v_1 of T_1 which coordinates (x,y). The face we are searching is
	     * connected to v_0!
	     */
	    assert level == 0;
	    v = getDown(v, level+1);
	    ITriangulation<P, V, E, F> tri = getLevel(level);

	    /**
	     * Contains should also work for holes!
	     */
		face = tri.getMesh().streamFaces(v).filter(f -> tri.contains(point.getX(), point.getY(), f)).findAny().get();
	    faces.addFirst(face);

        return faces;
    }

    @Override
    public Optional<F> locate(final P point) {
        LinkedList<F> allFaces = locateAll(point);
        prevLocationResult = allFaces;
        prevLocationPoint = point;
        if(!allFaces.isEmpty()) {
            return Optional.of(allFaces.getFirst());
        }
        else {
            return Optional.empty();
        }
    }

    @Override
    public Optional<F> locate(double x, double y) {
        return locate(base.getMesh().createPoint(x, y));
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("");
        for(int i = 0; i < hierarchySets.size(); i++) {
            builder.append("["+i+"]:" + hierarchySets.get(i).getMesh().getNumberOfVertices()+"\n");
        }
        return builder.toString();
    }

    /**
     * Returns vertex of the triangulation of the face with the smallest distance to point.
     * Assumption: The face has to be part of the mesh of the triangulation.
     *
     * @param triangulation the triangulation
     * @param face          the face of the trianuglation
     * @param point         the point
     * @return vertex of the triangulation of the face with the smallest distance to point
     */
    public V getNearestPoint(final ITriangulation<P, V, E, F> triangulation, final F face, final P point) {
        IMesh<P, V, E, F> mesh = triangulation.getMesh();
        return mesh.streamEdges(face).map(edge -> mesh.getVertex(edge)).reduce((p1, p2) -> p1.distance(point) > p2.distance(point) ? p2 : p1).get();
	}
}
