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

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * This class receives {@link SimulatorOutput.PedestrianAtTime} by using a {@link ZMQ.Socket}.
 */
public class Sender {

	private final static String SENDER_ADDRESS = "tcp://localhost:5011";

	private Logger logger = LogManager.getLogger(Sender.class);

	private List<Future<?>> submittedSends;

	private Publisher publisher;

	private ExecutorService executor;

	private boolean running;

	public Sender (@NotNull final String address) {
		this.publisher = new Publisher(address);
		this.running = false;
		this.submittedSends = new LinkedList<>();
		this.executor = Executors.newSingleThreadExecutor();
	}

	public synchronized void start() {
		publisher.open();
		running = true;
	}

	public synchronized void close() {
		running = false;


		// cancel all not jet started sends
		/*for(Future<?> send : submittedSends) {
			send.cancel(false);
		}*/

		executor.submit(() -> {
			publisher.close();
		});

		// close the publisher in a thread since .close() can hang
		executor.shutdown();

		try {
			// wait a certain amount of time such that running submission can finish their job
			if(!executor.awaitTermination(4000, TimeUnit.MILLISECONDS)) {
				logger.error("send or socket.close / socked.unbind was interrupted!");
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		finally {
			// hard shut down, might fail!
			executor.shutdownNow();
			publisher.close();
			submittedSends.clear();
		}

		logger.debug("sender is closed.");
	}

	public synchronized void send(@NotNull final Pedestrian pedestrian, @NotNull final Timestamp timestamp, @NotNull final Common.UTMCoordinate referenc) {
		if(running) {
			Runnable runnable = () -> {
				org.vadere.s2ucre.generated.Pedestrian.PedMsg simulatedPedestrians = toPedestrianAtTime(pedestrian, timestamp, referenc);
				publisher.send(simulatedPedestrians.toByteArray());
			};
			submittedSends.add(executor.submit(runnable));
		}
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
