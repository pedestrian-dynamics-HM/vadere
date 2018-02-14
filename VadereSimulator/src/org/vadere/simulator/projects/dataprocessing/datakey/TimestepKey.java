package org.vadere.simulator.projects.dataprocessing.datakey;

import org.vadere.simulator.projects.dataprocessing.outputfile.TimestepOutputFile;

/**
 * @author Mario Teixeira Parente
 *
 */
@OutputFileMap(outputFileClass = TimestepOutputFile.class)
public class TimestepKey implements DataKey<TimestepKey> {
	private final int timestep;
    public TimestepKey(int timestep) {
    	this.timestep = timestep;
    }

    @Override
    public int compareTo(final TimestepKey o) {
        return Integer.compare(timestep, o.timestep);
    }

    public static String getHeader() {
        return "timeStep";
    }

	public Integer getTimestep() {
		return timestep;
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
		TimestepKey other = (TimestepKey) obj;
		if (timestep != other.timestep)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return Integer.toString(this.timestep);
	}
}
