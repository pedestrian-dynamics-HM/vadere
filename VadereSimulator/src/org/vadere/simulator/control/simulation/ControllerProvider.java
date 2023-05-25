package org.vadere.simulator.control.simulation;

import java.util.Collection;
import org.vadere.simulator.control.scenarioelements.*;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;

public interface ControllerProvider {

  Collection<SourceController> getSourceControllers();

  Collection<TargetController> getTargetControllers();

  Collection<TargetChangerController> getTargetChangerControllers();

  Collection<AbsorbingAreaController> getAbsorbingAreaControllers();

  TeleporterController getTeleporterController();

  TopographyController getTopographyController();

  ProcessorManager getProcessorManager();
}
