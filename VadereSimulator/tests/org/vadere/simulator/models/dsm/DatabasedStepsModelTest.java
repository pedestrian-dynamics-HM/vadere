package org.vadere.simulator.models.dsm;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.vadere.simulator.control.simulation.ScenarioRun;
import org.vadere.simulator.entrypoints.ScenarioFactory;
import org.vadere.simulator.projects.Scenario;
import org.vadere.simulator.utils.cache.ScenarioCache;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


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
}