package org.vadere.state.util;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.vadere.state.attributes.scenario.AttributesSource;
import org.vadere.state.psychology.perception.json.StimulusInfoStore;
import org.vadere.state.scenario.Source;
import org.vadere.util.geometry.shapes.ShapeType;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.state.scenario.DynamicElement;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.types.ScenarioElementType;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VPolygon;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class JacksonObjectMapper extends ObjectMapper {

	private static final long serialVersionUID = 1L;

	public JacksonObjectMapper() {
		configure(DeserializationFeature.ACCEPT_FLOAT_AS_INT, true); // otherwise 4.7 will automatically be casted to 4 for integers, with this it throws an error
		enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION); // forbids duplicate keys
		disable(SerializationFeature.FAIL_ON_EMPTY_BEANS); // to allow empty attributes like "attributes.SeatingAttr": {}, useful while in dev
		setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY); // otherwise private fields won't be usable
		// these three are to forbid deriving class variables from getters/setters, otherwise e.g. Pedestrian would have too many fields
		setVisibility(PropertyAccessor.SETTER, JsonAutoDetect.Visibility.NONE);
		setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE);
		setVisibility(PropertyAccessor.IS_GETTER, JsonAutoDetect.Visibility.NONE);

		SimpleModule sm = new SimpleModule();

		sm.addDeserializer(boolean.class, new JsonDeserializer<Boolean>() { // make boolean parsing more strict, otherwise integers are accepted with 0=false and all other integers=true
			@Override
			public Boolean deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
					throws IOException {
				if (!jsonParser.getCurrentToken().isBoolean())
					throw new JsonParseException(jsonParser,
							"Can't parse \"" + jsonParser.getValueAsString() + "\" as boolean");
				return jsonParser.getValueAsBoolean();
			}
		});

		sm.addDeserializer(VRectangle.class, new JsonDeserializer<VRectangle>() {
			@Override
			public VRectangle deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
					throws IOException {
				return deserializeVRectangle(jsonParser.readValueAsTree());
			}
		});

		sm.addSerializer(VRectangle.class, new JsonSerializer<VRectangle>() {
			@Override
			public void serialize(VRectangle vRect, JsonGenerator jsonGenerator, SerializerProvider serializerProvider)
					throws IOException {
				jsonGenerator.writeTree(serializeVRectangle(vRect));
			}
		});



		sm.addDeserializer(VShape.class, new JsonDeserializer<VShape>() {
			@Override
			public VShape deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
					throws IOException {
				JsonNode node = jsonParser.readValueAsTree();
				ShapeType shapeType = convertValue(node.get("type"), ShapeType.class);
				switch (shapeType) {
					case CIRCLE:
						return convertValue(node, CircleStore.class).newVCircle();
					case POLYGON:
						return convertValue(node, Polygon2DStore.class).newVPolygon();
					case RECTANGLE:
						return deserializeVRectangle(node);
					default:
						return null;
				}
			}
		});

		sm.addSerializer(VShape.class, new JsonSerializer<VShape>() {
			@Override
			public void serialize(VShape vShape, JsonGenerator jsonGenerator, SerializerProvider serializerProvider)
					throws IOException {
				switch (vShape.getType()) {
					case CIRCLE:
						jsonGenerator.writeTree(convertValue(new CircleStore((VCircle) vShape), JsonNode.class));
						break;
					case POLYGON:
						jsonGenerator
								.writeTree(convertValue(new Polygon2DStore((VPolygon) vShape), JsonNode.class));
						break;
					case RECTANGLE:
						jsonGenerator.writeTree(serializeVRectangle((VRectangle) vShape)); // this doesn't seem to get called ever, the VRectangle serializer always seem to get called
						break;
					default:
						break;
				}
			}
		});

		sm.addDeserializer(DynamicElement.class, new JsonDeserializer<DynamicElement>() {
			@Override
			public DynamicElement deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
					throws IOException {
				JsonNode node = jsonParser.readValueAsTree();
				ScenarioElementType type = convertValue(node.get("type"), ScenarioElementType.class);
				switch (type) {
					case PEDESTRIAN:
						return convertValue(node, Pedestrian.class);
					// ... ?
					default: 
						return null;
				}
			}
		});

		registerModule(sm);
	}

	private VRectangle deserializeVRectangle(JsonNode node) {
		return convertValue(node, VRectangleStore.class).newVRectangle();
	}

	private JsonNode serializeVRectangle(VRectangle vRect) {
		return convertValue(new VRectangleStore(vRect), JsonNode.class);
	}

	@SuppressWarnings("unused")
	private static class VRectangleStore {
		public double x;
		public double y;
		public double width;
		public double height;
		public ShapeType type = ShapeType.RECTANGLE;

		public VRectangleStore() {}

		public VRectangleStore(VRectangle vRect) {
			x = vRect.x;
			y = vRect.y;
			height = vRect.height;
			width = vRect.width;
		}

		public VRectangle newVRectangle() {
			return new VRectangle(x, y, width, height);
		}
	}

	@SuppressWarnings("unused")
	private static class Polygon2DStore {
		public ShapeType type = ShapeType.POLYGON;
		public List<VPoint> points;

		public Polygon2DStore() {}

		public Polygon2DStore(VPolygon vPoly) {
			points = vPoly.getPoints();
		}

		public VPolygon newVPolygon() {
			return GeometryUtils.polygonFromPoints2D(points);
		}
	}

	@SuppressWarnings("unused")
	private static class CircleStore {
		public double radius;
		public VPoint center;
		public ShapeType type = ShapeType.CIRCLE;

		public CircleStore() {}

		public CircleStore(VCircle vCircle) {
			radius = vCircle.getRadius();
			center = vCircle.getCenter();
		}

		public VCircle newVCircle() {
			return new VCircle(center, radius);
		}
	}

}
