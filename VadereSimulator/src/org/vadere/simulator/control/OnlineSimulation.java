package org.vadere.simulator.control;


import com.google.protobuf.Timestamp;

import org.jetbrains.annotations.Nullable;
import org.vadere.s2ucre.Utils;
import org.vadere.s2ucre.generated.Common;
import org.vadere.s2ucre.generated.IosbOutput;

import org.vadere.s2ucre.generated.Pedestrian.PedMsg;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.vadere.simulator.models.MainModel;
import org.vadere.simulator.projects.ScenarioStore;
import org.vadere.simulator.projects.SimulationResult;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.state.attributes.AttributesSimulation;
import org.vadere.state.scenario.DynamicElement;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.vadere.s2ucre.client.Receiver;
import org.vadere.s2ucre.client.Sender;
import org.vadere.util.geometry.shapes.VPoint;

/**
 * An OnlineSimulation {@link OnlineSimulation} is a simulation which receives its initial data i.e. the positions of its
 * pedestrian in an online fashion.The simulation starts if data arrive and re-starts after it has finished and new data arrive.
 * Therefore, an OnlineSimulation can be seen as multiple simulation runs executing {@link Simulation#preLoop()} and
 * {@link Simulation#postLoop()} only once. This has, for example the effect that floor-field computation for static floor field is
 * done once for all those simulation runs. The same is true for the output-processors {@link org.vadere.simulator.projects.dataprocessing.processor.DataProcessor}
 * which will write as if all those simulations are one large simulation!
 *
 * Note: The generation of Pedestrian positions is done by using accu:rate-component TranslateIt or our own implementation Translate.
 * Both are separated applications communicating via zeroMQ and protobuf and are interchangeable.
 *
 * @author Benedikt Zoennchen
 */
public class OnlineSimulation extends Simulation implements Consumer<LinkedList<PedMsg>> {

	private static Logger logger = LogManager.getLogger(OnlineSimulation.class);
	private Random random;
	private MainModel mainModel;
	private Topography topography;
	private AttributesSimulation attributesSimulation;

	/**
	 * A helper from which the simulation gets its initial data using zeroMQ and protobuf.
	 */
	private Receiver receiver;

	/**
	 * A helper which sends the simulation results using zeroMQ and protobuf.
	 */
	private Sender sender;

	/**
	 * A container holding the current simulation data and information about the place and time of the simulation.
	 */
	private @Nullable PedestriansAtTimeStep currentSimulationData;


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
		this.attributesSimulation = scenarioStore.getAttributesSimulation();
		this.currentSimulationData = null;
		this.mainModel = mainModel;
		this.topography = scenarioStore.getTopography();
		this.receiver = new Receiver(attributesSimulation.getOnlineModeSubscriber());
		this.sender = new Sender(attributesSimulation.getOnlineModePublisher());
		this.receiver.addPedMsgConsumer(this);
		this.sender.start();
		this.receiver.start();

		logger.info(attributesSimulation.getOnlineModeSubscriber() + " -> VADERE -> " + attributesSimulation.getOnlineModePublisher());
	}

	/**
	 * Sets the time at which the current simulation started.
	 *
	 * @param startTime
	 */
	private void setStartTime(double startTime) {
		startTimeInSec = startTime;
		simTimeInSec = startTime;
	}

	@Override
	protected synchronized void iterate() {

		/**
		 * (1) Wait for data to be arrived see {@link OnlineSimulation#accept(LinkedList)}.
 		 */
		while (runSimulation && !Thread.currentThread().isInterrupted() && topography.getPedestrianDynamicElements().getElements().isEmpty()) {
			logger.debug("wait for data to arrive " + Thread.currentThread());

			try {
				wait();
			} catch (InterruptedException e) {
				logger.debug("thread interrupted while waiting.");
				Thread.currentThread().interrupt();
			}
		}
		/**
		 * (2) Simulate as long as there are pedestrians in the topography.
		 */
		super.iterate();

		/**
		 * (3) Send the results.
		 */
		sendPedestrians();
	}

	@Override
	public void loop() {
		while (runSimulation && !Thread.currentThread().isInterrupted()) {
			iterate();
		}
	}

	@Override
	protected synchronized void postLoop() {
		try {
			super.postLoop();
		}
		finally {
			try {
				sender.close();
			}
			finally {
				receiver.close();
			}
		}
	}

	@Override
	protected boolean hasFinished() {
		return Thread.currentThread().isInterrupted();
	}

	@Override
	protected double nextSimTimeInSec() {
		return simTimeInSec + attributesSimulation.getSimTimeStepLength();
	}

	/**
	 * This will be called by the receiver {@link Receiver} whenever data arrive. The data are
	 * pre-processed by TranslateIt or {@link org.vadere.s2ucre.translate.Translate}. This method
	 * will be called by a thread which is not the simulation thread.
	 *
	 * @param pedMsgs the arrived data
	 */
	@Override
	public synchronized void accept(@NotNull final LinkedList<PedMsg> pedMsgs) {
		if(topography.getPedestrianDynamicElements().getElements().isEmpty()) {
			pause();
			topography.getPedestrianDynamicElements().clear();
			currentSimulationData = toVaderePedestrians(pedMsgs);
			logger.debug("re-initialize simulation.");
			currentSimulationData.pedestrians.stream().forEach(ped -> topography.addElement(ped));
			setStartTime(Utils.toSeconds(currentSimulationData.timestamp));
			resume();
			notifyAll();
		}
	}

	/**
	 * Sends the current simulation state (pedestrians, time, place) using zeroMQ and protobuf.
	 */
	public void sendPedestrians() {
		Timestamp timestamp = Utils.addSeconds(currentSimulationData.timestamp, simTimeInSec);
		for(DynamicElement dynamicElement : topography.getPedestrianDynamicElements().getElements()) {
			Pedestrian ped = (Pedestrian) dynamicElement;
			sender.send(ped, timestamp, currentSimulationData.reference);
		}
		logger.debug("send positions");
	}

	public PedestriansAtTimeStep toVaderePedestrians(@NotNull final List<PedMsg> pedMsgs) {

		PedestriansAtTimeStep pedestriansAtTimeStep = new PedestriansAtTimeStep();

		if(!pedMsgs.isEmpty()) {
			List<DynamicElement> allPeds = new ArrayList<>();
			LinkedList<Integer> targetIds = topography.getTargets().stream().map(t -> t.getId()).collect(Collectors.toCollection(LinkedList::new));

			for(PedMsg pedMsg : pedMsgs) {

				VPoint position = Utils.toVPoint(pedMsg.getPosition());
				double radius = topography.getAttributesPedestrian().getRadius();

				if(topography.getBounds().contains(position.getX(), position.getY()) && topography.getObstacles().stream().allMatch(obs -> obs.getShape().distance(position) >= radius)) {

					Pedestrian ped = (Pedestrian) mainModel.createElement(position, pedMsg.getPedId(), Pedestrian.class);
					ped.setTargets(targetIds);
					ped.setVelocity(Utils.toVelocity(pedMsg.getVelocity()));
					allPeds.add(ped);
				}
				else {
					logger.error("invalid initial pedestrian position!");
				}

			}

			pedestriansAtTimeStep.pedestrians = allPeds;
			Timestamp timestamp = pedMsgs.get(0).getTime();
			pedestriansAtTimeStep.timestamp = timestamp;
			pedestriansAtTimeStep.reference = pedMsgs.get(0).getPosition();
		}
		else {
			pedestriansAtTimeStep.pedestrians = Collections.EMPTY_LIST;
			pedestriansAtTimeStep.timestamp = Timestamp.newBuilder().build();
			pedestriansAtTimeStep.reference = Common.UTMCoordinate.newBuilder().build();
		}

		return pedestriansAtTimeStep;
	}

	private static class PedestriansAtTimeStep {
		public Timestamp timestamp;
		public Common.UTMCoordinate reference;
		public List<DynamicElement> pedestrians;
	}
}
