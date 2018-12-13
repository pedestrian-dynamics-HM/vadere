package org.vadere.simulator.models.groups;

import org.vadere.simulator.models.Model;
import org.vadere.simulator.models.potential.fields.IPotentialFieldTarget;
import org.vadere.state.scenario.DynamicElementAddListener;
import org.vadere.state.scenario.DynamicElementRemoveListener;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.ScenarioElement;

import java.util.List;
import java.util.Map;

public interface GroupModel<T extends Group> extends Model, DynamicElementAddListener<Pedestrian>, DynamicElementRemoveListener<Pedestrian> {

	/**
	 * @param pedestrian	Pedestrian object
	 * @return		The group the pedestrian object is a part of.
	 */
	T getGroup(ScenarioElement pedestrian);

	/**
	 * @return Map of Pedestrians and their group.
	 */
	Map<ScenarioElement, T > getPedestrianGroupMap();

	/**
	 * Register a Pedestrian to the specified group. The function does not check if the
	 * pedestrian is already a member of another group. The caller must make sure of that.
	 * TODO: why is the group not created within the GroupModel.
	 * @param ped
	 * @param currentGroup
	 */
	void registerMember(ScenarioElement ped, T currentGroup);

	T getNewGroup(int size);



	void setPotentialFieldTarget(IPotentialFieldTarget potentialFieldTarget);

	IPotentialFieldTarget getPotentialFieldTarget();

	/**
	 *
	 * @param sourceId		{@link org.vadere.state.scenario.Source}Id for which the
	 * 						{@link GroupFactory} is needed.
	 */
	GroupFactory getGroupFactory(int sourceId);

	/**
	 *
	 * @param sourceId					{@link org.vadere.state.scenario.Source}Id for which the
	 * @param groupSizeDistribution		Distribution indicating the size of the groups
	 *                                  generated for this source.
	 */
	void initializeGroupFactory(int sourceId, List<Double> groupSizeDistribution);
}
