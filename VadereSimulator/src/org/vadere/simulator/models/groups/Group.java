package org.vadere.simulator.models.groups;

import java.util.List;
import org.vadere.simulator.models.potential.fields.IPotentialFieldTarget;
import org.vadere.state.scenario.AgentListener;
import org.vadere.state.scenario.Pedestrian;

public interface Group extends AgentListener {
  int getID();

  int getSize();

  boolean isMember(Pedestrian ped);

  List<Pedestrian> getMembers();

  void addMember(Pedestrian ped);

  /**
   * @param ped
   * @return Retrun True if ped was the last one.
   */
  boolean removeMember(Pedestrian ped);

  boolean isFull();

  int getOpenPersons();

  boolean equals(Group other);

  void setPotentialFieldTarget(IPotentialFieldTarget potentialFieldTarget);

  IPotentialFieldTarget getPotentialFieldTarget();
}
