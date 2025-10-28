package org.vadere.simulator.models.dsm;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class TrajectoryBufferTest {

    final String trajectoryFileValidData = """
            pedestrianId simTime endTime-PID1 startX-PID1 startY-PID1 endX-PID1 endY-PID1 targetId-PID2
            1 0.0 0.6545002537605311 2.103 8.103 2.15447399660384 7.32698625668509 4
            2 0.0 0.5172823125786546 8.103 8.103 7.787126377750859 7.365961548085342 3
            2 0.5172823125786546 1.0345646251573093 7.787126377750859 7.365961548085342 7.223836811295502 6.739683559240709 3
            1 0.6545002537605311 1.3090005075210622 2.15447399660384 7.32698625668509 2.296672663575293 6.562377571636631 4
            3 0.8 1.324716125838843 8.103 8.103 7.520725931908418 7.62096382501538 3""";

    final String trajectoryFileEmptyData = """
            pedestrianId simTime endTime-PID1 startX-PID1 startY-PID1 endX-PID1 endY-PID1 targetId-PID2""";

    @TempDir
    Path testTrajectoryFilePath;

    @Test
    void testWithValidTrajectoryFile() throws IOException {
        final Path testTrajectoryFile = Files.createFile(testTrajectoryFilePath.resolve("postvis.traj"));
        Files.writeString(testTrajectoryFile, trajectoryFileValidData);

        TrajectoryBuffer trajectoryBuffer = new TrajectoryBuffer(testTrajectoryFile.toAbsolutePath().toString(), 2);
        trajectoryBuffer.read();

        DSMStep step;
        StringBuilder actualBuilder = new StringBuilder();

        while ((step = trajectoryBuffer.getNextStep()) != null) {
            actualBuilder.append(step).append("\n");
            trajectoryBuffer.removeStep();
        }

        // close inputStream on file
        trajectoryBuffer.closeReader();

        String expected = trajectoryFileValidData.substring(trajectoryFileValidData.indexOf('\n') + "\n".length());
        String actual = actualBuilder.toString().stripTrailing();

        assertEquals(expected, actual);
    }

    @Test
    void testWithNoneExistingTrajectoryFile() {
        assertDoesNotThrow(() -> new TrajectoryBuffer("non.existing.file", 10));
    }

    @Test
    void testWithEmptyTrajectoryFile() throws IOException {
        final Path testTrajectoryFile = Files.createFile(testTrajectoryFilePath.resolve("postvis.traj"));
        Files.writeString(testTrajectoryFile, trajectoryFileEmptyData);

        TrajectoryBuffer trajectoryBuffer = new TrajectoryBuffer(testTrajectoryFile.toAbsolutePath().toString(), 10);
        trajectoryBuffer.read();

        assertNull(trajectoryBuffer.getNextStep());

        // close inputStream on file
        trajectoryBuffer.closeReader();
    }
}
