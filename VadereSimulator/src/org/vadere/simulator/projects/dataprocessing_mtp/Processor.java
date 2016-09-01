package org.vadere.simulator.projects.dataprocessing_mtp;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.vadere.simulator.control.SimulationState;

public abstract class Processor<K extends Comparable<K>, V> {
	private int id;

	private String[] headers;
	private Map<K, V> column;

	private int lastStep;

	protected Processor() {
		this(new String[] { });
	}

	protected Processor(final String... headers) {
		this.headers = headers;
		this.column = new TreeMap<>();

		this.lastStep = 0;
	}

	protected Map<K, V> getColumn() {
		return this.column;
	}

	public int getId() {
		return this.id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String[] getHeaders() {
		return this.headers;
	}

	public void setHeader(final String header) {
		this.headers = new String[] { header };
	}

	public Set<K> getKeys() {
		return this.getColumn().keySet();
	}

	public Collection<V> getValues() {
		return this.getColumn().values();
	}

	public boolean hasValue(final K key) {
		return this.column.containsKey(key);
	}

	public V getValue(final K key) {
		return column.get(key);
	}

	protected void setValue(final K key, final V value) {
		this.column.put(key, value);
	}

	public void preLoop(final SimulationState state) { }

	protected abstract void doUpdate(final SimulationState state);

	public final void update(final SimulationState state) {
		int step = state.getStep();

		if (this.lastStep < step) {
			this.doUpdate(state);
			this.lastStep = step;
		}
	}

	public void postLoop(final SimulationState state) { }

	abstract void init(final AttributesProcessor attributes, final ProcessorManager manager);

	public String[] toStrings(final K key) {
		return new String[] { this.hasValue(key) ? this.getValue(key).toString() : "NaN" };
	}
}
