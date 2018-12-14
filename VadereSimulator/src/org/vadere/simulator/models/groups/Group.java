package org.vadere.simulator.models.groups;

import org.vadere.simulator.models.potential.fields.IPotentialFieldTarget;
import org.vadere.state.scenario.Pedestrian;

import java.util.List;

public interface Group {
	int getID();

	int getSize();

	boolean isMember(Pedestrian ped);

	List<Pedestrian> getMembers();

	void addMember(Pedestrian ped);

	void removeMember(Pedestrian ped);

	boolean isFull();

	int getOpenPersons();

	boolean equals(Group other);

	void setPotentialFieldTarget(IPotentialFieldTarget potentialFieldTarget);

	IPotentialFieldTarget getPotentialFieldTarget();
}
