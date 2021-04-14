package org.vadere.simulator.control.simulation;

import org.vadere.simulator.control.scenarioelements.AbsorbingAreaController;
import org.vadere.simulator.control.scenarioelements.SourceController;
import org.vadere.simulator.control.scenarioelements.TargetChangerController;
import org.vadere.simulator.control.scenarioelements.TargetController;
import org.vadere.simulator.control.scenarioelements.TeleporterController;
import org.vadere.simulator.control.scenarioelements.TopographyController;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;

import java.util.Collection;

public interface ControllerProvider {

	Collection<SourceController> getSourceControllers();
	Collection<TargetController> getTargetControllers();
	Collection<TargetChangerController> getTargetChangerControllers();
	Collection<AbsorbingAreaController> getAbsorbingAreaControllers();
	TeleporterController getTeleporterController();
	TopographyController getTopographyController();
	ProcessorManager getProcessorManager();
}
