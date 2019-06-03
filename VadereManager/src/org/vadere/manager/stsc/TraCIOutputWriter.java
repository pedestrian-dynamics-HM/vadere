package org.vadere.manager.stsc;

import org.vadere.manager.TraCiException;
import org.vadere.manager.stsc.sumo.TrafficLightPhase;
import org.vadere.util.geometry.shapes.VPoint;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class TraCIOutputWriter implements TraCIWriter {

	ByteArrayOutputStream data;



	public TraCIOutputWriter() {
		data = new ByteArrayOutputStream();
	}

	ByteBuffer asByteBuffer(){
		return ByteBuffer.wrap(data.toByteArray());
	}

	byte[] asByteArray(){
		return data.toByteArray();
	}

	void rest(){
		data.reset();
	}

	@Override
	public void writeByte(int val) {
		data.write(val);
	}

	@Override
	public void writeBytes(byte[] buf) {
		data.writeBytes(buf);
	}

	@Override
	public void writeBytes(byte[] buf, int offset, int len) {
		data.write(buf, offset, len);
	}


	@Override
	public void writeString(String val){
		writeString(val, StandardCharsets.US_ASCII);
	}

	@Override
	public void writeStringList(List<String> val){
		writeInt(val.size());
		val.forEach(this::writeString);
	}

	private void writeString(String val, Charset c){
		byte[] byteString = val.getBytes(c);
		writeInt(byteString.length);
		if (byteString.length > 0)
			writeBytes(byteString);
	}

	@Override
	public void write2DPosition(double x, double y){
		writeUnsignedByte(TraCIDataTypes.POS_2D.identifier);
		writeDouble(x);
		writeDouble(y);
	}

	@Override
	public void write3DPosition(double x, double y, double z){
		writeUnsignedByte(TraCIDataTypes.POS_3D.identifier);
		writeDouble(x);
		writeDouble(y);
		writeDouble(z);
	}


	@Override
	public void writeRoadMapPosition(String roadId, double pos, int laneId){
		writeUnsignedByte(TraCIDataTypes.POS_ROAD_MAP.identifier);
		writeString(roadId);
		writeDouble(pos);
		writeUnsignedByte(laneId);
	}

	@Override
	public void writeLonLatPosition(double lon, double lat){
		writeUnsignedByte(TraCIDataTypes.POS_LON_LAT.identifier);
		writeDouble(lon);
		writeDouble(lat);
	}

	@Override
	public void writeLonLatAltPosition(double lon, double lat, double alt){
		writeUnsignedByte(TraCIDataTypes.POS_LON_LAT_ALT.identifier);
		writeDouble(lon);
		writeDouble(lat);
		writeDouble(alt);
	}

	@Override
	public void writePolygon(VPoint... points){
		writePolygon(Arrays.asList(points));
	}

	@Override
	public void writePolygon(List<VPoint> points){
		writeUnsignedByte(TraCIDataTypes.POLYGON.identifier);
		if(points.size() > 255)
			throw new TraCiException("Polygon to big. TraCI only supports polygon up to 255 points.");
		writeUnsignedByte(points.size());
		points.forEach(p -> {
			writeDouble(p.getX());
			writeDouble(p.getY());
		});
	}

	@Override
	public void writeTrafficLightPhaseList(List<TrafficLightPhase> phases){
		writeUnsignedByte(TraCIDataTypes.TRAFFIC_LIGHT_PHASE_LIST.identifier);
		if(phases.size() > 255)
			throw new TraCiException("Traffic Light Phase List to big. TraCI only supports list up to 255 elements.");
		writeUnsignedByte(phases.size());
		phases.forEach( phase -> {
			writeString(phase.getPrecRoad());
			writeString(phase.getSuccRoad());
			writeUnsignedByte(phase.getPhase().id);
		});
	}

	@Override
	public void writeColor(Color color){
		writeUnsignedByte(TraCIDataTypes.COLOR.identifier);
		writeUnsignedByte(color.getRed());
		writeUnsignedByte(color.getGreen());
		writeUnsignedByte(color.getBlue());
		writeUnsignedByte(color.getAlpha());
	}

	@Override
	public int stringByteCount(String str) {
		return str.getBytes(StandardCharsets.US_ASCII).length;
	}

	/**
	 * Check if the given cmdLen fits into a single byte. If not use the extended
	 * cmdLen format which nulls the first byte and introduces a int field for the
	 * cmdLen.
	 *
	 * @param cmdLen	number of bytes of command *including* one byte for the cmdLen field.
	 */
	@Override
	public void writeCommandLength(int cmdLen) {

		if (cmdLen <= 255){ //
			writeUnsignedByte(cmdLen);
		} else {
			// use extended cmdLen field (+4 byte)
			cmdLen += 4;
			writeUnsignedByte(0); // first byte must be null
			writeInt(cmdLen); // write cmdLen as integer
		}
	}

	public int size(){
		return data.size();
	}
}
