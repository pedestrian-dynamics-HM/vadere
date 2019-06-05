package org.vadere.manager.stsc;

import org.vadere.manager.stsc.sumo.RoadMapPosition;
import org.vadere.manager.stsc.sumo.TrafficLightPhase;
import org.vadere.util.geometry.Vector3D;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VPolygon;

import java.awt.*;
import java.util.List;

public interface TraCIReader extends ByteReader {


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
}
