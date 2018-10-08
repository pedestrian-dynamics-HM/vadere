package org.vadere.util.geometry.mesh.triangulation;

import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;
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
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * @author Benedikt Zoennchen
 *
 * This class is for computing the DelaunayTriangulation using the BowyerWatson-Algorithm. In average the algorithm should perfom in O(n log(n)) but
 * in degenerated cases its runtime can be in O(n^2) where n is the number of points.
 */
@Deprecated
public class BowyerWatsonSlow<P extends IPoint> {
    private List<Triple<P, P, P>> triangles;
    private Collection<P> points;
    private List<P> initPoints;
	private final BiFunction<Double, Double, P> pointConstructor;

    public BowyerWatsonSlow(final Collection<P> points, final BiFunction<Double, Double, P> pointConstructor) {
        this.points = points;
	    this.pointConstructor = pointConstructor;
    }

    public void execute() {
       // P bound = points.parallelStream().reduce(pointConstructor.apply(Double.MIN_VALUE, Double.MIN_VALUE), (a, b) -> pointConstructor.apply(Math.bound(a.getX(), b.getX()), Math.bound(a.getY(), b.getY())));
       // P min = points.parallelStream().reduce(pointConstructor.apply(Double.MAX_VALUE, Double.MAX_VALUE), (a, b) -> pointConstructor.apply(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY())));

	    P max = points.parallelStream().reduce(pointConstructor.apply(Double.MIN_VALUE,Double.MIN_VALUE), (a, b) -> pointConstructor.apply(Math.max(a.getX(), b.getX()), Math.max(a.getY(), b.getY())));
	    P min = points.parallelStream().reduce(pointConstructor.apply(Double.MAX_VALUE,Double.MAX_VALUE), (a, b) -> pointConstructor.apply(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY())));


	    VRectangle bound = new VRectangle(min.getX(), min.getY(), max.getX()-min.getX(), max.getY()- min.getY());
	    init(bound);
        points.stream().forEach(point -> handle(point));
        cleanUp();
    }

    /*public List<Triple<P, P, P>> getTriangles() {
        return triangles;
    }*/

	/*public void setTriangles(List<VTriangle> triangles) {
		this.triangles = triangles;
	}*/

    public List<VTriangle> getTriangles() {
	    return triangles.stream().map(this::pointsToTriangle).collect(Collectors.toList());
    }

    public Set<VLine> getEdges() {
        return triangles.parallelStream().map(triple -> pointsToTriangle(triple)).flatMap(triangle -> triangle.getLineStream()).collect(Collectors.toSet());
    }

    private void init(final VRectangle bound) {
        triangles = new ArrayList<>();
        initPoints = new ArrayList<>();
	    Triple<P, P, P> superTriangle = getSuperTriangle(bound);
        triangles.add(superTriangle);
	    initPoints.add(superTriangle.getLeft());
	    initPoints.add(superTriangle.getMiddle());
	    initPoints.add(superTriangle.getRight());
    }

    private Triple<P, P, P> getSuperTriangle(final VRectangle bound) {
        double gap = 1.0;
        double max = Math.max(bound.getWidth(), bound.getHeight());
        P p1 = pointConstructor.apply(bound.getX() - max - gap, bound.getY() - gap);
        P p2 = pointConstructor.apply(bound.getX() + 2 * max + gap, bound.getY() - gap);
        P p3 = pointConstructor.apply(bound.getX() + (max+2*gap)/2, bound.getY() + 2 * max+ gap);
        return ImmutableTriple.of(p1, p2, p3);
    }

    private void handle(final P point) {
        HashSet<Line> edges = new HashSet<>();

	    // This is way to expensive O(n) instead of O(log(n))
        Map<Boolean, List<Triple<P, P, P>>> partition = triangles.parallelStream().collect(Collectors.partitioningBy(t -> pointsToTriangle(t).isInCircumscribedCycle(point)));

	    List<Triple<P, P, P>> badTriangles = partition.get(true);
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


    private List<Line> getEdges(Triple<P, P, P> triangle) {
	    List<Line> list = new ArrayList<>();
	    list.add(new Line(triangle.getLeft(), triangle.getMiddle()));
	    list.add(new Line(triangle.getMiddle(), triangle.getRight()));
	    list.add(new Line(triangle.getRight(), triangle.getLeft()));
	    return list;
    }

    private void cleanUp() {
        triangles = triangles.stream().filter(triangle -> !isTriangleConnectedToInitialPoints(triangle)).collect(Collectors.toList());
    }

	public void removeTriangleIf(final Predicate<Triple<P, P, P>> predicate) {
		triangles.removeIf(predicate);
	}

    private boolean isTriangleConnectedToInitialPoints(final Triple<P, P, P> trianglePoints) {
        return Stream.of(pointsToTriangle(trianglePoints).getLines()).anyMatch(edge -> {
            VPoint p1 = new VPoint(edge.getP1().getX(), edge.getP1().getY());
            VPoint p2 = new VPoint(edge.getP2().getX(), edge.getP2().getY());
            return initPoints.stream().anyMatch(initPoint -> p1.equals(initPoint) || p2.equals(initPoint));
        });
    }

    private VTriangle pointsToTriangle(Triple<P, P, P> points) {
	    return new VTriangle(
			    new VPoint(points.getLeft().getX(), points.getLeft().getY()),
			    new VPoint(points.getMiddle().getX(), points.getMiddle().getY()),
			    new VPoint(points.getRight().getX(), points.getRight().getY()));
    }

    private class Line {
		final P p1;
		final P p2;

		private Line(P p1,  P p2) {
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

        Set<VPoint> points = new HashSet<>();
		/*points.add(new VPoint(20,20));
		points.add(new VPoint(20,40));
		points.add(new VPoint(75,53));
		points.add(new VPoint(80,70));*/

        Random r = new Random(1);
        for(int i=0; i<100; i++) {
            VPoint point = new VPoint(width*r.nextDouble(), height*r.nextDouble());
            points.add(point);
        }

        BowyerWatsonSlow<VPoint> bw = new BowyerWatsonSlow<VPoint>(points, (x, y) -> new VPoint(x, y));
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
        private Set<VPoint> points;
        private final int max;

        public Lines(final Set<VLine> edges, final Set<VPoint> points, final int max){
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
