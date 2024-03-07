package org.vadere.simulator.models.dsm;

import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.logging.Logger;

import java.io.*;
import java.util.*;

/**
 * @author Kevin Becker
 */
public class TrajectoryBuffer {

    private final Logger logger = Logger.getLogger(TrajectoryBuffer.class);
    private final int bufferedLines;
    private BufferedReader reader;
    private final LinkedList<DSMStep> trajectoryBuffer;

    public TrajectoryBuffer(String trajectoryPath, int bufferedLines) {
        this.bufferedLines = bufferedLines;
        trajectoryBuffer = new LinkedList<>();
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(trajectoryPath)));
            reader.readLine();
        } catch (IOException e) {
            logger.error("Error reading trajectoryFile on init: {}", e.getMessage());
        }
    }

    protected void read() {
        try {
            String line;
            while (trajectoryBuffer.size() < bufferedLines && (line = reader.readLine()) != null) {
                String[] values = line.split(" ");
                int pedestrianId = Integer.parseInt(values[0]);
                double startTime = Double.parseDouble(values[1]);
                double endTime = Double.parseDouble(values[2]);
                VPoint startPosition = new VPoint(Double.parseDouble(values[3]), Double.parseDouble(values[4]));
                VPoint endPosition = new VPoint(Double.parseDouble(values[5]), Double.parseDouble(values[6]));
                int targetId = Integer.parseInt(values[7]);

                trajectoryBuffer.add(new DSMStep(pedestrianId, startTime, endTime, startPosition, endPosition, targetId));
            }
        } catch (IOException e) {
            logger.error("Error reading trajectoryFile: {}", e.getMessage());
        }
    }

    protected DSMStep getNextStep() {
        if (trajectoryBuffer.isEmpty()) return null;
        return trajectoryBuffer.getFirst();
    }

    protected void removeStep() {
        trajectoryBuffer.remove();
        if (trajectoryBuffer.isEmpty()) read();
    }

    protected void closeReader() {
        try {
            reader.close();
        } catch (IOException e) {
            logger.error("Error closing trajectoryBuffer: {}", e.getMessage());
        }
    }
}
