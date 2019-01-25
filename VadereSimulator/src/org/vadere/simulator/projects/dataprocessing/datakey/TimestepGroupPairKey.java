package org.vadere.simulator.projects.dataprocessing.datakey;

import org.jetbrains.annotations.NotNull;
import org.vadere.simulator.projects.dataprocessing.outputfile.GroupPairOutputFile;
import org.vadere.state.scenario.PedestrianPair;

import java.util.Objects;

@OutputFileMap(outputFileClass = GroupPairOutputFile.class)
public class TimestepGroupPairKey implements DataKey<TimestepGroupPairKey> {
	private final int timestep;
	private final int groupId;
	private final int pedFirst;
	private final int pedSecond;

	public TimestepGroupPairKey(int timestep, int groupId, PedestrianPair pair){
		this(timestep, groupId, pair.getLeftId(), pair.getRightId());
	}

	public TimestepGroupPairKey(int timestep, int groupId, int ped1, int ped2) {
		this.timestep = timestep;
		this.groupId = groupId;
		if (ped1 <= ped2){
			this.pedFirst = ped1;
			this.pedSecond = ped2;
		} else {
			this.pedFirst = ped2;
			this.pedSecond = ped1;
		}
	}

	public static String[] getHeaders() { return new String[]{TimestepKey.getHeader(), "GroupId", "Pedestrian1", "Pedestrian2" }; }

	public String[] toStrings(){
		return new String[]{Integer.toString(timestep), Integer.toString(groupId), Integer.toString(pedFirst), Integer.toString(pedSecond)};
	}

	@Override
	public int compareTo(@NotNull TimestepGroupPairKey o) {
		int ret = Integer.compare(timestep, o.timestep);
		if (ret == 0){
			ret = Integer.compare(groupId, o.groupId);
			if(ret == 0){
				ret = Integer.compare(pedFirst, o.pedFirst);
				if(ret == 0) {
					return Integer.compare(pedSecond, o.pedSecond);
				}
			}
		}
		return ret;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		TimestepGroupPairKey that = (TimestepGroupPairKey) o;
		return groupId == that.groupId &&
				pedFirst == that.pedFirst &&
				pedSecond == that.pedSecond;
	}

	@Override
	public int hashCode() {
		return Objects.hash(groupId, pedFirst, pedSecond);
	}

	@Override
	public String toString() {
		return "TimestepGroupPairKey{" +
				"groupId=" + groupId +
				", pedFirst=" + pedFirst +
				", pedSecond=" + pedSecond +
				'}';
	}
}
