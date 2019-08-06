package org.vadere.simulator.models.osm.updateScheme;


import org.jetbrains.annotations.NotNull;
import org.vadere.simulator.models.osm.PedestrianOSM;
import org.vadere.simulator.models.osm.opencl.CLParallelOSMLocalMem;
import org.vadere.simulator.models.osm.opencl.ICLOptimalStepsModel;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.io.CollectionUtils;
import org.vadere.util.logging.Logger;
import org.vadere.util.opencl.OpenCLException;

import java.util.List;

/**
 * @author Benedikt Zoennchen
 */
public class CLUpdateSchemeParallel extends UpdateSchemeParallel {

	private ICLOptimalStepsModel clOptimalStepsModel;

	private int counter = 0;
	private Logger logger = Logger.getLogger(CLUpdateSchemeParallel.class);

	public CLUpdateSchemeParallel(@NotNull final Topography topography, @NotNull final ICLOptimalStepsModel clOptimalStepsModel) {
		super(topography);
		this.clOptimalStepsModel = clOptimalStepsModel;
	}

	/*
	pedestrian.setTimeCredit(pedestrian.getTimeCredit() + timeStepInSec);
		pedestrian.setDurationNextStep(pedestrian.getStepSize() / pedestrian.getDesiredSpeed());

		if (pedestrian.getTimeCredit() > pedestrian.getDurationNextStep()) {
			pedestrian.updateNextPosition();
			movedPedestrians.add(pedestrian);
		}
	 */

	@Override
	public void update(double timeStepInSec, double currentTimeInSec) {
		try {
			clearStrides(topography);
			movedPedestrians.clear();

			List<PedestrianOSM> pedestrianOSMList = CollectionUtils.select(topography.getElements(Pedestrian.class), PedestrianOSM.class);

			if(counter == 0) {
				clOptimalStepsModel.setPedestrians(pedestrianOSMList);
			}

			long ms = System.currentTimeMillis();
			while (clOptimalStepsModel.update((float)timeStepInSec, (float)currentTimeInSec)) {}
			clOptimalStepsModel.readFromDevice();
			List<VPoint> result = clOptimalStepsModel.getPositions();

			ms = System.currentTimeMillis() - ms;
			logger.debug("runtime for next step computation = " + ms + " [ms]");

			for(int i = 0; i < pedestrianOSMList.size(); i++) {
				PedestrianOSM pedestrian = pedestrianOSMList.get(i);
				pedestrian.clearStrides();
				movePedestrian(topography, pedestrian, pedestrian.getPosition(), result.get(i));
			}

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
