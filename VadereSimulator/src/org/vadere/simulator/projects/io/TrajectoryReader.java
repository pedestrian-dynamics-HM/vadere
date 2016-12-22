package org.vadere.simulator.projects.io;

import org.apache.commons.math3.util.Pair;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.simulator.projects.Scenario;
import org.vadere.simulator.projects.dataprocessing.processor.PedestrianPositionProcessor;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.simulation.Step;
import org.vadere.util.geometry.shapes.VPoint;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * A TrajectoryReader is the counterpart of the
 * {@link PedestrianPositionProcessor}.
 * 
 *
 *         This reader trys to generate a {@link java.util.stream.Stream< scenario.Pedestrian >} by
 *         reading it from a file.
 */
public class TrajectoryReader {

	private static Logger logger = LogManager.getLogger(IOVadere.class);

	private Path trajectoryFilePath;

	private AttributesAgent attributesPedestrian;

	public TrajectoryReader(final Path trajectoryFilePath, final Scenario scenario) throws IOException {
		this.trajectoryFilePath = trajectoryFilePath;
		this.attributesPedestrian = scenario.getAttributesPedestrian();
	}

	public TrajectoryReader(final Path trajectoryFilePath) {
		this.trajectoryFilePath = trajectoryFilePath;
		this.attributesPedestrian = new AttributesAgent();
	}

	public Map<Step, List<Agent>> readFile() throws IOException {
		return Files.lines(this.trajectoryFilePath)
					.skip(1) // Skip header line
					.map(line -> line.split(" "))
					.map(cells -> {
						int step = Integer.parseInt(cells[0]);
						int pedestrianId = Integer.parseInt(cells[1]);
						VPoint pos = new VPoint(Double.parseDouble(cells[2]), Double.parseDouble(cells[3]));
						int targetId = Integer.parseInt(cells[4]);

						Pedestrian ped = new Pedestrian(new AttributesAgent(this.attributesPedestrian, pedestrianId), new Random());
						ped.setPosition(pos);
						LinkedList<Integer> targets = new LinkedList<Integer>();
						targets.addFirst(targetId);
						ped.setTargets(targets);

						return Pair.create(new Step(Integer.parseInt(cells[0])), ped);
					})
					.collect(Collectors.groupingBy(pair -> pair.getKey(), Collectors.mapping(pair -> pair.getValue(), Collectors.toList())));
	}
}
