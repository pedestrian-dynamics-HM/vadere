package org.vadere.simulator.projects.dataprocessing.datakey;

import org.jetbrains.annotations.NotNull;
import org.vadere.simulator.projects.dataprocessing.outputfile.TimeRectangleGridOutputFile;

import java.util.Objects;

@OutputFileMap(outputFileClass = TimeRectangleGridOutputFile.class)
public class TimeRectangleGridKey implements DataKey<TimeRectangleGridKey> {

    private final double simTime;
    private final double x;
    private final double y;

    private final double width;

    private final double height;

    public TimeRectangleGridKey(double simTime, double x, double y, double width, double height) {
        this.simTime = simTime;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getSimTime() {
        return simTime;
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }

    public static String[] getHeaders() {
        return new String[]{"simTime", "x", "y", "width", "height"};
    }

    public String[] toStrings() {
        return new String[]{Double.toString(simTime), Double.toString(x), Double.toString(y), Double.toString(width),
                Double.toString(height)};
    }

    @Override
    public int compareTo(@NotNull TimeRectangleGridKey other) {
        int ret;
        if ((ret = Double.compare(simTime, other.simTime)) == 0) {
            if ((ret = Double.compare(x, other.x)) == 0) {
                if ((ret = Double.compare(y, other.y)) == 0) {
                    if ((ret = Double.compare(width, other.width)) == 0) {
                        if ((ret = Double.compare(height, other.height)) == 0) {
                            return 0;
                        }
                        return ret;
                    }
                    return ret;
                }
                return ret;
            }
            return ret;
        }
        return ret;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TimeRectangleGridKey that = (TimeRectangleGridKey) o;
        return Double.compare(that.simTime, simTime) == 0 &&
                Double.compare(that.x, x) == 0 &&
                Double.compare(that.y, y) == 0 &&
                Double.compare(that.width, width) == 0 &&
                Double.compare(that.height, height) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(simTime, x, y, width, height);
    }
}
