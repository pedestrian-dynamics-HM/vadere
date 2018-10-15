package client;

import com.google.protobuf.Timestamp;

import de.s2ucre.protobuf.generated.Common;
import de.s2ucre.protobuf.generated.SimulatorOutput;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.vadere.state.scenario.Pedestrian;
import org.zeromq.ZMQ;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * This class receives {@link SimulatorOutput.PedestrianAtTime} by using a {@link ZMQ.Socket}.
 */
public class Sender {

	private final static String SENDER_ADDRESS = "tcp://localhost:5011";
	private Logger logger = LogManager.getLogger(Sender.class);

	private ZMQ.Socket publisher;
	private ZMQ.Context context;
	private String address;

	private static Sender sender = null;

	private Executor executor;

	public static Sender getInstance() {
		if(sender == null) {
			sender = new Sender(SENDER_ADDRESS);
		}
		return sender;
	}

	public Sender (@NotNull final String address) {
		this.executor = Executors.newSingleThreadExecutor();
		this.address = address;
	}

	public void start() {
		context = ZMQ.context(1);
		publisher = context.socket(ZMQ.PUB);
		publisher.bind(address);
	}

	public void close() {
		publisher.close();
		context.term();
	}

	public void send(final double[] positions, final int[] ids, final long timeStepInSec) {
		assert positions.length / 2 == ids.length;
		Runnable runnable = () -> {
			SimulatorOutput.SimulatedPedestrians simulatedPedestrians = toSimulatedPedestrians(positions, ids, timeStepInSec);
			publisher.send(simulatedPedestrians.toByteArray());
		};
		executor.execute(runnable);
	}

	public void send(@NotNull final Pedestrian pedestrian, final long timeStepInSec) {
		Runnable runnable = () -> {
			SimulatorOutput.PedestrianAtTime simulatedPedestrians = toPedestrianAtTime(pedestrian, timeStepInSec);
			publisher.send(simulatedPedestrians.toByteArray());
		};
		executor.execute(runnable);
	}

	private SimulatorOutput.PedestrianAtTime toPedestrianAtTime(@NotNull final Pedestrian pedestrian, final long timeStepInSec) {
		Common.UTMCoordinate coordinate = Common.UTMCoordinate.newBuilder()
				.setEasting(pedestrian.getPosition().getX())
				.setNorthing(pedestrian.getPosition().getY()).build();

		Timestamp timestamp = Timestamp.newBuilder().setSeconds(timeStepInSec).build();

		SimulatorOutput.PedestrianAtTime pedestrianAtTime = SimulatorOutput.PedestrianAtTime.newBuilder()
				.setTime(timestamp)
				.setPedId(pedestrian.getId())
				.setPosition(coordinate)
				.build();

		return pedestrianAtTime;
	}

	/**
	 *
	 *
	 * @param positions
	 * @param ids
	 * @param timeStepInSec
	 */
	private SimulatorOutput.SimulatedPedestrians toSimulatedPedestrians(@NotNull final double[] positions, @NotNull final int[] ids, final long timeStepInSec) {
		SimulatorOutput.SimulatedPedestrians.Builder builder = SimulatorOutput.SimulatedPedestrians.newBuilder();
		Timestamp timestamp = Timestamp.newBuilder().setSeconds(timeStepInSec).build();

		for(int i = 0; i < positions.length; i+=2) {
			Common.UTMCoordinate coordinate = Common.UTMCoordinate.newBuilder()
					.setEasting(positions[i])
					.setNorthing(positions[i+1]).build();

			SimulatorOutput.PedestrianAtTime pedestrianAtTime = SimulatorOutput.PedestrianAtTime.newBuilder()
					.setTime(timestamp)
					.setPedId(ids[i / 2])
					.setPosition(coordinate)
					.build();

			builder.addPedestrians(pedestrianAtTime);
		}

		return builder.build();
	}
}
