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
		this.receiver = Receiver.getInstance(attributesSimulation.getOnlineModeSubscriber());
		this.sender = Sender.getInstance(attributesSimulation.getOnlineModePublisher());
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
	}

	@Override
	protected synchronized void iterate() {
		while (topography.getPedestrianDynamicElements().getElements().isEmpty()) {
			logger.info("wait for data to arrive");

			try {
				wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		super.iterate();
		sendPedestrians();
	}

	@Override
	public void loop() {
		while (!Thread.currentThread().isInterrupted() && !hasFinished()) {
			iterate();
		}
	}

	@Override
	protected void postLoop() {
		super.postLoop();
		sender.close();
		receiver.close();
	}

	@Override
	protected boolean hasFinished() {
		return false;
	}

	@Override
	protected double nextSimTimeInSec() {
		return simTimeInSec + attributesSimulation.getSimTimeStepLength();
	}

	/**
	 * This will be called by the receiver {@link Receiver} whenever data arrive. The data are
	 * pre-processed by TranslateIt or {@link org.vadere.s2ucre.translate.Translate}.
	 *
	 * @param pedMsgs the arrived data
	 */
	@Override
	public synchronized void accept(@NotNull final LinkedList<PedMsg> pedMsgs) {
		if(topography.getPedestrianDynamicElements().getElements().isEmpty()) {
			pause();
			topography.getPedestrianDynamicElements().clear();
			currentSimulationData = toVaderePedestrians(pedMsgs);
			logger.info("re-initialize simulation.");
			currentSimulationData.pedestrians.stream().forEach(ped -> topography.addElement(ped));
			setStartTime(0.0);
			resume();
			notifyAll();
		}
	}

	/**
	 * Sends the current simulation state (pedestrians, time, place) using zeroMQ and protobuf.
	 */
	public void sendPedestrians() {
		for(DynamicElement dynamicElement : topography.getPedestrianDynamicElements().getElements()) {
			Pedestrian ped = (Pedestrian) dynamicElement;
			sender.send(ped, Utils.addSeconds(currentSimulationData.timestamp, simTimeInSec), currentSimulationData.reference);
		}
		logger.info("send positions");
	}

	public PedestriansAtTimeStep toVaderePedestrians(@NotNull final List<PedMsg> pedMsgs) {

		PedestriansAtTimeStep pedestriansAtTimeStep = new PedestriansAtTimeStep();

		if(!pedMsgs.isEmpty()) {
			List<DynamicElement> allPeds = new ArrayList<>();
			LinkedList<Integer> targetIds = topography.getTargets().stream().map(t -> t.getId()).collect(Collectors.toCollection(LinkedList::new));

			for(PedMsg pedMsg : pedMsgs) {
				Pedestrian ped = (Pedestrian) mainModel.createElement(Utils.toVPoint(pedMsg.getPosition()), pedMsg.getPedId(), Pedestrian.class);
				ped.setTargets(targetIds);
				ped.setVelocity(Utils.toVelocity(pedMsg.getVelocity()));
				allPeds.add(ped);
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
