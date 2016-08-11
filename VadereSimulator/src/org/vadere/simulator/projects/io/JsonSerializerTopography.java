package org.vadere.simulator.projects.io;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.scenario.*;
import org.vadere.state.scenario.*;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.io.IOUtils;
import org.vadere.util.io.JsonSerializerVShape;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.LinkedList;

@Deprecated
public class JsonSerializerTopography {

	private static Logger logger = LogManager
			.getLogger(JsonSerializerTopography.class);

	/**
	 * Serialization type for a {@link Topography}. The names of the variables
	 * are written to file, so do not change "obstacles" to
	 * "attributesObstacles" for example.
	 */
	private static class TopographyStore {
		AttributesTopography attributes = new AttributesTopography();
		Collection<AttributesObstacle> obstacles = new LinkedList<AttributesObstacle>();
		Collection<AttributesStairs> stairs = new LinkedList<AttributesStairs>();
		Collection<AttributesTarget> targets = new LinkedList<AttributesTarget>();
		Collection<AttributesSource> sources = new LinkedList<AttributesSource>();
		Collection<? extends DynamicElement> dynamicElements = new LinkedList<>();
		Collection<Pedestrian> pedestrians = new LinkedList<>(); // TODO [priority=medium] [task=refactoring] Wait to remove it since we want to use old topography files. this is only here for compatibilities sake.
		AttributesTeleporter teleporter = null;
	}
	/*
	 * public static Topography topographyFromJson(String json) {
	 * 
	 * GsonBuilder gsonBuilder = IOUtils.getGsonBuilder();
	 * gsonBuilder.registerTypeAdapter(VShape.class,
	 * new JsonSerializerVShape());
	 * gsonBuilder.registerTypeAdapter(DynamicElement.class,
	 * new JsonSerializerDynamicElement());
	 * 
	 * Gson gson = gsonBuilder.create();
	 * 
	 * TopographyStore store = gson.fromJson(json, TopographyStore.class);
	 * 
	 * // logger.info("jsonToTopography");
	 * 
	 * return topographyFromJson(gson.toJsonTree(store));
	 * }
	 * 
	 * public static Topography topographyFromJson(JsonElement json) {
	 * GsonBuilder gsonBuilder = IOUtils.getGsonBuilder();
	 * gsonBuilder.registerTypeAdapter(VShape.class,
	 * new JsonSerializerVShape());
	 * gsonBuilder.registerTypeAdapter(DynamicElement.class,
	 * new JsonSerializerDynamicElement());
	 * 
	 * Gson gson = gsonBuilder.create();
	 * 
	 * TopographyStore store = gson.fromJson(json, TopographyStore.class);
	 * 
	 * Topography topography = new Topography(store.attributes);
	 * 
	 * for (AttributesObstacle obstacle : store.obstacles) {
	 * topography.addObstacle(new Obstacle(obstacle));
	 * }
	 * for (AttributesStairs stairs : store.stairs) {
	 * topography.addStairs(new Stairs(stairs));
	 * }
	 * for (AttributesTarget target : store.targets) {
	 * topography.addTarget(new Target(target));
	 * }
	 * for (AttributesSource source : store.sources) {
	 * topography.addSource(new Source(source));
	 * }
	 * for (DynamicElement element : store.dynamicElements) {
	 * topography.addInitialElement(element);
	 * }
	 * for (Pedestrian pedestrian : store.pedestrians) {
	 * topography.addInitialElement(pedestrian);
	 * }
	 * if (store.teleporter != null) {
	 * topography.setTeleporter(new Teleporter(store.teleporter));
	 * }
	 * 
	 * return topography;
	 * }
	 * 
	 * public static JsonElement topographyToJson(Topography topography) {
	 * GsonBuilder gsonBuilder = IOUtils.getGsonBuilder();
	 * gsonBuilder.registerTypeAdapter(VShape.class,
	 * new JsonSerializerVShape());
	 * gsonBuilder.registerTypeAdapter(DynamicElement.class,
	 * new JsonSerializerDynamicElement());
	 * 
	 * Gson gson = gsonBuilder.create();
	 * 
	 * TopographyStore topographyStore = new TopographyStore();
	 * 
	 * topographyStore.attributes = topography.getAttributes();
	 * topographyStore.obstacles = getAttributes(topography.getObstacles(),
	 * AttributesObstacle.class);
	 * topographyStore.stairs = getAttributes(topography.getStairs(),
	 * AttributesStairs.class);
	 * topographyStore.targets = getAttributes(topography.getTargets(),
	 * AttributesTarget.class);
	 * topographyStore.sources = getAttributes(topography.getSources(),
	 * AttributesSource.class);
	 * topographyStore.dynamicElements = topography.getInitialElements(Pedestrian.class);
	 * 
	 * if (topography.hasTeleporter()) {
	 * topographyStore.teleporter = topography.getTeleporter()
	 * .getAttributes();
	 * } else {
	 * topographyStore.teleporter = null;
	 * }
	 * 
	 * // logger.info("topographyToJson");
	 * 
	 * return gson.toJsonTree(topographyStore);
	 * }
	 * 
	 * @SuppressWarnings("unchecked")
	 * private static <T extends Attributes, S extends ScenarioElement> Collection<T> getAttributes(
	 * Collection<S> elements, Class<T> type) {
	 * Collection<T> result = new LinkedList<T>();
	 * 
	 * for (S element : elements) {
	 * Field field;
	 * try {
	 * field = element.getClass().getDeclaredField("attributes");
	 * field.setAccessible(true);
	 * result.add((T) field.get(element));
	 * } catch (NoSuchFieldException | SecurityException
	 * | IllegalArgumentException | IllegalAccessException e) {
	 * e.printStackTrace();
	 * }
	 * 
	 * }
	 * 
	 * return result;
	 * }
	 */
}
