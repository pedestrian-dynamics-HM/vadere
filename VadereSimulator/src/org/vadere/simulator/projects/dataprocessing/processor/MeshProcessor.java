package org.vadere.simulator.projects.dataprocessing.processor;

import org.jetbrains.annotations.NotNull;
import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.meshing.mesh.gen.MeshRenderer;
import org.vadere.meshing.mesh.gen.PFace;
import org.vadere.meshing.mesh.gen.PHalfEdge;
import org.vadere.meshing.mesh.gen.PMesh;
import org.vadere.meshing.mesh.gen.PVertex;
import org.vadere.meshing.mesh.impl.PMeshPanel;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.meshing.mesh.inter.IMesh;
import org.vadere.meshing.mesh.triangulation.improver.eikmesh.gen.GenEikMesh;
import org.vadere.meshing.utils.color.Colors;
import org.vadere.meshing.utils.io.poly.MeshPolyWriter;
import org.vadere.meshing.utils.io.tex.TexGraphGenerator;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.projects.SimulationResult;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.NoDataKey;
import org.vadere.state.attributes.processor.AttributesMeshProcessor;
import org.vadere.state.scenario.MeasurementArea;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.VPolygon;
import org.vadere.util.math.DistanceFunction;

import java.awt.*;
import java.util.Collections;
import java.util.function.Function;

/**
 * @author Benedikt Zoennchen
 */
@DataProcessorClass(label = "MeshProcessor")
public class MeshProcessor extends NoDataKeyProcessor<IMesh<PVertex, PHalfEdge, PFace>> {

	private IMesh<PVertex, PHalfEdge, PFace> mesh;
	private IIncrementalTriangulation<PVertex, PHalfEdge, PFace> triangulation;
	private MeasurementArea measurementArea;

	public MeshProcessor() {
		super("mesh");
		setAttributes(new AttributesMeshProcessor());
	}

	@Override
	public void init(ProcessorManager manager) {
		super.init(manager);
		AttributesMeshProcessor att = getAttributes();
		this.measurementArea = manager.getMeasurementArea(att.getMeasurementAreaId(), false);
	}

	public IIncrementalTriangulation<PVertex, PHalfEdge, PFace> getTriangulation() {
		return triangulation;
	}

	public MeasurementArea getMeasurementArea() {
		return measurementArea;
	}

	@Override
	public void postLoopAddResultInfo(@NotNull final SimulationState state, @NotNull final SimulationResult result){
		result.addData(getSimulationResultHeader(), getTriangulation().getMesh().getMeshInformations());
	}

	@Override
	public String getSimulationResultHeader() {
		return "mesh (" + getTriangulation().getMesh().hashCode() + ")";
	}

	@Override
	public String[] toStrings(final NoDataKey key) {
		MeshPolyWriter<PVertex, PHalfEdge, PFace> meshPolyWriter = new MeshPolyWriter<>();
		return new String[] { this.hasValue(key) ? meshPolyWriter.to2DPoly(this.getValue(key)) : "NA" };
	}

	@Override
	public void preLoop(SimulationState state) {
		super.preLoop(state);
		VPolygon measurementPolygon = measurementArea.asPolygon();

		GenEikMesh<PVertex, PHalfEdge, PFace> meshImprover = new GenEikMesh(
				new DistanceFunction(measurementPolygon, Collections.EMPTY_LIST),
				h -> getAttributes().getEdgeLength(),
				Collections.EMPTY_LIST,
				getAttributes().getEdgeLength(),
				GeometryUtils.boundRelative(measurementPolygon.getPoints()),
				Collections.singleton(measurementPolygon),
				() -> new PMesh()
		);


		Function<PVertex, Color> vertexColorFunction = v -> {
			if(meshImprover.isSlidePoint(v)){
				return Colors.BLUE;
			} else if(meshImprover.isFixPoint(v)) {
				return Colors.RED;
			} else {
				return Color.BLACK;
			}
		};


		var meshRenderer = new MeshRenderer<>(meshImprover.getMesh(), f -> false, f -> Color.WHITE, e -> Color.GRAY, vertexColorFunction);
		var meshPanel = new PMeshPanel(meshRenderer, 1000, 800);
		meshPanel.display();
		meshImprover.improve();
		while (!meshImprover.isFinished()) {
			synchronized (meshImprover.getMesh()) {
				meshImprover.improve();
			}
			//Thread.sleep(500);
			meshPanel.repaint();
		}


		triangulation = meshImprover.generate();
		triangulation = meshImprover.getTriangulation();
		mesh = triangulation.getMesh();

		this.putValue(NoDataKey.key() ,mesh);

		//var meshRenderer = new MeshRenderer<>(meshImprover.getMesh(), f -> false, f -> Color.WHITE, e -> Color.GRAY);
		//var meshPanel = new PMeshPanel(meshRenderer, 300, 300);
		meshPanel.display();
		System.out.println(TexGraphGenerator.toTikz(meshImprover.getMesh(), f -> Color.WHITE, null, vertexColorFunction,1.0f, true));
	}

	@Override
	protected void doUpdate(@NotNull final SimulationState state) {}

	@Override
	public AttributesMeshProcessor getAttributes() {
		if (super.getAttributes() == null) {
			setAttributes(new AttributesMeshProcessor());
		}

		return (AttributesMeshProcessor)super.getAttributes();
	}
}
