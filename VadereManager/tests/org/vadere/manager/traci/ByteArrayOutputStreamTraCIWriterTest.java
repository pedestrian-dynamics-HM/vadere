package org.vadere.manager.traci;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.vadere.state.traci.TraCIException;
import org.vadere.manager.traci.sumo.LightPhase;
import org.vadere.manager.traci.sumo.RoadMapPosition;
import org.vadere.manager.traci.sumo.TrafficLightPhase;
import org.vadere.manager.traci.writer.ByteArrayOutputStreamTraCIWriter;
import org.vadere.state.traci.TraCIDataType;
import org.vadere.util.geometry.Vector3D;
import org.vadere.util.geometry.shapes.VPoint;

import java.awt.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.stream.IntStream;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

public class ByteArrayOutputStreamTraCIWriterTest {

	ByteArrayOutputStreamTraCIWriter writer;

	@Before
	public void before(){
		writer = new ByteArrayOutputStreamTraCIWriter();
	}

	@After
	public void after(){
		writer.rest();
	}

	@Test
	public void getAsByteBuffer() {
		ByteBuffer buf = writer.asByteBuffer();
		assertThat(buf.limit(), equalTo(0));
	}

	@Test
	public void getAsByteArray() {
		byte[] buf = writer.asByteArray();
		assertThat(buf.length, equalTo(0));

	}

	@Test
	public void writeByte() {
		writer.writeByte(33);
		writer.writeByte(-33);
		writer.writeByte(0);

		byte[] buf = writer.asByteArray();

		assertThat(buf.length, equalTo(3));
		assertThat(buf[0], equalTo((byte)33));
		assertThat(buf[1], equalTo((byte)-33));
		assertThat(buf[2], equalTo((byte)0));
	}


	@Test
	public void writeUnsignedByte() {
		writer.writeUnsignedByte(33);
		writer.writeUnsignedByte(200);
		writer.writeUnsignedByte(0);

		byte[] buf = writer.asByteArray();

		assertThat(buf.length, equalTo(3));
		assertThat((int)buf[0] & 0xff, equalTo(33));
		assertThat((int)buf[1] & 0xff, equalTo(200));
		assertThat((int)buf[2] & 0xff, equalTo(0));
	}

	@Test(expected = TraCIException.class)
	public void writeUnsignedByte1() {
		writer.writeUnsignedByte(999);
	}

	@Test
	public void writeBytes() {
		byte[] dataIn = new byte[]{18,19, 20};

		writer.writeBytes(dataIn);

		byte[] dataOut = writer.asByteArray();
		assertThat(dataOut, equalTo(dataIn));
	}

	@Test
	public void writeBytes1() {
		byte[] dataIn = new byte[]{18, 19, 20, 21, 22};

		writer.writeBytes(dataIn, 1, 2);

		byte[] dataOut = writer.asByteArray();
		assertThat(dataOut, equalTo(new byte[]{19, 20} ));

	}

	@Test
	public void writeEmptyString(){
		writer.writeString("");


		ByteBuffer buf= writer.asByteBuffer();

		assertThat(buf.capacity(), equalTo(4));
		assertThat(buf.getInt(), equalTo(0));

		// buf must be empty
		checkEmpty(buf);
	}

	@Test
	public void writeString() {
		String str = "Hello  World !";
		int strLen = str.getBytes(StandardCharsets.US_ASCII).length;


		writer.writeString(str);
		ByteBuffer buf = writer.asByteBuffer();

		// preceding int value holding length of string
		assertThat(buf.capacity(), equalTo(4 + strLen));

		checkString(buf,  str);

		// buf must be empty
		checkEmpty(buf);
	}

	@Test
	public void writeStringList() {
		String s1 = "Hello World.";
		int strLen1 = s1.getBytes(StandardCharsets.US_ASCII).length;
		String s2 = "Hello TraCI from Vadere.";
		int strLen2 = s2.getBytes(StandardCharsets.US_ASCII).length;

		ArrayList<String> stringList = new ArrayList<>();
		stringList.add(s1);
		stringList.add(s2);

		writer.writeStringList(stringList);

		ByteBuffer buf = writer.asByteBuffer();

		assertThat(buf.capacity(), equalTo(4 + 4 + strLen1 + 4 + strLen2));
		// number of strings
		assertThat(buf.getInt(), equalTo(2));

		// check first string
		checkString(buf, s1);

		// check second string
		checkString(buf, s2);

		// must be empty
		checkEmpty(buf);
	}

	@Test
	public void write2DPosition() {
		writer.write2DPosition(new VPoint(23.456,3.3));
		ByteBuffer buf = writer.asByteBuffer();

		// id (ubyte)
		checkIdentifier(buf.get(), TraCIDataType.POS_2D.id);

		// check x, y
		assertThat(buf.getDouble(), equalTo(23.456));
		assertThat(buf.getDouble(), equalTo(3.3));

		// buf must be empty
		checkEmpty(buf);
	}

	@Test
	public void write3DPosition() {
		writer.write3DPosition(new Vector3D(3.34, 12.33, 56.8889));
		ByteBuffer buf = writer.asByteBuffer();

		// id (ubyte)
		checkIdentifier(buf.get(), TraCIDataType.POS_3D.id);

		// check x, y, z
		assertThat(buf.getDouble(), equalTo(3.34));
		assertThat(buf.getDouble(), equalTo(12.33));
		assertThat(buf.getDouble(), equalTo(56.8889));

		// buf must be empty
		checkEmpty(buf);
	}

	@Test
	public void writeRoadMapPosition() {

		writer.writeRoadMapPosition(new RoadMapPosition("r001", 12.4, 3));
		int roadIdLen = "r001".getBytes(StandardCharsets.US_ASCII).length;
		ByteBuffer buf = writer.asByteBuffer();

		// id (ubyte)
		checkIdentifier(buf.get(), TraCIDataType.POS_ROAD_MAP.id);

		// check roadId
		checkString(buf, "r001");

		// check pos
		assertThat(buf.getDouble(), equalTo(12.4));

		// check laneId (ubyte)
		assertThat(buf.get() & 0xff, equalTo(3));

		// buf must be empty
		checkEmpty(buf);
	}

	@Test
	public void writeLonLatPosition() {
		writer.writeLonLatPosition(new VPoint(49.3345, 10.10453));
		ByteBuffer buf = writer.asByteBuffer();

		// id (ubyte)
		checkIdentifier(buf.get(), TraCIDataType.POS_LON_LAT.id);

		// check lon, lat
		assertThat(buf.getDouble(), equalTo(49.3345));
		assertThat(buf.getDouble(), equalTo(10.10453));

		// buf must be empty
		checkEmpty(buf);
	}

	@Test
	public void writeLonLatAltPosition() {
		writer.writeLonLatAltPosition(new Vector3D(49.33, 15.223, 12.33));
		ByteBuffer buf = writer.asByteBuffer();

		// id (ubyte)
		checkIdentifier(buf.get(), TraCIDataType.POS_LON_LAT_ALT.id);

		// check lon, lat, alt
		assertThat(buf.getDouble(), equalTo(49.33));
		assertThat(buf.getDouble(), equalTo(15.223));
		assertThat(buf.getDouble(), equalTo(12.33));

		// buf must be empty
		checkEmpty(buf);
	}

	@Test(expected = TraCIException.class)
	public void writePolygonWithError(){
		ArrayList<VPoint> points = new ArrayList<>();
		IntStream.range(0, 300).forEach( i -> points.add(new VPoint(i,i)));

		writer.writePolygon(points);
	}

	@Test
	public void writePolygon() {
		ArrayList<VPoint> points = new ArrayList<>();
		points.add(new VPoint(3.3, 4.4));
		points.add(new VPoint(5.0, 10.0));
		points.add(new VPoint(10.1, 1.0));

		writer.writePolygon(points);
		ByteBuffer buf = writer.asByteBuffer();

		// id (ubyte)
		checkIdentifier(buf.get(), TraCIDataType.POLYGON.id);

		// check number of points (ubyte)
		assertThat(buf.get() & 0xff, equalTo(3));

		// check x,y for each point
		assertThat(buf.getDouble(), equalTo(3.3));
		assertThat(buf.getDouble(), equalTo(4.4));

		assertThat(buf.getDouble(), equalTo(5.0));
		assertThat(buf.getDouble(), equalTo(10.0));

		assertThat(buf.getDouble(), equalTo(10.1));
		assertThat(buf.getDouble(), equalTo(1.0));

		// buf must be empty
		checkEmpty(buf);
	}


	@Test(expected = TraCIException.class)
	public void writeTrafficLightPhaseListError(){
		ArrayList<TrafficLightPhase> phases = new ArrayList<>();
		IntStream.range(0, 256)
				.forEach( i -> phases.add(
						new TrafficLightPhase("", "",
								LightPhase.GREEN)));
		writer.writeTrafficLightPhaseList(phases);
	}

	@Test
	public void writeTrafficLightPhaseList() {
		ArrayList<TrafficLightPhase> phases = new ArrayList<>();

		phases.add(new TrafficLightPhase("road001", "road002",
				LightPhase.GREEN));
		writer.writeTrafficLightPhaseList(phases);
		ByteBuffer buf = writer.asByteBuffer();

		// id (ubyte)
		checkIdentifier(buf.get(), TraCIDataType.TRAFFIC_LIGHT_PHASE_LIST.id);

		// check number of phases
		assertThat(buf.get() & 0xff, equalTo(1));

		// check precRoad and succRoad
		checkString(buf, "road001");
		checkString(buf, "road002");

		// check phase
		assertThat(buf.get() & 0xff, equalTo(LightPhase.GREEN.id));

		// buf must be empty
		checkEmpty(buf);
	}

	@Test
	public void writeColor() {
		writer.writeColor(new Color(10, 20, 40, 50));
		ByteBuffer buf = writer.asByteBuffer();

		// id (ubyte)
		checkIdentifier(buf.get(), TraCIDataType.COLOR.id);

		// check color rgba
		assertThat(buf.get() & 0xff, equalTo(10) );
		assertThat(buf.get() & 0xff, equalTo(20) );
		assertThat(buf.get() & 0xff, equalTo(40) );
		assertThat(buf.get() & 0xff, equalTo(50) );

		// buf must be empty
		checkEmpty(buf);
	}


	private void checkString(ByteBuffer buf, String str){

		int byteLen = str.getBytes(StandardCharsets.US_ASCII).length;
		byte[] matchWith = str.getBytes(StandardCharsets.US_ASCII);

		// string length
		assertThat("String length wrong", buf.getInt(), equalTo(byteLen));

		byte[] actual = getBytes(buf, byteLen);
		assertThat("String bytes do not match", actual, equalTo(matchWith));
	}

	private byte[] getBytes(ByteBuffer buf, int len){
		byte[] bytes = new byte[len];
		buf.get(bytes, 0, bytes.length);
		return bytes;
	}


	private void checkIdentifier(byte actual, int matchWith){
		assertThat("Wrong Identifer", (int)actual & 0xff, equalTo(matchWith));
	}

	private void checkEmpty(ByteBuffer buf){
		assertThat("Buffer must be empty at this point",buf.hasRemaining(), equalTo(false));
	}
}