package org.vadere.manager.stsc;

import org.vadere.manager.TraCiException;
import org.vadere.manager.stsc.sumo.LightPhase;
import org.vadere.manager.stsc.sumo.RoadMapPosition;
import org.vadere.manager.stsc.sumo.TrafficLightPhase;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.Vector3D;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VPolygon;

import java.awt.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class TraCIReader implements ByteReader {

	private ByteBuffer buf;

	static TraCIReader wrap(byte[] data){
		TraCIReader traCIReader = new TraCIReader();
		traCIReader.buf = ByteBuffer.wrap(data);
		return traCIReader;
	}

	private TraCIReader(){ }

	@Override
	public byte readByte() {
		return buf.get();
	}

	@Override
	public byte[] readBytes(int num) {
		byte[] data = new byte[num];
		buf.get(data, 0, data.length);
		return data;
	}

	@Override
	public int readInt() {
		return buf.getInt();
	}

	@Override
	public double readDouble() {
		return buf.getDouble();
	}

	public String readString(){
		ensureBytes(4);
		int len = buf.getInt();

		ensureBytes(len);
		byte[] stringBytes = new byte[len];
		buf.get(stringBytes, 0, stringBytes.length);

		return new String(stringBytes, StandardCharsets.US_ASCII);
	}

	public List<String> readStringList(){
		ensureBytes(4); // 1x int
		int numOfStrings = buf.getInt();

		ArrayList<String> stringList = new ArrayList<>();
		for(int i=0; i < numOfStrings; i++){
			stringList.add(readString());
		}

		return stringList;
	}

	public VPoint read2DPosition(){
		// identifier already consumed
		ensureBytes(16); // 2x double
		double x = buf.getDouble();
		double y = buf.getDouble();
		return new VPoint(x,y);
	}

	public Vector3D read3DPosition(){
		// identifier already consumed
		ensureBytes(24); // 3x double
		double x = buf.getDouble();
		double y = buf.getDouble();
		double z = buf.getDouble();
		return new Vector3D(x, y, z);
	}

	public RoadMapPosition readRoadMapPosition(){
		// identifier already consumed
		String roadId = readString();
		ensureBytes(9); // double + ubyte
		double pos = readDouble();
		int laneId = readUnsignedByte();
		return new RoadMapPosition(roadId, pos, laneId);
	}

	public VPoint readLonLatPosition(){
		// identifier already consumed
		ensureBytes(16); // 2x double
		double lon = buf.getDouble();
		double lat = buf.getDouble();
		return new VPoint(lon, lat);
	}

	public Vector3D readLonLatAltPosition(){
		// identifier already consumed
		ensureBytes(24); // 3x double
		double lon = buf.getDouble();
		double lat = buf.getDouble();
		double alt = buf.getDouble();
		return new Vector3D(lon, lat, alt);
	}

	public VPolygon readPolygon(){
		// identifier already consumed
		ensureBytes(1); // ubyte
		int numberOfPoints = readUnsignedByte();

		ensureBytes(numberOfPoints * 16); // 2x double values for each numberOfPoints
		IPoint[] points = new IPoint[numberOfPoints];
		for (int i = 0; i < points.length; i++) {
			double x = buf.getDouble();
			double y = buf.getDouble();
			points[i] = new VPoint(x, y);
		}
		return  GeometryUtils.polygonFromPoints2D(points);
	}

	public List<TrafficLightPhase> readTrafficLightPhaseList(){
		// identifier already consumed
		ensureBytes(1); // 1x ubyte
		int numberOfPhases = readUnsignedByte();

		ArrayList<TrafficLightPhase> phases = new ArrayList<>();
		for (int i=0; i < numberOfPhases; i++){
			String precRoad = readString();
			String succRoad = readString();
			ensureBytes(1); // 1x ubyte
			int lightPhaseId = readUnsignedByte();
			phases.add(new TrafficLightPhase(precRoad, succRoad, LightPhase.fromId(lightPhaseId)));
		}

		return phases;
	}

	public Color readColor(){
		// identifier already consumed
		ensureBytes(4); // 4x ubyte (RGBA)

		int r = readUnsignedByte();
		int g = readUnsignedByte();
		int b = readUnsignedByte();
		int a = readUnsignedByte();

		return new Color(r, g, b, a);
	}


	public boolean hasRemaining(){
		return buf.hasRemaining();
	}

	private void ensureBytes(int num){
		int bytesLeft = buf.limit() - buf.position();
		if (bytesLeft < num)
			throw new TraCiException("Not enough bytes left." + "Expected " + num + "Bytes but only " + bytesLeft + " found.");
	}



}
