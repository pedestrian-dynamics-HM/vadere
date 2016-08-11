package org.vadere.util.io;

import java.lang.reflect.Type;
import java.util.List;

import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.ShapeType;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VPolygon;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;

import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class JsonSerializerVShape implements JsonSerializer<VShape>,
		JsonDeserializer<VShape> {

	private static class Polygon2DStore {
		public Polygon2DStore(VPolygon polygon) {
			this.points = polygon.getPoints();
			if (this.points.size() > 0) {
				// move the last point to the front to match the order of
				// this.points.getPoints
				this.points.add(0, this.points.remove(this.points.size() - 1));
			}
		}

		public ShapeType type = ShapeType.POLYGON;
		public List<VPoint> points;
	}

	private static class VRectangleStore {
		public VRectangleStore(VRectangle shape) {
			this.x = shape.x;
			this.y = shape.y;
			this.width = shape.width;
			this.height = shape.height;
		}

		public double x;
		public double y;
		public double width;
		public double height;
		public ShapeType type = ShapeType.RECTANGLE;
	}

	private static class CircleStore {
		public CircleStore(VCircle shape) {
			this.radius = shape.getRadius();
			this.center = shape.getCenter();
		}

		public double radius;
		public VPoint center;
		public ShapeType type = ShapeType.CIRCLE;
	}

	@Override
	public VShape deserialize(JsonElement arg0, Type arg1,
			JsonDeserializationContext arg2) throws JsonParseException {

		JsonObject shapeObj = arg0.getAsJsonObject();
		Gson g = IOUtils.getGson();

		ShapeType shapeType = g.fromJson(shapeObj.get("type"), ShapeType.class);

		switch (shapeType) {
			case CIRCLE:
				CircleStore circleStore = g.fromJson(shapeObj, CircleStore.class);
				return new VCircle(circleStore.center, circleStore.radius);
			case POLYGON:
				Polygon2DStore polygonStore = g.fromJson(shapeObj,
						Polygon2DStore.class);
				return GeometryUtils.polygonFromPoints2D(polygonStore.points);
			case RECTANGLE:
				VRectangleStore rectangleStore = g.fromJson(shapeObj,
						VRectangleStore.class);
				return new VRectangle(rectangleStore.x, rectangleStore.y, rectangleStore.width, rectangleStore.height);
			default:
				break;
		}
		return null;
	}

	@Override
	public JsonElement serialize(VShape shape, Type arg1,
			JsonSerializationContext arg2) {

		Gson g = IOUtils.getGson();

		if (shape instanceof VPolygon) {
			Polygon2DStore pstore = new Polygon2DStore((VPolygon) shape);
			return g.toJsonTree(pstore);
		}
		if (shape instanceof VRectangle) {
			VRectangleStore pstore = new VRectangleStore((VRectangle) shape);
			return g.toJsonTree(pstore);
		}
		if (shape instanceof VCircle) {
			CircleStore cstore = new CircleStore((VCircle) shape);
			return g.toJsonTree(cstore);
		}

		return null;
	}

}
