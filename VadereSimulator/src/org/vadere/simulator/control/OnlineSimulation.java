package org.vadere.simulator.control;


import com.google.protobuf.Timestamp;

import de.s2ucre.protobuf.generated.Common;
import de.s2ucre.protobuf.generated.IosbOutput;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.vadere.simulator.models.MainModel;
import org.vadere.simulator.projects.ScenarioStore;
import org.vadere.simulator.projects.SimulationResult;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.state.scenario.DynamicElement;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VPolygon;

import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import client.Receiver;
import client.Sender;

/**
 * @author Benedikt Zoennchen
 */
public class OnlineSimulation extends Simulation implements Consumer<LinkedList<IosbOutput.AreaInfoAtTime>> {

	private static Logger logger = LogManager.getLogger(OnlineSimulation.class);
	private String subscriberURI = "tcp://localhost:5556";
	private Random random;
	private MainModel mainModel;
	private Topography topography;

	private Receiver receiver;

	private Sender sender;

	public OnlineSimulation(
			MainModel mainModel,
			double startTimeInSec,
			String name,
			ScenarioStore scenarioStore,
			List<PassiveCallback> passiveCallbacks,
			Random random,
			ProcessorManager processorManager,
			SimulationResult simulationResult) {
		super(mainModel, startTimeInSec, name, scenarioStore, passiveCallbacks, random, processorManager, simulationResult);
		this.random = random;
		this.mainModel = mainModel;
		this.topography = scenarioStore.getTopography();
		this.receiver = Receiver.getInstance();
		this.sender = Sender.getInstance();
		this.receiver.addConsumer(this);

		this.sender.start();
		this.receiver.start();
	}

	private void setStartTime(double startTime) {
		startTimeInSec = startTime;
	}

	@Override
	protected synchronized void iterate() {
		if(topography.getPedestrianDynamicElements().getElements().isEmpty()) {
			pause();
		}
		super.iterate();
		sendPedestrians();
	}

	@Override
	public synchronized void pause() {
		super.pause();
		logger.info("pause online simulation");
	}

	@Override
	public synchronized void resume() {
		super.resume();
		logger.info("resume online simulation");
	}

	@Override
	public void loop() {
		while (!hasFinished()) {
			iterate();
		}
	}

	@Override
	protected boolean hasFinished() {
		return false;
	}

	@Override
	protected double nextSimTimeInSec() {
		return simTimeInSec + attributesSimulation.getSimTimeStepLength();
	}

	@Override
	public synchronized void accept(@NotNull final LinkedList<IosbOutput.AreaInfoAtTime> areaInfoAtTimes) {
		if(topography.getPedestrianDynamicElements().getElements().isEmpty()) {
			pause();
			topography.getPedestrianDynamicElements().clear();
			PedestriansAtTimeStep pedestriansAtTimeStep = generatePedestrians(areaInfoAtTimes);
			logger.info("re-initialize simulation.");
			pedestriansAtTimeStep.pedestrians.stream().forEach(ped -> topography.addElement(ped));

			//TODO: this is certainly not correct!
			setStartTime(pedestriansAtTimeStep.timeStepInSec);
			resume();
			notifyAll();
		}
	}

	public void sendPedestrians() {
		for(DynamicElement dynamicElement : topography.getPedestrianDynamicElements().getElements()) {
			Pedestrian ped = (Pedestrian) dynamicElement;
			sender.send(ped, (int)(startTimeInSec + simTimeInSec));
			logger.info("send positions");
		}
	}


	/*public void sendPedestrians() {
		double [] positions = new double[topography.getPedestrianDynamicElements().getElements().size() * 2];
		int[] ids = new int[topography.getPedestrianDynamicElements().getElements().size()];
		int i = 0;
		for(DynamicElement dynamicElement : topography.getPedestrianDynamicElements().getElements()) {
			Pedestrian ped = (Pedestrian) dynamicElement;
			positions[i * 2] = ped.getPosition().getX();
			positions[i * 2 + 1] = ped.getPosition().getY();
			ids[i] = ped.getId();
		}
		sender.send(positions, ids, (int)(startTimeInSec + simTimeInSec));
		logger.info("send positions");
	}*/

	/**
	 * This method blocks until new messages has been received.
	 *
	 * @return
	 */
	public PedestriansAtTimeStep generatePedestrians(@NotNull final LinkedList<IosbOutput.AreaInfoAtTime> areaInfoAtTimes) {

		PedestriansAtTimeStep pedestriansAtTimeStep = new PedestriansAtTimeStep();

		if(!areaInfoAtTimes.isEmpty()) {
			List<DynamicElement> allPeds = new ArrayList<>();

			for(IosbOutput.AreaInfoAtTime areaInfoAtTime : areaInfoAtTimes) {
				List<DynamicElement> peds = generatePedestrians(areaInfoAtTime);
				allPeds.addAll(peds);
			}

			pedestriansAtTimeStep.pedestrians = allPeds;
			Timestamp timestamp = areaInfoAtTimes.getFirst().getTime();
			pedestriansAtTimeStep.timeStepInSec = timestamp.getSeconds() + timestamp.getNanos();
		}
		else {
			pedestriansAtTimeStep.pedestrians = Collections.EMPTY_LIST;
			pedestriansAtTimeStep.timeStepInSec = -1;
		}

		return pedestriansAtTimeStep;
	}

	private List<DynamicElement> generatePedestrians(final IosbOutput.AreaInfoAtTime msg) {
		VPolygon polygon = toPolygon(msg);
		return generatePedestrians(polygon, msg.getPedestrians());
	}

	private List<DynamicElement> generatePedestrians(@NotNull final VPolygon polygon, final double numberOfPedestrians) {
		List<DynamicElement> pedestrians = new ArrayList<>();
		int n =  (int)Math.round(numberOfPedestrians);
		List<VPoint> randomPositions = generateRandomPositions(polygon, n);

		LinkedList<Integer> targetIds = topography.getTargets().stream().map(t -> t.getId()).collect(Collectors.toCollection(LinkedList::new));


		for(int id = 1; id < n + 1; id++) {
			Pedestrian ped = (Pedestrian) mainModel.createElement(randomPositions.get(id-1), -1, Pedestrian.class);
			ped.setTargets(targetIds);
			pedestrians.add(ped);
		}

		return pedestrians;
	}

	private List<VPoint> generateRandomPositions(@NotNull final VPolygon polygon, int n) {
		List<VPoint> randomPoints = new ArrayList<>();
		for(int i = 0; i < n; i++) {
			VPoint randomPoint;
			do {
				Rectangle2D bound = polygon.getBounds2D();
				randomPoint = new VPoint(bound.getMinX() + random.nextDouble() * bound.getWidth(), bound.getMinY() + random.nextDouble() * bound.getHeight());
			}
			while (!polygon.contains(randomPoint));
			randomPoints.add(randomPoint);
		}

		return randomPoints;
	}

	private VPolygon toPolygon(final IosbOutput.AreaInfoAtTime msg) {
		Common.RegionOfInterest regionOfInterest = msg.getPolygon();

		Path2D.Double path = new Path2D.Double();
		boolean first = true;
		for(Common.UTMCoordinate coord : regionOfInterest.getCoordinateList()) {
			if(first) {
				first = false;
				path.moveTo(coord.getEasting(), coord.getNorthing());
			}
			else {
				path.lineTo(coord.getEasting(), coord.getNorthing());
			}
		}
		path.closePath();
		return new VPolygon(path);
	}

	private static class PedestriansAtTimeStep {
		public long timeStepInSec;
		public List<DynamicElement> pedestrians;
	}
}
