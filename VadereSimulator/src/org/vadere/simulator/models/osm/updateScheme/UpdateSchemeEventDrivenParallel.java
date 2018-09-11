package org.vadere.simulator.models.osm.updateScheme;

import org.jetbrains.annotations.NotNull;
import org.vadere.simulator.models.osm.PedestrianOSM;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.LinkedCellsGrid;
import org.vadere.util.geometry.shapes.VRectangle;

import java.util.ArrayList;
import java.util.List;

public class UpdateSchemeEventDrivenParallel extends UpdateSchemeEventDriven {

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
		double maxStepSize = 0;
		for(PedestrianOSM pedestrianOSM : topography.getElements(PedestrianOSM.class)) {
			pedestrianOSM.clearStrides();
			maxStepSize = Math.max(pedestrianOSM.getStepSize(), maxStepSize);
		}



		// event driven update ignores time credits
		do{
			linkedCellsGrid = new LinkedCellsGrid<>(new VRectangle(topography.getBounds()), pedestrianPotentialWidth + maxStepSize);
			locked = new boolean[linkedCellsGrid.getGridWidth()][linkedCellsGrid.getGridHeight()];

			for(PedestrianOSM pedestrianOSM : topography.getElements(PedestrianOSM.class)) {
				linkedCellsGrid.addObject(pedestrianOSM);
			}

			List<PedestrianOSM> parallelUpdatablePeds = new ArrayList<>();
			List<PedestrianOSM> unUpdatablePedsd = new ArrayList<>();

			while (!pedestrianEventsQueue.isEmpty() && pedestrianEventsQueue.peek().getTimeOfNextStep() < currentTimeInSec) {
				PedestrianOSM ped = pedestrianEventsQueue.poll();
				int[] gridPos = linkedCellsGrid.gridPos(ped.getPosition());

				if(!locked[gridPos[0]][gridPos[1]]) {
					parallelUpdatablePeds.add(ped);
					for(int y = -1; y <= 1; y++) {
						for(int x = -1; x <= 1; x++) {
							locked[gridPos[0]+x][gridPos[1]+y] = true;
						}
					}
				}
				else {
					unUpdatablePedsd.add(ped);
				}
			}

			parallelUpdatablePeds.parallelStream().forEach(ped -> {
				update(ped, currentTimeInSec);
			});

			pedestrianEventsQueue.addAll(unUpdatablePedsd);
			pedestrianEventsQueue.addAll(parallelUpdatablePeds);
		} while (!pedestrianEventsQueue.isEmpty() && pedestrianEventsQueue.peek().getTimeOfNextStep() < currentTimeInSec);

	}
}
