package server;

import java.util.Random;

import com.google.protobuf.Timestamp;
import org.zeromq.ZMQ;
import de.s2ucre.protobuf.generated.IosbOutput.*;

//
//  Binds PUB socket to tcp://*:5556
//
public class Server {

	public static void main(String[] args) throws Exception {
		//  Prepare our context and publisher
		ZMQ.Context context = ZMQ.context(1);

		ZMQ.Socket publisher = context.socket(ZMQ.PUB);
		publisher.bind("tcp://*:5556");

		//  Initialize random number generator
		Random srandom = new Random(System.currentTimeMillis());
		while (!Thread.currentThread().isInterrupted()) {

			AreaInfoAtTime msg = AreaInfoAtTime.newBuilder()
					.setPedestrians(srandom.nextInt(10000))
					.setTime(Timestamp.newBuilder()
							.setSeconds(srandom.nextInt(10000))
							.build())
					.build();
			publisher.send(msg.toByteArray());
			Thread.sleep(1000);
		}

		publisher.close();
		context.term();
	}
}
