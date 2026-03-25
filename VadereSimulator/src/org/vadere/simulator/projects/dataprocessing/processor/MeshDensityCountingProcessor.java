package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.meshing.mesh.gen.mesh.pointerBased.elements.PFace;
import org.vadere.meshing.mesh.gen.mesh.pointerBased.elements.PHalfEdge;
import org.vadere.meshing.mesh.gen.mesh.pointerBased.elements.PVertex;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.meshing.mesh.inter.mesh.IMesh;
import org.vadere.meshing.mesh.inter.mesh.data.IMeshDataStorage;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepFaceIdKey;
import org.vadere.state.attributes.processor.AttributesMeshDensityCountingProcessor;
import org.vadere.state.scenario.MeasurementArea;
import org.vadere.state.scenario.Pedestrian;

import java.util.Collection;

@DataProcessorClass(label = "MeshDensityCountingProcessor")
public class MeshDensityCountingProcessor extends DataProcessor<TimestepFaceIdKey, Integer> {

	protected final static String propertyNameNumberOfPedestrians = "numberOfPedestrians";
	protected MeshProcessor meshProcessor;

	public MeshDensityCountingProcessor() {
		super("meshDensityCounting");
		setAttributes(new AttributesMeshDensityCountingProcessor());
	}

	@Override
	public void init(ProcessorManager manager) {
		super.init(manager);
		this.meshProcessor = (MeshProcessor) manager.getProcessor(getAttributes().getMeshProcessorId());
	}

	protected IMesh<PVertex, PHalfEdge, PFace> getMesh() {
		return meshProcessor.getTriangulation().getMesh();
	}

	protected IMeshDataStorage <PVertex, PHalfEdge, PFace> getMeshDataStorage(){
		return meshProcessor.getTriangulation().getMeshDataStorage();
	}

	protected MeasurementArea getMeasurementArea() {
		return meshProcessor.getMeasurementArea();
	}

	protected IIncrementalTriangulation<PVertex, PHalfEdge, PFace> getTriangulation() {
		return meshProcessor.getTriangulation();
	}

	protected void doUpdateOnPed(Pedestrian ped){
		if(getMeasurementArea().asPolygon().contains(ped.getPosition())) {
			PFace f = getTriangulation().getMesh().readConnectivity().locateNonBoundaryByFullScan(ped.getPosition(), ped).get();
			int n = getMeshDataStorage().getIntegerData(f, propertyNameNumberOfPedestrians) + 1;
			getMeshDataStorage().setIntegerData(f, propertyNameNumberOfPedestrians, n);
			assert !getMesh().faces().isBoundary(f);
		}
	}

	protected void reset_count(){
		// reset count
		for(PFace f : getMesh().faces()) {
			getMeshDataStorage().setIntegerData(f, propertyNameNumberOfPedestrians, 0);
		}
	}

	protected void write_count(SimulationState state){
		// write count
		int faceId = 1;
		for(PFace f : getMesh().faces()) {
			int n = getMeshDataStorage().getIntegerData(f, propertyNameNumberOfPedestrians);
			this.putValue(new TimestepFaceIdKey(state.getStep(), faceId), n);
			faceId++;
		}
	}

	@Override
	protected void doUpdate(SimulationState state) {
		Collection<Pedestrian> peds = state.getTopography().getElements(Pedestrian.class);

		reset_count();

		// compute count
		for(Pedestrian ped : peds) {
			doUpdateOnPed(ped);
		}

		write_count(state);
	}

	@Override
	public AttributesMeshDensityCountingProcessor getAttributes() {
		if(super.getAttributes() == null) {
			setAttributes(new AttributesMeshDensityCountingProcessor());
		}
		return (AttributesMeshDensityCountingProcessor)super.getAttributes();
	}
}
