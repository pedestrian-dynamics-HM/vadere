package org.vadere.simulator.models.dsm;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.vadere.simulator.control.simulation.ScenarioRun;
import org.vadere.simulator.entrypoints.ScenarioFactory;
import org.vadere.simulator.projects.Domain;
import org.vadere.simulator.projects.Scenario;
import org.vadere.simulator.utils.cache.ScenarioCache;
import org.vadere.state.attributes.models.AttributesDSM;
import org.vadere.state.scenario.Topography;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;


public class DatabasedStepsModelTest {

    static final String TRAJECTORY_FILE_NAME = "postvis.traj";

    String modelDSM = """
            "mainModel" : "org.vadere.simulator.models.dsm.DatabasedStepsModel",
            "attributesModel" : {
              "org.vadere.state.attributes.models.AttributesDSM" : {
                "trajectoryFileOrFolder" : "%s",
                "submodels" : [ ],
                "bufferedLines" : 1000
              }
            },""";

    String modelDSMwithFallbackModel = """
            "mainModel" : "org.vadere.simulator.models.dsm.DatabasedStepsModel",
            "attributesModel" : {
              "org.vadere.state.attributes.models.AttributesDSM" : {
                  "trajectoryFileOrFolder" : "%s",
                  "bufferedLines" : 1000,
                  "submodels" : [ ],
                  "fallbackMainModel" : "org.vadere.simulator.models.osm.OptimalStepsModel",
                  "attributesFallbackModel" : {
                    "org.vadere.state.attributes.models.AttributesOSM" : {
                      "stepCircleResolution" : 4,
                      "numberOfCircles" : 1,
                      "optimizationType" : "NELDER_MEAD",
                      "varyStepDirection" : true,
                      "movementType" : "ARBITRARY",
                      "stepLengthIntercept" : 0.4625,
                      "stepLengthSlopeSpeed" : 0.2345,
                      "stepLengthSD" : 0.036,
                      "movementThreshold" : 0.0,
                      "minStepLength" : 0.1,
                      "minimumStepLength" : true,
                      "maxStepDuration" : 1.7976931348623157E308,
                      "dynamicStepLength" : true,
                      "updateType" : "EVENT_DRIVEN",
                      "seeSmallWalls" : false,
                      "targetPotentialModel" : "org.vadere.simulator.models.potential.fields.PotentialFieldTargetGrid",
                      "pedestrianPotentialModel" : "org.vadere.simulator.models.potential.PotentialFieldPedestrianCompactSoftshell",
                      "obstaclePotentialModel" : "org.vadere.simulator.models.potential.PotentialFieldObstacleCompactSoftshell",
                      "submodels" : [ "org.vadere.simulator.models.infection.AirTransmissionModel" ]
                    },
                    "org.vadere.state.attributes.models.AttributesPotentialCompactSoftshell" : {
                      "pedPotentialIntimateSpaceWidth" : 0.45,
                      "pedPotentialPersonalSpaceWidth" : 1.2,
                      "pedPotentialHeight" : 50.0,
                      "obstPotentialWidth" : 0.8,
                      "obstPotentialHeight" : 6.0,
                      "intimateSpaceFactor" : 1.2,
                      "personalSpacePower" : 1,
                      "intimateSpacePower" : 1
                    },
                    "org.vadere.state.attributes.models.AttributesFloorField" : {
                      "createMethod" : "HIGH_ACCURACY_FAST_MARCHING",
                      "potentialFieldResolution" : 0.1,
                      "obstacleGridPenalty" : 0.1,
                      "targetAttractionStrength" : 1.0,
                      "cacheType" : "NO_CACHE",
                      "cacheDir" : "",
                      "timeCostAttributes" : {
                        "standardDeviation" : 0.7,
                        "type" : "UNIT",
                        "obstacleDensityWeight" : 3.5,
                        "pedestrianSameTargetDensityWeight" : 3.5,
                        "pedestrianOtherTargetDensityWeight" : 3.5,
                        "pedestrianWeight" : 3.5,
                        "queueWidthLoading" : 1.0,
                        "pedestrianDynamicWeight" : 6.0,
                        "loadingType" : "CONSTANT",
                        "width" : 0.2,
                        "height" : 1.0
                      }
                    }
                  }
              }
            },""";

    String processors = """
            "processWriters" : {
              "files" : [{
                "type" : "org.vadere.simulator.projects.dataprocessing.outputfile.EventtimePedestrianIdOutputFile",
                "filename" : "postvis.traj",
                "processors" : [ 1, 2 ]
              }],
              "processors" : [{
                "type" : "org.vadere.simulator.projects.dataprocessing.processor.FootStepProcessor",
                "id" : 1
              }, {
                "type" : "org.vadere.simulator.projects.dataprocessing.processor.FootStepTargetIDProcessor",
                "id" : 2
              }],
              "isTimestamped" : true,
              "isWriteMetaData" : false
            },""";

    @TempDir
    Path testDir;
    @TempDir
    Path outputDir;

    @Test
    void testOptimalStepsModel() throws IOException {
        testModel(Path.of("../Scenarios/ModelTests/TestOSM/scenarios/basic_2_density_pso.scenario"));
    }

    @Test
    void testSocialForceModel() throws IOException {
        testModel(Path.of("../Scenarios/ModelTests/TestSFM/scenarios/basic_1_chicken_sfm1.scenario"));
    }

    @Test
    void testGradientNavigationModel() throws IOException {
        testModel(Path.of("../Scenarios/ModelTests/TestGNM/scenarios/rimea_12_evacuation_gnm1.scenario"));
    }

    String buildScenario(String scenario) {
        String[] split1 = scenario.split("\"processWriters\"");
        String[] split2 = split1[1].split("\"scenario\"");
        return split1[0] + processors + "\"scenario\"" + split2[1];
    }

    void testModel(Path scenarioPath) throws IOException {
        String scenarioString = buildScenario(Files.readString(scenarioPath));

        final Path scenarioFile = Files.createFile(testDir.resolve("scenario.model"));
        Files.writeString(scenarioFile, scenarioString);

        Scenario scenario = ScenarioFactory.createScenarioWithScenarioJson(scenarioString);
        ScenarioCache cache = ScenarioCache.load(scenario, scenarioFile.toAbsolutePath().getParent());
        ScenarioRun run = new ScenarioRun(scenario, outputDir.toAbsolutePath().toString(), null, outputDir.toAbsolutePath(), cache);

        run.run();

        Path outputTrajectoryPath = run.getOutputPath().resolve(TRAJECTORY_FILE_NAME).toAbsolutePath();

        runDSM(scenarioString, outputTrajectoryPath);
    }

    void runDSM(String scenarioString, Path trajectoryFile) throws IOException {
        String myModelDSM = String.format(modelDSM, trajectoryFile.toUri().getPath());

        String[] split = scenarioString.split("\"scenario\"");
        String[] split2 = split[1].split("\"attributesSimulation\"");

        scenarioString = split[0] + "\"scenario\": {\n" + myModelDSM + "\"attributesSimulation\"" + split2[1];

        Path scenarioFile = Files.createFile(testDir.resolve("dsm.scenario"));
        Files.writeString(scenarioFile, scenarioString);

        Scenario scenario = ScenarioFactory.createScenarioWithScenarioJson(scenarioString);
        ScenarioCache cache = ScenarioCache.load(scenario, scenarioFile.toAbsolutePath().getParent());
        ScenarioRun run = new ScenarioRun(scenario, outputDir.toAbsolutePath().toString(), null, outputDir.toAbsolutePath(), cache);

        run.run();

        String expected = Files.readString(trajectoryFile);
        String actual = Files.readString(run.getOutputPath().resolve(TRAJECTORY_FILE_NAME).toAbsolutePath());

        compareTrajectoryFiles(expected, actual);
    }

    void compareTrajectoryFiles(String expected, String actual) {
        List<String> expectedLines = expected.lines().toList();
        List<String> actualLines = actual.lines().toList();

        Set<Integer> spawnedPedestrians = new HashSet<>();

        for (int i=1; i < actualLines.size(); i++) {
            String[] actualLineSplit = actualLines.get(i).split(" ");
            Integer pedId = Integer.parseInt(actualLineSplit[0]);
            if (spawnedPedestrians.contains(pedId)) {
                assertEquals(expectedLines.get(i), actualLines.get(i));
            }
            else {
                spawnedPedestrians.add(pedId);
                String[] expectedLineSplit = actualLines.get(i).split(" ");
                assertEquals(expectedLineSplit[0], actualLineSplit[0]);
                assertEquals(expectedLineSplit[1], actualLineSplit[1]);
                assertEquals(expectedLineSplit[2], actualLineSplit[2]);
                assertEquals(expectedLineSplit[5], actualLineSplit[5]);
                assertEquals(expectedLineSplit[6], actualLineSplit[6]);
                assertEquals(expectedLineSplit[7], actualLineSplit[7]);
            }
        }
    }

    @Test
    public void testCheckIfCanExtractStepsFromFile() throws IOException {
        // Setup - Create temporary directory and files
        Path tempDir = Files.createTempDirectory("test_traj");
        String testHash = "abc123";
        String trajFileName = "postvis_" + testHash + ".traj";
        File trajFileInDir = new File(tempDir.toFile(), trajFileName);
        trajFileInDir.createNewFile();

        File directTrajFile = new File(tempDir.toFile(), "direct.traj");
        directTrajFile.createNewFile();
        AttributesDSM attributesDSM = new AttributesDSM();
        DatabasedStepsModel dsm = new DatabasedStepsModel();
        dsm.setAttributesDSM(attributesDSM);

        try {
            // Case 1: Direct .traj file exists
            attributesDSM.setTrajectoryFileOrFolder(directTrajFile.getAbsolutePath());
            assertTrue(dsm.checkIfCanExtractStepsFromFile(),
                    "Should return true for direct .traj file");

            // Case 2: Directory contains .traj file with matching hash
            attributesDSM.setTrajectoryFileOrFolder(tempDir.toString());
            dsm.setLocomotionHash(testHash);
            assertTrue(dsm.checkIfCanExtractStepsFromFile(),
                    "Should return true when directory contains matching .traj file");
            assertEquals(trajFileInDir.getAbsolutePath(),
                    attributesDSM.getTrajectoryFileOrFolder(),
                    "Should update path to found .traj file");

            // Case 3: Directory exists but .traj file with hash does not exist
            attributesDSM.setTrajectoryFileOrFolder(tempDir.toString());
            dsm.setLocomotionHash("differentHash");
            assertFalse(dsm.checkIfCanExtractStepsFromFile(),
                    "Should return false when directory doesn't contain matching .traj file");

            // Case 4: Path is neither .traj file nor directory
            attributesDSM.setTrajectoryFileOrFolder(tempDir.toString() + "/nonexistent.txt");
            assertThrows(IllegalArgumentException.class,
                    () -> dsm.checkIfCanExtractStepsFromFile(),
                    "Should throw exception when path is neither .traj file nor directory");

            // Case 5: Null path
            attributesDSM.setTrajectoryFileOrFolder(null);
            assertThrows(IllegalArgumentException.class,
                    () -> dsm.checkIfCanExtractStepsFromFile(),
                    "Should throw exception when path is null");

        } finally {
            trajFileInDir.delete();
            directTrajFile.delete();
            Files.delete(tempDir);
        }
    }
}