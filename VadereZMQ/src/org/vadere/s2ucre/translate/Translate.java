package org.vadere.s2ucre.translate;

import com.google.protobuf.InvalidProtocolBufferException;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.vadere.s2ucre.Publisher;
import org.vadere.s2ucre.Subscriber;
import org.vadere.s2ucre.Utils;
import org.vadere.s2ucre.generated.IosbOutput;
import org.vadere.s2ucre.generated.Pedestrian;

import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Our TranslateIt.
 *
 * @author Benedikt Zoennchen
 */
public class Translate {

	private final static Logger logger = LogManager.getLogger(Translate.class);

	private Queue<IosbOutput.AreaInfoAtTime> inQueue;
	private Queue<Pedestrian.PedMsg> outQueue;

	private ExecutorService executor;

	private Subscriber subscriber;
	private Runnable subscriberRun;

	private Publisher publisher;
	private Runnable publisherRun;

	private Runnable translater;

	private Random random = new Random(0);

	public static void main(final String... args) {
		Translate translate = new Translate(args[0], args[1]);
		logger.info(args[0] + " -> " + args[1]);
		translate.start();
	}


	public Translate(@NotNull final String subscriberAdr, @NotNull final String publisherAdr) {
		subscriber = new Subscriber(subscriberAdr);
		publisher = new Publisher(publisherAdr);
		executor = Executors.newFixedThreadPool(3);
		inQueue = new ConcurrentLinkedQueue<>();
		outQueue = new ConcurrentLinkedQueue<>();

		subscriberRun = () -> {
			subscriber.open();
			while (!Thread.currentThread().isInterrupted()) {
				byte[] msgBytes = subscriber.receive();
				try {
					IosbOutput.AreaInfoAtTime message = IosbOutput.AreaInfoAtTime.parseFrom(msgBytes);
					inQueue.add(message);
					logger.info("message received");

					synchronized (inQueue) {
						inQueue.notifyAll();
					}

				} catch (InvalidProtocolBufferException e) {
					logger.error(e.getMessage() + " - could not parse message.");
				}
			}
			subscriber.close();
		};

		publisherRun = () -> {
			publisher.open();
			while (!Thread.currentThread().isInterrupted()) {
				while (outQueue.isEmpty()) {
					synchronized (outQueue) {
						try {
							logger.info("wait for message to be translated.");
							outQueue.wait();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
				publisher.send(outQueue.poll().toByteArray());
				logger.info("message sent.");
			}
			publisher.close();
		};

		translater = () -> {
			while (!Thread.currentThread().isInterrupted()) {
				while (inQueue.isEmpty()) {
					synchronized (inQueue) {
						try {
							logger.info("wait for messages.");
							inQueue.wait();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}

				IosbOutput.AreaInfoAtTime areaInfoAtTime = inQueue.poll();
				List<Pedestrian.PedMsg> pedMsgs = translate(areaInfoAtTime);
				for(Pedestrian.PedMsg pedMsg : pedMsgs) {
					outQueue.add(pedMsg);
				}
				logger.info("message translated.");

				synchronized (outQueue) {
					outQueue.notifyAll();
				}
			}
		};
	}

	public void start() {
		executor.submit(translater);
		executor.submit(subscriberRun);
		executor.submit(publisherRun);
	}

	public void stop() {
		executor.shutdownNow();
		subscriber.close();
		publisher.close();
	}

	private List<Pedestrian.PedMsg> translate(@NotNull final IosbOutput.AreaInfoAtTime msg) {
		return Utils.toPedMsg(msg, random);
	}
}
