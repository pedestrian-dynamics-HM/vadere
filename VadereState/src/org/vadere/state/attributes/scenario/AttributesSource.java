package org.vadere.state.attributes.scenario;
import com.fasterxml.jackson.annotation.JsonView;
import org.vadere.state.attributes.spawner.AttributesSpawner;
import org.vadere.state.util.Views;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.reflection.VadereAttribute;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class AttributesSource extends AttributesVisualElement {
	@VadereAttribute
	@JsonView(Views.CacheViewExclude.class)
	private AttributesSpawner spawner;
	@VadereAttribute
	@JsonView(Views.CacheViewExclude.class) // ignore when determining if floor field cache is valid
	private List<Integer> targetIds = new LinkedList<>();
	/**
	 *  This Attribute only takes affect if a model org.vadere.simulator.models.groups.GroupModel
	 *  is present in the scenario. When this is the case this list defines the group size
	 *  distribution of this source. The list can be arbitrary long but must add up to 1.
	 *  The index of the list represents the size of the  groups and the value the probability
	 *  index 0 => GroupSize = 1
	 *  index 1 => GroupSize = 2
	 *  ...
	 *
	 *  Example: ----------------------------------------------------------------------------------
	 *  probability [ 0.0, 0.0, 0.25, 0.25, 0.25, .... ] ------------------------------------------
  	 *  GroupSize   [  1    2    3     4     5         ] ------------------------------------------
	 *  uniform distribution of groups of the size from 3 to 5 ------------------------------------
	 */

	@JsonView(Views.CacheViewExclude.class) // ignore when determining if floor field cache is valid
	private List<Double> groupSizeDistribution = Arrays.asList(1.0);

	public AttributesSource() {}

	public AttributesSource(int id) {
		this.id = id;
	}

	public AttributesSource(int id, VShape shape) {
		this.id = id;
		this.shape = shape;
	}

	public AttributesSpawner getSpawnerAttributes() {
		return spawner;
	}

	public void setSpawnerAttributes(AttributesSpawner spawner) {
		checkSealed();
		this.spawner = spawner;
	}

	public List<Integer> getTargetIds() {
		return targetIds;
	}

	public List<Double> getGroupSizeDistribution() {
		return groupSizeDistribution;
	}

	public void setGroupSizeDistribution(List<Double> groupSizeDistribution) {
		checkSealed();
		this.groupSizeDistribution = groupSizeDistribution;
	}


	public void setTargetIds(List<Integer> targetIds) {
		checkSealed();
		this.targetIds = targetIds;
	}

	@Override
	public void check() throws IOException {
		try {
			/*VadereDistribution<?> distribution = DistributionFactory.create(
					this.getInterSpawnTimeDistribution(),
					this.getDistributionParameters(),
					this.getSpawnNumber(),
					new JDKRandomGenerator(42)
			);*/
		} catch (Exception e) {
			//throw new IOException("Cannot build " + this.getInterSpawnTimeDistribution());
		}

	}
}
