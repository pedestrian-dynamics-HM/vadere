package org.vadere.manager.traci.reader;

import org.vadere.state.traci.TraCIException;
import org.vadere.state.traci.TraCIDataType;
import org.vadere.state.traci.CompoundObject;
import org.vadere.manager.traci.sumo.LightPhase;
import org.vadere.manager.traci.sumo.RoadMapPosition;
import org.vadere.manager.traci.sumo.TrafficLightPhase;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.Vector3D;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VPolygon;

import java.awt.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A {@link ByteBuffer} based implementation of the {@link TraCIReader} interface. The {@link
 * ByteBuffer} wraps a byte[] array and allows getX access to the given byte[] array.
 */
public class TraCIByteBuffer implements TraCIReader {

	private ByteBuffer byteBuffer;

	protected TraCIByteBuffer() {
	}

	protected TraCIByteBuffer(byte[] buffer) {
		byteBuffer = ByteBuffer.wrap(buffer);
	}

	protected TraCIByteBuffer(ByteBuffer buffer) {
		byteBuffer = buffer;
	}

	public static TraCIByteBuffer wrap(byte[] data) {
		TraCIByteBuffer traCIByteBuffer = new TraCIByteBuffer();
		traCIByteBuffer.byteBuffer = ByteBuffer.wrap(data);
		return traCIByteBuffer;
	}

	public static TraCIByteBuffer wrap(ByteBuffer buffer) {
		TraCIByteBuffer traCIByteBuffer = new TraCIByteBuffer();
		traCIByteBuffer.byteBuffer = buffer;
		return traCIByteBuffer;
	}

	@Override
	public byte readByte() {
		return byteBuffer.get();
	}

	@Override
	public byte[] readBytes(int num) {
		ensureBytes(num);
		byte[] data = new byte[num];
		byteBuffer.get(data, 0, data.length);
		return data;
	}

	@Override
	public void readBytes(byte[] data) {
		byteBuffer.get(data, 0, data.length);
	}

	@Override
	public int readInt() {
		return byteBuffer.getInt();
	}

	@Override
	public double readDouble() {
		return byteBuffer.getDouble();
	}

	@Override
	public String readString(int numOfBytes) {
		byte[] data = readBytes(numOfBytes);

		return new String(data, StandardCharsets.US_ASCII);
	}

	@Override
	public String readString() {
		ensureBytes(4);
		int len = byteBuffer.getInt();

		if (len == 0)
			return "";

		ensureBytes(len);
		byte[] stringBytes = new byte[len];
		byteBuffer.get(stringBytes, 0, stringBytes.length);

		return new String(stringBytes, StandardCharsets.US_ASCII);
	}

	@Override
	public List<String> readStringList() {
		ensureBytes(4); // 1x int
		int numOfStrings = byteBuffer.getInt();

		ArrayList<String> stringList = new ArrayList<>();
		for (int i = 0; i < numOfStrings; i++) {
			stringList.add(readString());
		}

		return stringList;
	}

	@Override
	public VPoint read2DPosition() {
		// id already consumed
		ensureBytes(16); // 2x double
		double x = byteBuffer.getDouble();
		double y = byteBuffer.getDouble();
		return new VPoint(x, y);
	}

	@Override
	public Map<String, VPoint> read2DPositionList() {
		ensureBytes(4); // 1x int
		int numOfKeyValuePairs = byteBuffer.getInt();

		Map<String, VPoint> map = new HashMap<>();
		for (int i = 0; i < numOfKeyValuePairs; i++) {
			String id = readString();
			VPoint position = read2DPosition();
			map.put(id, position);
		}

		return map;
	}

	@Override
	public Vector3D read3DPosition() {
		// id already consumed
		ensureBytes(24); // 3x double
		double x = byteBuffer.getDouble();
		double y = byteBuffer.getDouble();
		double z = byteBuffer.getDouble();
		return new Vector3D(x, y, z);
	}

	@Override
	public RoadMapPosition readRoadMapPosition() {
		// id already consumed
		String roadId = readString();
		ensureBytes(9); // double + ubyte
		double pos = readDouble();
		int laneId = readUnsignedByte();
		return new RoadMapPosition(roadId, pos, laneId);
	}

	@Override
	public VPoint readLonLatPosition() {
		// id already consumed
		ensureBytes(16); // 2x double
		double lon = byteBuffer.getDouble();
		double lat = byteBuffer.getDouble();
		return new VPoint(lon, lat);
	}

	@Override
	public Vector3D readLonLatAltPosition() {
		// id already consumed
		ensureBytes(24); // 3x double
		double lon = byteBuffer.getDouble();
		double lat = byteBuffer.getDouble();
		double alt = byteBuffer.getDouble();
		return new Vector3D(lon, lat, alt);
	}

	@Override
	public VPolygon readPolygon() {
		// id already consumed
		ensureBytes(1); // ubyte
		int numberOfPoints = readUnsignedByte();

		ensureBytes(numberOfPoints * 16); // 2x double values for each numberOfPoints
		IPoint[] points = new IPoint[numberOfPoints];
		for (int i = 0; i < points.length; i++) {
			double x = byteBuffer.getDouble();
			double y = byteBuffer.getDouble();
			points[i] = new VPoint(x, y);
		}
		return GeometryUtils.polygonFromPoints2D(points);
	}

	@Override
	public List<TrafficLightPhase> readTrafficLightPhaseList() {
		// id already consumed
		ensureBytes(1); // 1x ubyte
		int numberOfPhases = readUnsignedByte();

		ArrayList<TrafficLightPhase> phases = new ArrayList<>();
		for (int i = 0; i < numberOfPhases; i++) {
			String precRoad = readString();
			String succRoad = readString();
			ensureBytes(1); // 1x ubyte
			int lightPhaseId = readUnsignedByte();
			phases.add(new TrafficLightPhase(precRoad, succRoad, LightPhase.fromId(lightPhaseId)));
		}

		return phases;
	}

	@Override
	public Color readColor() {
		// id already consumed
		ensureBytes(4); // 4x ubyte (RGBA)

		int r = readUnsignedByte();
		int g = readUnsignedByte();
		int b = readUnsignedByte();
		int a = readUnsignedByte();

		return new Color(r, g, b, a);
	}

	@Override
	public CompoundObject readCompoundObject() {
		ensureBytes(4);
		int noElements = readInt();

		CompoundObject compoundObject = new CompoundObject(noElements);

		for (int i = 0; i < noElements; i++) {
			TraCIDataType type = TraCIDataType.fromId(readUnsignedByte());
			if (type.equals(TraCIDataType.COMPOUND_OBJECT))
				throw new TraCIException("Recursive CompoundObject are not allowed.");
			compoundObject.add(type, readTypeValue(type));
		}

		return compoundObject;
	}

	@Override
	public Object readTypeValue(TraCIDataType type) {

		switch (type) {
			case U_BYTE:
				return readUnsignedByte();
			case BYTE:
				return readByte();
			case INTEGER:
				return readInt();
			case DOUBLE:
				return readDouble();
			case STRING:
				return readString();
			case STRING_LIST:
				return readStringList();
			case POS_2D:
				return read2DPosition();
			case POS_2D_LIST:
				return read2DPositionList();
			case POS_3D:
				return read3DPosition();
			case POS_ROAD_MAP:
				return readRoadMapPosition();
			case POS_LON_LAT:
				return readLonLatPosition();
			case POS_LON_LAT_ALT:
				return readLonLatAltPosition();
			case POLYGON:
				return readPolygon();
			case TRAFFIC_LIGHT_PHASE_LIST:
				return readTrafficLightPhaseList();
			case COLOR:
				return readColor();
			case COMPOUND_OBJECT:
				return readCompoundObject();
			case NULL:
				return null;
			default:
				throw new TraCIException("Unknown Datatype: " + type.toString());
		}
	}

	@Override
	public boolean hasRemaining() {
		return byteBuffer.hasRemaining();
	}

	@Override
	public void ensureBytes(int num) {
		int bytesLeft = byteBuffer.limit() - byteBuffer.position();
		if (bytesLeft < num)
			throw new TraCIException("Not enough bytes left." + "Expected " + num + "Bytes but only " + bytesLeft + " found.");
	}

	@Override
	public int limit() {
		return byteBuffer.limit();
	}

	@Override
	public int position() {
		return byteBuffer.position();
	}


}
