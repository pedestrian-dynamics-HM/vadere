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
import org.vadere.util.io.IOUtils;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A TrajectoryReader is the counterpart of the {@link PedestrianPositionProcessor}.
 */
public class TrajectoryReader {

	private static final String SPLITTER = " ";
	private static Logger logger = LogManager.getLogger(IOVadere.class);
	private Path trajectoryFilePath;
	private AttributesAgent attributesPedestrian;
	private Set<String> pedestrianIdKeys;
	private Set<String> stepKeys;
	private Set<String> xKeys;
	private Set<String> yKeys;
	private Set<String> targetIdKeys;
	private Set<String> groupIdKeys;


	private int pedIdIndex;
	private int stepIndex;
	private int xIndex;
	private int yIndex;
	private int targetIdIndex;
	private int groupIdIndex;

	public TrajectoryReader(final Path trajectoryFilePath, final Scenario scenario) {
		this(trajectoryFilePath, scenario.getAttributesPedestrian());
	}

	public TrajectoryReader(final Path trajectoryFilePath) {
		this(trajectoryFilePath, new AttributesAgent());
	}

	private TrajectoryReader(final Path trajectoryFilePath, final AttributesAgent attributesAgent) {
		this.trajectoryFilePath = trajectoryFilePath;
		this.attributesPedestrian = attributesAgent;
		pedestrianIdKeys = new HashSet<>();
		stepKeys = new HashSet<>();
		xKeys = new HashSet<>();
		yKeys = new HashSet<>();
		targetIdKeys = new HashSet<>();
		groupIdKeys = new HashSet<>();

		//should be set via Processor.getHeader
		pedestrianIdKeys.add("id");
		pedestrianIdKeys.add("pedestrianId");
		stepKeys.add("timeStep");
		stepKeys.add("step");
		xKeys.add("x");
		yKeys.add("y");
		targetIdKeys.add("targetId");
		groupIdKeys.add("groupId");

		pedIdIndex = -1;
		stepIndex = -1;
		xIndex = -1;
		yIndex = -1;
		targetIdIndex = -1;
		groupIdIndex = -1;

	}

	public Map<Step, List<Agent>> readFile() throws IOException {
		// 1. Get the correct column
		String header;
		//read only first line.
		try (BufferedReader in = IOUtils.defaultBufferedReader(this.trajectoryFilePath)) {
			header = in.readLine();
		}
		String[] columns = header.split(SPLITTER);

		for (int index = 0; index < columns.length; index++) {
			if (pedestrianIdKeys.contains(columns[index])) {
				pedIdIndex = index;
			} else if (stepKeys.contains(columns[index])) {
				stepIndex = index;
			} else if (xKeys.contains(columns[index])) {
				xIndex = index;
			} else if (yKeys.contains(columns[index])) {
				yIndex = index;
			} else if (targetIdKeys.contains(columns[index])) {
				targetIdIndex = index;
			} else if (groupIdKeys.contains(columns[index])){
				groupIdIndex = index;
			}
		}
		try {
			if (pedIdIndex != -1 && xIndex != -1 && yIndex != -1 && stepIndex != -1 && groupIdIndex == -1) {
				// load default values with no groups
				return readStandardTrajectoryFile();

			} else if(pedIdIndex != -1 && xIndex != -1 && yIndex != -1 && stepIndex != -1) {//here groupIdIndex is != -1
				// load values with group information
				return  readGroupTrajectoryFile();
			}
			else {
				throw new IOException("could not read trajectory file, some colums are missing.");
			}
		} catch (Exception e) {
			logger.warn("could not read trajectory file. The file format might not be compatible or it is missing.");
			throw e;
		}

	}

	private Map<Step, List<Agent>> readStandardTrajectoryFile() throws IOException {
		try (BufferedReader in = IOUtils.defaultBufferedReader(this.trajectoryFilePath)) {
			return in.lines()
					.skip(1)  //Skip header line
					.map(line -> line.split(SPLITTER))
					.map(cells -> {
						int step = Integer.parseInt(cells[stepIndex]);
						int pedestrianId = Integer.parseInt(cells[pedIdIndex]);
						VPoint pos = new VPoint(Double.parseDouble(cells[xIndex]), Double.parseDouble(cells[yIndex]));


						int targetId = targetIdIndex != -1 ? Integer.parseInt(cells[targetIdIndex]) : -1;

						Pedestrian ped = new Pedestrian(new AttributesAgent(this.attributesPedestrian, pedestrianId), new Random());
						ped.setPosition(pos);
						LinkedList<Integer> targets = new LinkedList<>();
						targets.addFirst(targetId);
						ped.setTargets(targets);

						return Pair.create(new Step(Integer.parseInt(cells[0])), ped);
					})
					.collect(Collectors.groupingBy(Pair::getKey, Collectors.mapping(Pair::getValue, Collectors.toList())));
		} catch (Exception e){
			logger.warn("could not read trajectory file. The file format might not be compatible or it is missing.");
			throw e;
		}
	}

	private Map<Step, List<Agent>> readGroupTrajectoryFile() throws IOException{
		try (BufferedReader in = IOUtils.defaultBufferedReader(this.trajectoryFilePath)) {
			return in.lines()
					.skip(1)  //Skip header line
					.map(line -> line.split(SPLITTER))
					.map(cells -> {
						int step = Integer.parseInt(cells[stepIndex]);
						int pedestrianId = Integer.parseInt(cells[pedIdIndex]);
						VPoint pos = new VPoint(Double.parseDouble(cells[xIndex]), Double.parseDouble(cells[yIndex]));


						int targetId = targetIdIndex != -1 ? Integer.parseInt(cells[targetIdIndex]) : -1;
						int groupId = targetIdIndex != -1 ? Integer.parseInt(cells[groupIdIndex]) : -1;

						Pedestrian ped = new Pedestrian(new AttributesAgent(this.attributesPedestrian, pedestrianId), new Random());
						ped.setPosition(pos);
						ped.addGroupId(groupId);
						LinkedList<Integer> targets = new LinkedList<>();
						targets.addFirst(targetId);
						ped.setTargets(targets);

						return Pair.create(new Step(Integer.parseInt(cells[0])), ped);
					})
					.collect(Collectors.groupingBy(Pair::getKey, Collectors.mapping(Pair::getValue, Collectors.toList())));
		} catch (Exception e){
			logger.warn("could not read trajectory file. The file format might not be compatible or it is missing.");
			throw e;
		}
	}
}
