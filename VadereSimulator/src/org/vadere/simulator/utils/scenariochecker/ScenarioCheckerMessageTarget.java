package org.vadere.simulator.utils.scenariochecker;

import org.vadere.state.scenario.ScenarioElement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class ScenarioCheckerMessageTarget {

	private  List<ScenarioElement> targets;

	public ScenarioCheckerMessageTarget(List<ScenarioElement> targets) {
		this.targets = targets;
	}

	public ScenarioCheckerMessageTarget(ScenarioElement... targets) {
		this.targets = new ArrayList<>(Arrays.asList(targets));
	}

	public List<ScenarioElement> getTargets() {
		return targets;
	}

	public void setTargets(List<ScenarioElement> targets) {
		this.targets = targets;
	}

	public void setTargets(ScenarioElement... targets) {
		this.targets = new ArrayList<>(Arrays.asList(targets));;
	}

	public boolean affectsAllTargets(Integer... ids){
		for (Integer id : ids) {
			if(targets.stream().noneMatch(e -> e.getId() == id)){
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ScenarioCheckerMessageTarget that = (ScenarioCheckerMessageTarget) o;
		return Objects.equals(targets, that.targets);
	}

	@Override
	public int hashCode() {
		return Objects.hash(targets);
	}

	@Override
	public String toString() {

		StringBuilder sb = new StringBuilder();
		sb.append("ScenarioCheckerMessageTarget{ targets=");
		sb.append("[");
		targets.forEach(t -> sb.append(t.getClass().getSimpleName()).append(", "));
		sb.setLength(sb.length() -2);
		sb.append("]}");

		return sb.toString();
	}
}
