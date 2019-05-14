package org.vadere.simulator.projects.dataprocessing.datakey;


import org.jetbrains.annotations.NotNull;
import org.vadere.simulator.projects.dataprocessing.outputfile.BonnMotionTrajectoryFile;

import java.util.Objects;

/**
 * It's nearly identical to {@link PedestrianIdKey} with the distinction that the key part is not
 * printed. The key is not needed here.
 *
 * @author Stefan Schuhbäck
 */
@OutputFileMap(outputFileClass = BonnMotionTrajectoryFile.class)
public class BonnMotionKey implements DataKey<BonnMotionKey> {

	private final int pedId;

	public BonnMotionKey(int pedId) {
		this.pedId = pedId;
	}

	public static String getHeader() {
		return "";
	}


	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		BonnMotionKey that = (BonnMotionKey) o;
		return pedId == that.pedId;
	}

	@Override
	public int hashCode() {
		return Objects.hash(pedId);
	}

	@Override
	public int compareTo(@NotNull BonnMotionKey o) {
		return Integer.compare(pedId, o.pedId);
	}

	@Override
	public String toString() {
		return Integer.toString(this.pedId);
	}
}
