package org.vadere.state.util;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.vadere.state.scenario.DynamicElement;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.distribution.VDistribution;
import org.vadere.state.scenario.spawner.VSpawner;
import org.vadere.state.types.ScenarioElementType;
import org.vadere.util.Attributes;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.*;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class JacksonObjectMapper extends ObjectMapper {

	private static final long serialVersionUID = 1L;
	private Random random;
	public JacksonObjectMapper(Random random){
		this();
		this.random = random;
	}
	public JacksonObjectMapper() {
		this.random = random;
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

		sm.addDeserializer(VSpawner.class, new JsonDeserializer<>() {
			@Override
			public VSpawner deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
				System.out.println("In deserialising VSpawner");
				JsonNode node = jsonParser.readValueAsTree();
				String propertyName = VSpawner.class.getAnnotation(JsonTypeInfo.class).property();
				var values = VSpawner.class.getAnnotation(JsonSubTypes.class).value();
				var registeredTypes = Arrays.stream(values).collect(Collectors.toMap(t -> t.name(), t -> t.value()));
				var requestesType = node.get(propertyName);
				if (registeredTypes.containsKey(requestesType)) {
					((ObjectNode) node).remove(propertyName);
					var attributeClass = registeredTypes.get(requestesType).getGenericSuperclass().getClass();
					var attributes = (Attributes) convertValue(node, registeredTypes.get(requestesType));
					try {
						return (VSpawner) registeredTypes.get(requestesType).getDeclaredConstructor(attributeClass).newInstance(attributes);
					} catch (InstantiationException e) {
						throw new RuntimeException(e);
					} catch (IllegalAccessException e) {
						throw new RuntimeException(e);
					} catch (InvocationTargetException e) {
						throw new RuntimeException(e);
					} catch (NoSuchMethodException e) {
						throw new RuntimeException(e);
					}
				}
				throw new JsonParseException(jsonParser, "cannot deserialize VSpawner");
			}
		});

		sm.addSerializer(VSpawner.class, new JsonSerializer<VSpawner>() {
			@Override
			public void serialize(VSpawner vSpawner, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {

				System.out.println();
			}
		});

		sm.addDeserializer(DynamicElement.class, new JsonDeserializer<DynamicElement>() {
			@Override
			public DynamicElement deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
					throws IOException {
				JsonNode node = jsonParser.readValueAsTree();
				ScenarioElementType type = convertValue(node.get("type"), ScenarioElementType.class);
				if (type == ScenarioElementType.PEDESTRIAN) {
					return convertValue(node, Pedestrian.class);
					// ... ?
				}
				return null;
			}
		});

		sm.addDeserializer(VDistribution.class, new JsonDeserializer<VDistribution>() {
			@Override
			public VDistribution deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
				System.out.println("In deserialising VDistribution");
				return null;
			}
		});
		sm.addSerializer(VDistribution.class, new JsonSerializer<VDistribution>() {
			@Override
			public void serialize(VDistribution distribution, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {

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
	private static class VRectangleStore {
		public double x;
		public double y;
		public double width;
		public double height;
		public ShapeType type = ShapeType.RECTANGLE;

		public VRectangleStore() {}

		public VRectangleStore(VRectangle vRect) {
			x = vRect.getX();
			y = vRect.getY();
			height = vRect.getHeight();
			width = vRect.getWidth();
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
