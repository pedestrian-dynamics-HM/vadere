package org.vadere.simulator.projects.dataprocessing.datakey;

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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
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
		TimestepDataKey other = (TimestepDataKey) obj;
		if (timestep != other.timestep)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return Integer.toString(this.timestep);
	}
}
