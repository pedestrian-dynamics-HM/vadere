package org.vadere.simulator.models.airflow;

import org.vadere.simulator.models.Model;
import org.vadere.simulator.projects.Domain;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.models.airflow.AttributesAirFlowModel;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.AirFlow;
import org.vadere.util.logging.Logger;

import java.io.*;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class AirFlowModel implements Model {

    private static final Logger logger = Logger.getLogger(AirFlow.class);

    // private static final String CONDA_EXE = "/opt/miniconda3/bin/conda";
    // private static final String CONDA_ENV = "shk";

    private static final String X_VELOCITY_FILE_ENDING = "_U.txt";
    private static final String Y_VELOCITY_FILE_ENDING = "_V.txt";

    private AttributesAirFlowModel attributesAirFlowModel;

    private AirFlow airFlow;

    @Override
    public void initialize(List<Attributes> attributesList, Domain domain, AttributesAgent attributesPedestrian, Random random) {
        this.attributesAirFlowModel = Model.findAttributes(attributesList, AttributesAirFlowModel.class);
        this.airFlow = domain.getTopography().getAirFlow();
    }

    @Override
    public void preLoop(double simTimeInSec) {
        File f_x_velocity = new File(airFlow.getScenarioPath() + X_VELOCITY_FILE_ENDING);
        File f_y_velocity = new File(airFlow.getScenarioPath() + Y_VELOCITY_FILE_ENDING);
        if(!(f_x_velocity.exists() && !f_x_velocity.isDirectory()) && !(f_y_velocity.exists() && !f_y_velocity.isDirectory())) {
            calculateAirFlow(airFlow.getScenarioPath());
        }
        init(airFlow.getScenarioPath());
    }

    @Override
    public void postLoop(double simTimeInSec) {
        // ignore
    }

    @Override
    public void update(double simTimeInSec) {
        // ignore
    }

    public void init(String scenarioName) {
        try {
            airFlow.setX_velocity(readArrayFromFIle(scenarioName + X_VELOCITY_FILE_ENDING));
            airFlow.setY_velocity(readArrayFromFIle(scenarioName + Y_VELOCITY_FILE_ENDING));
            airFlow.setGridSize(attributesAirFlowModel.getGridSize());
        } catch (IOException e) {
            logger.error("Error reading airflow matrices: {}", e.getMessage());
        }
    }

    private void calculateAirFlow(String scenarioFile) {
        logger.info("Running python script for calculating airflow");
        try {
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command(attributesAirFlowModel.getCondaPath(),  "run", "-n", attributesAirFlowModel.getCondaEnv(),
                    "python", "poisson_fem_v2.py", scenarioFile, Double.toString(attributesAirFlowModel.getGridSize()));
            Process process = processBuilder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            logger.info(reader.lines().collect(Collectors.toList()));
            int exitCode = process.waitFor();
            logger.info("Finished python Script with exitCode: {}", exitCode);
        } catch (InterruptedException | IOException e) {
            logger.error(e.getMessage());
        }
    }

    private double[][] readArrayFromFIle(String filename) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(filename));

        String header = reader.readLine();
        String[] split = header.split(" ");
        int x_dim = Integer.parseInt(split[1]);
        int y_dim = Integer.parseInt(split[2]);

        double[][] result = new double[x_dim][y_dim];

        for (int i = 0; i < x_dim; i++) {
            String line = reader.readLine();
            String[] lineSplit = line.split(" ");
            for (int j = 0; j < y_dim; j++) {
                try {
                    result[i][j] = Double.parseDouble(lineSplit[j]);
                } catch (NumberFormatException ignored) {}
            }
        }
        return result;
    }
}
