package org.vadere.state.scenario;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class TopographyIterator implements Iterator<ScenarioElement> {

	private ArrayList<Collection<? extends ScenarioElement>> elements;
	private Iterator<? extends ScenarioElement> currentIterator;
	private int index;

	public TopographyIterator(final Topography topography, final Collection<? extends Agent> agents) {
		elements = new ArrayList<>();
		index = 0;

		if (!agents.isEmpty()) {
			elements.add(agents);
		}

		if (!topography.getStairs().isEmpty()) {
			elements.add(topography.getStairs());
		}

		if (!topography.getSources().isEmpty()) {
			elements.add(topography.getSources());
		}

		if (!topography.getTargets().isEmpty()) {
			elements.add(topography.getTargets());
		}

		if (!topography.getObstacles().isEmpty()) {
			elements.add(topography.getObstacles());
		}

		if (!topography.getAbsorbingAreas().isEmpty()) {
			elements.add(topography.getAbsorbingAreas());
		}

		if (!topography.getAerosolClouds().isEmpty()) {
			elements.add(topography.getAerosolClouds());
		}

		if (!topography.getDroplets().isEmpty()) {
			elements.add(topography.getDroplets());
		}

		if (!topography.getTargetChangers().isEmpty()) {
			elements.add(topography.getTargetChangers());
		}

		if (!elements.isEmpty()) {
			currentIterator = elements.get(0).iterator();
		}
	}

	public TopographyIterator(final Topography topography) {
		this(topography, topography.getElements(Agent.class));
	}

	@Override
	public boolean hasNext() {
		if (elements.size() == 0) {
			return false;
		}

		if (index == elements.size() - 1) {
			return currentIterator.hasNext();
		}
		return index < elements.size();
	}

	@Override
	public ScenarioElement next() {
		if (!currentIterator.hasNext()) {
			currentIterator = elements.get(++index).iterator();
		}

		return currentIterator.next();
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException("not yet implemented.");
	}
}
