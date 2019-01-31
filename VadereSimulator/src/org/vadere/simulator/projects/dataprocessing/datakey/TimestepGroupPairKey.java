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
		this.pedFirst = ped1;
		this.pedSecond = ped2;

	}

	public static String[] getHeaders() { return new String[]{TimestepKey.getHeader(), "GroupId", "Pedestrian1", "Pedestrian2" }; }

	public String[] toStrings(){
		return new String[]{Integer.toString(timestep), Integer.toString(groupId), Integer.toString(pedFirst), Integer.toString(pedSecond)};
	}

	// order of ped1 and ped2 does not matter. (3,4) == (4,2) is True.
	@Override
	public int compareTo(@NotNull TimestepGroupPairKey o) {
		if (timestep == o.timestep){	// order by timestep
			if (groupId == o.groupId){ 	// oder by groupId
				if (pedFirst == o.pedFirst  && pedSecond == o.pedSecond)  {	// (3,4) == (3,4)
					return  0;
				} else if (pedFirst == o.pedSecond && pedSecond == o.pedFirst) { // (3,4) == (4,3)
					return 0;
				} else { // order by ped1 then by ped2
					return  ( pedFirst != o.pedFirst) ?  Integer.compare(pedFirst, o.pedSecond) : Integer.compare(pedSecond, o.pedSecond);
				}
			} else {
				return Integer.compare(groupId, o.groupId);
			}
		} else {
			return Integer.compare(timestep, o.timestep);
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		TimestepGroupPairKey that = (TimestepGroupPairKey) o;

		return timestep == that.timestep &&
				groupId == that.groupId &&
				(	(pedFirst == that.pedFirst && pedSecond == that.pedSecond) ||
					(pedFirst == that.pedSecond && pedSecond == that.pedFirst)
				);
	}

	@Override
	public int hashCode() {
		return Objects.hash(groupId, pedFirst, pedSecond);
	}

	@Override
	public String toString() {
		return "TimestepGroupPairKey{" +
				"timestep=" + timestep +
				", groupId=" + groupId +
				", pedFirst=" + pedFirst +
				", pedSecond=" + pedSecond +
				'}';
	}
}
