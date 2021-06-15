package org.vadere.simulator.projects.dataprocessing.datakey;

import org.vadere.simulator.projects.dataprocessing.outputfile.TimestepIdDataOutputFile;

/**
 * @author Simon Rahn
 * Data key for time dependent custom data aggregators when the keys timestep and/or IdDataKey are not enough.
 * If applied to pedestrians ids use TimestepPedestrianIdKey instead.
 */

@OutputFileMap(outputFileClass = TimestepIdDataOutputFile.class)
public class TimestepIdDataKey implements DataKey<TimestepIdDataKey> {
    private final int timestep;
    private final int id; // e.g. scenarioElementId (if id refers to pedestrians, use TimeStepPedestrianIdKey instead)

    public TimestepIdDataKey(int timestep, int id) {
        this.timestep = timestep;
        this.id = id;
    }

    public Integer getTimestep() {
        return timestep;
    }

    public Integer getId() {
        return id;
    }

    @Override
    public int compareTo(TimestepIdDataKey o) {
        int result = Integer.compare(timestep, o.timestep);
        if (result == 0) {
            return Integer.compare(id, o.id);
        }
        return result;
    }

    public static String[] getHeaders() {
        return new String[] { TimestepKey.getHeader(), IdDataKey.getHeaders()[0] };
    }

    @Override
    public String toString() {
        return "TimestepIdDataKey{" +
                "timestep=" + timestep +
                ", id=" + id +
                '}';
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + id;
        result = prime * result + timestep;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        TimestepIdDataKey other = (TimestepIdDataKey) obj;
        if (id != other.id)
            return false;
        if (timestep != other.timestep)
            return false;
        return true;
    }
}
