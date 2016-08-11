package org.vadere.util.geometry.jts;

import org.vadere.util.geometry.shapes.VRectangle;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.util.GeometricShapeFactory;


public class ShapeConverter {

	public Polygon rectangleToPoylgon(final VRectangle rectangle) {

		GeometricShapeFactory gsf = new GeometricShapeFactory();
		gsf.setBase(new Coordinate(rectangle.getX(), rectangle.getY()));
		gsf.setWidth(rectangle.getWidth());
		gsf.setWidth(rectangle.getHeight());


		return gsf.createRectangle();
	}

}
