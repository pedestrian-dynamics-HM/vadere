package org.vadere.simulator.projects.dataprocessing.processor;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.meshing.WeilerAtherton;
import org.vadere.meshing.mesh.gen.MeshRenderer;
import org.vadere.meshing.mesh.gen.PFace;
import org.vadere.meshing.mesh.gen.PHalfEdge;
import org.vadere.meshing.mesh.gen.PMesh;
import org.vadere.meshing.mesh.gen.PVertex;
import org.vadere.meshing.mesh.impl.PMeshPanel;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.meshing.mesh.inter.IMesh;
import org.vadere.meshing.mesh.triangulation.improver.eikmesh.gen.GenEikMesh;
import org.vadere.meshing.utils.io.poly.MeshPolyWriter;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.projects.SimulationResult;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.NoDataKey;
import org.vadere.simulator.projects.migration.GeometryCleaner;
import org.vadere.state.attributes.processor.AttributesMeshProcessor;
import org.vadere.state.scenario.MeasurementArea;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.VPolygon;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.math.DistanceFunction;
import org.vadere.util.math.IDistanceFunction;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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
		// 1. clean geometry
		List<VPolygon> obstacleShapes = state.getTopography().getObstacleShapes().stream().map(v -> new VPolygon(v)).collect(Collectors.toList());
		GeometryCleaner geometryCleaner = new GeometryCleaner(
				measurementArea.asPolygon(),
				obstacleShapes,
				GeometryUtils.DOUBLE_EPS);
		Pair<VPolygon, List<VPolygon>> measurementPolygonAndHoles = geometryCleaner.cutObstacles();
		VPolygon measurementPolygon = measurementPolygonAndHoles.getLeft();
		List<VPolygon> holes = measurementPolygonAndHoles.getRight();

		// 2. mesh geometry
		IDistanceFunction distanceFunction = IDistanceFunction.create(measurementPolygon, holes);
		List<VPolygon> allPolygons = new ArrayList<>();
		allPolygons.add(measurementPolygon);
		allPolygons.addAll(holes);

		GenEikMesh<PVertex, PHalfEdge, PFace> meshImprover = new GenEikMesh(
				distanceFunction,
				h -> getAttributes().getEdgeLength(),
				Collections.EMPTY_LIST,
				getAttributes().getEdgeLength(),
				GeometryUtils.boundRelative(measurementPolygon.getPoints()),
				allPolygons,
				() -> new PMesh()
		);


		/*Function<PVertex, Color> vertexColorFunction = v -> {
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
		}*/


		triangulation = meshImprover.generate();
		triangulation = meshImprover.getTriangulation();
		mesh = triangulation.getMesh();

		this.putValue(NoDataKey.key() ,mesh);

		var meshRenderer = new MeshRenderer<>(meshImprover.getMesh(), f -> false, f -> Color.WHITE, e -> Color.GRAY);
		var meshPanel = new PMeshPanel(meshRenderer, 300, 300);

		if (getAttributes().isDisplayMesh()) meshPanel.display();
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
