package org.vadere.simulator.projects.dataprocessing.datakey;

import org.jetbrains.annotations.NotNull;
import org.vadere.simulator.projects.dataprocessing.outputfile.TimeGridOutputFile;

import java.util.Objects;

@OutputFileMap(outputFileClass = TimeGridOutputFile.class)
public class TimeGridKey implements DataKey<TimeGridKey> {

    private final int timestep;
    private final double x;
    private final double y;
    private final double size;

    public TimeGridKey(int timestep, double x, double y, double size) {
        this.timestep = timestep;
        this.x = x;
        this.y = y;
        this.size = size;
    }

    public int getTimestep() {
        return timestep;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getSize() {
        return size;
    }

    public static String[] getHeaders() {
        return new String[] { TimestepKey.getHeader(), "x", "y", "size"};
    }

    public String[] toStrings(){
        return new String[]{Integer.toString(timestep), Double.toString(x), Double.toString(y), Double.toString(size)};
    }

    @Override
    public int compareTo(@NotNull TimeGridKey other) {
        int ret;
        if ((ret = Integer.compare(timestep, other.timestep))==0){
            if ((ret = Double.compare(x, other.x)) == 0){
                if ((ret = Double.compare(y, other.y)) == 0){
                    if ((ret = Double.compare(size, other.size)) == 0){
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TimeGridKey that = (TimeGridKey) o;
        return timestep == that.timestep &&
                Double.compare(that.x, x) == 0 &&
                Double.compare(that.y, y) == 0 &&
                Double.compare(that.size, size) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestep, x, y, size);
    }
}
