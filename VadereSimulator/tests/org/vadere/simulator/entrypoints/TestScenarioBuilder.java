package org.vadere.simulator.entrypoints;

import org.junit.Test;
import org.vadere.simulator.projects.Scenario;
import org.vadere.simulator.projects.io.IOVadere;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.AttributesBuilder;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.attributes.scenario.AttributesSource;
import org.vadere.state.attributes.scenario.AttributesTopography;
import org.vadere.util.geometry.shapes.VRectangle;

import java.io.IOException;

import static junit.framework.TestCase.assertTrue;

/**
 * @author Benedikt Zoennchen
 */
public class TestScenarioBuilder {

    @Test
    public void testChangeAttributes() throws IOException {
        // 1. read the scenario from a string
        Scenario scenario = IOVadere.fromJson(jsonScenario);

        // 2. change the source id = -1 to source id = 1
        assertTrue(scenario.getTopography().getSources().get(0).getId() == -1);
        ScenarioBuilder builder = new ScenarioBuilder(scenario);
        builder.setSourceField("id", -1, 1);
        assertTrue(scenario.getTopography().getSources().get(0).getId() == -1);
        scenario = builder.build();
        assertTrue(scenario.getTopography().getSources().get(0).getId() == 1);

        // 3. change pedestrianAttributes / radius
        builder.setAttributesField("radius", 30.0, AttributesAgent.class);
        scenario = builder.build();
        assertTrue(scenario.getAttributesPedestrian().getRadius() == 30.0);

        // 4. change topographyAttributes / radius
        builder.setAttributesField("bounds", new VRectangle(10, 10, 100, 100), AttributesTopography.class);
        scenario = builder.build();
        assertTrue(scenario.getTopography().getBounds().equals(new VRectangle(10, 10, 100, 100)));

        // 5. change sourceAttributes
        builder.setSourceField("spawnNumber", 1, 400);
        scenario = builder.build();
        assertTrue(scenario.getTopography().getSources().get(0).getAttributes().getSpawnNumber() == 400);
    }

    private static String jsonScenario = "{\n" +
            "  \"name\" : \"basic_1_chicken_osm1\",\n" +
            "  \"description\" : \"\",\n" +
            "  \"release\" : \"0.2\",\n" +
            "  \"processWriters\" : {\n" +
            "    \"files\" : [ {\n" +
            "      \"type\" : \"org.vadere.simulator.projects.dataprocessing.outputfile.TimestepPedestrianIdOutputFile\",\n" +
            "      \"filename\" : \"out.txt\",\n" +
            "      \"processors\" : [ 1 ]\n" +
            "    } ],\n" +
            "    \"processors\" : [ {\n" +
            "      \"type\" : \"org.vadere.simulator.projects.dataprocessing.processor.PedestrianPositionProcessor\",\n" +
            "      \"id\" : 1\n" +
            "    } ],\n" +
            "    \"isTimestamped\" : true\n" +
            "  },\n" +
            "  \"scenario\" : {\n" +
            "    \"mainModel\" : \"org.vadere.simulator.models.osm.OptimalStepsModel\",\n" +
            "    \"attributesModel\" : {\n" +
            "      \"org.vadere.state.attributes.models.AttributesPotentialCompact\" : {\n" +
            "        \"pedPotentialWidth\" : 0.5,\n" +
            "        \"pedPotentialHeight\" : 12.6,\n" +
            "        \"obstPotentialWidth\" : 0.25,\n" +
            "        \"obstPotentialHeight\" : 20.1,\n" +
            "        \"useHardBodyShell\" : false,\n" +
            "        \"obstDistanceDeviation\" : 0.0,\n" +
            "        \"visionFieldRadius\" : 5.0\n" +
            "      },\n" +
            "      \"org.vadere.state.attributes.models.AttributesOSM\" : {\n" +
            "        \"stepCircleResolution\" : 18,\n" +
            "        \"numberOfCircles\" : 1,\n" +
            "        \"varyStepDirection\" : false,\n" +
            "        \"stepLengthIntercept\" : 0.4625,\n" +
            "        \"stepLengthSlopeSpeed\" : 0.2345,\n" +
            "        \"stepLengthSD\" : 0.036,\n" +
            "        \"movementThreshold\" : 0.0,\n" +
            "        \"optimizationType\" : \"DISCRETE\",\n" +
            "        \"movementType\" : \"ARBITRARY\",\n" +
            "        \"dynamicStepLength\" : false,\n" +
            "        \"updateType\" : \"EVENT_DRIVEN\",\n" +
            "        \"seeSmallWalls\" : false,\n" +
            "        \"minimumStepLength\" : false,\n" +
            "        \"targetPotentialModel\" : \"org.vadere.simulator.models.potential.fields.PotentialFieldTargetGrid\",\n" +
            "        \"pedestrianPotentialModel\" : \"org.vadere.simulator.models.potential.PotentialFieldPedestrianCompact\",\n" +
            "        \"obstaclePotentialModel\" : \"org.vadere.simulator.models.potential.PotentialFieldObstacleCompact\",\n" +
            "        \"submodels\" : [ ]\n" +
            "      },\n" +
            "      \"org.vadere.state.attributes.models.AttributesFloorField\" : {\n" +
            "        \"createMethod\" : \"HIGH_ACCURACY_FAST_MARCHING\",\n" +
            "        \"potentialFieldResolution\" : 0.1,\n" +
            "        \"obstacleGridPenalty\" : 0.1,\n" +
            "        \"targetAttractionStrength\" : 1.0,\n" +
            "        \"timeCostAttributes\" : {\n" +
            "          \"standardDerivation\" : 0.7,\n" +
            "          \"type\" : \"UNIT\",\n" +
            "          \"obstacleDensityWeight\" : 3.5,\n" +
            "          \"pedestrianSameTargetDensityWeight\" : 3.5,\n" +
            "          \"pedestrianOtherTargetDensityWeight\" : 3.5,\n" +
            "          \"pedestrianWeight\" : 3.5,\n" +
            "          \"queueWidthLoading\" : 1.0,\n" +
            "          \"pedestrianDynamicWeight\" : 6.0,\n" +
            "          \"loadingType\" : \"CONSTANT\"\n" +
            "        }\n" +
            "      }\n" +
            "    },\n" +
            "    \"attributesSimulation\" : {\n" +
            "      \"finishTime\" : 200.0,\n" +
            "      \"simTimeStepLength\" : 0.4,\n" +
            "      \"realTimeSimTimeRatio\" : 0.0,\n" +
            "      \"writeSimulationData\" : true,\n" +
            "      \"visualizationEnabled\" : true,\n" +
            "      \"printFPS\" : false,\n" +
            "      \"needsBoundary\" : false,\n" +
            "      \"digitsPerCoordinate\" : 2,\n" +
            "      \"useRandomSeed\" : true,\n" +
            "      \"randomSeed\" : 1\n" +
            "    },\n" +
            "    \"topography\" : {\n" +
            "      \"attributes\" : {\n" +
            "        \"bounds\" : {\n" +
            "          \"x\" : 0.0,\n" +
            "          \"y\" : 0.0,\n" +
            "          \"width\" : 35.0,\n" +
            "          \"height\" : 60.0\n" +
            "        },\n" +
            "        \"boundingBoxWidth\" : 0.5,\n" +
            "        \"bounded\" : true\n" +
            "      },\n" +
            "      \"obstacles\" : [ {\n" +
            "        \"shape\" : {\n" +
            "          \"x\" : 10.0,\n" +
            "          \"y\" : 40.0,\n" +
            "          \"width\" : 15.0,\n" +
            "          \"height\" : 1.0,\n" +
            "          \"type\" : \"RECTANGLE\"\n" +
            "        },\n" +
            "        \"id\" : -1\n" +
            "      }, {\n" +
            "        \"shape\" : {\n" +
            "          \"x\" : 9.0,\n" +
            "          \"y\" : 21.0,\n" +
            "          \"width\" : 1.0,\n" +
            "          \"height\" : 20.0,\n" +
            "          \"type\" : \"RECTANGLE\"\n" +
            "        },\n" +
            "        \"id\" : -1\n" +
            "      }, {\n" +
            "        \"shape\" : {\n" +
            "          \"x\" : 25.0,\n" +
            "          \"y\" : 21.0,\n" +
            "          \"width\" : 1.0,\n" +
            "          \"height\" : 20.0,\n" +
            "          \"type\" : \"RECTANGLE\"\n" +
            "        },\n" +
            "        \"id\" : -1\n" +
            "      } ],\n" +
            "      \"stairs\" : [ ],\n" +
            "      \"targets\" : [ {\n" +
            "        \"id\" : 1,\n" +
            "        \"absorbing\" : true,\n" +
            "        \"shape\" : {\n" +
            "          \"x\" : 10.0,\n" +
            "          \"y\" : 51.0,\n" +
            "          \"width\" : 15.0,\n" +
            "          \"height\" : 5.0,\n" +
            "          \"type\" : \"RECTANGLE\"\n" +
            "        },\n" +
            "        \"waitingTime\" : 0.0,\n" +
            "        \"waitingTimeYellowPhase\" : 0.0,\n" +
            "        \"parallelWaiters\" : 0,\n" +
            "        \"individualWaiting\" : true,\n" +
            "        \"deletionDistance\" : 0.1,\n" +
            "        \"startingWithRedLight\" : false,\n" +
            "        \"nextSpeed\" : -1.0\n" +
            "      } ],\n" +
            "      \"sources\" : [ {\n" +
            "        \"id\" : -1,\n" +
            "        \"shape\" : {\n" +
            "          \"x\" : 10.0,\n" +
            "          \"y\" : 6.0,\n" +
            "          \"width\" : 15.0,\n" +
            "          \"height\" : 5.0,\n" +
            "          \"type\" : \"RECTANGLE\"\n" +
            "        },\n" +
            "        \"interSpawnTimeDistribution\" : \"org.vadere.state.scenario.ConstantDistribution\",\n" +
            "        \"distributionParameters\" : [ 1.0 ],\n" +
            "        \"spawnNumber\" : 200,\n" +
            "        \"maxSpawnNumberTotal\" : -1,\n" +
            "        \"startTime\" : 0.0,\n" +
            "        \"endTime\" : 0.0,\n" +
            "        \"spawnAtRandomPositions\" : true,\n" +
            "        \"useFreeSpaceOnly\" : false,\n" +
            "        \"targetIds\" : [ 1 ],\n" +
            "        \"dynamicElementType\" : \"PEDESTRIAN\"\n" +
            "      } ],\n" +
            "      \"dynamicElements\" : [ ],\n" +
            "      \"attributesPedestrian\" : {\n" +
            "        \"radius\" : 0.195,\n" +
            "        \"densityDependentSpeed\" : false,\n" +
            "        \"speedDistributionMean\" : 1.34,\n" +
            "        \"speedDistributionStandardDeviation\" : 0.26,\n" +
            "        \"minimumSpeed\" : 0.3,\n" +
            "        \"maximumSpeed\" : 3.0,\n" +
            "        \"acceleration\" : 2.0\n" +
            "      },\n" +
            "      \"attributesCar\" : null\n" +
            "    }\n" +
            "  }\n" +
            "}";
}
