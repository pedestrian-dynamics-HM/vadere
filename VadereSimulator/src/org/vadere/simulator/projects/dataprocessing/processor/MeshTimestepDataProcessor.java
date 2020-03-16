package org.vadere.simulator.projects.dataprocessing.processor;

import org.jetbrains.annotations.NotNull;
import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.meshing.mesh.gen.IncrementalTriangulation;
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
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepFaceIdKey;
import org.vadere.state.attributes.processor.AttributesMeshTimestepProcessor;
import org.vadere.state.scenario.MeasurementArea;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.VPolygon;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.math.DistanceFunction;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @author Benedikt Zoennchen
 *
 * @param <V>
 */
@DataProcessorClass(label = "MeshTimestepDataProcessor")
public abstract class MeshTimestepDataProcessor<V> extends DataProcessor<TimestepFaceIdKey, V> {

	private IMesh<PVertex, PHalfEdge, PFace> mesh;
	private IIncrementalTriangulation<PVertex, PHalfEdge, PFace> triangulation;
	private MeasurementArea measurementArea;

	protected MeshTimestepDataProcessor(final String... headers) {
		super(headers);
	}

	@Override
	public void init(final ProcessorManager manager) {
		super.init(manager);
		AttributesMeshTimestepProcessor att = this.getAttributes();
		this.measurementArea = manager.getMeasurementArea(att.getMeasurementAreaId(), false);
	}
	/*

		@NotNull final IDistanceFunction distanceFunc,
			@NotNull final IEdgeLengthFunction edgeLengthFunc,
			@NotNull final Collection<? extends IPoint> fixPoints,
			final double initialEdgeLen,
			@NotNull final VRectangle bound,
			@NotNull final Collection<? extends VShape> shapes,
			@NotNull final IMeshSupplier<V, E, F> meshSupplier)
	 */

	@Override
	public void preLoop(@NotNull final SimulationState state) {
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

		var meshRenderer = new MeshRenderer<>(meshImprover.getMesh(), f -> false, f -> Color.WHITE, e -> Color.GRAY);
		var meshPanel = new PMeshPanel(meshRenderer, 300, 300);
		meshPanel.display();
		//System.out.println(mesh.toPythonTriangulation(null));
	}

	public IMesh<PVertex, PHalfEdge, PFace> getMesh() {
		return mesh;
	}

	public MeasurementArea getMeasurementArea() {
		return this.measurementArea;
	}

	public IIncrementalTriangulation<PVertex, PHalfEdge, PFace> getTriangulation() {
		return triangulation;
	}

	@Override
	public AttributesMeshTimestepProcessor getAttributes() {
		if(super.getAttributes() == null) {
			setAttributes(new AttributesMeshTimestepProcessor());
		}
		return (AttributesMeshTimestepProcessor)super.getAttributes();
	}
}
