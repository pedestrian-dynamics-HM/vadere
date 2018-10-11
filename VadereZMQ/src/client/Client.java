package client;

import com.google.protobuf.InvalidProtocolBufferException;

import de.s2ucre.protobuf.generated.IosbOutput;

import org.zeromq.ZMQ;

//
//  Connects SUB socket to tcp://localhost:5556
//
public class Client {

    public static void main(String[] args) throws InvalidProtocolBufferException {
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
    }

    public IosbOutput.AreaInfoAtTime receiveAreaInfoAtTime() throws InvalidProtocolBufferException {
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

	    return msg;
    }
}

