package org.vadere.simulator.models.osm.opencl;

import org.jetbrains.annotations.NotNull;
import org.vadere.simulator.models.osm.PedestrianOSM;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.opencl.OpenCLException;

import java.util.List;

public interface ICLOptimalStepsModel {
	void setPedestrians(@NotNull final List<PedestrianOSM> pedestrians) throws OpenCLException;
	boolean update(float timeStepInSec, float currentTimeInSec) throws OpenCLException;
	void readFromDevice();
	List<VPoint> getPositions();
	float[] getTimeCredits();
}
