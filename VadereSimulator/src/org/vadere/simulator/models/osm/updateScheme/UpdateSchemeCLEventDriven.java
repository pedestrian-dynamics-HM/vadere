package org.vadere.simulator.models.osm.updateScheme;


import org.jetbrains.annotations.NotNull;
import org.vadere.simulator.models.osm.PedestrianOSM;
import org.vadere.simulator.models.osm.opencl.CLParallelEventDrivenOSM;
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
public class UpdateSchemeCLEventDriven extends UpdateSchemeParallel {

	private CLParallelEventDrivenOSM clOptimalStepsModel;

	private int counter = 0;
	private float[] eventTimes;
	private static Logger logger = Logger.getLogger(UpdateSchemeCLEventDriven.class);

	static {
		logger.setDebug();
	}

	public UpdateSchemeCLEventDriven(@NotNull final Topography topography, @NotNull final CLParallelEventDrivenOSM clOptimalStepsModel) {
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
				List<CLParallelEventDrivenOSM.PedestrianOpenCL> pedestrians = new ArrayList<>();
				double maxStepSize = -1.0;
				for(int i = 0; i < pedestrianOSMList.size(); i++) {
					PedestrianOSM pedestrianOSM = pedestrianOSMList.get(i);
					CLParallelEventDrivenOSM.PedestrianOpenCL pedestrian = new CLParallelEventDrivenOSM.PedestrianOpenCL(
							pedestrianOSM.getPosition(),
							(float)pedestrianOSM.getDesiredStepSize(),
							(float)pedestrianOSM.getDesiredSpeed());
					pedestrians.add(pedestrian);
					maxStepSize = Math.max(maxStepSize, pedestrianOSM.getDesiredSpeed() * timeStepInSec);
				}
				clOptimalStepsModel.setPedestrians(pedestrians);
				eventTimes = new float[pedestrianOSMList.size()];
			}


			long ms = System.currentTimeMillis();
			int count = 0;

			if(clOptimalStepsModel.getMinEventTime() < timeStepInSec + currentTimeInSec) {

				while (clOptimalStepsModel.update((float)(timeStepInSec + currentTimeInSec))) {}
				clOptimalStepsModel.readFromDevice();

				List<VPoint> result = clOptimalStepsModel.getPositions();
				eventTimes = clOptimalStepsModel.getEventTimes();

				int numberOfUpdates = clOptimalStepsModel.getCounter() - counter;
				counter = clOptimalStepsModel.getCounter();


				logger.debug("iteration (" + numberOfUpdates + ")");
				logger.debug("runtime for next step computation = " + (System.currentTimeMillis() - ms) + " [ms] for " + timeStepInSec + "[s]");

				for(int i = 0; i < pedestrianOSMList.size(); i++) {
					PedestrianOSM pedestrian = pedestrianOSMList.get(i);
					pedestrian.clearStrides();
					movePedestrian(topography, pedestrian, pedestrian.getPosition(), result.get(i));
				}
			}
		} catch (OpenCLException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	private int updates(float[] eventTimes1, float[] eventTimes2) {
		int count = 0;
		for(int i = 0; i < eventTimes1.length; i++) {
			if(eventTimes1[i] != eventTimes2[i]) {
				count++;
			}
		}
		return count;
	}

	private boolean checkEventTimes(@NotNull final float[] eventTimes, float simTimeInSec) {
		for(int i = 0; i < eventTimes.length; i++) {
			if(eventTimes[i] < simTimeInSec) {
				return false;
			}
		}
		return true;
	}

	@Override
	protected void updateParallelConflicts(@NotNull final PedestrianOSM pedestrian) {
		pedestrian.refreshRelevantPedestrians();
		super.updateParallelConflicts(pedestrian);
	}
}
