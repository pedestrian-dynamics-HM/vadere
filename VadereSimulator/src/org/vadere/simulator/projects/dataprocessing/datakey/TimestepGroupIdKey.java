package org.vadere.simulator.projects.dataprocessing.datakey;

import org.vadere.simulator.projects.dataprocessing.outputfile.TimestepGroupIdOutputFile;

/**
 * @author Manuel Hertle
 *
 */

@OutputFileMap(outputFileClass = TimestepGroupIdOutputFile.class)
public class TimestepGroupIdKey implements DataKey<TimestepGroupIdKey> {
    private final int timestep;
    private final int groupId;

    public TimestepGroupIdKey(int timestep, int groupId) {
        this.timestep = timestep;
        this.groupId = groupId;
    }

    public Integer getTimestep() {
        return timestep;
    }

    public Integer getGroupId() {
        return groupId;
    }

    @Override
    public int compareTo(TimestepGroupIdKey o) {
        int result = Integer.compare(timestep, o.timestep);
        if (result == 0) {
            return Integer.compare(groupId, o.groupId);
        }
        return result;
    }

    public static String[] getHeaders() {
        return new String[] { TimestepKey.getHeader(), "groupId" };
    }

    @Override
    public String toString() {
        return "TimestepGroupIdKey{" +
                "timestep=" + timestep +
                ", groupId=" + groupId +
                '}';
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + groupId;
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
        TimestepGroupIdKey other = (TimestepGroupIdKey) obj;
        if (groupId != other.groupId)
            return false;
        if (timestep != other.timestep)
            return false;
        return true;
    }
}
