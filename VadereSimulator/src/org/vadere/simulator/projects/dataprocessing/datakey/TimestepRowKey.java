package org.vadere.simulator.projects.dataprocessing.datakey;

import org.jetbrains.annotations.NotNull;
import org.vadere.simulator.projects.dataprocessing.outputfile.TimestepRowOutputFile;

/**
 * @author Mario Teixeira Parente
 *
 */

@OutputFileMap(outputFileClass = TimestepRowOutputFile.class)
public class TimestepRowKey implements DataKey<TimestepRowKey> {
    private int timeStep;
    private int row;

    public TimestepRowKey(int timeStep, int row) {
        this.timeStep = timeStep;
        this.row = row;
    }

    public int getTimeStep() {
        return this.timeStep;
    }

    public int getRow() {
        return this.row;
    }

    @Override
    public int compareTo(@NotNull TimestepRowKey o) {
        int result = Integer.compare(this.timeStep, o.timeStep);

        if (result == 0) {
            return Integer.compare(this.row, o.row);
        }

        return result;
    }
}
