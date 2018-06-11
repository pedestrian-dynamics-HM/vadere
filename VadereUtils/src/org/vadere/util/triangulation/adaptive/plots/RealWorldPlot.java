package org.vadere.util.triangulation.adaptive.plots;

import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.triangulation.adaptive.DistanceFunction;
import org.vadere.util.triangulation.adaptive.IDistanceFunction;
import org.vadere.util.triangulation.improver.PPSMeshing;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by bzoennchen on 10.06.18.
 */
public class RealWorldPlot {

	private String topographyString = "{\n" +
			"  \"attributes\" : {\n" +
			"    \"bounds\" : {\n" +
			"      \"x\" : 0.0,\n" +
			"      \"y\" : 0.0,\n" +
			"      \"width\" : 15.0,\n" +
			"      \"height\" : 20.0\n" +
			"    },\n" +
			"    \"boundingBoxWidth\" : 0.5,\n" +
			"    \"bounded\" : true\n" +
			"  },\n" +
			"  \"obstacles\" : [ {\n" +
			"    \"shape\" : {\n" +
			"      \"x\" : 6.9,\n" +
			"      \"y\" : 5.0,\n" +
			"      \"width\" : 0.1,\n" +
			"      \"height\" : 8.8,\n" +
			"      \"type\" : \"RECTANGLE\"\n" +
			"    },\n" +
			"    \"id\" : -1\n" +
			"  }, {\n" +
			"    \"shape\" : {\n" +
			"      \"x\" : 4.9,\n" +
			"      \"y\" : 5.0,\n" +
			"      \"width\" : 0.1,\n" +
			"      \"height\" : 8.8,\n" +
			"      \"type\" : \"RECTANGLE\"\n" +
			"    },\n" +
			"    \"id\" : -1\n" +
			"  }, {\n" +
			"    \"shape\" : {\n" +
			"      \"x\" : 6.9,\n" +
			"      \"y\" : 13.8,\n" +
			"      \"width\" : 0.4,\n" +
			"      \"height\" : 5.7,\n" +
			"      \"type\" : \"RECTANGLE\"\n" +
			"    },\n" +
			"    \"id\" : -1\n" +
			"  }, {\n" +
			"    \"shape\" : {\n" +
			"      \"x\" : 4.5,\n" +
			"      \"y\" : 13.0,\n" +
			"      \"width\" : 0.5,\n" +
			"      \"height\" : 0.8,\n" +
			"      \"type\" : \"RECTANGLE\"\n" +
			"    },\n" +
			"    \"id\" : -1\n" +
			"  }, {\n" +
			"    \"shape\" : {\n" +
			"      \"x\" : 4.5,\n" +
			"      \"y\" : 5.5,\n" +
			"      \"width\" : 0.1,\n" +
			"      \"height\" : 7.5,\n" +
			"      \"type\" : \"RECTANGLE\"\n" +
			"    },\n" +
			"    \"id\" : -1\n" +
			"  }, {\n" +
			"    \"shape\" : {\n" +
			"      \"x\" : 0.1,\n" +
			"      \"y\" : 5.2,\n" +
			"      \"width\" : 4.5,\n" +
			"      \"height\" : 0.3,\n" +
			"      \"type\" : \"RECTANGLE\"\n" +
			"    },\n" +
			"    \"id\" : -1\n" +
			"  }, {\n" +
			"    \"shape\" : {\n" +
			"      \"x\" : 2.0,\n" +
			"      \"y\" : 5.5,\n" +
			"      \"width\" : 0.6,\n" +
			"      \"height\" : 14.0,\n" +
			"      \"type\" : \"RECTANGLE\"\n" +
			"    },\n" +
			"    \"id\" : -1\n" +
			"  }, {\n" +
			"    \"shape\" : {\n" +
			"      \"x\" : 2.6,\n" +
			"      \"y\" : 19.1,\n" +
			"      \"width\" : 4.3,\n" +
			"      \"height\" : 0.4,\n" +
			"      \"type\" : \"RECTANGLE\"\n" +
			"    },\n" +
			"    \"id\" : -1\n" +
			"  } ],\n" +
			"  \"stairs\" : [ {\n" +
			"    \"shape\" : {\n" +
			"      \"x\" : 5.0,\n" +
			"      \"y\" : 5.0,\n" +
			"      \"width\" : 1.9,\n" +
			"      \"height\" : 1.77,\n" +
			"      \"type\" : \"RECTANGLE\"\n" +
			"    },\n" +
			"    \"id\" : -1,\n" +
			"    \"treadCount\" : 6,\n" +
			"    \"upwardDirection\" : {\n" +
			"      \"x\" : 0.0,\n" +
			"      \"y\" : 1.0\n" +
			"    }\n" +
			"  }, {\n" +
			"    \"shape\" : {\n" +
			"      \"x\" : 5.0,\n" +
			"      \"y\" : 8.29,\n" +
			"      \"width\" : 1.9,\n" +
			"      \"height\" : 4.72,\n" +
			"      \"type\" : \"RECTANGLE\"\n" +
			"    },\n" +
			"    \"id\" : -1,\n" +
			"    \"treadCount\" : 16,\n" +
			"    \"upwardDirection\" : {\n" +
			"      \"x\" : 0.0,\n" +
			"      \"y\" : 1.0\n" +
			"    }\n" +
			"  } ],\n" +
			"  \"targets\" : [ {\n" +
			"    \"id\" : 20,\n" +
			"    \"absorbing\" : false,\n" +
			"    \"shape\" : {\n" +
			"      \"x\" : 5.0,\n" +
			"      \"y\" : 13.0,\n" +
			"      \"width\" : 1.9,\n" +
			"      \"height\" : 0.4,\n" +
			"      \"type\" : \"RECTANGLE\"\n" +
			"    },\n" +
			"    \"waitingTime\" : 0.0,\n" +
			"    \"waitingTimeYellowPhase\" : 0.0,\n" +
			"    \"parallelWaiters\" : 0,\n" +
			"    \"individualWaiting\" : true,\n" +
			"    \"deletionDistance\" : 0.1,\n" +
			"    \"startingWithRedLight\" : false,\n" +
			"    \"nextSpeed\" : -1.0\n" +
			"  }, {\n" +
			"    \"id\" : 10,\n" +
			"    \"absorbing\" : false,\n" +
			"    \"shape\" : {\n" +
			"      \"x\" : 5.0,\n" +
			"      \"y\" : 4.5,\n" +
			"      \"width\" : 1.9,\n" +
			"      \"height\" : 0.5,\n" +
			"      \"type\" : \"RECTANGLE\"\n" +
			"    },\n" +
			"    \"waitingTime\" : 0.0,\n" +
			"    \"waitingTimeYellowPhase\" : 0.0,\n" +
			"    \"parallelWaiters\" : 0,\n" +
			"    \"individualWaiting\" : true,\n" +
			"    \"deletionDistance\" : 0.1,\n" +
			"    \"startingWithRedLight\" : false,\n" +
			"    \"nextSpeed\" : -1.0\n" +
			"  }, {\n" +
			"    \"id\" : 21,\n" +
			"    \"absorbing\" : true,\n" +
			"    \"shape\" : {\n" +
			"      \"x\" : 2.6,\n" +
			"      \"y\" : 18.4,\n" +
			"      \"width\" : 4.3,\n" +
			"      \"height\" : 0.7,\n" +
			"      \"type\" : \"RECTANGLE\"\n" +
			"    },\n" +
			"    \"waitingTime\" : 0.0,\n" +
			"    \"waitingTimeYellowPhase\" : 0.0,\n" +
			"    \"parallelWaiters\" : 0,\n" +
			"    \"individualWaiting\" : true,\n" +
			"    \"deletionDistance\" : 0.1,\n" +
			"    \"startingWithRedLight\" : false,\n" +
			"    \"nextSpeed\" : -1.0\n" +
			"  }, {\n" +
			"    \"id\" : 11,\n" +
			"    \"absorbing\" : true,\n" +
			"    \"shape\" : {\n" +
			"      \"x\" : 5.5,\n" +
			"      \"y\" : 0.5,\n" +
			"      \"width\" : 5.0,\n" +
			"      \"height\" : 0.5,\n" +
			"      \"type\" : \"RECTANGLE\"\n" +
			"    },\n" +
			"    \"waitingTime\" : 0.0,\n" +
			"    \"waitingTimeYellowPhase\" : 0.0,\n" +
			"    \"parallelWaiters\" : 0,\n" +
			"    \"individualWaiting\" : true,\n" +
			"    \"deletionDistance\" : 0.1,\n" +
			"    \"startingWithRedLight\" : false,\n" +
			"    \"nextSpeed\" : -1.0\n" +
			"  } ],\n" +
			"  \"sources\" : [ {\n" +
			"    \"id\" : -1,\n" +
			"    \"shape\" : {\n" +
			"      \"x\" : 8.7,\n" +
			"      \"y\" : 5.1,\n" +
			"      \"width\" : 1.0,\n" +
			"      \"height\" : 0.5,\n" +
			"      \"type\" : \"RECTANGLE\"\n" +
			"    },\n" +
			"    \"interSpawnTimeDistribution\" : \"org.vadere.state.scenario.ConstantDistribution\",\n" +
			"    \"distributionParameters\" : [ 1.0 ],\n" +
			"    \"spawnNumber\" : 5,\n" +
			"    \"maxSpawnNumberTotal\" : 5,\n" +
			"    \"startTime\" : 0.0,\n" +
			"    \"endTime\" : 0.0,\n" +
			"    \"spawnAtRandomPositions\" : true,\n" +
			"    \"useFreeSpaceOnly\" : false,\n" +
			"    \"targetIds\" : [ 10, 20, 21 ],\n" +
			"    \"dynamicElementType\" : \"PEDESTRIAN\"\n" +
			"  } ],\n" +
			"  \"dynamicElements\" : [ ],\n" +
			"  \"attributesPedestrian\" : {\n" +
			"    \"radius\" : 0.195,\n" +
			"    \"densityDependentSpeed\" : false,\n" +
			"    \"speedDistributionMean\" : 1.34,\n" +
			"    \"speedDistributionStandardDeviation\" : 0.0,\n" +
			"    \"minimumSpeed\" : 0.3,\n" +
			"    \"maximumSpeed\" : 3.0,\n" +
			"    \"acceleration\" : 2.0\n" +
			"  },\n" +
			"  \"attributesCar\" : {\n" +
			"    \"id\" : -1,\n" +
			"    \"radius\" : 0.195,\n" +
			"    \"densityDependentSpeed\" : false,\n" +
			"    \"speedDistributionMean\" : 1.34,\n" +
			"    \"speedDistributionStandardDeviation\" : 0.0,\n" +
			"    \"minimumSpeed\" : 0.3,\n" +
			"    \"maximumSpeed\" : 3.0,\n" +
			"    \"acceleration\" : 2.0,\n" +
			"    \"length\" : 4.5,\n" +
			"    \"width\" : 1.7,\n" +
			"    \"direction\" : {\n" +
			"      \"x\" : 1.0,\n" +
			"      \"y\" : 0.0\n" +
			"    }\n" +
			"  }\n" +
			"}";

	private static void realWorldExample() {

		/*VRectangle bound = new VRectangle(getTopographyBound());
		Collection<Obstacle> obstacles = Topography.createObstacleBoundary(getTopography());
		obstacles.addAll(getTopography().getObstacles());

		List<VShape> shapes = obstacles.stream().map(obstacle -> obstacle.getShape()).collect(Collectors.toList());

		IDistanceFunction distanceFunc = new DistanceFunction(bound, shapes);

		PPSMeshing meshImprover = new PPSMeshing(
				distanceFunc,
				p -> Math.min(1.0 + Math.max(-distanceFunc.apply(p), 0), 5.0),
				0.5,
				bound, getTopography().getObstacles().stream().map(obs -> obs.getShape()).collect(Collectors.toList()));*/
	}

}
