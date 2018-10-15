package client;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Timestamp;

import de.s2ucre.protobuf.generated.Common;
import de.s2ucre.protobuf.generated.IosbOutput;
import de.s2ucre.protobuf.generated.SimulatorOutput;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.zeromq.ZMQ;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * This class receives {@link IosbOutput.AreaInfoAtTime} by using a {@link ZMQ.Socket}.
 */
public class Receiver {

	/**
	 * The address from which the receiver, recives its messages e.g. the address of the publisher
	 * of ParseIt.
	 */
	private final static String RECEIVER_ADDRESS = "tcp://localhost:5000";

	private static Receiver receiver = null;

	private static Logger logger = LogManager.getLogger(Receiver.class);

	private ZMQ.Socket subscriber;
	private ZMQ.Context context;
	private final String address;

	private boolean running;
	private TreeMap<Timestamp, LinkedList<IosbOutput.AreaInfoAtTime>> data;
	private List<Consumer<LinkedList<IosbOutput.AreaInfoAtTime>>> consumers;
	private Executor executor;

	public static Receiver getInstance() {
		if(receiver == null) {
			receiver = new Receiver(RECEIVER_ADDRESS);
		}
		return receiver;
	}

	public Receiver(@NotNull final String address) {
		this.address = address;
		this.data = new TreeMap<>(new TimestampComparator());
		this.executor = Executors.newFixedThreadPool(2);
		this.context = ZMQ.context(1);
		this.running = false;
		this.consumers = new ArrayList<>(1);
	}


	public void start() {
		running = true;
		// start the thread which receives messages.
		Runnable messagesReceiver = () -> {
			try {
				receiveAreaInfoAtTimes();
			} catch (InvalidProtocolBufferException e) {
				e.printStackTrace();
			}
		};

		// start the thread which gives the newest message to the consumer.
		Runnable messageReceiver = () -> {
			while (running) {
				LinkedList<IosbOutput.AreaInfoAtTime> message = receiveAreaInfoAtTime();
				logger.info("consume: " + message.getFirst());
				consumers.forEach(c -> c.accept(message));
			}
		};

		executor.execute(messagesReceiver);
		executor.execute(messageReceiver);
	}

	public void close() {
		running = false;
		subscriber.close();
		context.term();
	}

	public void addConsumer(@NotNull final Consumer<LinkedList<IosbOutput.AreaInfoAtTime>> consumer) {
		consumers.add(consumer);
	}

	/**
	 * Returns the List of area information for the most current timestep for which the information is valid.
	 * @return
	 */
	public synchronized LinkedList<IosbOutput.AreaInfoAtTime> receiveAreaInfoAtTime() {
		if(data.size() < 2) {
			try {
				logger.info("waiting for message " + Thread.currentThread());
				wait();
				return receiveAreaInfoAtTime();
			} catch (InterruptedException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		} else {
			// return the second element since we can only be sure that it is complete i.e. we received all informations.
			logger.info("return information");
			Timestamp secondMinTimeStep = data.higherEntry(data.firstKey()).getKey();
			return data.remove(secondMinTimeStep);
		}
	}

	/**
	 * Returns the List of area information for a specific timestep for which the information is valid.
	 * @param timeStepInSec
	 * @return
	 */
	public synchronized List<IosbOutput.AreaInfoAtTime> receiveAreaInfoAtTime(final long timeStepInSec) {
		Timestamp min = data.firstKey();

		if(min.getSeconds() + min.getNanos() <= timeStepInSec) {
			try {
				logger.info("waiting for message " + Thread.currentThread());
				wait();
				return receiveAreaInfoAtTime(timeStepInSec);
			} catch (InterruptedException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}
		else {
			logger.info("return information");
			return data.get(timeStepInSec);
		}
	}

	/**
	 * This method receives and stores messages IosbOutput.AreaInfoAtTime.
	 *
	 * @throws InvalidProtocolBufferException
	 */
    private void receiveAreaInfoAtTimes() throws InvalidProtocolBufferException {
    	subscriber = context.socket(ZMQ.SUB);
	    subscriber.connect(address);
	    subscriber.subscribe("".getBytes());

	    while (running) {
		    //logger.info("Waiting for Message");
		    IosbOutput.AreaInfoAtTime msg = IosbOutput.AreaInfoAtTime.parseFrom(subscriber.recv());
		    //System.out.println(msg);
		    Timestamp timestamp = msg.getTime();

		    synchronized(this) {
			    if(!data.containsKey(timestamp)){
				    data.put(timestamp, new LinkedList<>());
			    }

			    data.get(timestamp).add(msg);
			    notifyAll();
		    }
	    }
	    //  Socket to talk to server

	    subscriber.close();
	    context.term();
    }

    private class TimestampComparator implements Comparator<Timestamp> {

	    @Override
	    public int compare(Timestamp o1, Timestamp o2) {
	    	if(o1.getSeconds() + o1.getNanos() < o2.getSeconds() + o2.getNanos()) {
	    		return 1;
		    }
		    else if(o1.getSeconds() + o1.getNanos() > o2.getSeconds() + o2.getNanos()) {
		    	return -1;
		    }

		    return 0;
	    }
    }

}

