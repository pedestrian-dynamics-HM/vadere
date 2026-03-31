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

        File xVelocityFile = new File(cacheDir, scenarioName + "_" + hash + X_VELOCITY_FILE_ENDING);
        File yVelocityFile = new File(cacheDir, scenarioName + "_" + hash + Y_VELOCITY_FILE_ENDING);

        // To make sure suq controller doesn't compute airflow multiple times for same scenario: 
        // If files don't exist with exact name, try finding files with alternative pattern but same hash
        if(!(xVelocityFile.exists() && !xVelocityFile.isDirectory()) && !(yVelocityFile.exists() && !yVelocityFile.isDirectory())) {
            File[] alternativeFiles = findAlternativeVelocityFiles(hash, cacheDir);
            if (alternativeFiles != null) {
                xVelocityFile = alternativeFiles[0];
                yVelocityFile = alternativeFiles[1];
            } else {
                calculateAirFlow(hash);
            }
        }

        try {
            setupVelocityFromFile(xVelocityFile, yVelocityFile);
        } catch (IllegalArgumentException e) {
            calculateAirFlow(hash);
            try {
                setupVelocityFromFile(xVelocityFile, yVelocityFile);

            } catch (IOException ex) {
                logger.error("Error reading airflow matrices: {}", ex.getMessage());
            }
        } catch (IOException e) {
            logger.error("Error reading airflow matrices: {}", e.getMessage());
        }
    }


    private void setupVelocityFromFile(File xVelocityFile, File yVelocityFile) throws IOException, IllegalArgumentException {
        airFlow.setX_velocity(readArrayFromFile(xVelocityFile.getAbsolutePath()));
        airFlow.setY_velocity(readArrayFromFile(yVelocityFile.getAbsolutePath()));
        airFlow.setRectangularGridCellSize(attributesAirFlowModel.getRectangularGridCellSize());
        airFlow.setPeriod(attributesAirFlowModel.getOnPeriod(), attributesAirFlowModel.getOffPeriod());
        airFlow.setBlockingObstaclesIDs(attributesAirFlowModel.getBlockingObstacles());
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
            logger.info("Executing command: {} run -n {} python {} {} {}",
                    attributesAirFlowModel.getCondaPath(),
                    attributesAirFlowModel.getCondaEnv(),
                    attributesAirFlowModel.getPythonPath(),
                    airFlow.getScenarioPath(),
                    hash
            );

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
            if (header == null) {
                throw new IOException("Velocity file is empty: " + filename);
            }
            String[] split = header.substring(2).split("_");
            int x_dim;
            int y_dim;
            try {
                x_dim = Integer.parseInt(split[0]);
                y_dim = Integer.parseInt(split[1]);
            } catch (NumberFormatException e) {
                throw new IOException("Header dimensions must be valid integers in file: " + filename, e);
            }

            double[][] result = new double[x_dim][y_dim];

            for (int row = 0; row < x_dim; row++) {
                String line = reader.readLine();
                if (line == null) {
                    throw new IOException("Unexpected end of file at row " + row + " in: " + filename);
                }
                String[] lineSplit = line.split(" ");
                for (int column = 0; column < y_dim; column++) {
                    try {
                        result[row][column] = Double.parseDouble(lineSplit[column]);
                    } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                        throw new IOException("Corrupted data at row " + row + ", column " + column + " in: " + filename, e);
                    }
                }
            }
            return result;
        } catch (IndexOutOfBoundsException | NumberFormatException e) {
            throw new IOException("Malformed file format in: " + filename, e);
        }
    }
}