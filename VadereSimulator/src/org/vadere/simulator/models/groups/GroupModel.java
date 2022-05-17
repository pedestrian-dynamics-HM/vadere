package org.vadere.simulator.models.groups;

import org.vadere.simulator.models.Model;
import org.vadere.simulator.models.potential.fields.IPotentialFieldTarget;
import org.vadere.state.scenario.*;

import java.beans.PropertyEditor;
import java.util.Iterator;
import java.util.Map;


/**
 * The {@link GroupModel} manges the behaviour from Groups. The generation of groups is manged
 * internally for each source generating groups. Each source must register a groupSizeDistribution
 * which will be used to generate the groups.
 *
 * At the time a pedestrian is placed in the topography the {@link GroupModel} is triggered over the
 * {@link DynamicElementAddListener} and will assign the pedestrian to a group based on the source.
 *
 * @param <T> Group Type
 */
public interface GroupModel<T extends Group>
		extends Model, DynamicElementAddListener<Pedestrian>, DynamicElementRemoveListener<Pedestrian>, GroupIterator {

	/**
	 * @param pedestrian Pedestrian object
	 * @return The group the pedestrian object is a part of.
	 */
	T getGroup(Pedestrian pedestrian);

	/**
	 * @return Map of Pedestrians and their group.
	 */
//	Map<Pedestrian, T> getPedestrianGroupMap();

	Map<Integer, T> getGroupsById();


	void setPotentialFieldTarget(IPotentialFieldTarget potentialFieldTarget);

	IPotentialFieldTarget getPotentialFieldTarget();

	/**
	 * Register groupSizeDistribution for a source.
	 *
	 * @param sourceId Source Id
	 * @param gsD      Distribution for group size
	 */
	void registerGroupSizeDeterminator(int sourceId, GroupSizeDeterminator gsD);


	/**
	 * Generate groups for source based on initializedSizeDistribution.
	 *
	 * @param sourceId Source Id
	 * @return Size of next group for given source
	 */
	int nextGroupForSource(int sourceId);

	default Iterator<Group> getGroupIterator(){
		return (Iterator<Group>) getGroupsById().values().iterator();
	}


}
