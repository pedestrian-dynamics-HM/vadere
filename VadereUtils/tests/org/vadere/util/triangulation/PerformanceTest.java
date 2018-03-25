package org.vadere.util.triangulation;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.triangulate.DelaunayTriangulationBuilder;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.util.geometry.mesh.gen.AFace;
import org.vadere.util.geometry.mesh.gen.AHalfEdge;
import org.vadere.util.geometry.mesh.gen.AVertex;
import org.vadere.util.geometry.mesh.gen.PFace;
import org.vadere.util.geometry.mesh.gen.PHalfEdge;
import org.vadere.util.geometry.mesh.gen.PVertex;
import org.vadere.util.geometry.mesh.inter.IPointLocator;
import org.vadere.util.geometry.mesh.inter.ITriangulation;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.voronoi.VoronoiDiagram;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Created by bzoennchen on 06.03.18.
 */
public class PerformanceTest {

	private static Logger log = LogManager.getLogger(TestBoyerWatson.class);

	public static void main(String... args) {
		testPerformanceForDifferentPointLocators();
	}

	public static void testPerformanceForDifferentPointLocators() {
		List<VPoint> points = new ArrayList<>();
		int width = 300;
		int height = 300;
		Random r = new Random();
		assert false;
		int numberOfPoints = 8000;

		for(int i=0; i< numberOfPoints; i++) {
			VPoint point = new VPoint(width*r.nextDouble(), height*r.nextDouble());
			points.add(point);
		}

		/*Collections.sort(points, (p1, p2) -> {
			if(p1.getX() > p2.getX()){
				return -1;
			}
			else {
				return 1;
			}
		});*/




		long ms = System.currentTimeMillis();
		ITriangulation<VPoint, PVertex<VPoint>, PHalfEdge<VPoint>, PFace<VPoint>> pdelaunayTriangulation = ITriangulation.createPTriangulation(IPointLocator.Type.DELAUNAY_HIERARCHY, points, (x, y) -> new VPoint(x, y));
		pdelaunayTriangulation.finish();
		log.info("runtime of the BowyerWatson for " + numberOfPoints + " vertices =" + (System.currentTimeMillis() - ms) + " using the delaunay-hierarchy and a pointer-based data structure");

		ms = System.currentTimeMillis();
		ITriangulation<VPoint, AVertex<VPoint>, AHalfEdge<VPoint>, AFace<VPoint>> adelaunayTriangulation = ITriangulation.createATriangulation(IPointLocator.Type.DELAUNAY_HIERARCHY, points, (x, y) -> new VPoint(x, y));
		adelaunayTriangulation.finish();
		log.info("runtime of the BowyerWatson for " + numberOfPoints + " vertices =" + (System.currentTimeMillis() - ms) + " using the delaunay-hierarchy and a array-based data structure");

		VoronoiDiagram voronoiDiagram = new VoronoiDiagram(new VRectangle(0,0,width, height));
		ms = System.currentTimeMillis();
		voronoiDiagram.computeVoronoiDiagram(points);
		log.info("runtime of the Sweepline for " + numberOfPoints + " vertices =" + (System.currentTimeMillis() - ms) + " using the vadere-voronoi");


		log.info("start checking the delaunay property, this can take some time");
		/*Collection<VTriangle> triangles = delaunayTriangulation.streamTriangles().collect(Collectors.toList());

		for(VTriangle triangle : triangles) {

			List<VPoint> trianglePoints = triangle.getPoints();

			for(VTriangle t : triangles) {
				assertTrue(t.getPoints().stream().noneMatch(p -> !trianglePoints.contains(p) && triangle.isInCircumscribedCycle(p)));
			}
		}
		log.info("end checking the delaunay property");*/

		log.info("check vertex adjustment");
		/*final IMesh<VPoint, PVertex<VPoint>, PHalfEdge<VPoint>, PFace<VPoint>> mesh1 = delaunayTriangulation.getMesh();
		mesh1.streamEdges().forEach(edge -> assertEquals(mesh1.getVertex(edge), mesh1.getVertex(mesh1.getEdge(mesh1.getVertex(edge)))));*/

		ms = System.currentTimeMillis();
		//ITriangulation<VPoint, PVertex<VPoint>, PHalfEdge<VPoint>, PFace<VPoint>> delaunayTriangulation = ITriangulation.createPTriangulation(IPointLocator.Type.DELAUNAY_HIERARCHY, points, (x, y) -> new VPoint(x, y));
		//delaunayTriangulation.finish();
		log.info("runtime of the BowyerWatson for " + numberOfPoints + " vertices =" + (System.currentTimeMillis() - ms) + " using the delaunay-hierarchy");

		/*log.info("start checking the delaunay property, this can take some time");
		List<VTriangle> triangles = delaunayTriangulation.streamTriangles().collect(Collectors.toList());

		for(VTriangle triangle : triangles) {

			List<VPoint> trianglePoints = triangle.getPoints();

			for(VTriangle t : triangles) {
				assertTrue(t.getPoints().stream().noneMatch(p -> !trianglePoints.contains(p) &&
						triangle.isInCircumscribedCycle(p)));
			}
		}
		log.info("end checking the delaunay property");*/

		log.info("check vertex adjustment");
		/*final IMesh<VPoint, PVertex<VPoint>, PHalfEdge<VPoint>, PFace<VPoint>> mesh2 = delaunayTriangulation.getMesh();
		mesh2.streamEdges().forEach(edge -> assertEquals(mesh2.getVertex(edge), mesh2.getVertex(mesh2.getEdge(mesh2.getVertex(edge)))));*/

		GeometryFactory fact = new GeometryFactory();
		Collection<Coordinate> coords = points.stream().map(p -> new Coordinate(p.getX(), p.getY())).collect(Collectors.toList());
		ms = System.currentTimeMillis();
		DelaunayTriangulationBuilder builder = new DelaunayTriangulationBuilder();
		builder.setSites(coords);
		builder.getTriangles(fact);
		log.info("runtime of the ? for " + numberOfPoints + " vertices =" + (System.currentTimeMillis() - ms) + " using the JTS-Delaunay-Triangulation");
	}

}
