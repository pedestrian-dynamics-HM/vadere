package org.vadere.simulator.models.groups;

import org.vadere.simulator.models.groups.cgm.CentroidGroup;
import org.vadere.state.scenario.Pedestrian;

public abstract class AbstractGroupModel<T extends Group> implements GroupModel<CentroidGroup> {

  /**
   * Register a Pedestrian to the specified group. The function does not check if the pedestrian is
   * already a member of another group. The caller must make sure of that.
   */
  protected abstract void registerMember(Pedestrian ped, T currentGroup);

  protected abstract T getNewGroup(int size);

  protected abstract T getNewGroup(final int id, final int size);

  protected abstract void assignToGroup(Pedestrian pedestrian);
}
