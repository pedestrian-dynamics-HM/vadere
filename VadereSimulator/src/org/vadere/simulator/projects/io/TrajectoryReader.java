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
import java.util.*;
import java.util.stream.Collectors;

/**
 * A TrajectoryReader is the counterpart of the {@link PedestrianPositionProcessor}.
 */
public class TrajectoryReader {

	private static Logger logger = LogManager.getLogger(IOVadere.class);

	private Path trajectoryFilePath;

	private AttributesAgent attributesPedestrian;

	private static final String SPLITTER = " ";

	private Set<String> pedestrianIdKeys;
	private Set<String> stepKeys;
	private Set<String> xKeys;
	private Set<String> yKeys;
	private Set<String> targetIdKeys;

	private int pedIdIndex;
	private int stepIndex;
	private int xIndex;
	private int yIndex;
	private int targetIdIndex;

	public TrajectoryReader(final Path trajectoryFilePath, final Scenario scenario) throws IOException {
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

	    pedestrianIdKeys.add("id");
        pedestrianIdKeys.add("pedestrianId");
	    stepKeys.add("timeStep");
	    stepKeys.add("step");
	    xKeys.add("x");
	    yKeys.add("y");
	    targetIdKeys.add("targetId");

	    pedIdIndex = -1;
	    stepIndex = -1;
	    xIndex = -1;
	    yIndex = -1;
	    targetIdIndex = -1;

    }

	public Map<Step, List<Agent>> readFile() throws IOException {
	    // 1. Get the correct column
        String header = Files.lines(this.trajectoryFilePath).findFirst().get();
        String[] columns = header.split(SPLITTER);

        for(int index = 0; index < columns.length; index++) {
            if(pedestrianIdKeys.contains(columns[index])) {
                pedIdIndex = index;
            }
            else if(stepKeys.contains(columns[index])) {
                stepIndex = index;
            }
            else if(xKeys.contains(columns[index])) {
                xIndex = index;
            }
            else if(yKeys.contains(columns[index])) {
                yIndex = index;
            }
            else if(targetIdKeys.contains(columns[index])) {
                targetIdIndex = index;
            }
        }
        try {
            if(pedIdIndex != -1 && xIndex != -1 && yIndex != -1 && stepIndex != -1) {

                return Files.lines(this.trajectoryFilePath)
                        .skip(1) // Skip header line
                        .map(line -> line.split(SPLITTER))
                        .map(cells -> {
                            int step = Integer.parseInt(cells[stepIndex]);
                            int pedestrianId = Integer.parseInt(cells[pedIdIndex]);
                            VPoint pos = new VPoint(Double.parseDouble(cells[xIndex]), Double.parseDouble(cells[yIndex]));


                            int targetId = targetIdIndex != -1 ? Integer.parseInt(cells[targetIdIndex]) : -1;

                            Pedestrian ped = new Pedestrian(new AttributesAgent(this.attributesPedestrian, pedestrianId), new Random());
                            ped.setPosition(pos);
                            LinkedList<Integer> targets = new LinkedList<Integer>();
                            targets.addFirst(targetId);
                            ped.setTargets(targets);

                            return Pair.create(new Step(Integer.parseInt(cells[0])), ped);
                        })
                        .collect(Collectors.groupingBy(pair -> pair.getKey(), Collectors.mapping(pair -> pair.getValue(), Collectors.toList())));
            }
            else {
                throw new IOException("could not read trajectory file, some colums are missing.");
            }
        }
        catch (Exception e) {
	        logger.warn("could not read trajectory file. The file format might not be compatible or it is missing.");
	        throw e;
        }

	}
}
