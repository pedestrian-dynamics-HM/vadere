package org.vadere.simulator.models.osm.updateScheme;

import org.jetbrains.annotations.NotNull;
import org.vadere.simulator.models.osm.PedestrianOSM;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.LinkedCellsGrid;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.logging.Logger;

import java.util.ArrayList;
import java.util.List;

public class UpdateSchemeEventDrivenParallel extends UpdateSchemeEventDriven {

	private final static Logger logger = Logger.getLogger(UpdateSchemeEventDrivenParallel.class);

	private final Topography topography;
	private LinkedCellsGrid<PedestrianOSM> linkedCellsGrid;
	private boolean[][] locked;
	private double pedestrianPotentialWidth;


	public UpdateSchemeEventDrivenParallel(@NotNull final Topography topography, @NotNull final double pedestrianPotentialWidth) {
		super(topography);
		this.topography = topography;
		this.pedestrianPotentialWidth = pedestrianPotentialWidth;
	}

	@Override
	public void update(final double timeStepInSec, final double currentTimeInSec) {

		/*for(PedestrianOSM pedestrianOSM : topography.getElements(PedestrianOSM.class)) {
			pedestrianOSM.clearStrides();
		}

		if(!pedestrianEventsQueue.isEmpty()) {
			// event driven update ignores time credits!
			while (pedestrianEventsQueue.peek().getTimeOfNextStep() < currentTimeInSec) {
				PedestrianOSM ped = pedestrianEventsQueue.poll();
				update(ped, currentTimeInSec);
				pedestrianEventsQueue.add(ped);
			}
		}*/

		double maxStepSize = 0;
		double maxDesiredSpeed = 0;
		for(PedestrianOSM pedestrianOSM : topography.getElements(PedestrianOSM.class)) {
			pedestrianOSM.clearStrides();
			maxStepSize = Math.max(pedestrianOSM.getDesiredStepSize(), maxStepSize);
			maxDesiredSpeed = Math.max(pedestrianOSM.getDesiredSpeed(), maxDesiredSpeed);
		}


		int counter = 1;
		// event driven update ignores time credits
		do{
			double stepSize = Math.max(maxStepSize, maxDesiredSpeed * timeStepInSec);
			linkedCellsGrid = new LinkedCellsGrid<>(new VRectangle(topography.getBounds()), 2*(pedestrianPotentialWidth + stepSize));
			locked = new boolean[linkedCellsGrid.getGridWidth()][linkedCellsGrid.getGridHeight()];

			List<PedestrianOSM> parallelUpdatablePeds = new ArrayList<>();
			List<PedestrianOSM> unUpdatablePedsd = new ArrayList<>();

			while (!pedestrianEventsQueue.isEmpty() && pedestrianEventsQueue.peek().getTimeOfNextStep() < currentTimeInSec) {
				PedestrianOSM ped = pedestrianEventsQueue.poll();
				int[] gridPos = linkedCellsGrid.gridPos(ped.getPosition());

				if(!locked[gridPos[0]][gridPos[1]]) {
					parallelUpdatablePeds.add(ped);
				}
				else {
					unUpdatablePedsd.add(ped);
				}

				for(int y = -1; y <= 1; y++) {
					for(int x = -1; x <= 1; x++) {
						int col = Math.min(locked.length-1, Math.max(0, gridPos[0]+x));
						int row = Math.min(locked[0].length-1, Math.max(0, gridPos[1]+y));
						locked[col][row] = true;
					}
				}
			}
			logger.info("update " + parallelUpdatablePeds.size() + " in parallel in round " + counter + ".");
			parallelUpdatablePeds.parallelStream().forEach(ped -> {
				//logger.info(ped.getTimeOfNextStep());
				//System.out.println(ped.getId());
				update(ped, currentTimeInSec);
			});

			pedestrianEventsQueue.addAll(unUpdatablePedsd);
			pedestrianEventsQueue.addAll(parallelUpdatablePeds);
			counter++;
		} while (!pedestrianEventsQueue.isEmpty() && pedestrianEventsQueue.peek().getTimeOfNextStep() < currentTimeInSec);

	}
}
