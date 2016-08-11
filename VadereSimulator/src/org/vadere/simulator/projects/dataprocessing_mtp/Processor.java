package org.vadere.simulator.projects.dataprocessing_mtp;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.vadere.simulator.control.SimulationState;

public abstract class Processor<K extends Comparable<K>, V> {
	private int id;

	private String header;
	private Map<K, V> column;

	private int lastStep;

	protected Processor() {
		this("");
	}

	protected Processor(final String header) {
		this.setHeader(header);
		this.column = new HashMap<>();

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

	public String getHeader() {
		return this.header;
	}

	public void setHeader(String header) {
		this.header = header;
	}

	public Set<K> getKeys() {
		return this.column.keySet();
	}

	public boolean hasValue(K key) {
		return this.column.containsKey(key);
	}

	public V getValue(final K key) {
		return column.containsKey(key) ? column.get(key) : null;
	}

	protected void setValue(final K key, final V value) {
		this.column.put(key, value);
	}

	protected abstract void doUpdate(final SimulationState state);

	public final void update(final SimulationState state) {
		int step = state.getStep();

		if (this.lastStep < step) {
			this.doUpdate(state);
			this.lastStep = step;
		}
	}

	abstract void init(final AttributesProcessor attributes, final ProcessorFactory factory);

	public String toString(K key) {
		return this.hasValue(key) ? this.getValue(key).toString() : "NaN";
	}
}
