package org.vadere.manager.traci.reader;

import org.vadere.state.traci.TraCIDataType;
import org.vadere.state.traci.CompoundObject;
import org.vadere.manager.traci.sumo.RoadMapPosition;
import org.vadere.manager.traci.sumo.TrafficLightPhase;
import org.vadere.util.geometry.Vector3D;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VPolygon;

import java.awt.*;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;


/**
 * Definition of read methods used to deserialize TraCICommands / TraCIResponses received over a
 * socket.
 */
public interface TraCIReader {


	byte readByte();

	default int readUnsignedByte() {
		// (signed)byte --cast--> (signed)int --(& 0xff)--> cut highest three bytes.
		// This result represents the an unsigned byte value (0..255) as an int.
		return (int) readByte() & 0xff;
	}

	byte[] readBytes(int num);

	default ByteBuffer readByteBuffer(int num) {
		return ByteBuffer.wrap(readBytes(num));
	}

	void readBytes(byte[] data);

	int readInt();

	double readDouble();

	String readString(int numOfBytes);

	String readString();

	List<String> readStringList();

	VPoint read2DPosition();

	Map<String, VPoint> read2DPositionList();

	Vector3D read3DPosition();

	RoadMapPosition readRoadMapPosition();

	VPoint readLonLatPosition();

	Vector3D readLonLatAltPosition();

	VPolygon readPolygon();

	List<TrafficLightPhase> readTrafficLightPhaseList();

	Object readTypeValue(TraCIDataType type);

	Color readColor();

	CompoundObject readCompoundObject();

	boolean hasRemaining();

	void ensureBytes(int num);

	int limit();

	int position();
}
