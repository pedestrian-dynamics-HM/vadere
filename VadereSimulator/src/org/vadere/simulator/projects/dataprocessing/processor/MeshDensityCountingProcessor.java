package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.meshing.mesh.gen.PFace;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepFaceIdKey;
import org.vadere.state.attributes.processor.AttributesMeshTimestepProcessor;
import org.vadere.state.scenario.Pedestrian;

import java.util.Collection;

@DataProcessorClass()
public class MeshDensityCountingProcessor extends MeshTimestepDataProcessor<Integer>{

	private final static String propertyNameNumberOfPedestrians = "numberOfPedestrians";

	public MeshDensityCountingProcessor() {
		super("meshDensityCounting");
		setAttributes(new AttributesMeshTimestepProcessor());
	}

	@Override
	protected void doUpdate(SimulationState state) {
		Collection<Pedestrian> peds = state.getTopography().getElements(Pedestrian.class);

		// reset count
		for(PFace f : getMesh().getFaces()) {
			getMesh().setIntegerData(f, propertyNameNumberOfPedestrians, 0);
		}

		// compute count
		for(Pedestrian ped : peds) {

			if(getMeasurementArea().asPolygon().contains(ped.getPosition())) {
				PFace f = getTriangulation().locate(ped.getPosition(), ped).get();
				int n = getMesh().getIntegerData(f, propertyNameNumberOfPedestrians) + 1;
				getMesh().setIntegerData(f, propertyNameNumberOfPedestrians, n);
				assert !getMesh().isBoundary(f);
			}
		}

		// write count
		int faceId = 1;
		for(PFace f : getMesh().getFaces()) {
			int n = getMesh().getIntegerData(f, propertyNameNumberOfPedestrians);
			this.putValue(new TimestepFaceIdKey(state.getStep(), faceId), n);
			faceId++;
		}
	}

}
