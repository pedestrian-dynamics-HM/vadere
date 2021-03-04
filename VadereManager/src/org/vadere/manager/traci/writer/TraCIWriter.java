package org.vadere.manager.traci.writer;

import org.vadere.state.traci.TraCIException;
import org.vadere.state.traci.TraCIDataType;
import org.vadere.state.traci.CompoundObject;
import org.vadere.manager.traci.sumo.RoadMapPosition;
import org.vadere.manager.traci.sumo.TrafficLightPhase;
import org.vadere.util.geometry.Vector3D;
import org.vadere.util.geometry.shapes.VPoint;

import java.awt.*;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

public interface TraCIWriter {


	ByteBuffer asByteBuffer();

	byte[] asByteArray();

	TraCIWriter rest();

	TraCIWriter writeObjectWithId(TraCIDataType dataType, Object data);

	TraCIWriter writeUnsignedByteWithId(int val);

	TraCIWriter writeByteWithId(byte val);

	TraCIWriter writeIntWithId(int val);

	TraCIWriter writeDoubleWithId(double val);

	TraCIWriter writeStringWithId(String val);

	TraCIWriter writeStringListWithId(List<String> val);

	TraCIWriter write2DPositionListWithId(Map<String, VPoint> data);

	TraCIWriter writeByte(int val);

	default TraCIWriter writeUnsignedByte(int val) {
		if (val >= 0 && val <= 255) {
			writeByte(val);
		} else {
			throw new TraCIException(
					"unsignedByte must be within (including) 0..255 but was: " + val);
		}
		return this;
	}

	TraCIWriter writeBytes(byte[] buf);

	TraCIWriter writeBytes(byte[] buf, int offset, int len);

	default TraCIWriter writeBytes(ByteBuffer buf, int offset, int len) {
		writeBytes(buf.array(), offset, len);
		return this;
	}

	default TraCIWriter writeBytes(ByteBuffer buf) {
		writeBytes(buf, 0, buf.array().length);
		return this;
	}

	default TraCIWriter writeInt(int val) {
		writeBytes(ByteBuffer.allocate(4).putInt(val).array());
		return this;
	}

	default TraCIWriter writeDouble(double val) {
		writeBytes(ByteBuffer.allocate(8).putDouble(val).array());
		return this;
	}

	TraCIWriter writeString(String val);

	TraCIWriter writeStringList(List<String> val);

	TraCIWriter write2DPosition(VPoint val);

	TraCIWriter write2DPositionList(Map<String, VPoint> data);

	TraCIWriter write3DPosition(Vector3D val);

	TraCIWriter writeRoadMapPosition(RoadMapPosition val);

	TraCIWriter writeLonLatPosition(VPoint lonLat);

	TraCIWriter writeLonLatAltPosition(Vector3D lonLatAlt);

	TraCIWriter writePolygon(VPoint... points);

	TraCIWriter writePolygon(List<VPoint> points);

	TraCIWriter writeTrafficLightPhaseList(List<TrafficLightPhase> phases);

	TraCIWriter writeColor(Color color);

	TraCIWriter writeCompoundObject(CompoundObject compoundObject);

	TraCIWriter writeNull();

	TraCIWriter writeCommandLength(int cmdLen);

	int stringByteCount(String str);

	int size();

	int getStringByteCount(String val);

}
