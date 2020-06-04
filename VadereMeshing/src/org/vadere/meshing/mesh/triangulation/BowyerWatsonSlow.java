package org.vadere.meshing.mesh.triangulation;

import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;
import org.vadere.meshing.mesh.gen.IncrementalTriangulation;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VTriangle;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * @author Benedikt Zoennchen
 *
 * This class is for computing the Delaunay triangulation using the Bowyer-Watson algorithm.
 * In average the algorithm should perfom in O(n log(n)) but in degenerated cases its runtime can be in O(n^2)
 * where n is the number of points. This implementation is only used to compare results since it is an easy
 * version of the Bowyer-Watson algorithm. It can be used to check for correctness of more sophisticated
 * implementations line {@link IncrementalTriangulation}.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Delaunay_triangulation">Delaunay triangulation</a>
 * @see <a href="https://en.wikipedia.org/wiki/Bowyer%E2%80%93Watson_algorithm">Bowyer-Watson algorithm</a>
 */
@Deprecated
public class BowyerWatsonSlow {

	/**
	 * a {@link List} of triples {@link Triple} each defining a triangle.
	 */
    private List<Triple<IPoint, IPoint, IPoint>> triangles;

	/**
	 * a {@link Collection} containing all points of the triangulation.
	 */
	private Collection<IPoint> points;

	/**
	 * the so called virtual points i.e. points that are not part of the actual triangulation but
	 * help by constructing it.
	 */
    private List<IPoint> virtualPoints;

	/**
	 * indicates if the computation has been executed.
	 */
	private boolean finished;

	/**
	 * The default constructor.
	 *  @param points            a {@link Collections} of points which will be part of the Delaunay triangulation
	 *
	 */
    public BowyerWatsonSlow(final Collection<IPoint> points) {
        this.points = points;
	    this.finished = false;
    }

    private VPoint create(final double x, final double y) {
    	return new VPoint(x, y);
    }

	/**
	 * Computes the Delaunay triangulation of the points.
	 */
	public void execute() {
		// construct a new point which is upper right point of all points
	    IPoint max = points.parallelStream().reduce(create(Double.MIN_VALUE,Double.MIN_VALUE), (a, b) -> create(Math.max(a.getX(), b.getX()), Math.max(a.getY(), b.getY())));

	    // construct a new point which is lower left of all points.
	    IPoint min = points.parallelStream().reduce(create(Double.MAX_VALUE,Double.MAX_VALUE), (a, b) -> create(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY())));

	    // construct a bound containing all points by using the upper right and lower left point.
	    VRectangle bound = new VRectangle(min.getX(), min.getY(), max.getX()-min.getX(), max.getY()- min.getY());

	    // initialize the the computation by constructing a super triangle
	    init(bound);

	    // compute the triangulation by inserting all points
        points.stream().forEach(point -> insert(point));

        // remove the super triangle and its faces / triangles
        cleanUp();

        finished = true;
    }

    public List<VTriangle> getTriangles() {
		if(!finished) {
			execute();
		}
	    return streamTriangles().collect(Collectors.toList());
    }

    public Stream<VTriangle> streamTriangles() {
	    return streamTriples().map(tripple -> new VTriangle(
			    new VPoint(tripple.getLeft()),
			    new VPoint(tripple.getMiddle()),
			    new VPoint(tripple.getRight())));
    }

    public Set<VLine> getEdges() {
	    if(!finished) {
		    execute();
	    }
        return triangles.parallelStream().map(triple -> pointsToTriangle(triple)).flatMap(triangle -> triangle.streamLines()).collect(Collectors.toSet());
    }

	private void init(final VRectangle bound) {
		triangles = new ArrayList<>();
		virtualPoints = new ArrayList<>();
		Triple<IPoint, IPoint, IPoint> superTriangle = getSuperTriangle(bound);
		triangles.add(superTriangle);
		virtualPoints.add(superTriangle.getLeft());
		virtualPoints.add(superTriangle.getMiddle());
		virtualPoints.add(superTriangle.getRight());
	}

    private Triple<IPoint, IPoint, IPoint> getSuperTriangle(final VRectangle bound) {
        double gap = 1.0;
        double max = Math.max(bound.getWidth(), bound.getHeight());
        IPoint p1 = create(bound.getX() - max - gap, bound.getY() - gap);
        IPoint p2 = create(bound.getX() + 2 * max + gap, bound.getY() - gap);
        IPoint p3 = create(bound.getX() + (max+2*gap)/2, bound.getY() + 2 * max+ gap);
        return ImmutableTriple.of(p1, p2, p3);
    }

    private void insert(final IPoint point) {
        HashSet<Line> edges = new HashSet<>();

	    // This is way to expensive O(n) instead of O(log(n))
        Map<Boolean, List<Triple<IPoint, IPoint, IPoint>>> partition = triangles.parallelStream().collect(Collectors.partitioningBy(t -> pointsToTriangle(t).isInCircumscribedCycle(point)));

	    List<Triple<IPoint, IPoint, IPoint>> badTriangles = partition.get(true);
        triangles = partition.get(false);
        IntStream s;

        HashSet<Line> toRemove = new HashSet<>();

	    // duplicated edges
        badTriangles.stream().flatMap(t -> getEdges(t).stream()).forEach(line -> {
            if(!edges.add(line)) {
                toRemove.add(line);
            }
        });

        toRemove.stream().forEach(removeEdge -> edges.remove(removeEdge));

	    // identifier ?
        edges.stream().forEach(edge -> triangles.add(Triple.of(edge.p1, edge.p2, point)));
    }


    private List<Line> getEdges(Triple<IPoint, IPoint, IPoint> triangle) {
	    List<Line> list = new ArrayList<>();
	    list.add(new Line(triangle.getLeft(), triangle.getMiddle()));
	    list.add(new Line(triangle.getMiddle(), triangle.getRight()));
	    list.add(new Line(triangle.getRight(), triangle.getLeft()));
	    return list;
    }

    private void cleanUp() {
        triangles = triangles.stream().filter(triangle -> !isTriangleConnectedToInitialPoints(triangle)).collect(Collectors.toList());
    }

    private boolean isTriangleConnectedToInitialPoints(final Triple<IPoint, IPoint, IPoint> trianglePoints) {
        return Stream.of(pointsToTriangle(trianglePoints).getLines()).anyMatch(edge -> {
            VPoint p1 = new VPoint(edge.getP1().getX(), edge.getP1().getY());
            VPoint p2 = new VPoint(edge.getP2().getX(), edge.getP2().getY());
            return virtualPoints.stream().anyMatch(initPoint -> p1.equals(initPoint) || p2.equals(initPoint));
        });
    }

    private VTriangle pointsToTriangle(Triple<IPoint, IPoint, IPoint> points) {
	    return new VTriangle(
			    new VPoint(points.getLeft().getX(), points.getLeft().getY()),
			    new VPoint(points.getMiddle().getX(), points.getMiddle().getY()),
			    new VPoint(points.getRight().getX(), points.getRight().getY()));
    }

	public Stream<Triple<IPoint, IPoint, IPoint>> streamTriples() {
		if(!finished) {
			execute();
		}
		return triangles.stream();
	}

	public Stream<IPoint> streamPoints() {
		if(!finished) {
			execute();
		}
		return points.stream();
	}

	private class Line {
		final IPoint p1;
		final IPoint p2;

		private Line(IPoint p1, IPoint p2) {
			this.p1 = p1;
			this.p2 = p2;
		}

	    @Override
	    public boolean equals(Object o) {
		    if (this == o) return true;
		    if (o == null || getClass() != o.getClass()) return false;

		    Line line = (Line) o;

		    return (p1.equals(line.p1) && p2.equals(line.p2)) || (p2.equals(line.p1) && p1.equals(line.p2));

	    }

	    @Override
	    public int hashCode() {
		    return p1.hashCode() * p2.hashCode();
	    }
    }







    // TODO: the following code can be deleted, this is only for visual checks
    public static void main(String[] args) {
        // TODO Auto-generated method stub
        int height = 1000;
        int width = 1000;
        int max = Math.max(height, width);

        Set<IPoint> points = new HashSet<>();
		/*points.add(new VPoint(20,20));
		points.add(new VPoint(20,40));
		points.add(new VPoint(75,53));
		points.add(new VPoint(80,70));*/

        Random r = new Random(1);
        for(int i=0; i<100; i++) {
            VPoint point = new VPoint(width*r.nextDouble(), height*r.nextDouble());
            points.add(point);
        }

        BowyerWatsonSlow bw = new BowyerWatsonSlow(points);
        bw.execute();
        Set<VLine> edges = bw.getEdges();

        JFrame window = new JFrame();
        window.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        window.setBounds(0, 0, max, max);
        window.getContentPane().add(new Lines(edges, points, max));
        window.setVisible(true);
    }

    private static class Lines extends JComponent{
        private Set<VLine> edges;
        private Set<IPoint> points;
        private final int max;

        public Lines(final Set<VLine> edges, final Set<IPoint> points, final int max){
            this.edges = edges;
            this.points = points;
            this.max = max;
        }

        public void paint(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setBackground(Color.white);
            g2.setStroke(new BasicStroke(1.0f));
            g2.setColor(Color.gray);

            edges.stream().forEach(edge -> {
                Shape k = new VLine(edge.getP1().getX(), edge.getP1().getY(), edge.getP2().getX(), edge.getP2().getY());
                g2.draw(k);
            });

            points.stream().forEach(point -> {
                VCircle k = new VCircle(point.getX(), point.getY(), 1.0);
                g2.draw(k);
            });

        }
    }
}
