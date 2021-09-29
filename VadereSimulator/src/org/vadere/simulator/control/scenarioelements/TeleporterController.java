package org.vadere.simulator.control.scenarioelements;

import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Teleporter;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.Collection;

public class TeleporterController extends ScenarioElementController  {
	private final Teleporter teleporter;
	private final Topography scenario;

	public TeleporterController(Teleporter teleporter, Topography scenario) {
		this.teleporter = teleporter;
		this.scenario = scenario;
	}

	public void update(double simTimeInSec) {
		Collection<Pedestrian> pedestrians = scenario.getElements(Pedestrian.class);

		for (Pedestrian ped : pedestrians) {
			VPoint position = ped.getPosition();

			if (position.x > teleporter.getTeleporterPosition().x) {
				VPoint newPos = new VPoint(position.x
						+ teleporter.getTeleporterShift().x, position.y);
				ped.setPosition(newPos);
				scenario.moveElement(ped, position);
			}
			if (position.x < teleporter.getTeleporterPosition().x
					+ teleporter.getTeleporterShift().x) {
				VPoint newPos = new VPoint(position.x
						- teleporter.getTeleporterShift().x, position.y);
				ped.setPosition(newPos);
				scenario.moveElement(ped, position);
			}
		}
	}
}
