package org.vadere.simulator.models.osm.updateScheme;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.gen.IncrementalTriangulation;
import org.vadere.meshing.mesh.gen.MeshPanel;
import org.vadere.meshing.mesh.gen.PFace;
import org.vadere.meshing.mesh.gen.PHalfEdge;
import org.vadere.meshing.mesh.gen.PMesh;
import org.vadere.meshing.mesh.gen.PVertex;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.simulator.models.osm.PedestrianOSM;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.LinkedCellsGrid;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.logging.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class UpdateSchemeEventDrivenParallel extends UpdateSchemeEventDriven {

	private final static Logger logger = Logger.getLogger(UpdateSchemeEventDrivenParallel.class);

	static {
		logger.setDebug();
	}

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
		topography.getElements(PedestrianOSM.class).parallelStream().forEach(pedestrianOSM -> pedestrianOSM.clearStrides());

		double maxStepSize = topography.getElements(PedestrianOSM.class).parallelStream().mapToDouble(ped -> ped.getDesiredStepSize()).max().orElse(0);
		double maxDesiredSpeed = topography.getElements(PedestrianOSM.class).parallelStream().mapToDouble(ped -> ped.getDesiredSpeed()).max().orElse(0);

		double stepSize = Math.max(maxStepSize, maxDesiredSpeed * timeStepInSec);
		double sideLength = (stepSize+pedestrianPotentialWidth) * 2.0;
		//logger.debug("initial grid with a grid edge length equal to " + sideLength);

		int counter = 1;
		// event driven update ignores time credits
		do {
			linkedCellsGrid = new LinkedCellsGrid<>(new VRectangle(topography.getBounds()), sideLength);
			locked = new boolean[linkedCellsGrid.getGridWidth()][linkedCellsGrid.getGridHeight()];

			List<PedestrianOSM> updateAbleAgents = new LinkedList<>();
			List<PedestrianOSM> notUpdateAbleAgents = new LinkedList<>();

			while (!pedestrianEventsQueue.isEmpty() && pedestrianEventsQueue.peek().getTimeOfNextStep() < currentTimeInSec) {
				PedestrianOSM ped = pedestrianEventsQueue.poll();
				int[] gridPos = linkedCellsGrid.gridPos(ped.getPosition());

				// lock cell of the agent
				if(!locked[gridPos[0]][gridPos[1]]) {
					updateAbleAgents.add(ped);

					// lock neighbours
					for(int y = -1; y <= 1; y++) {
						for(int x = -1; x <= 1; x++) {
							int col = Math.min(locked.length-1, Math.max(0, gridPos[0]+x));
							int row = Math.min(locked[0].length-1, Math.max(0, gridPos[1]+y));
							locked[col][row] = true;
						}
					}
				} else {
					notUpdateAbleAgents.add(ped);
				}
			}

			//logger.debug("update " + updateAbleAgents.size() + " in parallel in round " + counter + ".");
			//logger.debug("not updated " + notUpdateAbleAgents.size() + " " + counter + ".");
			updateAbleAgents.parallelStream().forEach(ped -> {
				//logger.info(ped.getTimeOfNextStep());
				//System.out.println(ped.getId());
				update(ped, timeStepInSec, currentTimeInSec);
			});

			pedestrianEventsQueue.addAll(notUpdateAbleAgents);
			pedestrianEventsQueue.addAll(updateAbleAgents);
			counter++;
		} while (!pedestrianEventsQueue.isEmpty() && pedestrianEventsQueue.peek().getTimeOfNextStep() < currentTimeInSec);
	}
}
