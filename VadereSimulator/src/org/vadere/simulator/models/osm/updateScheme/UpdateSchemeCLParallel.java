package org.vadere.simulator.models.osm.updateScheme;


import org.jetbrains.annotations.NotNull;
import org.vadere.simulator.models.osm.PedestrianOSM;
import org.vadere.simulator.models.osm.opencl.CLParallelOSMLocalMem;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.io.CollectionUtils;
import org.vadere.util.logging.Logger;
import org.vadere.util.opencl.OpenCLException;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Benedikt Zoennchen
 */
public class UpdateSchemeCLParallel extends UpdateSchemeParallel {

	private CLParallelOSMLocalMem clOptimalStepsModel;

	private int counter = 0;
	private Logger logger = Logger.getLogger(UpdateSchemeCLParallel.class);

	public UpdateSchemeCLParallel(@NotNull final Topography topography, @NotNull final CLParallelOSMLocalMem clOptimalStepsModel) {
		super(topography);
		this.clOptimalStepsModel = clOptimalStepsModel;
	}

	/*
	pedestrian.setTimeCredit(pedestrian.getTimeCredit() + timeStepInSec);
		pedestrian.setDurationNextStep(pedestrian.getStepSize() / pedestrian.getDesiredSpeed());

		if (pedestrian.getTimeCredit() > pedestrian.getDurationNextStep()) {
			pedestrian.updateNextPosition();
			movePedestrians.add(pedestrian);
		}
	 */

	@Override
	public void update(double timeStepInSec, double currentTimeInSec) {
		try {
			clearStrides(topography);
			movePedestrians.clear();

			List<PedestrianOSM> pedestrianOSMList = CollectionUtils.select(topography.getElements(Pedestrian.class), PedestrianOSM.class);

			if(counter == 0) {
				List<CLParallelOSMLocalMem.PedestrianOpenCL> pedestrians = new ArrayList<>();
				double maxStepSize = -1.0;
				for(int i = 0; i < pedestrianOSMList.size(); i++) {
					PedestrianOSM pedestrianOSM = pedestrianOSMList.get(i);
					CLParallelOSMLocalMem.PedestrianOpenCL pedestrian = new CLParallelOSMLocalMem.PedestrianOpenCL(
							pedestrianOSM.getPosition(),
							(float)pedestrianOSM.getDesiredStepSize(),
							(float)pedestrianOSM.getDesiredSpeed());
					pedestrians.add(pedestrian);
					maxStepSize = Math.max(maxStepSize, pedestrianOSM.getDesiredSpeed() * timeStepInSec);
				}
				clOptimalStepsModel.setPedestrians(pedestrians);
			}

			long ms = System.currentTimeMillis();
			List<VPoint> result = clOptimalStepsModel.update();
			ms = System.currentTimeMillis() - ms;
			logger.debug("runtime for next step computation = " + ms + " [ms]");


			for(int i = 0; i < pedestrianOSMList.size(); i++) {
				//logger.info("not equals for index = " + i + ": " + pedestrianOSMList.get(i).getPosition() + " -> " + result.get(i));
				PedestrianOSM pedestrian = pedestrianOSMList.get(i);
				pedestrian.clearStrides();

				//pedestrian.setTimeCredit(pedestrian.getTimeCredit() + timeStepInSec);

				//if (pedestrian.getTimeCredit() > pedestrian.getDurationNextStep()) {
					//pedestrian.setNextPosition(result.get(i));
					movePedestrian(topography, pedestrian, pedestrian.getPosition(), result.get(i));
					//movePedestrians.add(pedestrian);
				//}
			}

			// these call methods run on the CPU
			/*CallMethod[] callMethods = {CallMethod.MOVE, CallMethod.CONFLICTS, CallMethod.STEPS};
			List<Future<?>> futures;

			for (CallMethod callMethod : callMethods) {
				futures = new LinkedList<>();
				for (final PedestrianOSM pedestrian : pedestrianOSMList) {
					Runnable worker = () -> update(pedestrian, timeStepInSec, currentTimeInSec, callMethod);
					futures.add(executorService.submit(worker));
				}
				collectFutures(futures);
			}*/

			counter++;

		} catch (OpenCLException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	@Override
	protected void updateParallelConflicts(@NotNull final PedestrianOSM pedestrian) {
		pedestrian.refreshRelevantPedestrians();
		super.updateParallelConflicts(pedestrian);
	}
}
