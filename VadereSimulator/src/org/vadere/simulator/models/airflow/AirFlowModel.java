package org.vadere.simulator.models.airflow;

import org.vadere.annotation.factories.models.ModelClass;
import org.vadere.simulator.models.Model;
import org.vadere.simulator.projects.Domain;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.models.airflow.AttributesAirFlowModel;
import org.vadere.state.attributes.models.airflow.AttributesInOutLet;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.AirFlow;
import org.vadere.state.util.StateJsonConverter;
import org.vadere.util.logging.Logger;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

@ModelClass
public class AirFlowModel extends AbstractAirFlowModel {

    private static final Logger logger = Logger.getLogger(AirFlow.class);

    protected static final String X_VELOCITY_FILE_ENDING = "_Vx.txt";
    protected static final String Y_VELOCITY_FILE_ENDING = "_Vy.txt";

    protected AttributesAirFlowModel attributesAirFlowModel;


    @Override
    public void initialize(List<Attributes> attributesList, Domain domain, AttributesAgent attributesPedestrian, Random random) {
        super.initialize(attributesList, domain, attributesPedestrian, random);
        this.attributesAirFlowModel = Model.findAttributes(attributesList, AttributesAirFlowModel.class);
        
        String hash = StateJsonConverter.getAirFlowHash(domain.getTopography(), attributesAirFlowModel);
        airFlow.setAirflowHash(hash);
    }

    @Override
    public void setupAirFlow() {
        String hash = airFlow.getAirflowHash();
        
        File f_x_velocity = new File(airFlow.getScenarioPath() + "_" + hash + X_VELOCITY_FILE_ENDING);
        File f_y_velocity = new File(airFlow.getScenarioPath() + "_" + hash + Y_VELOCITY_FILE_ENDING);

        // To make sure suq controller doesn't compute airflow multiple times for same scenario: 
        // If files don't exist with exact name, try finding files with alternative pattern
        if(!(f_x_velocity.exists() && !f_x_velocity.isDirectory()) && !(f_y_velocity.exists() && !f_y_velocity.isDirectory())) {
            File[] alternativeFiles = findAlternativeVelocityFiles(hash);
            if (alternativeFiles != null) {
                f_x_velocity = alternativeFiles[0];
                f_y_velocity = alternativeFiles[1];
            } else {
                calculateAirFlow(hash);
            }
        }

        try {
            airFlow.setX_velocity(readArrayFromFile(f_x_velocity.getAbsolutePath()));
            airFlow.setY_velocity(readArrayFromFile(f_y_velocity.getAbsolutePath()));
            airFlow.setGridSize(attributesAirFlowModel.getGridSize());
            airFlow.setPeriod(attributesAirFlowModel.getOnPeriod(), attributesAirFlowModel.getOffPeriod());
            airFlow.setBlockingObstaclesIDs(attributesAirFlowModel.getBlockingObstacles());

        } catch (IllegalArgumentException e) {
            calculateAirFlow(hash);

            try {
                airFlow.setX_velocity(readArrayFromFile(f_x_velocity.getAbsolutePath()));
                airFlow.setY_velocity(readArrayFromFile(f_y_velocity.getAbsolutePath()));
                airFlow.setGridSize(attributesAirFlowModel.getGridSize());
                airFlow.setPeriod(attributesAirFlowModel.getOnPeriod(), attributesAirFlowModel.getOffPeriod());
                airFlow.setBlockingObstaclesIDs(attributesAirFlowModel.getBlockingObstacles());

            } catch (IOException ex) {
                logger.error("Error reading airflow matrices: {}", e.getMessage());
            }
        } catch (IOException e) {
            logger.error("Error reading airflow matrices: {}", e.getMessage());
        }
    }


    private File[] findAlternativeVelocityFiles(String hash) {
        File scenarioFile = new File(airFlow.getScenarioPath());
        File parentDir = scenarioFile.getParentFile();
        if (parentDir == null || !parentDir.exists()) {
            return null;
        }

        String basePath = parentDir.getAbsolutePath();
        
        File[] files = parentDir.listFiles((dir, name) -> 
            name.matches("\\d+_\\d+\\.scenario_" + hash + X_VELOCITY_FILE_ENDING));
        
        if (files == null || files.length == 0) {
            return null;
        }

        for (File x_velocity : files) {
            String y_velocity_name = x_velocity.getName().replace(X_VELOCITY_FILE_ENDING, Y_VELOCITY_FILE_ENDING);
            File y_velocity = new File(basePath + File.separator + y_velocity_name);

            if (y_velocity.exists() && !y_velocity.isDirectory()) {
                return new File[]{x_velocity, y_velocity};
            }
        }
        return null;
    }

    protected void calculateAirFlow(String hash) {
        logger.info("Running python script for calculating airflow");
        try {
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command(attributesAirFlowModel.getCondaPath(),  "run", "-n", attributesAirFlowModel.getCondaEnv(),
                    "python", attributesAirFlowModel.getPythonPath(), airFlow.getScenarioPath(), hash
            );
            Process process = processBuilder.start();

            System.out.println(attributesAirFlowModel.getCondaPath() + " " +  "run" + " " + "-n" + " " + attributesAirFlowModel.getCondaEnv() + " " +
                    "python" + " " + attributesAirFlowModel.getPythonPath() + " " + airFlow.getScenarioPath() + " " + hash);

            // BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            // logger.info(reader.lines().collect(Collectors.toList()));

            int exitCode = process.waitFor();
            logger.info("Finished python script with exitCode: {}", exitCode);
        } catch (InterruptedException | IOException e) {
            logger.error(e.getMessage());
        }
    }

    private double[][] readArrayFromFile(String filename) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(filename));

        String expectedParameters = getAttributesString(attributesAirFlowModel);

        String header = reader.readLine();
        String[] split = header.substring(2).split("_");
        int x_dim = Integer.parseInt(split[0]);
        int y_dim = Integer.parseInt(split[1]);

        if (!expectedParameters.equals(split[2])) {
            throw new IllegalArgumentException("Wrong parameters for airflow");
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

    public static String getAttributesString(AttributesAirFlowModel attributes) {
        StringBuilder result = new StringBuilder();
        result.append(attributes.getGridSize()).append("-");
        result.append(attributes.getAreaThreshold()).append("-");
        result.append(attributes.getInletVelocity()).append("-");

        for (AttributesInOutLet inlet : attributes.getInlets()) {
            result.append(inlet.getSide()).append("[").append(inlet.getStart()).append(",").append(inlet.getEnd()).append("]");
        }
        result.append("-");

        for (AttributesInOutLet outlet : attributes.getOutlets()) {
            result.append(outlet.getSide()).append("[").append(outlet.getStart()).append(",").append(outlet.getEnd()).append("]");
        }
        result.append("-");

        result.append(Arrays.toString(attributes.getBlockingObstacles().toArray(new Integer[0])));
        return result.toString();
    }
}
