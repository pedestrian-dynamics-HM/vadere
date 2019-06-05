package org.vadere.manager.stsc;

import org.vadere.manager.stsc.sumo.TrafficLightPhase;
import org.vadere.util.geometry.shapes.VPoint;

import java.awt.*;
import java.util.List;

public interface TraCIWriter extends ByteWriter {


	void writeUnsignedByteWithId(int val);
	void writeByteWithId(byte val);
	void writeIntWithId(int val);
	void writeDoubleWithId(double val);
	void writeStringWithId(String val);
	void writeStringListWithId(List<String> val);


	void writeString(String val);

	void writeStringList(List<String> val);

	void write2DPosition(double x, double y);

	void write3DPosition(double x, double y, double z);

	void writeRoadMapPosition(String roadId, double pos, int laneId);

	void writeLonLatPosition(double lon, double lat);

	void writeLonLatAltPosition(double lon, double lat, double alt);

	void writePolygon(VPoint... points);

	void writePolygon(List<VPoint> points);

	void writeTrafficLightPhaseList(List<TrafficLightPhase> phases);

	void writeColor(Color color);

	void writeCommandLength(int cmdLen);

	int stringByteCount(String str);
}
