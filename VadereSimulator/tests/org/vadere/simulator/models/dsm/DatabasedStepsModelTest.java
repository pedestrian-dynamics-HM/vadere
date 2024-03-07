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


public class DatabasedStepsModelTest {

    static final String TRAJECTORY_FILE_NAME = "postvis.traj";

    String modelDSM = """
            "mainModel" : "org.vadere.simulator.models.dsm.DatabasedStepsModel",
            "attributesModel" : {
              "org.vadere.state.attributes.models.AttributesDSM" : {
                "trajotoryFile" : "%s",
                "submodels" : [ ],
                "bufferedLines" : 100
              }
            },""";

    @TempDir
    Path testDir;
    @TempDir
    Path outputDir;

    @Test
    void testOptimalStepsModel() throws IOException {
        testModel(Path.of("./tests/org/vadere/simulator/models/dsm/scenarios/osm.scenario"));
    }

    @Test
    void testSocialForceModel() throws IOException {
        testModel(Path.of("./tests/org/vadere/simulator/models/dsm/scenarios/sfm.scenario"));
    }

    @Test
    void testGradientNavigationModel() throws IOException {
        testModel(Path.of("./tests/org/vadere/simulator/models/dsm/scenarios/gnm.scenario"));
    }

//    @Test
//    void testBehaviouralHeuristicsModel() throws IOException {
//        testModel(Path.of("./tests/org/vadere/simulator/models/dsm/scenarios/bhm.scenario"));
//    }

    void testModel(Path scenarioPath) throws IOException {
        String scenarioString = Files.readString(scenarioPath);

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

        assertEquals(expected, actual);
    }
}