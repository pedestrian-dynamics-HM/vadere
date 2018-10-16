package org.vadere.s2ucre.client;

import com.google.protobuf.Timestamp;

import org.vadere.s2ucre.Publisher;
import org.vadere.s2ucre.Utils;
import org.vadere.s2ucre.generated.Common;
import org.vadere.s2ucre.generated.SimulatorOutput;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.vadere.state.scenario.Pedestrian;
import org.zeromq.ZMQ;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This class receives {@link SimulatorOutput.PedestrianAtTime} by using a {@link ZMQ.Socket}.
 */
public class Sender {

	private final static String SENDER_ADDRESS = "tcp://localhost:5011";

	private Logger logger = LogManager.getLogger(Sender.class);

	private Publisher publisher;

	private static Sender sender = null;

	private ExecutorService executor;

	public static Sender getInstance() {
		return getInstance(SENDER_ADDRESS);
	}

	public static Sender getInstance(@NotNull final String address) {
		if(sender == null) {
			sender = new Sender(address);
		}
		else {
			if (sender.publisher.getAddress().equals(address)) {
				return sender;
			}
			else {
				sender.close();
				sender = new Sender(address);
			}
		}
		return sender;
	}

	public Sender (@NotNull final String address) {
		this.executor = Executors.newSingleThreadExecutor();
		this.publisher = new Publisher(address);
	}

	public void start() {
		publisher.open();
	}

	public void close() {
		publisher.close();
		executor.shutdownNow();
	}

	public void send(@NotNull final Pedestrian pedestrian, @NotNull final Timestamp timestamp, @NotNull final Common.UTMCoordinate referenc) {
		Runnable runnable = () -> {
			org.vadere.s2ucre.generated.Pedestrian.PedMsg simulatedPedestrians = toPedestrianAtTime(pedestrian, timestamp, referenc);
			publisher.send(simulatedPedestrians.toByteArray());
		};
		executor.execute(runnable);
	}

	private org.vadere.s2ucre.generated.Pedestrian.PedMsg toPedestrianAtTime(
			@NotNull final Pedestrian pedestrian,
			@NotNull final Timestamp timestamp,
			@NotNull final Common.UTMCoordinate reference) {

		Common.UTMCoordinate coordinate = Utils.toUTMCoordinate(pedestrian.getPosition(), reference);

		Common.Point3D velocity = Common.Point3D.newBuilder()
				.setX(pedestrian.getVelocity().x)
				.setY(pedestrian.getVelocity().y)
				.setZ(0.0).build();

		org.vadere.s2ucre.generated.Pedestrian.PedMsg pedMsg = org.vadere.s2ucre.generated.Pedestrian.PedMsg.newBuilder()
				.setTime(timestamp)
				.setPedId(pedestrian.getId())
				.setPosition(coordinate)
				.setVelocity(velocity)
				.build();

		return pedMsg;
	}
}
