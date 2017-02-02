package org.vadere.util.triangulation;

import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VTriangle;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.swing.*;
/**
 * @author Benedikt Zoennchen
 *
 * This class is for computing the DelaunayTriangulation using the BowyerWatson-Algorithm. In average the algorithm should perfom in O(n LOG(n)) but
 * in degenerated cases its runtime can be in O(n^2) where n is the number of points.
 */
public class BowyerWatson3 {
	private List<VTriangle> triangles;
	private Collection<VPoint> points;
	private List<VPoint> initPoints;

	public BowyerWatson3(final Collection<VPoint> points) {
		this.points = points;
	}

	public void execute() {
		VPoint max = points.parallelStream().reduce(new VPoint(Integer.MIN_VALUE,Integer.MIN_VALUE), (a, b) -> new VPoint(Math.max(a.getX(), b.getX()), Math.max(a.getY(), b.getY())));
		VPoint min = points.parallelStream().reduce(new VPoint(Integer.MAX_VALUE,Integer.MAX_VALUE), (a, b) -> new VPoint(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY())));

		VRectangle bound = new VRectangle(min.getX(), min.getY(), max.getX()-min.getX(), max.getY()- min.getY());

		init(bound);

		points.stream().forEach(point -> handle(point));
		cleanUp();
	}

	public List<VTriangle> getTriangles() {
		return triangles;
	}

	public Set<VLine> getEdges() {
		return triangles.parallelStream().flatMap(triangle -> Stream.of(triangle.getLines())).collect(Collectors.toSet());
	}

	private void init(final VRectangle bound) {
		triangles = new ArrayList<>();
		initPoints = new ArrayList<>();
		VTriangle superTriangle = getSuperTriangle(bound);
		triangles.add(superTriangle);
		initPoints.addAll(superTriangle.getPoints());
	}

	private VTriangle getSuperTriangle(final VRectangle bound) {
		double gap = 1.0;
		double max = Math.max(bound.getWidth(), bound.getHeight());
		VPoint p1 = new VPoint(bound.getX() - max - gap, bound.getY() - gap);
		VPoint p2 = new VPoint(bound.getX() + 2 * max + gap, bound.getY() - gap);
		VPoint p3 = new VPoint(bound.getX() + (max+2*gap)/2, bound.getY() + 2 * max+ gap);
		return new VTriangle(p1, p2, p3);
	}

	private void handle(final VPoint point) {
		HashSet<VLine> edges = new HashSet<>();

		Map<Boolean, List<VTriangle>> partition = triangles.parallelStream().collect(Collectors.partitioningBy(triangle -> triangle.isInCircumscribedCycle(point)));
		List<VTriangle> badTriangles = partition.get(true);
		triangles = partition.get(false);
		IntStream s;

		HashSet<VLine> toRemove = new HashSet<>();
		// duplicated edges
		badTriangles.stream().flatMap(tri -> Stream.of(tri.getLines())).forEach(line -> {
			if(!edges.add(line)) {
				toRemove.add(line);
			}
		});

		toRemove.stream().forEach(removeEdge -> edges.remove(removeEdge));

		edges.stream().forEach(edge -> {
			String[] id = edge.getIdentifier().split(":");
			VPoint p1 = new VPoint(edge.getP1().getX(), edge.getP1().getY(), Integer.parseInt(id[0]));
			VPoint p2 = new VPoint(edge.getP2().getX(), edge.getP2().getY(), Integer.parseInt(id[1]));
			triangles.add(new VTriangle(p1, p2, point));
		});
	}

	private void cleanUp() {
		triangles = triangles.stream().filter(triangle -> !isTriangleConnectedToInitialPoints(triangle)).collect(Collectors.toList());
	}

	private boolean isTriangleConnectedToInitialPoints(final VTriangle triangle) {
		return Stream.of(triangle.getLines()).anyMatch(edge -> {
			VPoint p1 = new VPoint(edge.getP1().getX(), edge.getP1().getY());
			VPoint p2 = new VPoint(edge.getP2().getX(), edge.getP2().getY());
			return initPoints.stream().anyMatch(initPoint -> p1.equals(initPoint) || p2.equals(initPoint));
		});
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

		Random r = new Random();
		for(int i=0; i<10000; i++) {
			VPoint point = new VPoint(width*r.nextDouble(), height*r.nextDouble());
			points.add(point);
		}

		BowyerWatson3 bw = new BowyerWatson3(points);
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

	public void setTriangles(List<VTriangle> triangles) {
		this.triangles = triangles;
	}
}

