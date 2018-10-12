package server;

import java.util.Random;

import com.google.protobuf.Timestamp;
import org.zeromq.ZMQ;

import de.s2ucre.protobuf.generated.Common;
import de.s2ucre.protobuf.generated.IosbOutput.*;

//
//  Binds PUB socket to tcp://*:5556
//
public class Server {

	/*
	Kai coordinates of the bridge:

POINT (564303.4638894079 5933436.907734532) POINT (564311.2959240791
5933433.506680049) POINT (564300.9318720899 5933401.544835466) POINT
(564312.1291207125 5933396.936819093) POINT (564309.9431192318
5933391.478288522) POINT (564287.7114777192 5933401.030658626) POINT
(564280.3263510215 5933402.349890053) POINT (564283.8886836024
5933411.251412248) POINT (564290.0683166787 5933406.240937642) POINT
(564295.4593781466 5933403.808810473) POINT (564303.4638894079
5933436.907734532)
	 */

	public static void main(String[] args) throws Exception {
		//  Prepare our context and publisher
		ZMQ.Context context = ZMQ.context(1);

		ZMQ.Socket publisher = context.socket(ZMQ.PUB);
		publisher.bind("tcp://*:5556");

		//  Initialize random number generator
		Random srandom = new Random(System.currentTimeMillis());
		long timeStep = 10000;
		while (!Thread.currentThread().isInterrupted()) {

			Common.RegionOfInterest regionOfInterest = Common.RegionOfInterest.newBuilder()
					.addCoordinate(
							Common.UTMCoordinate.newBuilder()
							.setEasting(564303.4638894079)
							.setNorthing(5933436.907734532).build())
					.addCoordinate(
							Common.UTMCoordinate.newBuilder()
									.setEasting(564311.2959240791)
									.setNorthing(5933433.506680049).build())
					.addCoordinate(
							Common.UTMCoordinate.newBuilder()
									.setEasting(564300.9318720899)
									.setNorthing(5933401.544835466).build())
					.addCoordinate(
							Common.UTMCoordinate.newBuilder()
									.setEasting(564312.1291207125)
									.setNorthing(5933396.936819093).build())
					.addCoordinate(
							Common.UTMCoordinate.newBuilder()
									.setEasting(564309.9431192318)
									.setNorthing(5933391.478288522).build())
					.addCoordinate(
							Common.UTMCoordinate.newBuilder()
									.setEasting(564287.7114777192)
									.setNorthing(5933401.030658626).build())
					.addCoordinate(
							Common.UTMCoordinate.newBuilder()
									.setEasting(564280.3263510215)
									.setNorthing(5933402.349890053).build())
					.addCoordinate(
							Common.UTMCoordinate.newBuilder()
									.setEasting(564283.8886836024)
									.setNorthing(5933411.251412248).build())
					.addCoordinate(
							Common.UTMCoordinate.newBuilder()
									.setEasting(564290.0683166787)
									.setNorthing(5933406.240937642).build())
					.addCoordinate(
							Common.UTMCoordinate.newBuilder()
									.setEasting(564295.4593781466)
									.setNorthing(5933403.808810473).build())
					.addCoordinate(
							Common.UTMCoordinate.newBuilder()
									.setEasting(564303.4638894079)
									.setNorthing(5933436.907734532).build())
					.build();

			AreaInfoAtTime msg = AreaInfoAtTime.newBuilder()
					.setPedestrians(srandom.nextInt(300))
					.setPolygon(regionOfInterest)
					.setTime(Timestamp.newBuilder()
							.setSeconds(timeStep)
							.build())
					.build();
			publisher.send(msg.toByteArray());
			Thread.sleep(4000);
			timeStep += 1;
		}

		publisher.close();
		context.term();
	}
}
