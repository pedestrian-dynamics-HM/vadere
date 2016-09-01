package org.vadere.simulator.projects.dataprocessing_mtp;

public class TimestepDataKey implements Comparable<TimestepDataKey> {
	private final int timestep;
    public TimestepDataKey(int timestep) {
    	this.timestep = timestep;
    }

    @Override
    public int compareTo(final TimestepDataKey o) {
        return Integer.compare(timestep, o.timestep);
    }

    public static String getHeader() {
        return "ts";
    }
}
