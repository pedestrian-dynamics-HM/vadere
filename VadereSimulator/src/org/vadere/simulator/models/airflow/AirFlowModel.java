package org.vadere.simulator.models.airflow;

import org.vadere.simulator.models.Model;
import org.vadere.simulator.projects.Domain;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.models.airflow.AttributesAirFlowModel;
import org.vadere.state.attributes.models.airflow.AttributesInOutLet;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.AirFlow;
import org.vadere.util.logging.Logger;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class AirFlowModel extends AbstractAirFlowModel {

    private static final Logger logger = Logger.getLogger(AirFlow.class);

    protected static final String X_VELOCITY_FILE_ENDING = "_Vx.txt";
    protected static final String Y_VELOCITY_FILE_ENDING = "_Vy.txt";

    protected AttributesAirFlowModel attributesAirFlowModel;


    @Override
    public void initialize(List<Attributes> attributesList, Domain domain, AttributesAgent attributesPedestrian, Random random) {
        super.initialize(attributesList, domain, attributesPedestrian, random);
        this.attributesAirFlowModel = Model.findAttributes(attributesList, AttributesAirFlowModel.class);
    }

    @Override
    public void setupAirFlow() {
        File f_x_velocity = new File(airFlow.getScenarioPath() + "_" +  airFlow.getScenarioHash() + X_VELOCITY_FILE_ENDING);
        File f_y_velocity = new File(airFlow.getScenarioPath() + "_" +  airFlow.getScenarioHash() + Y_VELOCITY_FILE_ENDING);
        if(!(f_x_velocity.exists() && !f_x_velocity.isDirectory()) && !(f_y_velocity.exists() && !f_y_velocity.isDirectory())) {
            calculateAirFlow();
        }
        try {
            airFlow.setX_velocity(readArrayFromFile(airFlow.getScenarioPath() + "_" +  airFlow.getScenarioHash() + X_VELOCITY_FILE_ENDING));
            airFlow.setY_velocity(readArrayFromFile(airFlow.getScenarioPath() + "_" +  airFlow.getScenarioHash() + Y_VELOCITY_FILE_ENDING));
            airFlow.setGridSize(attributesAirFlowModel.getGridSize());
        } catch (IOException e) {
            logger.error("Error reading airflow matrices: {}", e.getMessage());
        }
    }

    protected void calculateAirFlow() {
        logger.info("Running python script for calculating airflow");
        try {
            ProcessBuilder processBuilder = new ProcessBuilder();
            System.out.println(attributesAirFlowModel.getCondaPath() + " " +  "run" + " " + "-n" + " " + attributesAirFlowModel.getCondaEnv() + " " +
                    "python" + " " + "VadereSimulator/src/org/vadere/simulator/models/airflow/python/scikit-fem_stokes_flow_v3.py" + " " + airFlow.getScenarioPath() + " " + airFlow.getScenarioHash());
            processBuilder.command(attributesAirFlowModel.getCondaPath(),  "run", "-n", attributesAirFlowModel.getCondaEnv(),
                    "python", "VadereSimulator/src/org/vadere/simulator/models/airflow/python/scikit-fem_stokes_flow_v3.py", airFlow.getScenarioPath(), airFlow.getScenarioHash()
            );
            Process process = processBuilder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            logger.info(reader.lines().collect(Collectors.toList()));
            int exitCode = process.waitFor();
            logger.info("Finished python Script with exitCode: {}", exitCode);
        } catch (InterruptedException | IOException e) {
            logger.error(e.getMessage());
        }
    }

    private double[][] readArrayFromFile(String filename) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(filename));

        String expectedParameters = getAttributesString(attributesAirFlowModel);
        System.out.println(expectedParameters);

        String header = reader.readLine();
        String[] split = header.split(" ");
        int x_dim = Integer.parseInt(split[1]);
        int y_dim = Integer.parseInt(split[2]);

        if (!expectedParameters.equals(split[3])) {
            System.out.println("Wrong parameter ---------------------------------------------------");
        }

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

    private String getAttributesString(AttributesAirFlowModel attributes) {
        String result = "";
        result += attributes.getGridSize();
        result += "-" + attributes.getAreaThreshold();
        result += "-" + attributes.getInletVelocity() + "-";
        for (AttributesInOutLet inlet : attributes.getInlets()) {
            result += inlet.getSide() + "[" + inlet.getStart() + "," + inlet.getEnd() + "]";
        }
        result += "-";
        for (AttributesInOutLet outlet : attributes.getOutlets()) {
            result += outlet.getSide() + "[" + outlet.getStart() + "," + outlet.getEnd() + "]";
        }
        result += "-" + Arrays.toString(attributes.getNotBlockingObstacles().toArray(new Integer[0]));
        return result;
    }
}
