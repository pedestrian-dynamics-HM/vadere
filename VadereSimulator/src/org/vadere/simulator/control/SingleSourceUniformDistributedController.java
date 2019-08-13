package org.vadere.simulator.control;

import org.vadere.simulator.models.DynamicElementFactory;
import org.vadere.simulator.models.osm.PedestrianOSM;
import org.vadere.state.attributes.scenario.AttributesDynamicElement;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Source;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.shapes.VPoint;

import java.awt.geom.Rectangle2D;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class SingleSourceUniformDistributedController extends SourceController {

	private int numberToSpawn;

	public SingleSourceUniformDistributedController(Topography scenario, Source source, DynamicElementFactory dynamicElementFactory, AttributesDynamicElement attributesDynamicElement, Random random) {
		super(scenario, source, dynamicElementFactory, attributesDynamicElement, random);
	}

	@Override
	public void update(double simTimeInSec) {
		if (!isSourceFinished(simTimeInSec)) {
			if (!sourceAttributes.isUseFreeSpaceOnly() && (simTimeInSec >= timeOfNextEvent || numberToSpawn > 0)) {
				determineNumberOfSpawnsAndNextEvent(simTimeInSec);

				for (int i = 0; i < numberToSpawn; i++) {
					Rectangle2D rec = source.getAttributes().getShape().getBounds2D();
					if (!isMaximumNumberOfSpawnedElementsReached()) {
						addNewAgentToScenario(new VPoint(rec.getMinX() + random.nextDouble() * rec.getWidth(), rec.getMinY() + random.nextDouble() * rec.getHeight()));
						dynamicElementsCreatedTotal++;
					}
				}
			} else if(simTimeInSec >= timeOfNextEvent || numberToSpawn > 0) {
				determineNumberOfSpawnsAndNextEvent(simTimeInSec);

				for (int i = 0; i < numberToSpawn; i++) {
					Rectangle2D rec = source.getAttributes().getShape().getBounds2D();
					VPoint candidate;
					boolean conflict;
					List<Pedestrian> pedestrianList;

					do{
						final VPoint randomPoint = new VPoint(rec.getMinX() + random.nextDouble() * rec.getWidth(), rec.getMinY() + random.nextDouble() * rec.getHeight());
						pedestrianList = getTopography().getSpatialMap(Pedestrian.class).getObjects(randomPoint, getTopography().getAttributesPedestrian().getRadius());
						candidate = randomPoint;
						conflict = pedestrianList.stream().anyMatch(ped -> ped.getPosition().distance(randomPoint) <= 2.5 * getTopography().getAttributesPedestrian().getRadius());

					} while (conflict);

					if (!isMaximumNumberOfSpawnedElementsReached()) {
						addNewAgentToScenario(candidate);
						dynamicElementsCreatedTotal++;
					}
				}
			}
		}
	}

	@Override
	protected boolean isQueueEmpty() {
		return numberToSpawn == 0;
	}

	@Override
	protected void determineNumberOfSpawnsAndNextEvent(double simTimeInSec) {
		while (timeOfNextEvent <= simTimeInSec) {
			numberToSpawn += sourceAttributes.getSpawnNumber();
			createNextEvent();
		}
	}
}
