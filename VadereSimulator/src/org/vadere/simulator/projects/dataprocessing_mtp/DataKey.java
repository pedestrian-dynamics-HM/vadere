package org.vadere.simulator.projects.dataprocessing_mtp;

public abstract class DataKey<K extends Comparable<K>> {
	private K key;

	protected DataKey(final K key) {
		this.key = key;
	}

	public K getKey() {
		return this.key;
	}

	public boolean equals(Object o) {
		if (o instanceof DataKey)
			return this.key.equals(((DataKey) o).key);

		return false;
	}

	public int hashCode() {
		return this.key.hashCode();
	}

	public String toString() {
		return this.key.toString();
	}
}
