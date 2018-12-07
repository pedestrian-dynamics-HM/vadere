package org.vadere.simulator.models.groups;

import java.util.List;

import org.vadere.state.scenario.Pedestrian;

public interface Group {
	public int getID();

	public int getSize();

	public boolean isMember(Pedestrian ped);

	public List<Pedestrian> getMembers();

	public void addMember(Pedestrian ped);

	public void removeMember(Pedestrian ped);

	public boolean isFull();

	public int getOpenPersons();

	boolean equals(Group other);
}
