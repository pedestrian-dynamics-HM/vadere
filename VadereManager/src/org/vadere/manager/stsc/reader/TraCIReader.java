package org.vadere.manager.stsc.reader;

import org.vadere.manager.stsc.TraCIDataType;
import org.vadere.manager.stsc.sumo.RoadMapPosition;
import org.vadere.manager.stsc.sumo.TrafficLightPhase;
import org.vadere.util.geometry.Vector3D;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VPolygon;

import java.awt.*;
import java.nio.ByteBuffer;
import java.util.List;


/**
 * Definition of read methods used to deserialize TraCICommands / TraCIResponses received
 * over a socket.
 */
public interface TraCIReader {


	byte readByte();
	default int readUnsignedByte(){
		// (signed)byte --cast--> (signed)int --(& 0xff)--> cut highest three bytes.
		// This result represents the an unsigned byte value (0..255) as an int.
		return (int)readByte() & 0xff;
	}

	byte[] readBytes(int num);
	default ByteBuffer readByteBuffer(int num){
		return ByteBuffer.wrap(readBytes(num));
	}

	int readInt();
	double readDouble();

	String readString();

	List<String> readStringList();

	VPoint read2DPosition();

	Vector3D read3DPosition();

	RoadMapPosition readRoadMapPosition();

	VPoint readLonLatPosition();

	Vector3D readLonLatAltPosition();

	VPolygon readPolygon();

	List<TrafficLightPhase> readTrafficLightPhaseList();

	Object readTypeValue(TraCIDataType type);

	Color readColor();

	boolean hasRemaining();

	void ensureBytes(int num);

	int limit();
}
