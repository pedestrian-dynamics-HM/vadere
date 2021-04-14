package org.vadere.manager.traci;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.vadere.manager.traci.reader.TraCIByteBuffer;
import org.vadere.manager.traci.sumo.LightPhase;
import org.vadere.manager.traci.sumo.RoadMapPosition;
import org.vadere.manager.traci.sumo.TrafficLightPhase;
import org.vadere.manager.traci.writer.ByteArrayOutputStreamTraCIWriter;
import org.vadere.state.traci.TraCIDataType;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.Vector3D;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VPolygon;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

public class TraCIReaderTest {

	ByteArrayOutputStreamTraCIWriter writer;
	TraCIByteBuffer reader;

	@Before
	public void before(){
		writer = new ByteArrayOutputStreamTraCIWriter();
	}

	@After
	public void after(){
		writer.rest();
	}

	private void createReader(){
		reader = TraCIByteBuffer.wrap(writer.asByteArray());
	}

	private void checkEmpty(){
		assertThat("TraCIByteBuffer must be empty at this point", reader.hasRemaining(), equalTo(false));
	}

	private void checkIdentifier( int matchWith){
		int identifier = reader.readUnsignedByte();
		assertThat("Wrong Identifer", identifier, equalTo(matchWith));
	}

	@Test
	public void readByte() {
		writer.writeByte(33);
		createReader();

		assertThat(reader.readByte(), equalTo((byte)33));

		// buf must be empty
		checkEmpty();
	}

	@Test
	public void readBytes() {
		byte[] data = new byte[]{22, 33, 44};
		writer.writeBytes(data);
		createReader();

		assertThat(reader.readBytes(3), equalTo(data));

		// buf must be empty
		checkEmpty();
	}

	@Test
	public void readInt() {
		writer.writeInt(3);
		writer.writeInt(99);
		createReader();

		assertThat(reader.readInt(), equalTo(3));
		assertThat(reader.readInt(), equalTo(99));

		// buf must be empty
		checkEmpty();
	}

	@Test
	public void readDouble() {
		writer.writeDouble(3.1415);
		createReader();

		assertThat(reader.readDouble(), equalTo(3.1415));

		// buf must be empty
		checkEmpty();
	}

	@Test
	public void readString() {
		writer.writeString("Hello World from Vadere");
		createReader();

		assertThat(reader.readString(), equalTo("Hello World from Vadere"));

		// buf must be empty
		checkEmpty();
	}

	@Test
	public void readStringList() {
		List<String> strList = new ArrayList<>();
		strList.add("Hello World!");
		strList.add("Goodbye World.");
		writer.writeStringList(strList);
		createReader();

		List<String> strListOut = reader.readStringList();
		assertThat(strListOut, equalTo(strList));

		// buf must be empty
		checkEmpty();
	}

	@Test
	public void read2DPosition() {
		writer.write2DPosition(new VPoint(22.3, 4.0));
		createReader();

		checkIdentifier(TraCIDataType.POS_2D.id);
		VPoint p = reader.read2DPosition();
		assertThat(p.x, equalTo(22.3));
		assertThat(p.y, equalTo(4.0));

		// buf must be empty
		checkEmpty();
	}

	@Test
	public void read3DPosition() {
		writer.write3DPosition(new Vector3D(11.1, 22.2, 33.3));
		createReader();

		checkIdentifier(TraCIDataType.POS_3D.id);
		Vector3D vec = reader.read3DPosition();
		assertThat(vec.x, equalTo(11.1));
		assertThat(vec.y, equalTo(22.2));
		assertThat(vec.z, equalTo(33.3));

		// buf must be empty
		checkEmpty();
	}

	@Test
	public void readRoadMapPosition() {
		writer.writeRoadMapPosition(new RoadMapPosition("road_001", 12.5, 0));
		createReader();

		checkIdentifier(TraCIDataType.POS_ROAD_MAP.id);
		RoadMapPosition roadMapPosition = reader.readRoadMapPosition();
		assertThat(roadMapPosition.getRoadId(), equalTo("road_001"));
		assertThat(roadMapPosition.getPos(), equalTo(12.5));
		assertThat(roadMapPosition.getLaneId(), equalTo(0));

		// buf must be empty
		checkEmpty();
	}

	@Test
	public void readLonLatPosition() {
		writer.writeLonLatPosition(new VPoint(23.3, 11.9));
		createReader();

		checkIdentifier(TraCIDataType.POS_LON_LAT.id);
		VPoint lonLat = reader.readLonLatPosition();
		assertThat(lonLat.x, equalTo(23.3));
		assertThat(lonLat.y, equalTo(11.9));

		// buf must be empty
		checkEmpty();
	}

	@Test
	public void readLonLatAltPosition() {
		writer.writeLonLatAltPosition(new Vector3D(34.5, 34.0, 11.3436));
		createReader();

		checkIdentifier(TraCIDataType.POS_LON_LAT_ALT.id);
		Vector3D lonlatalt = reader.readLonLatAltPosition();
		assertThat(lonlatalt.x, equalTo(34.5));
		assertThat(lonlatalt.y, equalTo(34.0));
		assertThat(lonlatalt.z, equalTo(11.3436));

		// buf must be empty
		checkEmpty();
	}

	@Test
	public void readPolygon() {
		VPoint[] points = new VPoint[]{new VPoint(4.4, 2.4), new VPoint(11.3, 34.0)};
		writer.writePolygon(points);
		createReader();

		VPolygon match = GeometryUtils.polygonFromPoints2D(points);
		checkIdentifier(TraCIDataType.POLYGON.id);
		VPolygon actual = reader.readPolygon();
		assertThat(actual, equalTo(match));

		// buf must be empty
		checkEmpty();
	}

	@Test
	public void readTrafficLightPhaseList() {
		List<TrafficLightPhase>  phases = new ArrayList<>();
		phases.add(new TrafficLightPhase("r001", "r002", LightPhase.OFF_BLINK));
		phases.add(new TrafficLightPhase("r004", "r099", LightPhase.RED));
		writer.writeTrafficLightPhaseList(phases);
		createReader();

		checkIdentifier(TraCIDataType.TRAFFIC_LIGHT_PHASE_LIST.id);
		List<TrafficLightPhase> actualPhases = reader.readTrafficLightPhaseList();
		assertThat(actualPhases, equalTo(phases));

		// buf must be empty
		checkEmpty();
	}

	@Test
	public void readColor() {
		Color color = new Color(3,4,5,9);
		writer.writeColor(color);
		createReader();

		checkIdentifier(TraCIDataType.COLOR.id);
		assertThat(reader.readColor(), equalTo(color));

		// buf must be empty
		checkEmpty();
	}
}