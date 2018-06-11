package org.vadere.simulator.triangulation;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.simulator.models.potential.fields.IPotentialField;
import org.vadere.simulator.models.potential.fields.ObstacleDistancePotential;
import org.vadere.state.attributes.models.AttributesFloorField;
import org.vadere.state.scenario.Obstacle;
import org.vadere.state.scenario.Topography;
import org.vadere.state.util.StateJsonConverter;
import org.vadere.util.geometry.mesh.gen.AFace;
import org.vadere.util.geometry.mesh.gen.AHalfEdge;
import org.vadere.util.geometry.mesh.gen.AMesh;
import org.vadere.util.geometry.mesh.gen.AVertex;
import org.vadere.util.geometry.mesh.gen.PFace;
import org.vadere.util.geometry.mesh.gen.PHalfEdge;
import org.vadere.util.geometry.mesh.gen.PVertex;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.potential.CellGrid;
import org.vadere.util.potential.CellState;
import org.vadere.util.potential.PathFindingTag;
import org.vadere.util.triangulation.adaptive.DistanceFunction;
import org.vadere.util.triangulation.adaptive.IDistanceFunction;
import org.vadere.util.triangulation.adaptive.MeshPoint;
import org.vadere.util.triangulation.adaptive.PSDistmesh;
import org.vadere.util.triangulation.adaptive.PSDistmeshPanel;
import org.vadere.util.triangulation.adaptive.PSMeshingPanel;
import org.vadere.util.triangulation.improver.PPSMeshing;
import org.vadere.util.triangulation.improver.PSMeshing;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.swing.*;

/**
 * @author Benedikt Zoennchen
 */
public class RealWorldPlot {

	/*
	 * TODO:
	 * 1) Obstacle distance function
	 * 2) remove the remaining re-triangulations.
	 */

	private static Logger log = LogManager.getLogger(RealWorldPlot.class);

	private static String topographyString = "{\n" +
			"  \"attributes\" : {\n" +
			"    \"bounds\" : {\n" +
			"      \"x\" : 0.0,\n" +
			"      \"y\" : 0.0,\n" +
			"      \"width\" : 40.0,\n" +
			"      \"height\" : 20.0\n" +
			"    },\n" +
			"    \"boundingBoxWidth\" : 0.5,\n" +
			"    \"bounded\" : false\n" +
			"  },\n" +
			"  \"obstacles\" : [ {\n" +
			"    \"shape\" : {\n" +
			"      \"x\" : -0.2,\n" +
			"      \"y\" : 12.0,\n" +
			"      \"width\" : 1.5,\n" +
			"      \"height\" : 0.5,\n" +
			"      \"type\" : \"RECTANGLE\"\n" +
			"    },\n" +
			"    \"id\" : -1\n" +
			"  }, {\n" +
			"    \"shape\" : {\n" +
			"      \"type\" : \"POLYGON\",\n" +
			"      \"points\" : [ {\n" +
			"        \"x\" : 40.1,\n" +
			"        \"y\" : 0.5\n" +
			"      }, {\n" +
			"        \"x\" : 16.5,\n" +
			"        \"y\" : 0.5\n" +
			"      }, {\n" +
			"        \"x\" : 16.5,\n" +
			"        \"y\" : 4.0\n" +
			"      }, {\n" +
			"        \"x\" : 16.0,\n" +
			"        \"y\" : 4.0\n" +
			"      }, {\n" +
			"        \"x\" : 16.0,\n" +
			"        \"y\" : 0.5\n" +
			"      }, {\n" +
			"        \"x\" : 12.0,\n" +
			"        \"y\" : 0.5\n" +
			"      }, {\n" +
			"        \"x\" : 12.0,\n" +
			"        \"y\" : 4.0\n" +
			"      }, {\n" +
			"        \"x\" : 11.5,\n" +
			"        \"y\" : 4.0\n" +
			"      }, {\n" +
			"        \"x\" : 11.5,\n" +
			"        \"y\" : 0.5\n" +
			"      }, {\n" +
			"        \"x\" : 8.0,\n" +
			"        \"y\" : 0.5\n" +
			"      }, {\n" +
			"        \"x\" : 8.0,\n" +
			"        \"y\" : 4.0\n" +
			"      }, {\n" +
			"        \"x\" : 7.5,\n" +
			"        \"y\" : 4.0\n" +
			"      }, {\n" +
			"        \"x\" : 7.5,\n" +
			"        \"y\" : 0.5\n" +
			"      }, {\n" +
			"        \"x\" : 5.5,\n" +
			"        \"y\" : 0.5\n" +
			"      }, {\n" +
			"        \"x\" : 5.5,\n" +
			"        \"y\" : 4.0\n" +
			"      }, {\n" +
			"        \"x\" : 5.0,\n" +
			"        \"y\" : 4.0\n" +
			"      }, {\n" +
			"        \"x\" : 5.0,\n" +
			"        \"y\" : 0.5\n" +
			"      }, {\n" +
			"        \"x\" : 3.0,\n" +
			"        \"y\" : 0.5\n" +
			"      }, {\n" +
			"        \"x\" : 3.0,\n" +
			"        \"y\" : 3.5\n" +
			"      }, {\n" +
			"        \"x\" : 4.0,\n" +
			"        \"y\" : 3.5\n" +
			"      }, {\n" +
			"        \"x\" : 4.0,\n" +
			"        \"y\" : 4.0\n" +
			"      }, {\n" +
			"        \"x\" : 2.5,\n" +
			"        \"y\" : 4.0\n" +
			"      }, {\n" +
			"        \"x\" : 2.5,\n" +
			"        \"y\" : 0.5\n" +
			"      }, {\n" +
			"        \"x\" : 0.5,\n" +
			"        \"y\" : 0.5\n" +
			"      }, {\n" +
			"        \"x\" : 0.5,\n" +
			"        \"y\" : 3.5\n" +
			"      }, {\n" +
			"        \"x\" : 1.5,\n" +
			"        \"y\" : 3.5\n" +
			"      }, {\n" +
			"        \"x\" : 1.5,\n" +
			"        \"y\" : 4.0\n" +
			"      }, {\n" +
			"        \"x\" : 0.5,\n" +
			"        \"y\" : 4.0\n" +
			"      }, {\n" +
			"        \"x\" : 0.5,\n" +
			"        \"y\" : 5.0\n" +
			"      }, {\n" +
			"        \"x\" : 1.5,\n" +
			"        \"y\" : 5.0\n" +
			"      }, {\n" +
			"        \"x\" : 1.5,\n" +
			"        \"y\" : 5.5\n" +
			"      }, {\n" +
			"        \"x\" : 0.5,\n" +
			"        \"y\" : 5.5\n" +
			"      }, {\n" +
			"        \"x\" : 0.5,\n" +
			"        \"y\" : 12.0\n" +
			"      }, {\n" +
			"        \"x\" : 1.3,\n" +
			"        \"y\" : 12.0\n" +
			"      }, {\n" +
			"        \"x\" : 1.3,\n" +
			"        \"y\" : 12.5\n" +
			"      }, {\n" +
			"        \"x\" : 0.5,\n" +
			"        \"y\" : 12.5\n" +
			"      }, {\n" +
			"        \"x\" : 0.5,\n" +
			"        \"y\" : 13.4\n" +
			"      }, {\n" +
			"        \"x\" : 9.0,\n" +
			"        \"y\" : 13.4\n" +
			"      }, {\n" +
			"        \"x\" : 9.0,\n" +
			"        \"y\" : 14.0\n" +
			"      }, {\n" +
			"        \"x\" : 0.5,\n" +
			"        \"y\" : 14.0\n" +
			"      }, {\n" +
			"        \"x\" : 0.5,\n" +
			"        \"y\" : 19.5\n" +
			"      }, {\n" +
			"        \"x\" : 0.0,\n" +
			"        \"y\" : 19.5\n" +
			"      }, {\n" +
			"        \"x\" : 0.0,\n" +
			"        \"y\" : 0.0\n" +
			"      }, {\n" +
			"        \"x\" : 40.0,\n" +
			"        \"y\" : 0.0\n" +
			"      } ]\n" +
			"    },\n" +
			"    \"id\" : -1\n" +
			"  }, {\n" +
			"    \"shape\" : {\n" +
			"      \"type\" : \"POLYGON\",\n" +
			"      \"points\" : [ {\n" +
			"        \"x\" : 4.0,\n" +
			"        \"y\" : 12.5\n" +
			"      }, {\n" +
			"        \"x\" : 4.0,\n" +
			"        \"y\" : 12.0\n" +
			"      }, {\n" +
			"        \"x\" : 3.0,\n" +
			"        \"y\" : 12.0\n" +
			"      }, {\n" +
			"        \"x\" : 3.0,\n" +
			"        \"y\" : 5.5\n" +
			"      }, {\n" +
			"        \"x\" : 4.0,\n" +
			"        \"y\" : 5.5\n" +
			"      }, {\n" +
			"        \"x\" : 4.0,\n" +
			"        \"y\" : 5.0\n" +
			"      }, {\n" +
			"        \"x\" : 2.5,\n" +
			"        \"y\" : 5.0\n" +
			"      }, {\n" +
			"        \"x\" : 2.5,\n" +
			"        \"y\" : 12.5\n" +
			"      } ]\n" +
			"    },\n" +
			"    \"id\" : -1\n" +
			"  }, {\n" +
			"    \"shape\" : {\n" +
			"      \"type\" : \"POLYGON\",\n" +
			"      \"points\" : [ {\n" +
			"        \"x\" : 12.0,\n" +
			"        \"y\" : 12.5\n" +
			"      }, {\n" +
			"        \"x\" : 5.0,\n" +
			"        \"y\" : 12.5\n" +
			"      }, {\n" +
			"        \"x\" : 5.0,\n" +
			"        \"y\" : 5.0\n" +
			"      }, {\n" +
			"        \"x\" : 12.0,\n" +
			"        \"y\" : 5.0\n" +
			"      }, {\n" +
			"        \"x\" : 12.0,\n" +
			"        \"y\" : 10.0\n" +
			"      }, {\n" +
			"        \"x\" : 8.0,\n" +
			"        \"y\" : 10.0\n" +
			"      }, {\n" +
			"        \"x\" : 8.0,\n" +
			"        \"y\" : 5.6\n" +
			"      }, {\n" +
			"        \"x\" : 5.7,\n" +
			"        \"y\" : 5.6\n" +
			"      }, {\n" +
			"        \"x\" : 5.6,\n" +
			"        \"y\" : 12.0\n" +
			"      }, {\n" +
			"        \"x\" : 12.0,\n" +
			"        \"y\" : 12.0\n" +
			"      } ]\n" +
			"    },\n" +
			"    \"id\" : -1\n" +
			"  }, {\n" +
			"    \"shape\" : {\n" +
			"      \"type\" : \"POLYGON\",\n" +
			"      \"points\" : [ {\n" +
			"        \"x\" : 16.0,\n" +
			"        \"y\" : 5.0\n" +
			"      }, {\n" +
			"        \"x\" : 16.0,\n" +
			"        \"y\" : 7.0\n" +
			"      }, {\n" +
			"        \"x\" : 18.5,\n" +
			"        \"y\" : 7.0\n" +
			"      }, {\n" +
			"        \"x\" : 18.5,\n" +
			"        \"y\" : 5.5\n" +
			"      }, {\n" +
			"        \"x\" : 21.0,\n" +
			"        \"y\" : 5.5\n" +
			"      }, {\n" +
			"        \"x\" : 21.0,\n" +
			"        \"y\" : 5.7\n" +
			"      }, {\n" +
			"        \"x\" : 25.0,\n" +
			"        \"y\" : 5.7\n" +
			"      }, {\n" +
			"        \"x\" : 25.0,\n" +
			"        \"y\" : 4.0\n" +
			"      }, {\n" +
			"        \"x\" : 24.8,\n" +
			"        \"y\" : 4.0\n" +
			"      }, {\n" +
			"        \"x\" : 24.8,\n" +
			"        \"y\" : 5.5\n" +
			"      }, {\n" +
			"        \"x\" : 24.0,\n" +
			"        \"y\" : 5.5\n" +
			"      }, {\n" +
			"        \"x\" : 24.0,\n" +
			"        \"y\" : 4.0\n" +
			"      }, {\n" +
			"        \"x\" : 23.8,\n" +
			"        \"y\" : 4.0\n" +
			"      }, {\n" +
			"        \"x\" : 23.8,\n" +
			"        \"y\" : 5.5\n" +
			"      }, {\n" +
			"        \"x\" : 23.0,\n" +
			"        \"y\" : 5.5\n" +
			"      }, {\n" +
			"        \"x\" : 23.0,\n" +
			"        \"y\" : 4.0\n" +
			"      }, {\n" +
			"        \"x\" : 22.8,\n" +
			"        \"y\" : 4.0\n" +
			"      }, {\n" +
			"        \"x\" : 22.8,\n" +
			"        \"y\" : 5.5\n" +
			"      }, {\n" +
			"        \"x\" : 22.0,\n" +
			"        \"y\" : 5.5\n" +
			"      }, {\n" +
			"        \"x\" : 22.0,\n" +
			"        \"y\" : 4.0\n" +
			"      }, {\n" +
			"        \"x\" : 21.8,\n" +
			"        \"y\" : 4.0\n" +
			"      }, {\n" +
			"        \"x\" : 21.8,\n" +
			"        \"y\" : 5.5\n" +
			"      }, {\n" +
			"        \"x\" : 21.3,\n" +
			"        \"y\" : 5.5\n" +
			"      }, {\n" +
			"        \"x\" : 21.0,\n" +
			"        \"y\" : 5.0\n" +
			"      } ]\n" +
			"    },\n" +
			"    \"id\" : -1\n" +
			"  }, {\n" +
			"    \"shape\" : {\n" +
			"      \"type\" : \"POLYGON\",\n" +
			"      \"points\" : [ {\n" +
			"        \"x\" : 22.0,\n" +
			"        \"y\" : 3.0\n" +
			"      }, {\n" +
			"        \"x\" : 21.8,\n" +
			"        \"y\" : 3.0\n" +
			"      }, {\n" +
			"        \"x\" : 21.8,\n" +
			"        \"y\" : 1.5\n" +
			"      }, {\n" +
			"        \"x\" : 25.0,\n" +
			"        \"y\" : 1.5\n" +
			"      }, {\n" +
			"        \"x\" : 25.0,\n" +
			"        \"y\" : 3.0\n" +
			"      }, {\n" +
			"        \"x\" : 24.8,\n" +
			"        \"y\" : 3.0\n" +
			"      }, {\n" +
			"        \"x\" : 24.8,\n" +
			"        \"y\" : 1.7\n" +
			"      }, {\n" +
			"        \"x\" : 24.0,\n" +
			"        \"y\" : 1.7\n" +
			"      }, {\n" +
			"        \"x\" : 24.0,\n" +
			"        \"y\" : 3.0\n" +
			"      }, {\n" +
			"        \"x\" : 23.8,\n" +
			"        \"y\" : 3.0\n" +
			"      }, {\n" +
			"        \"x\" : 23.8,\n" +
			"        \"y\" : 1.7\n" +
			"      }, {\n" +
			"        \"x\" : 23.0,\n" +
			"        \"y\" : 1.7\n" +
			"      }, {\n" +
			"        \"x\" : 23.0,\n" +
			"        \"y\" : 3.0\n" +
			"      }, {\n" +
			"        \"x\" : 22.8,\n" +
			"        \"y\" : 3.0\n" +
			"      }, {\n" +
			"        \"x\" : 22.8,\n" +
			"        \"y\" : 1.7\n" +
			"      }, {\n" +
			"        \"x\" : 22.0,\n" +
			"        \"y\" : 1.7\n" +
			"      } ]\n" +
			"    },\n" +
			"    \"id\" : -1\n" +
			"  }, {\n" +
			"    \"shape\" : {\n" +
			"      \"type\" : \"POLYGON\",\n" +
			"      \"points\" : [ {\n" +
			"        \"x\" : 5.0,\n" +
			"        \"y\" : 16.0\n" +
			"      }, {\n" +
			"        \"x\" : 4.0,\n" +
			"        \"y\" : 17.0\n" +
			"      }, {\n" +
			"        \"x\" : 5.0,\n" +
			"        \"y\" : 18.0\n" +
			"      }, {\n" +
			"        \"x\" : 6.0,\n" +
			"        \"y\" : 17.0\n" +
			"      } ]\n" +
			"    },\n" +
			"    \"id\" : -1\n" +
			"  }, {\n" +
			"    \"shape\" : {\n" +
			"      \"type\" : \"POLYGON\",\n" +
			"      \"points\" : [ {\n" +
			"        \"x\" : 34.0,\n" +
			"        \"y\" : 14.0\n" +
			"      }, {\n" +
			"        \"x\" : 33.0,\n" +
			"        \"y\" : 15.0\n" +
			"      }, {\n" +
			"        \"x\" : 33.0,\n" +
			"        \"y\" : 17.0\n" +
			"      }, {\n" +
			"        \"x\" : 34.0,\n" +
			"        \"y\" : 18.0\n" +
			"      }, {\n" +
			"        \"x\" : 36.0,\n" +
			"        \"y\" : 18.0\n" +
			"      }, {\n" +
			"        \"x\" : 37.0,\n" +
			"        \"y\" : 17.0\n" +
			"      }, {\n" +
			"        \"x\" : 37.0,\n" +
			"        \"y\" : 15.0\n" +
			"      }, {\n" +
			"        \"x\" : 36.0,\n" +
			"        \"y\" : 14.0\n" +
			"      } ]\n" +
			"    },\n" +
			"    \"id\" : -1\n" +
			"  } ],\n" +
			"  \"stairs\" : [ ],\n" +
			"  \"targets\" : [ ],\n" +
			"  \"sources\" : [ ],\n" +
			"  \"dynamicElements\" : [ {\n" +
			"    \"source\" : null,\n" +
			"    \"targetIds\" : [ ],\n" +
			"    \"position\" : {\n" +
			"      \"x\" : 18.4,\n" +
			"      \"y\" : 1.6\n" +
			"    },\n" +
			"    \"velocity\" : {\n" +
			"      \"x\" : 0.0,\n" +
			"      \"y\" : 0.0\n" +
			"    },\n" +
			"    \"nextTargetListIndex\" : 0,\n" +
			"    \"freeFlowSpeed\" : 1.34,\n" +
			"    \"attributes\" : {\n" +
			"      \"id\" : -1,\n" +
			"      \"radius\" : 0.195,\n" +
			"      \"densityDependentSpeed\" : false,\n" +
			"      \"speedDistributionMean\" : 1.34,\n" +
			"      \"speedDistributionStandardDeviation\" : 0.0,\n" +
			"      \"minimumSpeed\" : 0.3,\n" +
			"      \"maximumSpeed\" : 3.0,\n" +
			"      \"acceleration\" : 2.0\n" +
			"    },\n" +
			"    \"idAsTarget\" : -1,\n" +
			"    \"modelPedestrianMap\" : { },\n" +
			"    \"isChild\" : false,\n" +
			"    \"isLikelyInjured\" : false,\n" +
			"    \"groupIds\" : [ ],\n" +
			"    \"type\" : \"PEDESTRIAN\"\n" +
			"  } ],\n" +
			"  \"attributesPedestrian\" : {\n" +
			"    \"radius\" : 0.195,\n" +
			"    \"densityDependentSpeed\" : false,\n" +
			"    \"speedDistributionMean\" : 1.34,\n" +
			"    \"speedDistributionStandardDeviation\" : 0.0,\n" +
			"    \"minimumSpeed\" : 0.3,\n" +
			"    \"maximumSpeed\" : 3.0,\n" +
			"    \"acceleration\" : 2.0\n" +
			"  },\n" +
			"  \"attributesCar\" : null\n" +
			"}";

	private static void realWorldExampleEikMesh() throws IOException {
		Topography topography = StateJsonConverter.deserializeTopographyFromNode(StateJsonConverter.readTree(topographyString));
		VRectangle bound = new VRectangle(topography.getBounds());
		Collection<Obstacle> obstacles = Topography.createObstacleBoundary(topography);
		obstacles.addAll(topography.getObstacles());

		List<VShape> shapes = obstacles.stream().map(obstacle -> obstacle.getShape()).collect(Collectors.toList());

		IDistanceFunction distanceFunc = new DistanceFunction(bound, shapes);

		IPotentialField distanceField = new ObstacleDistancePotential(
				topography.getObstacles().stream().map(obs -> obs.getShape()).collect(Collectors.toList()),
				new VRectangle(topography.getBounds()),
				new AttributesFloorField());
		Function<IPoint, Double> obstacleDistance = p -> distanceField.getPotential(p, null);
		//IDistanceFunction distanceFunc = p -> -obstacleDistance.apply(p);


		CellGrid cellGrid = new CellGrid(bound.getWidth(), bound.getHeight(), 0.1, new CellState());
		cellGrid.pointStream().forEach(p -> cellGrid.setValue(p, new CellState(distanceFunc.apply(cellGrid.pointToCoord(p)), PathFindingTag.Reachable)));
		Function<IPoint, Double> interpolationFunction = cellGrid.getInterpolationFunction();
		IDistanceFunction approxDistance = p -> interpolationFunction.apply(p);

		PSMeshing<MeshPoint, AVertex<MeshPoint>, AHalfEdge<MeshPoint>, AFace<MeshPoint>> meshGenerator = new PSMeshing<>(
				approxDistance,
				p -> Math.min(1.0 + Math.max(-approxDistance.apply(p), 0)*0.5, 5.0),
				0.5,
				bound,topography.getObstacles().stream().map(obs -> obs.getShape()).collect(Collectors.toList()),
				() -> new AMesh<>((x, y) -> new MeshPoint(x, y, false)));

		PSMeshingPanel<MeshPoint, AVertex<MeshPoint>, AHalfEdge<MeshPoint>, AFace<MeshPoint>> distmeshPanel = new PSMeshingPanel<>(meshGenerator.getMesh(),
				f -> false, 1000, 800, bound);
		JFrame frame = distmeshPanel.display();
		frame.setVisible(true);
		frame.setTitle("Real world example");
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		distmeshPanel.repaint();


		StopWatch overAllTime = new StopWatch();
		overAllTime.start();
		//meshGenerator.improve();
		//meshGenerator.improve();
		//meshGenerator.improve();


		int nSteps = 0;
		while (nSteps < 300) {
			nSteps++;
			meshGenerator.improve();
			overAllTime.suspend();
			/*try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}*/
			distmeshPanel.repaint();
			log.info("quality: " + meshGenerator.getQuality());
			log.info("min-quality: " + meshGenerator.getMinQuality());
			overAllTime.resume();
		}
		overAllTime.stop();

		log.info("#vertices: " + meshGenerator.getMesh().getVertices().size());
		log.info("#edges: " + meshGenerator.getMesh().getEdges().size());
		log.info("#faces: " + meshGenerator.getMesh().getFaces().size());
		log.info("quality: " + meshGenerator.getQuality());
		log.info("min-quality: " + meshGenerator.getMinQuality());
		log.info("overall time: " + overAllTime.getTime() + "[ms]");

	}

	private static void realWorldExampleDistMesh() throws IOException {
		Topography topography = StateJsonConverter.deserializeTopographyFromNode(StateJsonConverter.readTree(topographyString));
		VRectangle bound = new VRectangle(topography.getBounds());
		Collection<Obstacle> obstacles = Topography.createObstacleBoundary(topography);
		obstacles.addAll(topography.getObstacles());

		List<VShape> shapes = obstacles.stream().map(obstacle -> obstacle.getShape()).collect(Collectors.toList());

		IDistanceFunction distanceFunc = new DistanceFunction(bound, shapes);

		IPotentialField distanceField = new ObstacleDistancePotential(
				topography.getObstacles().stream().map(obs -> obs.getShape()).collect(Collectors.toList()),
				new VRectangle(topography.getBounds()),
				new AttributesFloorField());
		Function<IPoint, Double> obstacleDistance = p -> distanceField.getPotential(p, null);
		//IDistanceFunction distanceFunc = p -> -obstacleDistance.apply(p);


		CellGrid cellGrid = new CellGrid(bound.getWidth(), bound.getHeight(), 0.1, new CellState());
		cellGrid.pointStream().forEach(p -> cellGrid.setValue(p, new CellState(distanceFunc.apply(cellGrid.pointToCoord(p)), PathFindingTag.Reachable)));
		Function<IPoint, Double> interpolationFunction = cellGrid.getInterpolationFunction();
		IDistanceFunction approxDistance = p -> interpolationFunction.apply(p);

		PSDistmesh meshGenerator = new PSDistmesh(
				approxDistance,
				p -> Math.min(1.0 + Math.max(-approxDistance.apply(p), 0)*0.5, 5.0),
				0.18,
				bound,topography.getObstacles().stream().map(obs -> obs.getShape()).collect(Collectors.toList()));

		PSDistmeshPanel distmeshPanel = new PSDistmeshPanel(meshGenerator,
				 1000, 800, bound, f -> false);
		JFrame frame = distmeshPanel.display();
		frame.setVisible(true);
		frame.setTitle("Real world example");
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		distmeshPanel.repaint();


		StopWatch overAllTime = new StopWatch();
		overAllTime.start();
		//meshGenerator.improve();
		//meshGenerator.improve();
		//meshGenerator.improve();


		int nSteps = 0;
		while (nSteps < 300) {
			nSteps++;
			meshGenerator.improve();
			overAllTime.suspend();
			/*try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}*/
			distmeshPanel.repaint();
			log.info("quality: " + meshGenerator.getQuality());
			log.info("min-quality: " + meshGenerator.getMinQuality());
			overAllTime.resume();
		}
		overAllTime.stop();

		log.info("#vertices: " + meshGenerator.getPoints().size());
		//log.info("#edges: " + meshGenerator..size());
		log.info("#faces: " + meshGenerator.getTriangles().size());
		log.info("quality: " + meshGenerator.getQuality());
		log.info("min-quality: " + meshGenerator.getMinQuality());
		log.info("overall time: " + overAllTime.getTime() + "[ms]");

	}

	public static void main(String ... args) throws IOException {
		realWorldExampleEikMesh();
		//realWorldExampleDistMesh();
	}

}
