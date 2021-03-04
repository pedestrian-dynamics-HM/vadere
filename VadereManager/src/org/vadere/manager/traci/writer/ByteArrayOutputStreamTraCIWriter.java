package org.vadere.manager.traci.writer;

import org.apache.commons.lang3.tuple.Pair;
import org.vadere.state.traci.TraCIException;
import org.vadere.state.traci.TraCIDataType;
import org.vadere.state.traci.CompoundObject;
import org.vadere.manager.traci.sumo.RoadMapPosition;
import org.vadere.manager.traci.sumo.TrafficLightPhase;
import org.vadere.util.geometry.Vector3D;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.logging.Logger;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ByteArrayOutputStreamTraCIWriter implements TraCIWriter {

	private static Logger logger = Logger.getLogger(ByteArrayOutputStreamTraCIWriter.class);

	ByteArrayOutputStream data;


	public ByteArrayOutputStreamTraCIWriter() {
		data = new ByteArrayOutputStream();
	}

	@Override
	public ByteBuffer asByteBuffer() {
		return ByteBuffer.wrap(data.toByteArray());
	}

	@Override
	public byte[] asByteArray() {
		return data.toByteArray();
	}

	@Override
	public TraCIWriter rest() {
		data.reset();
		return this;
	}

	@Override
	public TraCIWriter writeObjectWithId(TraCIDataType dataType, Object data) {

		switch (dataType) {
			case U_BYTE:
				writeUnsignedByteWithId((int) data);
				break;
			case BYTE:
				writeByteWithId((byte) data);
				break;
			case INTEGER:
				writeIntWithId((int) data);
				break;
			case DOUBLE:
				writeDoubleWithId((double) data);
				break;
			case STRING:
				writeStringWithId((String) data);
				break;
			case STRING_LIST:
				writeStringListWithId((List<String>) data);
				break;
			case POS_2D:
				write2DPosition((VPoint) data);
				break;
			case POS_2D_LIST:
				write2DPositionListWithId((Map<String, VPoint>) data);
				break;
			case POS_3D:
				write3DPosition((Vector3D) data);
				break;
			case POS_ROAD_MAP:
				writeRoadMapPosition((RoadMapPosition) data);
				break;
			case POS_LON_LAT:
				writeLonLatPosition((VPoint) data);
				break;
			case POS_LON_LAT_ALT:
				writeLonLatAltPosition((Vector3D) data);
				break;
			case POLYGON:
				writePolygon((List<VPoint>) data);
				break;
			case TRAFFIC_LIGHT_PHASE_LIST:
				writeTrafficLightPhaseList((List<TrafficLightPhase>) data);
				break;
			case COLOR:
				writeColor((Color) data);
				break;
			case COMPOUND_OBJECT:
				writeCompoundObject((CompoundObject) data);
			case NULL:
				writeNull();
				break;
			default:
				logger.errorf("cannot write %s", dataType.toString());
		}

		return this;
	}

	@Override
	public TraCIWriter writeByte(int val) {
		data.write(val);
		return this;
	}

	@Override
	public TraCIWriter writeBytes(byte[] buf) {
		data.writeBytes(buf);
		return this;
	}

	@Override
	public TraCIWriter writeBytes(byte[] buf, int offset, int len) {
		data.write(buf, offset, len);
		return this;
	}


	@Override
	public TraCIWriter writeUnsignedByteWithId(int val) {
		writeUnsignedByte(TraCIDataType.U_BYTE.id);
		writeUnsignedByte(val);
		return this;
	}

	@Override
	public TraCIWriter writeByteWithId(byte val) {
		writeUnsignedByte(TraCIDataType.BYTE.id);
		writeByte(val);
		return this;
	}

	@Override
	public TraCIWriter writeIntWithId(int val) {
		writeUnsignedByte(TraCIDataType.INTEGER.id);
		writeInt(val);
		return this;
	}

	@Override
	public TraCIWriter writeDoubleWithId(double val) {
		writeUnsignedByte(TraCIDataType.DOUBLE.id);
		writeDouble(val);
		return this;
	}

	@Override
	public TraCIWriter writeStringWithId(String val) {
		writeUnsignedByte(TraCIDataType.STRING.id);
		writeString(val);
		return this;
	}

	@Override
	public TraCIWriter writeStringListWithId(List<String> val) {
		writeUnsignedByte(TraCIDataType.STRING_LIST.id);
		writeStringList(val);
		return this;
	}

	@Override
	public TraCIWriter writeString(String val) {
		writeString(val, StandardCharsets.US_ASCII);
		return this;
	}

	@Override
	public TraCIWriter writeStringList(List<String> val) {
		writeInt(val.size());
		val.forEach(this::writeString);
		return this;
	}

	@Override
	public int getStringByteCount(String val) {
		return val.getBytes(StandardCharsets.US_ASCII).length;
	}

	private TraCIWriter writeString(String val, Charset c) {
		byte[] byteString = val.getBytes(c);
		writeInt(byteString.length);
		if (byteString.length > 0)
			writeBytes(byteString);
		return this;
	}

	@Override
	public TraCIWriter write2DPosition(VPoint val) {
		writeUnsignedByte(TraCIDataType.POS_2D.id);
		writeDouble(val.x);
		writeDouble(val.y);
		return this;
	}

	@Override
	public TraCIWriter write2DPositionListWithId(Map<String, VPoint> data) {
		writeUnsignedByte(TraCIDataType.POS_2D_LIST.id);
		write2DPositionList(data);
		return this;
	}

	@Override
	public TraCIWriter write2DPositionList(Map<String, VPoint> data) {
		writeInt(data.entrySet().size());
		data.entrySet().stream().forEach(p -> {
			writeString(p.getKey());
			VPoint position = p.getValue();
			writeDouble(position.x);
			writeDouble(position.y);
		});
		return this;
	}

	@Override
	public TraCIWriter write3DPosition(Vector3D val) {
		writeUnsignedByte(TraCIDataType.POS_3D.id);
		writeDouble(val.x);
		writeDouble(val.y);
		writeDouble(val.z);
		return this;
	}

	@Override
	public TraCIWriter writeRoadMapPosition(RoadMapPosition val) {
		writeUnsignedByte(TraCIDataType.POS_ROAD_MAP.id);
		writeString(val.getRoadId());
		writeDouble(val.getPos());
		writeUnsignedByte(val.getLaneId());
		return this;
	}

	@Override
	public TraCIWriter writeLonLatPosition(VPoint lonLat) {
		writeUnsignedByte(TraCIDataType.POS_LON_LAT.id);
		writeDouble(lonLat.x);
		writeDouble(lonLat.y);
		return this;
	}

	@Override
	public TraCIWriter writeLonLatAltPosition(Vector3D lonLatAlt) {
		writeUnsignedByte(TraCIDataType.POS_LON_LAT_ALT.id);
		writeDouble(lonLatAlt.x);
		writeDouble(lonLatAlt.y);
		writeDouble(lonLatAlt.z);
		return this;
	}

	@Override
	public TraCIWriter writePolygon(VPoint... points) {
		writePolygon(Arrays.asList(points));
		return this;
	}

	@Override
	public TraCIWriter writePolygon(List<VPoint> points) {
		writeUnsignedByte(TraCIDataType.POLYGON.id);
		if (points.size() > 255)
			throw new TraCIException("Polygon to big. " +
					"TraCI only supports polygon up to 255 points.");
		writeUnsignedByte(points.size());
		points.forEach(p -> {
			writeDouble(p.getX());
			writeDouble(p.getY());
		});
		return this;
	}

	@Override
	public TraCIWriter writeTrafficLightPhaseList(List<TrafficLightPhase> phases) {
		writeUnsignedByte(TraCIDataType.TRAFFIC_LIGHT_PHASE_LIST.id);
		if (phases.size() > 255)
			throw new TraCIException("Traffic Light Phase List to big. " +
					"TraCI only supports list up to 255 elements.");
		writeUnsignedByte(phases.size());
		phases.forEach(phase -> {
			writeString(phase.getPrecRoad());
			writeString(phase.getSuccRoad());
			writeUnsignedByte(phase.getPhase().id);
		});
		return this;
	}

	@Override
	public TraCIWriter writeColor(Color color) {
		writeUnsignedByte(TraCIDataType.COLOR.id);
		writeUnsignedByte(color.getRed());
		writeUnsignedByte(color.getGreen());
		writeUnsignedByte(color.getBlue());
		writeUnsignedByte(color.getAlpha());
		return this;
	}

	@Override
	public TraCIWriter writeCompoundObject(CompoundObject compoundObject) {
		writeUnsignedByte(TraCIDataType.COMPOUND_OBJECT.id);
		writeInt(compoundObject.size());
		Iterator<Pair<TraCIDataType, Object>> iter = compoundObject.itemIterator();
		while (iter.hasNext()) {
			Pair<TraCIDataType, Object> p = iter.next();
			if (p.getLeft().equals(TraCIDataType.COMPOUND_OBJECT))
				throw new TraCIException("Recursive CompoundObject are not allowed.");
			writeObjectWithId(p.getLeft(), p.getRight());
		}
		return this;
	}

	@Override
	public TraCIWriter writeNull() {
		writeUnsignedByte(TraCIDataType.NULL.id);
		return this;
	}

	@Override
	public int stringByteCount(String str) {
		return str.getBytes(StandardCharsets.US_ASCII).length;
	}

	/**
	 * Check if the given cmdLen fits into a single byte. If not use the extended cmdLen format
	 * which nulls the first byte and introduces a int field for the cmdLen.
	 *
	 * @param cmdLen number of bytes of command *including* one byte for the cmdLen field.
	 */
	@Override
	public TraCIWriter writeCommandLength(int cmdLen) {

		if (cmdLen <= 255) { //
			writeUnsignedByte(cmdLen);
		} else {
			// use extended cmdLen field (+4 byte)
			cmdLen += 4;
			writeUnsignedByte(0); // first byte must be null
			writeInt(cmdLen); // write cmdLen as integer
		}
		return this;
	}

	@Override
	public int size() {
		return data.size();
	}
}
