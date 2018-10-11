package org.vadere.simulator.control;

import com.google.protobuf.InvalidProtocolBufferException;

import de.s2ucre.protobuf.generated.Common;
import de.s2ucre.protobuf.generated.IosbOutput;

import org.jetbrains.annotations.NotNull;
import org.vadere.simulator.models.MainModel;
import org.vadere.simulator.projects.ScenarioStore;
import org.vadere.simulator.projects.SimulationResult;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VPolygon;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import client.Client;

/**
 * @author Benedikt Zoennchen
 */
public class OnlineSimulation extends Simulation {

	private String subscriberURI = "tcp://localhost:5556";

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
	}

	@Override
	public void loop() {
		// before run wait for incoming message
		List<Pedestrian> receivedPedestrians = receivePedestrians();
		receivedPedestrians.stream().forEach(ped -> topography.addElement(ped));
		super.loop();
	}

	public List<Pedestrian> receivePedestrians() {
		Client client = new Client();
		IosbOutput.AreaInfoAtTime msg = null;
		try {
			msg = client.receiveAreaInfoAtTime();
		} catch (InvalidProtocolBufferException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		return generatePedestrians(msg);
	}

	private List<Pedestrian> generatePedestrians(final IosbOutput.AreaInfoAtTime msg) {
		Common.RegionOfInterest regionOfInterest = msg.getPolygon();
		VPolygon polygon = toPolygon(msg);
		return generatePedestrians(polygon, msg.getPedestrians());
	}

	private List<Pedestrian> generatePedestrians(@NotNull final VPolygon polygon, final double numberOfPedestrians) {
		return new ArrayList<>();
	}


	private VPolygon toPolygon(final IosbOutput.AreaInfoAtTime msg) {
		Common.RegionOfInterest regionOfInterest = msg.getPolygon();

		for(Common.UTMCoordinate coord : regionOfInterest.getCoordinateList()) {

		}

		return new VPolygon();
	}

}
