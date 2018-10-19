package org.vadere.s2ucre.client;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Timestamp;

import org.vadere.s2ucre.Subscriber;
import org.vadere.s2ucre.generated.IosbOutput;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.vadere.s2ucre.generated.Pedestrian;
import org.zeromq.ZMQ;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * This class receives {@link IosbOutput.AreaInfoAtTime} by using a {@link ZMQ.Socket}.
 */
public class Receiver {

	/**
	 * The address from which the receiver, receives its messages e.g. the address of the publisher
	 * of ParseIt, a dummy Server which produces random data or TranslateIt.
	 */
	private final static String RECEIVER_ADDRESS = "tcp://localhost:5009";

	private static Receiver receiver = null;

	private static Logger logger = LogManager.getLogger(Receiver.class);

	private Subscriber subscriber;

	private boolean running;
	private TreeMap<Timestamp, LinkedList<Pedestrian.PedMsg>> pedMsgContainer;
	private List<Consumer<LinkedList<Pedestrian.PedMsg>>> pedMsgConsumers;

	private ExecutorService executor;

	public Receiver(@NotNull final String address) {
		this.subscriber = new Subscriber(address);
		this.pedMsgContainer = new TreeMap<>(new TimestampComparator());
		this.executor = Executors.newFixedThreadPool(2);
		this.running = false;
		this.pedMsgConsumers = new ArrayList<>(1);
	}

	public synchronized void start() {
		running = true;
		// start the thread which receives messages.
		Runnable subscriberRun;

		// start the thread which gives the newest message to the consumer.
		Runnable messageReceiver;

		subscriber.open();
		subscriberRun = () -> {
			try {
				receivePedMsg();
			} catch (Exception e) {
				e.printStackTrace();
			}
			finally {
				synchronized (this) {
					notifyAll();
				}
			}
		};

		messageReceiver = () -> {
			while (running) {
				LinkedList<Pedestrian.PedMsg> message = receivePedMsgs();
				logger.info("consume: " + message.getFirst());
				pedMsgConsumers.forEach(c -> c.accept(message));
			}
		};

		executor.submit(subscriberRun);
		executor.submit(messageReceiver);
	}

	public synchronized void close() {
		running = false;
		executor.shutdown();

		try {
			// wait a certain amount of time such that running submission can finish their job
			if(!executor.awaitTermination(1000, TimeUnit.MILLISECONDS)) {
				logger.info("send was interrupted.");
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			executor.shutdownNow();
			subscriber.close();
		}
		logger.info("receiver is closed.");
	}

	public void addPedMsgConsumer(@NotNull final Consumer<LinkedList<Pedestrian.PedMsg>> consumer) {
		pedMsgConsumers.add(consumer);
	}

	private synchronized LinkedList<Pedestrian.PedMsg> receivePedMsgs() {
		if(!running) {
			return new LinkedList<>();
		}

		if(pedMsgContainer.size() < 2) {
			try {
				logger.info("waiting for message " + Thread.currentThread());
				wait();
				logger.info("woke up " + Thread.currentThread());
				return receivePedMsgs();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				logger.warn("interrupt while paused.");
				return new LinkedList<>();
			}
		} else {
			// return the second element since we can only be sure that it is complete i.e. we received all informations.
			logger.info("return information");
			Timestamp secondMinTimeStep = pedMsgContainer.higherEntry(pedMsgContainer.firstKey()).getKey();
			return pedMsgContainer.remove(secondMinTimeStep);
		}
	}

	/**
	 * This method receives and stores messages {@link org.vadere.s2ucre.generated.Pedestrian.PedMsg}
	 * from {@link Receiver#subscriber} by using a {@link ZMQ.Socket}.
	 *
	 * @throws InvalidProtocolBufferException
	 */
	private void receivePedMsg() throws InvalidProtocolBufferException, InterruptedException {
		while (running) {
			logger.info("Waiting for Message");
			Pedestrian.PedMsg msg = Pedestrian.PedMsg.parseFrom(subscriber.receive());
			//System.out.println(msg);
			Timestamp timestamp = msg.getTime();

			synchronized(this) {
				if(!pedMsgContainer.containsKey(timestamp)){
					pedMsgContainer.put(timestamp, new LinkedList<>());
				}

				pedMsgContainer.get(timestamp).add(msg);
				notifyAll();
			}
		}
		//  Socket to talk to server
	}

	/**
	 * (Reverse)-Comparator for {@link Timestamp}. The minimal element is the largest element such that
	 * {@link TreeMap} is sorted in a descendant fashion i.e. the element with the largest timestamp is
	 * the first element in the tree i.e. the root of the tree.
	 */
	private class TimestampComparator implements Comparator<Timestamp> {

	    @Override
	    public int compare(@NotNull final Timestamp o1, @NotNull final Timestamp o2) {
		    if(o1.getSeconds() < o2.getSeconds()) {
		    	return 1;
		    }
		    else if(o1.getSeconds() > o2.getSeconds()) {
		    	return -1;
		    }
		    else {
		    	if(o1.getNanos() < o2.getNanos()) {
		    		return 1;
			    }
			    else if(o1.getNanos() > o2.getNanos()) {
			    	return -1;
			    }
			    else {
			    	return 0;
			    }
		    }
	    }
    }

}

