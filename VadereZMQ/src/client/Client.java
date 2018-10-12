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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

//
//  Connects SUB socket to tcp://localhost:5556
//
public class Client {

	private Logger logger = LogManager.getLogger(Client.class);
	private ZMQ.Socket publisher;
	private ZMQ.Socket subscriber;
	private TreeMap<Long, LinkedList<IosbOutput.AreaInfoAtTime>> data;
	private Thread vadereThread;

	public Client() {
		this.data = new TreeMap<>((t1, t2) -> -Long.compare(t1, t2));

		// start the thread which receives messages.
		Runnable runnable = () -> {
			try {
				receiveAreaInfoAtTimes();
			} catch (InvalidProtocolBufferException e) {
				e.printStackTrace();
			}
		};
		new Thread(runnable).start();
	}

   /* public static void main(String[] args) throws InvalidProtocolBufferException {
        ZMQ.Context context = ZMQ.context(1);
	    
        //  Socket to talk to server
        System.out.println("Waiting for Message");
        ZMQ.Socket subscriber = context.socket(ZMQ.SUB);
        subscriber.connect("tcp://localhost:5556");

        subscriber.subscribe("");

        IosbOutput.AreaInfoAtTime msg = IosbOutput.AreaInfoAtTime.parseFrom(subscriber.recv());

        System.out.println(msg);

        subscriber.close();
        context.term();
    }*/

    public void send(final double[] positions, final int[] ids, final long timeStepInSec) {
    	if(publisher == null) {
		    ZMQ.Context context = ZMQ.context(1);
		    publisher = context.socket(ZMQ.PUB);
		    publisher.connect("tcp://localhost:5557");
	    }
	    assert positions.length / 2 == ids.length;

	    Runnable runnable = () -> {
			SimulatorOutput.SimulatedPedestrians.Builder builder = SimulatorOutput.SimulatedPedestrians.newBuilder();
			Timestamp timestamp = Timestamp.newBuilder().setSeconds(timeStepInSec).build();

			for(int i = 0; i < positions.length; i+=2) {
				Common.UTMCoordinate coordinate = Common.UTMCoordinate.newBuilder()
						.setEasting(564303.4638894079)
						.setNorthing(5933436.907734532).build();

				SimulatorOutput.PedestrianAtTime pedestrianAtTime = SimulatorOutput.PedestrianAtTime.newBuilder()
						.setTime(timestamp)
						.setPedId(ids[i / 2])
						.setPosition(coordinate)
						.build();

				builder.addPedestrians(pedestrianAtTime);
			}
			publisher.send(builder.build().toByteArray());
		};
	    new Thread(runnable).start();
    }

	/**
	 * Returns the List of area information for the most current timestep for which the information is valid.
	 * @return
	 */
	public synchronized LinkedList<IosbOutput.AreaInfoAtTime> receiveAreaInfoAtTime() {
		if(data.size() < 2) {
			vadereThread = Thread.currentThread();
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
			return data.remove(data.ceilingKey(data.firstKey()-1));
		}
	}

	/**
	 * Returns the List of area information for a specific timestep for which the information is valid.
	 * @param timeStepInSec
	 * @return
	 */
	public List<IosbOutput.AreaInfoAtTime> receiveAreaInfoAtTime(final long timeStepInSec) {
		if(data.firstKey() > timeStepInSec) {
			return data.get(timeStepInSec);
		}
		else {
			vadereThread = Thread.currentThread();
			try {
				vadereThread.wait();
				return receiveAreaInfoAtTime(timeStepInSec);
			} catch (InterruptedException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}
	}

	/**
	 * This method receives and stores messages IosbOutput.AreaInfoAtTime.
	 *
	 * @throws InvalidProtocolBufferException
	 */
    private void receiveAreaInfoAtTimes() throws InvalidProtocolBufferException {
	    ZMQ.Context context = ZMQ.context(1);

	    subscriber = context.socket(ZMQ.SUB);
	    subscriber.connect("tcp://localhost:5556");
	    subscriber.subscribe("");

	    while (!Thread.currentThread().isInterrupted()) {
		    logger.info("Waiting for Message");
		    IosbOutput.AreaInfoAtTime msg = IosbOutput.AreaInfoAtTime.parseFrom(subscriber.recv());
		    //System.out.println(msg);
		    Timestamp timestamp = msg.getTime();
		    Long timeInSec = timestamp.getSeconds();
		    synchronized(this) {
			    if(!data.containsKey(timeInSec)){
				    data.put(timeInSec, new LinkedList<>());
			    }

			    data.get(timeInSec).add(msg);
			    notifyAll();
		    }
	    }
	    //  Socket to talk to server

	    subscriber.close();
	    context.term();
    }

}

