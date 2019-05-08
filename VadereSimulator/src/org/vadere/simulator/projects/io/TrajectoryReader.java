package org.vadere.simulator.projects.io;

import org.apache.commons.math3.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.vadere.simulator.projects.Scenario;
import org.vadere.simulator.projects.dataprocessing.processor.PedestrianPositionProcessor;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.simulation.FootStep;
import org.vadere.state.simulation.Step;
import org.vadere.state.util.StateJsonConverter;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.io.IOUtils;
import org.vadere.util.logging.Logger;

import java.io.BufferedReader;
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
 *
 * Output file assumptions:
 * The TrajectoryReader assumes that the first row of the output is the headline and
 * that there exist certain columns named:
 *      (id or pedestrianId) [mandatory],
 *      (step or timeStep) [mandatory],
 *      x [mandatory],
 *      y [mandatory],
 *      targetId [optional] and
 *      groupId [optional].
 * The order of the rows (expect for the first row / header) can be arbitrary.
 * Columns has to be separated by {@link TrajectoryReader#SPLITTER}
 */
public class TrajectoryReader {

	private static final String SPLITTER = " ";
	private static Logger logger = Logger.getLogger(IOVadere.class);
	private Path trajectoryFilePath;
	private AttributesAgent attributesPedestrian;
	private Set<String> pedestrianIdKeys;
	private Set<String> stepKeys;
	private Set<String> xKeys;
	private Set<String> yKeys;
	private Set<String> targetIdKeys;
	private Set<String> groupIdKeys;
	private Set<String> groupSizeKeys;
	private Set<String> stridesKeys;


	private int pedIdIndex;
	private int stepIndex;
	private int xIndex;
	private int yIndex;
	private int targetIdIndex;
	private int groupIdIndex;
	private int groupSizeIndex;
	private int stridesIndex;

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
		groupSizeKeys = new HashSet<>();
		stridesKeys = new HashSet<>();

		//should be set via Processor.getHeader
		pedestrianIdKeys.add("id");
		pedestrianIdKeys.add("pedestrianId");
		stepKeys.add("timeStep");
		stepKeys.add("step");
		xKeys.add("x");
		yKeys.add("y");
		targetIdKeys.add("targetId");
		groupIdKeys.add("groupId");
		groupSizeKeys.add("groupSize");
		stridesKeys.add("strides");
		stridesKeys.add("footSteps");

		pedIdIndex = -1;
		stepIndex = -1;
		xIndex = -1;
		yIndex = -1;
		targetIdIndex = -1;
		groupIdIndex = -1;
		groupSizeIndex = -1;
		stridesIndex = -1;

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
			else if (groupSizeKeys.contains(columns[index])){
				groupSizeIndex = index;
			}
			else if(stridesKeys.contains(columns[index])) {
				stridesIndex = index;
			}
		}
		try {
			if (pedIdIndex != -1 && xIndex != -1 && yIndex != -1 && stepIndex != -1) {
				// load default values with no groups
				return readStandardTrajectoryFile();
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
			return in.lines()                                       // a stream of lines
					.skip(1)                                        // skip the first line i.e. the header
					.map(line -> split(line))              // split the line into string tokens
					.map(rowTokens -> parseRowTokens(rowTokens))    // transform those tokens into a pair of java objects (step, agent)
					.collect(Collectors.groupingBy(Pair::getKey,    // group all agent objects by the step.
							Collectors.mapping(Pair::getValue, Collectors.toList())));
		} catch (Exception e){
			logger.warn("could not read trajectory file. The file format might not be compatible or it is missing.");
			throw e;
		}
	}

	/**
	 * This method is used instead of {@link String#split(String)} since it is faster because no pattern matching is required.
	 *
	 * @param line
	 * @return
	 */
	private String[] split(@NotNull final String line) {
		int tokenCount = 0;
		int startIndex = 0;
		int endIndex = -1;
		do {
			endIndex = line.indexOf(SPLITTER, startIndex+1);
			startIndex = endIndex;
			tokenCount++;
		} while(endIndex != -1);

		startIndex = -1;
		endIndex = -1;
		String[] tokens = new String[tokenCount];
		for(int i = 0; i < tokenCount; i++) {
			endIndex = line.indexOf(SPLITTER, startIndex+1);
			tokens[i] = line.substring(startIndex+1, endIndex != -1 ? endIndex : line.length());
			startIndex = endIndex;
		}
		return tokens;
	}

	/**
	 * transforms the string tokens of the row (i.e. the values generated by the output processor of one row)
	 * into a {@link Pair} of ({@link Step}, {@link Agent}).
	 *
	 * @param rowTokens string tokens of the row
	 * @return a {@link Pair} of ({@link Step}, {@link Agent})
	 */
	private Pair<Step, Agent> parseRowTokens(@NotNull final String[] rowTokens) {
		// time step
		int step = Integer.parseInt(rowTokens[stepIndex]);

		// pedestrian id
		int pedestrianId = Integer.parseInt(rowTokens[pedIdIndex]);
		Pedestrian ped = new Pedestrian(new AttributesAgent(attributesPedestrian, pedestrianId), new Random());

		// pedestrian position
		VPoint pos = new VPoint(Double.parseDouble(rowTokens[xIndex]), Double.parseDouble(rowTokens[yIndex]));

		// pedestrian target
		int targetId = targetIdIndex != -1 ? Integer.parseInt(rowTokens[targetIdIndex]) : -1;
		ped.setPosition(pos);
		LinkedList<Integer> targets = new LinkedList<>();
		targets.addFirst(targetId);
		ped.setTargets(targets);

		if(groupIdIndex != -1) {
			int groupId = Integer.parseInt(rowTokens[groupIdIndex]);
			int groupSize = groupSizeIndex != -1 ? Integer.parseInt(rowTokens[groupSizeIndex]) : -1;
			ped.addGroupId(groupId, groupSize);
		}

		if(stridesIndex != -1) {
			FootStep[] footSteps = StateJsonConverter.deserializeObjectFromJson(rowTokens[stridesIndex], FootStep[].class);
			for(FootStep footStep : footSteps) {
				ped.getFootSteps().add(footStep);
			}
		}

		return Pair.create(new Step(step), ped);
	}
}
