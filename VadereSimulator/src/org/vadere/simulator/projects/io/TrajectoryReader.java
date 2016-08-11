package org.vadere.simulator.projects.io;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.simulator.projects.ScenarioRunManager;
import org.vadere.simulator.projects.dataprocessing.processors.PedestrianPositionProcessor;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.simulation.Step;
import org.vadere.util.data.Row;
import org.vadere.util.data.Table;
import org.vadere.util.data.Tupel;
import org.vadere.util.io.TableReader;

/**
 * A TrajectoryReader is the counterpart of the
 * {@link org.vadere.simulator.projects.dataprocessing.processors.PedestrianPositionProcessor}.
 * 
 *
 *         This reader trys to generate a {@link java.util.stream.Stream< scenario.Pedestrian >} by
 *         reading it from a file.
 */
public class TrajectoryReader {

	private static Logger logger = LogManager.getLogger(IOVadere.class);

	private Path trajectoryFilePath;

	private AttributesAgent attributesPedestrian;

	public TrajectoryReader(final Path trajectoryFilePath, final ScenarioRunManager scenario) throws IOException {
		this.trajectoryFilePath = trajectoryFilePath;
		this.attributesPedestrian = scenario.getAttributesPedestrian();
	}

	public TrajectoryReader(final Path trajectoryFilePath) {
		this.trajectoryFilePath = trajectoryFilePath;
		this.attributesPedestrian = new AttributesAgent();
	}

	public Map<Step, List<Agent>> readFile() throws IOException {
		Map<Step, List<Agent>> pedestrianByStep;
		TableReader tableReader = new TableReader();
		String[] headlines;
		try (Stream<String> lines = Files.lines(trajectoryFilePath)) {
			headlines = tableReader.readHeadLine(lines);
		}
		Table table;
		try (Stream<String> lines = Files.lines(trajectoryFilePath)) {
			table = tableReader.readTable(lines, headlines);
		}
		Stream<Row> rowStream =
				StreamSupport.stream(Spliterators.spliteratorUnknownSize(table.iterator(), Spliterator.ORDERED), false);
		Stream<Tupel<Step, Agent>> tupelStream =
				rowStream.map(row -> rowToTupel(row)).filter(tupel -> tupel.isPresent()).map(tupel -> tupel.get());
		pedestrianByStep = tupelStream.collect(
				Collectors.groupingBy(tupel -> tupel.v1, Collectors.mapping(tupel -> tupel.v2, Collectors.toList())));
		return pedestrianByStep;
	}


	private Optional<Tupel<Step, Agent>> rowToTupel(final Row row) {
		try {
			Agent ped = PedestrianPositionProcessor.rowToPedestrian(row, attributesPedestrian);
			Step step = PedestrianPositionProcessor.rowToStep(row);
			return Optional.of(Tupel.of(step, ped));
		} catch (NumberFormatException | NullPointerException e) {
			logger.warn("could not parse row " + row + " into a step or pedestrian");
			return Optional.empty();
		}
	}


	/*
	 * private static Step rowToStep(final Row row) {
	 * return new Step(Integer.parseInt(row.getEntry("step").toString()),
	 * Double.parseDouble(row.getEntry("time").toString()));
	 * }
	 * 
	 * private Pedestrian rowToPedestrian(final Row row) {
	 * Pedestrian pedestrian = new Pedestrian(new AttributesPedestrian(attributesPedestrian,
	 * Integer.parseInt(row.getEntry("id").toString())), new Random());
	 * pedestrian.setPosition(new VPoint(Double.parseDouble(row.getEntry("x").toString()),
	 * Double.parseDouble(row.getEntry("y").toString())));
	 * LinkedList<Integer> targets = new LinkedList<>();
	 * 
	 * if(row.getColumnNames().contains("targetId")) {
	 * targets.addFirst(Integer.parseInt(row.getEntry("targetId").toString()));
	 * }
	 * else {
	 * targets.addFirst(-1);
	 * }
	 * pedestrian.setTargets(targets);
	 * 
	 * 
	 * 
	 * 
	 * good idea but too slow!
	 * JsonElement jsonTree = IOUtils.getGson().toJsonTree(pedestrian, Pedestrian.class);
	 * for(String name : row.getColumnNames()) {
	 * setField(jsonTree, name, row.getEntry(name));
	 * }
	 * 
	 * pedestrian = IOUtils.getGson().fromJson(jsonTree, Pedestrian.class);
	 * return pedestrian;
	 * }
	 * 
	 * private static boolean setField(final JsonElement element, final String key, final Object
	 * value) {
	 * if(!element.isJsonObject()) {
	 * return false;
	 * }
	 * else {
	 * JsonObject obj = element.getAsJsonObject();
	 * JsonElement el = obj.get(key);
	 * 
	 * if(el != null && el.isJsonPrimitive()) {
	 * obj.add(key, IOUtils.getGson().toJsonTree(value));
	 * return true;
	 * }
	 * else if(el != null) {
	 * return false;
	 * }
	 * else {
	 * for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
	 * if(setField(entry.getValue(), key, value)) {
	 * return true;
	 * }
	 * }
	 * return false;
	 * }
	 * }
	 * }
	 * 
	 * private boolean isValid(final String[] cNames) {
	 * return requiredColumnNames.containsAll(Arrays.asList(cNames));
	 * }
	 */
}
