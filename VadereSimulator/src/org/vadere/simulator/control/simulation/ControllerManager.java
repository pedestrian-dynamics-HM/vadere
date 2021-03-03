package org.vadere.simulator.control.simulation;

import org.vadere.simulator.control.scenarioelements.*;

import java.util.Collection;

public interface ControllerManager {

	Collection<SourceController> getSourceControllers();
	Collection<TargetController> getTargetControllers();
	Collection<TargetChangerController> getTargetChangerControllers();
	Collection<AbsorbingAreaController> getAbsorbingAreaControllers();
	Collection<AerosolCloudController> getAerosolCloudControllers();
	TeleporterController getTeleporterController();
	TopographyController getTopographyController();
}
