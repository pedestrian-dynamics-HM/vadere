package org.vadere.util.geometry.rtree;

import com.github.davidmoten.rtree.RTree;
import com.github.davidmoten.rtree.geometry.Geometry;
import com.github.davidmoten.rtree.geometry.Rectangle;
import com.github.davidmoten.rtree.geometry.internal.LineDouble;
import com.github.davidmoten.rtree.geometry.internal.PointDouble;

import org.junit.Test;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VPolygon;
import org.vadere.util.geometry.shapes.VRectangle;

import java.util.Objects;

import static org.junit.Assert.assertEquals;

public class TestRTree {

	@Test
	public void searchRectangles(){
		RTree<String, VRectangle> rectangleRTree = RTree.create();
		rectangleRTree = rectangleRTree.add("rec1", new VRectangle(0, 0, 1, 1));
		rectangleRTree = rectangleRTree.add("rec2", new VRectangle(1.05, 1.05, 1, 1));
		rectangleRTree = rectangleRTree.add("rec3", new VRectangle(0.5, 0.6, 1, 3));

		Rectangle rectangle = rectangleRTree.mbr().get();
		Rectangle mbr = new VRectangle(0, 0, 2.05, 3.6).mbr();
		assertEquals(mbr, rectangle);

		var searchResult = rectangleRTree.search(rectangle);
		searchResult.count().subscribe(c -> assertEquals(3, c.intValue()));

		searchResult = rectangleRTree.search(new VRectangle(0.5, 0.6, 1, 3).mbr());
		searchResult.count().subscribe(c -> assertEquals(3, c.intValue()));

		searchResult = rectangleRTree.search(new VRectangle(0, 0, 1, 1).mbr());
		searchResult.count().subscribe(c -> assertEquals(2, c.intValue()));

		searchResult = rectangleRTree.search(new VRectangle(3, 3, 1, 1).mbr());
		searchResult.count().subscribe(c -> assertEquals(0, c.intValue()));
	}

	@Test
	public void searchPolygons(){
		RTree<String, VPolygon> polygonRTree = RTree.create();
		polygonRTree = polygonRTree.add("poly1", new VRectangle(0, 0, 1, 1).toPolygon());
		polygonRTree = polygonRTree.add("poly2", new VRectangle(1.05, 1.05, 1, 1).toPolygon());
		polygonRTree = polygonRTree.add("poly3", new VRectangle(0.5, 0.6, 1, 3).toPolygon());

		Rectangle rectangle = polygonRTree.mbr().get();
		Rectangle mbr = new VRectangle(0, 0, 2.05, 3.6).mbr();
		assertEquals(mbr, rectangle);

		var searchResult = polygonRTree.search(rectangle);
		searchResult.count().subscribe(c -> assertEquals(3, c.intValue()));

		searchResult = polygonRTree.search(new VRectangle(0.5, 0.6, 1, 3).mbr());
		searchResult.count().subscribe(c -> assertEquals(3, c.intValue()));

		searchResult = polygonRTree.search(new VRectangle(0, 0, 1, 1).mbr());
		searchResult.count().subscribe(c -> assertEquals(2, c.intValue()));

		searchResult = polygonRTree.search(new VRectangle(3, 3, 1, 1).mbr());
		searchResult.count().subscribe(c -> assertEquals(0, c.intValue()));
	}

	@Test
	public void searchLines(){
		RTree<String, VLine> lineRTree = RTree.create();
		VLine line1 = new VLine(0, 0, 1, 1);
		VLine line2 = new VLine(2, 0, 4, 0);
		VLine line3 = new VLine(0, 2, 1, -1);
		VLine line4 = new VLine(-1, 2, 4, -1);

		lineRTree = lineRTree.add("line1", line1);
		lineRTree = lineRTree.add("line2", line2);
		lineRTree = lineRTree.add("line3", line3);
		lineRTree = lineRTree.add("line4", line4);

		Rectangle rectangle = lineRTree.mbr().get();
		Rectangle mbr = new VRectangle(-1, -1, 5, 3).mbr();
		assertEquals(mbr, rectangle);

		lineRTree.search(line1.mbr()).count().subscribe(c -> assertEquals(3, c.intValue()));
		lineRTree.search(new VRectangle(-0.1, -0.9, 1, 1).mbr()).count().subscribe(c -> assertEquals(2, c.intValue()));

		lineRTree.search(line1.mbr()).count().subscribe(System.out::println);
		lineRTree.search(line1.mbr()).subscribe(System.out::println);
		lineRTree.search(line1.mbr()).subscribe(System.out::println);
	}

	/*@Test
	public void deleteLines(){
		RTree<String, VLine> lineRTree = RTree.create();
		VLine line1 = new VLine(0, 0, 1, 1);
		VLine line2 = new VLine(0, 0, 1, 1);
		VLine line3 = new VLine(0, 2, 1, -1);
		VLine line4 = new VLine(-1, 2, 4, -1);

		lineRTree = lineRTree.add(line1.toString(), line1);
		lineRTree = lineRTree.add(line2.toString(), line2);
		lineRTree = lineRTree.add(line3.toString(), line3);
		lineRTree = lineRTree.add(line4.toString(), line4);

		lineRTree = lineRTree.delete(line1.toString(), line2);

		lineRTree.entries().count().subscribe(c -> assertEquals(3, c.intValue()));

		lineRTree.entries().subscribe(System.out::println);
	}*/

	@Test
	public void searchPoints(){
		RTree<String, VPoint> pointRTree = RTree.create();

		pointRTree = pointRTree.add("point1", new VPoint(0,0));
		pointRTree = pointRTree.add("point2", new VPoint(0,1));
		pointRTree = pointRTree.add("point3", new VPoint(1, 0));
		pointRTree = pointRTree.add("point4", new VPoint(1, 1));

		Rectangle rectangle = pointRTree.mbr().get();
		Rectangle mbr = new VRectangle(0, 0, 1, 1).mbr();
		assertEquals(mbr, rectangle);

		pointRTree.search(PointDouble.create(0, 0)).count().subscribe(c -> assertEquals(1, c.intValue()));
		pointRTree.search(new VRectangle(0,0,1,1).mbr()).count().subscribe(c -> assertEquals(4, c.intValue()));
		pointRTree.search(new VRectangle(0.5,0,1,1).mbr()).count().subscribe(c -> assertEquals(2, c.intValue()));
	}

	@Test
	public void searchMixed(){
		RTree<String, Geometry> rTree = RTree.create();

		rTree = rTree.add("point1", new VPoint(0,0));
		rTree = rTree.add("point2", new VPoint(0,1));
		rTree = rTree.add("point3", new VPoint(1, 0));
		rTree = rTree.add("line1", new VLine(0, 0, 2, 2));

		Rectangle rectangle = rTree.mbr().get();
		Rectangle mbr = new VRectangle(0, 0, 2, 2).mbr();
		assertEquals(mbr, rectangle);

		// search for point 0,0 and line starting at 0,0
		rTree.search(PointDouble.create(0, 0)).count().subscribe(c -> assertEquals(2, c.intValue()));

		rTree.search(new VRectangle(0,0,1,1).mbr()).count().subscribe(c -> assertEquals(4, c.intValue()));

		rTree.search(new VRectangle(1.1,1.1,1,1).mbr()).count().subscribe(c -> assertEquals(1, c.intValue()));

		rTree.entries().toBlocking().subscribe(System.out::println);

		rTree.search(new VRectangle(1.1,1.1,1,1).mbr()).toBlocking().subscribe(System.out::println);
	}
}
