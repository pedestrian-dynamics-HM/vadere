package org.vadere.simulator.projects.dataprocessing.datakey;


import org.jetbrains.annotations.NotNull;
import org.vadere.simulator.projects.dataprocessing.outputfile.TopographyGridOutputFile;

@OutputFileMap(outputFileClass = TopographyGridOutputFile.class)
public class TopographyGridKey implements DataKey<TopographyGridKey> {

    private final int xId;
    private final int yId;

    public TopographyGridKey(int xId, int yId) {
        this.xId = xId;
        this.yId = yId;
    }

    public int getXId() {
        return xId;
    }

    public int getYId() {
        return yId;
    }

    public static String[] getHeaders() {
        return new String[]{"idx_x", "idx_y"};
    }

    @Override
    public int compareTo(@NotNull TopographyGridKey other) {
        int xComparison = Integer.compare(xId, other.xId);
        if (xComparison != 0) {
            return xComparison;
        }
        int yComparison = Double.compare(yId, other.yId);
        if (yComparison != 0) {
            return yComparison;
        }
        return 0;
    }
}
