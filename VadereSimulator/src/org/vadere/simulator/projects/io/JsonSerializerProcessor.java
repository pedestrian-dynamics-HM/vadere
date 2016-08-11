package org.vadere.simulator.projects.io;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.vadere.simulator.projects.dataprocessing.processors.*;
import org.vadere.state.attributes.processors.*;
import org.vadere.util.io.IOUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

/**
 * This class handles all the JSON serialization of Processors.
 * If one create a new processor he/she has to adapt this class to support serialization.
 * 
 *
 */
@Deprecated
public class JsonSerializerProcessor implements JsonDeserializer<Processor> {

	public static JsonElement toJson(final List<Processor> list) {
		List<String> processors = new LinkedList<>();
		for (Processor proc : list) {
			processors.add(JsonSerializerProcessor.toJson(proc));
		}
		return IOUtils.getGson().toJsonTree(processors);
	}

	public static JsonElement toJsonElement(final Processor processor) {
		GsonBuilder builder = IOUtils.getGsonBuilder();
		builder.setExclusionStrategies(new JsonProcessorExclusionStrategy());
		Gson gson = builder.create();
		return gson.toJsonTree(processor);
	}

	public static String toJson(final Processor processor) {
		return IOUtils.getGson().toJson(toJsonElement(processor));
	}

	public static Processor toProcessorFromJson(final JsonElement json) {
		JsonSerializerProcessor deserializer = new JsonSerializerProcessor();
		GsonBuilder builder = IOUtils.getGsonBuilder();
		builder.registerTypeAdapter(Processor.class, deserializer);
		Gson gson = builder.create();
		return gson.fromJson(json, Processor.class);
	}


	public static Processor toProcessorFromJson(final String json) {
		JsonSerializerProcessor deserializer = new JsonSerializerProcessor();
		GsonBuilder builder = IOUtils.getGsonBuilder();
		builder.registerTypeAdapter(Processor.class, deserializer);
		Gson gson = builder.create();
		return gson.fromJson(json, Processor.class);
	}

	@Override
	public Processor deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context)
			throws JsonParseException {
		return createProcessor(json);
	}

	/**
	 * Creates Processor by the simple class name.
	 * 
	 * @param json
	 * @return
	 */
	private Processor createProcessor(final JsonElement json) {

		Processor processor = null;
		JsonObject processorObj = json.getAsJsonObject();
		JsonArray jsonColumnNames = processorObj.get("columnNames").getAsJsonArray();
		String[] columnNames = new String[jsonColumnNames.size()];
		for (int i = 0; i < jsonColumnNames.size(); i++) {
			columnNames[i] = jsonColumnNames.get(i).getAsString();
		}
		String clazz = processorObj.get("clazz").getAsString();

		if (clazz.equals(PedestrianPositionProcessor.class.getSimpleName())) {
			AttributesPedestrianPositionProcessor attributes;
			if (processorObj.get("attributes") != null) {
				JsonElement jsonElement = processorObj.get("attributes").getAsJsonObject();
				attributes = IOUtils.getGson().fromJson(jsonElement, AttributesPedestrianPositionProcessor.class);
			} else {
				attributes = new AttributesPedestrianPositionProcessor();
			}

			processor = new PedestrianPositionProcessor(attributes);
			processor.addColumnNames(columnNames);
		}  else if (clazz.equals(PedestrianLastPositionProcessor.class.getSimpleName())) {
			processor = new PedestrianLastPositionProcessor();
			processor.addColumnNames(columnNames);
		} else if (clazz.equals(PedestrianAttributesProcessor.class.getSimpleName())) {
			processor = new PedestrianAttributesProcessor();
			processor.addColumnNames(columnNames);
		} else if (clazz.equals(DensityCountingProcessor.class.getSimpleName())) {
			JsonElement jsonElement = processorObj.get("attributes").getAsJsonObject();
			AttributesDensityCountingProcessor attributes =
					IOUtils.getGson().fromJson(jsonElement, AttributesDensityCountingProcessor.class);
			processor = new DensityCountingProcessor(attributes);
		} else if (clazz.equals(DensityGaussianProcessor.class.getSimpleName())) {
			JsonElement jsonElement = processorObj.get("attributes").getAsJsonObject();
			AttributesDensityGaussianProcessor attributes =
					IOUtils.getGson().fromJson(jsonElement, AttributesDensityGaussianProcessor.class);
			processor = new DensityGaussianProcessor(attributes);
		} else if (clazz.equals(DensityVoronoiGeoProcessor.class.getSimpleName())) {
			JsonElement jsonElement = processorObj.get("attributes").getAsJsonObject();
			AttributesDensityVoronoiProcessor attributes =
					IOUtils.getGson().fromJson(jsonElement, AttributesDensityVoronoiProcessor.class);
			processor = new DensityVoronoiGeoProcessor(attributes);
		} else if (clazz.equals(DensityVoronoiProcessor.class.getSimpleName())) {
			JsonElement jsonElement = processorObj.get("attributes").getAsJsonObject();
			AttributesDensityVoronoiProcessor attributes =
					IOUtils.getGson().fromJson(jsonElement, AttributesDensityVoronoiProcessor.class);
			processor = new DensityVoronoiProcessor(attributes);
		} else if (clazz.equals(AreaVoronoiProcessor.class.getSimpleName())) {
			JsonElement jsonElement = processorObj.get("attributes").getAsJsonObject();
			AttributesDensityVoronoiProcessor attributes =
					IOUtils.getGson().fromJson(jsonElement, AttributesDensityVoronoiProcessor.class);
			PedestrianVelocityProcessor velocityProcessor = (PedestrianVelocityProcessor) createProcessor(
					processorObj.get("pedestrianVelocityProcessor").getAsJsonObject());
			processor = new AreaVoronoiProcessor(attributes, velocityProcessor);
		} else if (clazz.equals(PedestrianDensityProcessor.class.getSimpleName())) {
			DensityProcessor pedDensityProcessor =
					(DensityProcessor) createProcessor(processorObj.get("densityProcessor").getAsJsonObject());
			processor = new PedestrianDensityProcessor(new PedestrianPositionProcessor(), pedDensityProcessor);
		} else if (clazz.equals(MeanEvacuationTimeProcessor.class.getSimpleName())) {
			PedestrianLastPositionProcessor lastPosProcessor = (PedestrianLastPositionProcessor) createProcessor(
					processorObj.get("lastPosProcessor").getAsJsonObject());
			processor = new MeanEvacuationTimeProcessor(lastPosProcessor);
			processor.addColumnNames(columnNames);
		} else if (clazz.equals(PedestrianVelocityProcessor.class.getSimpleName())) {
			PedestrianPositionProcessor pedPosProcessor = (PedestrianPositionProcessor) createProcessor(
					processorObj.get("pedestrianPositionProcessor").getAsJsonObject());
			JsonElement jsonElement = processorObj.get("attributes").getAsJsonObject();
			AttributesVelocityProcessor attributes =
					IOUtils.getGson().fromJson(jsonElement, AttributesVelocityProcessor.class);
			processor = new PedestrianVelocityProcessor(attributes, pedPosProcessor);
			processor.addColumnNames(columnNames);
		} else if (clazz.equals(PedestrianOverlapProcessor.class.getSimpleName())) {
			PedestrianPositionProcessor pedPosProcessor = (PedestrianPositionProcessor) createProcessor(
					processorObj.get("pedestrianPositionProcessor").getAsJsonObject());
			JsonElement jsonElement = processorObj.get("attributes").getAsJsonObject();
			AttributesOverlapProcessor attributes =
					IOUtils.getGson().fromJson(jsonElement, AttributesOverlapProcessor.class);
			processor = new PedestrianOverlapProcessor(attributes, pedPosProcessor);
			processor.addColumnNames(columnNames);
		} else if (clazz.equals(CombineProcessor.class.getSimpleName())) {
			JsonArray jsonArray = processorObj.get("processorList").getAsJsonArray();
			List<ForEachPedestrianPositionProcessor> processorList = new ArrayList<>();
			for (int index = 0; index < jsonArray.size(); index++) {
				ForEachPedestrianPositionProcessor proc =
						(ForEachPedestrianPositionProcessor) createProcessor(jsonArray.get(index));
				processorList.add(proc);
			}
			processor = new CombineProcessor(processorList);
		} else if (clazz.equals(PedestrianDensityTest.class.getSimpleName())) {
			PedestrianDensityProcessor densityProcessor = (PedestrianDensityProcessor) createProcessor(
					processorObj.get("pedestrianDensityProcessor").getAsJsonObject());
			JsonElement jsonElement = processorObj.get("attributes").getAsJsonObject();
			AttributesDensityTest attributes = IOUtils.getGson().fromJson(jsonElement, AttributesDensityTest.class);
			processor = new PedestrianDensityTest(densityProcessor, attributes);
		} else if (clazz.equals(PedestrianFlowProcessor.class.getSimpleName())) {
			PedestrianDensityProcessor densityProcessor = (PedestrianDensityProcessor) createProcessor(
					processorObj.get("pedestrianDensityProcessor").getAsJsonObject());
			PedestrianVelocityProcessor velocityProcessor = (PedestrianVelocityProcessor) createProcessor(
					processorObj.get("pedestrianVelocityProcessor").getAsJsonObject());
			processor = new PedestrianFlowProcessor(densityProcessor, velocityProcessor);
		} else if (clazz.equals(StrideLengthProcessor.class.getSimpleName())) {
			processor = new StrideLengthProcessor();
		} else if (clazz.equals(FloorFieldProcessor.class.getSimpleName())) {
			JsonElement jsonElement = processorObj.get("attributes").getAsJsonObject();
			AttributesFloorFieldProcessor attributes =
					IOUtils.getGson().fromJson(jsonElement, AttributesFloorFieldProcessor.class);
			return new FloorFieldProcessor(attributes);
		} else if (clazz.equals(PedestrianCountingAreaProcessor.class.getSimpleName())) {
			JsonElement jsonElement = processorObj.get("attributes").getAsJsonObject();
			AttributesPedestrianWaitingTimeProcessor attributes =
					IOUtils.getGson().fromJson(jsonElement, AttributesPedestrianWaitingTimeProcessor.class);
			return new PedestrianCountingAreaProcessor(attributes);
		} else if (clazz.equals(PedestrianWaitingTimeProcessor.class.getSimpleName())) {
			JsonElement jsonElement = processorObj.get("attributes").getAsJsonObject();
			AttributesPedestrianWaitingTimeProcessor attributes =
					IOUtils.getGson().fromJson(jsonElement, AttributesPedestrianWaitingTimeProcessor.class);
			return new PedestrianWaitingTimeProcessor(attributes);
		} else if (clazz.equals(PedestrianWaitingTimeTest.class.getSimpleName())) {
			PedestrianWaitingTimeProcessor waitingTimeProcessor = (PedestrianWaitingTimeProcessor) createProcessor(
					processorObj.get("pedestrianWaitingTimeProcessor").getAsJsonObject());
			JsonElement jsonElement = processorObj.get("attributes").getAsJsonObject();
			AttributesWaitingTimeTest attributes =
					IOUtils.getGson().fromJson(jsonElement, AttributesWaitingTimeTest.class);
			return new PedestrianWaitingTimeTest(waitingTimeProcessor, attributes);
		} else if (clazz.equals(PedestrianEvacuationTimeTest.class.getSimpleName())) {
			JsonElement jsonElement = processorObj.get("attributes").getAsJsonObject();
			AttributesEvacuationTimeTest attributes =
					IOUtils.getGson().fromJson(jsonElement, AttributesEvacuationTimeTest.class);
			return new PedestrianEvacuationTimeTest(attributes);
		} else if (clazz.equals(PedestrianTargetProcessor.class.getSimpleName())) {
			JsonElement jsonElement = processorObj.get("attributes").getAsJsonObject();
			AttributesPedestrianTargetProcessor attributes =
					IOUtils.getGson().fromJson(jsonElement, AttributesPedestrianTargetProcessor.class);
			return new PedestrianTargetProcessor(attributes);
		} else if (clazz.equals(SnapshotOutputProcessor.class.getSimpleName())) {
			return new SnapshotOutputProcessor();
		} else {
			throw new IllegalArgumentException(clazz + " is not supported.");
		}

		return processor;
	}

	/*
	 * // TODO [priority=high] [task=refactoring] this is the method above, but adapted to Jackson
	 * public static Processor createProcessor(final JsonNode processorNode, ObjectMapper mapper) {
	 * String[] columnNames = mapper.convertValue(processorNode.get("columnNames"), String[].class);
	 * Processor processor = null;
	 * String clazz = processorNode.get("clazz").asText();
	 * JsonNode attributesNode = null;
	 * if(processorNode.get("attributes") != null) {
	 * attributesNode = processorNode.get("attributes");
	 * }
	 * 
	 * if(clazz.equals(PedestrianPositionProcessor.class.getSimpleName())) {
	 * AttributesPedestrianPositionProcessor attributes;
	 * if(processorNode.get("attributes") != null)
	 * attributes = mapper.convertValue(attributesNode,
	 * AttributesPedestrianPositionProcessor.class);
	 * else
	 * attributes = new AttributesPedestrianPositionProcessor();
	 * processor = new PedestrianPositionProcessor(attributes);
	 * processor.addColumnNames(columnNames);
	 * }
	 * else if(clazz.equals(PedestrianPositionAndHeuristicProcessor.class.getSimpleName())) {
	 * processor = new PedestrianPositionAndHeuristicProcessor();
	 * processor.addColumnNames(columnNames);
	 * }
	 * else if(clazz.equals(PedestrianLastPositionProcessor.class.getSimpleName())) {
	 * processor = new PedestrianLastPositionProcessor();
	 * processor.addColumnNames(columnNames);
	 * }
	 * else if(clazz.equals(PedestrianAttributesProcessor.class.getSimpleName())) {
	 * processor = new PedestrianAttributesProcessor();
	 * processor.addColumnNames(columnNames);
	 * }
	 * else if(clazz.equals(DensityCountingProcessor.class.getSimpleName())) {
	 * AttributesDensityCountingProcessor attributes = mapper.convertValue(attributesNode,
	 * AttributesDensityCountingProcessor.class);
	 * processor = new DensityCountingProcessor(attributes);
	 * }
	 * else if(clazz.equals(DensityGaussianProcessor.class.getSimpleName())) {
	 * AttributesDensityGaussianProcessor attributes = mapper.convertValue(attributesNode,
	 * AttributesDensityGaussianProcessor.class);
	 * processor = new DensityGaussianProcessor(attributes);
	 * }
	 * else if(clazz.equals(DensityVoronoiGeoProcessor.class.getSimpleName())) {
	 * AttributesDensityVoronoiProcessor attributes = mapper.convertValue(attributesNode,
	 * AttributesDensityVoronoiProcessor.class);
	 * processor = new DensityVoronoiGeoProcessor(attributes);
	 * }
	 * else if(clazz.equals(DensityVoronoiProcessor.class.getSimpleName())) {
	 * AttributesDensityVoronoiProcessor attributes = mapper.convertValue(attributesNode,
	 * AttributesDensityVoronoiProcessor.class);
	 * processor = new DensityVoronoiProcessor(attributes);
	 * }
	 * else if(clazz.equals(AreaVoronoiProcessor.class.getSimpleName())) {
	 * AttributesDensityVoronoiProcessor attributes = mapper.convertValue(attributesNode,
	 * AttributesDensityVoronoiProcessor.class);
	 * PedestrianVelocityProcessor velocityProcessor = (PedestrianVelocityProcessor)
	 * createProcessor(processorNode.get("pedestrianVelocityProcessor"), mapper);
	 * processor = new AreaVoronoiProcessor(attributes, velocityProcessor);
	 * }
	 * else if(clazz.equals(PedestrianDensityProcessor.class.getSimpleName())){
	 * DensityProcessor pedDensityProcessor = (DensityProcessor)
	 * createProcessor(processorNode.get("densityProcessor"), mapper);
	 * processor = new PedestrianDensityProcessor(new PedestrianPositionProcessor(),
	 * pedDensityProcessor);
	 * }
	 * else if(clazz.equals(MeanEvacuationTimeProcessor.class.getSimpleName())) {
	 * PedestrianLastPositionProcessor lastPosProcessor = (PedestrianLastPositionProcessor)
	 * createProcessor(processorNode.get("lastPosProcessor"), mapper);
	 * processor = new MeanEvacuationTimeProcessor(lastPosProcessor);
	 * processor.addColumnNames(columnNames);
	 * }
	 * else if(clazz.equals(PedestrianVelocityProcessor.class.getSimpleName())) {
	 * PedestrianPositionProcessor pedPosProcessor = (PedestrianPositionProcessor)
	 * createProcessor(processorNode.get("pedestrianPositionProcessor"), mapper);
	 * AttributesVelocityProcessor attributes = mapper.convertValue(attributesNode,
	 * AttributesVelocityProcessor.class);
	 * processor = new PedestrianVelocityProcessor(attributes, pedPosProcessor);
	 * processor.addColumnNames(columnNames);
	 * }
	 * else if(clazz.equals(PedestrianOverlapProcessor.class.getSimpleName())) {
	 * PedestrianPositionProcessor pedPosProcessor = (PedestrianPositionProcessor)
	 * createProcessor(processorNode.get("pedestrianPositionProcessor"), mapper);
	 * AttributesOverlapProcessor attributes = mapper.convertValue(attributesNode,
	 * AttributesOverlapProcessor.class);
	 * processor = new PedestrianOverlapProcessor(attributes, pedPosProcessor);
	 * processor.addColumnNames(columnNames);
	 * }
	 * else if(clazz.equals(CombineProcessor.class.getSimpleName())) {
	 * JsonNode[] arr = mapper.convertValue(processorNode, JsonNode[].class);
	 * List<ForEachPedestrianPositionProcessor> processorList = new ArrayList<>();
	 * for(JsonNode node : arr) {
	 * ForEachPedestrianPositionProcessor proc = (ForEachPedestrianPositionProcessor)
	 * createProcessor(node, mapper);
	 * processorList.add(proc);
	 * }
	 * processor = new CombineProcessor(processorList);
	 * }
	 * else if(clazz.equals(PedestrianDensityTest.class.getSimpleName())) {
	 * PedestrianDensityProcessor densityProcessor = (PedestrianDensityProcessor)
	 * createProcessor(processorNode.get("pedestrianDensityProcessor"), mapper);
	 * AttributesDensityTest attributes = mapper.convertValue(attributesNode,
	 * AttributesDensityTest.class);
	 * processor = new PedestrianDensityTest(densityProcessor, attributes);
	 * }
	 * else if(clazz.equals(PedestrianFlowProcessor.class.getSimpleName())) {
	 * PedestrianDensityProcessor densityProcessor = (PedestrianDensityProcessor)
	 * createProcessor(processorNode.get("pedestrianDensityProcessor"), mapper);
	 * PedestrianVelocityProcessor velocityProcessor = (PedestrianVelocityProcessor)
	 * createProcessor(processorNode.get("pedestrianVelocityProcessor"), mapper);
	 * processor = new PedestrianFlowProcessor(densityProcessor, velocityProcessor);
	 * }
	 * else if(clazz.equals(StrideLengthProcessor.class.getSimpleName())) {
	 * processor = new StrideLengthProcessor();
	 * }
	 * else if(clazz.equals(FloorFieldProcessor.class.getSimpleName())) {
	 * AttributesFloorFieldProcessor attributes = mapper.convertValue(attributesNode,
	 * AttributesFloorFieldProcessor.class);
	 * return new FloorFieldProcessor(attributes);
	 * }
	 * else if(clazz.equals(PedestrianCountingAreaProcessor.class.getSimpleName())) {
	 * AttributesPedestrianWaitingTimeProcessor attributes = mapper.convertValue(attributesNode,
	 * AttributesPedestrianWaitingTimeProcessor.class);
	 * return new PedestrianCountingAreaProcessor(attributes);
	 * }
	 * else if(clazz.equals(PedestrianWaitingTimeProcessor.class.getSimpleName())) {
	 * AttributesPedestrianWaitingTimeProcessor attributes = mapper.convertValue(attributesNode,
	 * AttributesPedestrianWaitingTimeProcessor.class);
	 * return new PedestrianWaitingTimeProcessor(attributes);
	 * }
	 * else if(clazz.equals(PedestrianWaitingTimeTest.class.getSimpleName())) {
	 * PedestrianWaitingTimeProcessor waitingTimeProcessor = (PedestrianWaitingTimeProcessor)
	 * createProcessor(processorNode.get("pedestrianWaitingTimeProcessor"), mapper);
	 * AttributesWaitingTimeTest attributes = mapper.convertValue(attributesNode,
	 * AttributesWaitingTimeTest.class);
	 * return new PedestrianWaitingTimeTest(waitingTimeProcessor, attributes);
	 * }
	 * else if(clazz.equals(PedestrianEvacuationTimeTest.class.getSimpleName())) {
	 * AttributesEvacuationTimeTest attributes = mapper.convertValue(attributesNode,
	 * AttributesEvacuationTimeTest.class);
	 * return new PedestrianEvacuationTimeTest(attributes);
	 * }
	 * else if(clazz.equals(PedestrianTargetProcessor.class.getSimpleName())) {
	 * AttributesPedestrianTargetProcessor attributes = mapper.convertValue(attributesNode,
	 * AttributesPedestrianTargetProcessor.class);
	 * return new PedestrianTargetProcessor(attributes);
	 * }
	 * else if(clazz.equals(SnapshotOutputProcessor.class.getSimpleName())) {
	 * return new SnapshotOutputProcessor();
	 * }
	 * else {
	 * throw new IllegalArgumentException(clazz + " is not supported.");
	 * }
	 * 
	 * return processor;
	 * }
	 */

}
