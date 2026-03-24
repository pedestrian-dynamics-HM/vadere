package org.vadere.simulator.models.airflow;

import org.vadere.annotation.factories.models.ModelClass;
import org.vadere.simulator.models.Model;
import org.vadere.simulator.projects.Domain;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.models.airflow.AttributesAirFlowModel;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.AirFlow;
import org.vadere.state.util.StateJsonConverter;
import org.vadere.util.logging.Logger;

import java.io.*;
import java.util.List;
import java.util.Random;
import java.util.Map;

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

        File scenarioPath = new File(airFlow.getScenarioPath());
        File cacheDir = new File(scenarioPath.getParent(), "cache");
        String scenarioName = scenarioPath.getName().replaceFirst("\\.scenario$", "");

        File f_x_velocity = new File(cacheDir, scenarioName + "_" + hash + X_VELOCITY_FILE_ENDING);
        File f_y_velocity = new File(cacheDir, scenarioName + "_" + hash + Y_VELOCITY_FILE_ENDING);

        // To make sure suq controller doesn't compute airflow multiple times for same scenario: 
        // If files don't exist with exact name, try finding files with alternative pattern but same hash
        if(!(f_x_velocity.exists() && !f_x_velocity.isDirectory()) && !(f_y_velocity.exists() && !f_y_velocity.isDirectory())) {
            File[] alternativeFiles = findAlternativeVelocityFiles(hash, cacheDir);
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
            airFlow.setRectangularGridCellSize(attributesAirFlowModel.getRectangularGridCellSize());
            airFlow.setPeriod(attributesAirFlowModel.getOnPeriod(), attributesAirFlowModel.getOffPeriod());
            airFlow.setBlockingObstaclesIDs(attributesAirFlowModel.getBlockingObstacles());

        } catch (IllegalArgumentException e) {
            calculateAirFlow(hash);

            try {
                airFlow.setX_velocity(readArrayFromFile(f_x_velocity.getAbsolutePath()));
                airFlow.setY_velocity(readArrayFromFile(f_y_velocity.getAbsolutePath()));
                airFlow.setRectangularGridCellSize(attributesAirFlowModel.getRectangularGridCellSize());
                airFlow.setPeriod(attributesAirFlowModel.getOnPeriod(), attributesAirFlowModel.getOffPeriod());
                airFlow.setBlockingObstaclesIDs(attributesAirFlowModel.getBlockingObstacles());

            } catch (IOException ex) {
                logger.error("Error reading airflow matrices: {}", e.getMessage());
            }
        } catch (IOException e) {
            logger.error("Error reading airflow matrices: {}", e.getMessage());
        }
    }


    private File[] findAlternativeVelocityFiles(String hash, File cacheDir) {
        if (cacheDir == null || !cacheDir.exists()) {
            return null;
        }
        String basePath = cacheDir.getAbsolutePath();
        
        File[] files = cacheDir.listFiles((dir, name) ->
            name.matches("\\d+_\\d+_" + hash + X_VELOCITY_FILE_ENDING));
        
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
            File condaFile = new File(attributesAirFlowModel.getCondaPath());
            if (!condaFile.exists() || !condaFile.canExecute()) {
                throw new RuntimeException("Conda executable not found or not executable: " + attributesAirFlowModel.getCondaPath() +
                        ". Follow instructions on VadereSimulator/src/org/vadere/simulator/models/airflow/python/README.md for setting up a conda environment.");
            }

            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command(attributesAirFlowModel.getCondaPath(), "run", "-n", attributesAirFlowModel.getCondaEnv(),
                    "python", attributesAirFlowModel.getPythonPath(), airFlow.getScenarioPath(), hash
            );

            // Clean environment to prevent conda nesting conflicts
            // (e.g., when Vadere is launched from within another conda env or IDE)
            Map<String, String> env = processBuilder.environment();
            env.keySet().removeIf(key -> key.startsWith("CONDA_") || key.equals("PYTHONPATH"));

            // Ensure the directory containing the conda binary is on PATH
            String condaBinDir = condaFile.getParent();
            if (condaBinDir != null) {
                env.put("PATH", condaBinDir + File.pathSeparator + env.getOrDefault("PATH", ""));
            }
            Process process = processBuilder.start();

            System.out.println(attributesAirFlowModel.getCondaPath() + " run -n " + attributesAirFlowModel.getCondaEnv() + " " +
                    "python " + attributesAirFlowModel.getPythonPath() + " " + airFlow.getScenarioPath() + " " + hash);

            int exitCode = process.waitFor();
            logger.info("Finished python script with exitCode: {}", exitCode);
            if (exitCode != 0) {
                throw new RuntimeException("Python script returned non-zero exit code: " + exitCode);
            }
        } catch (InterruptedException | IOException e) {
            logger.error(e.getMessage());
        }
    }

    private double[][] readArrayFromFile(String filename) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String header = reader.readLine();
            String[] split = header.substring(2).split("_");
            int x_dim = Integer.parseInt(split[0]);
            int y_dim = Integer.parseInt(split[1]);

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
}