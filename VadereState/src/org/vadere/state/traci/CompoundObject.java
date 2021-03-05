package org.vadere.state.traci;

import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;
import java.util.Iterator;
import java.util.function.Predicate;

/**
 * CompoundObject implementation based on TraCI as described in https://sumo.dlr.de/docs/TraCI/Protocol.html#atomar_types
 *
 * This implementation consist of two equally long arrays {@link #type} and {@link #data}. The
 * {@link #type} array saves Type of the objects at the same index in {@link #data}.
 *
 * At time of creation it must be stated how many objects will be tide together. See @{@link
 * CompoundObjectBuilder} for usage of Constructor and the {@link #add(int, Object)} method.
 */
public class CompoundObject {

	private TraCIDataType[] type;
	private Object[] data;
	private int cur;

	public CompoundObject(int noElements) {
		this.type = new TraCIDataType[noElements];
		this.data = new Object[noElements];
		this.cur = 0;
	}

	public String types() {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		for (TraCIDataType i : this.type) {
			sb.append(i.name()).append(", ");
		}
		sb.delete(sb.length() - 2, sb.length());
		sb.append("]");
		return sb.toString();
	}

	public int size() {
		return data.length;
	}

	public CompoundObject add(int type, Object data) {
		return add(TraCIDataType.fromId(type), data);
	}

	public CompoundObject add(TraCIDataType type, Object data) {
		if (cur > this.data.length)
			throw new TraCIException("CompoundObject already full. Received " + types());

		this.type[cur] = type;
		this.data[cur] = data;

		cur++;
		return this;
	}

	public boolean hasIndex(int index) {
		return hasIndex(index, null);
	}

	public boolean hasIndex(int index, TraCIDataType type) {
		if (index >= 0 && index < this.data.length) {
			if (type != null) {
				return this.type[index].equals(type);
			}
			return true;
		}
		return false;
	}

	public Object getData(int index, TraCIDataType type) {
		if (index > this.data.length)
			throw new TraCIException("Cannot access data with index %d", index);
		if (!this.type[index].equals(type))
			throw new TraCIException("Type mismatch of CompoundObject element %s != %s  at index %d",
					this.type[index].name(), type.name(), index);
		return this.data[index];
	}

	public Object getData(int index) {
		if (index > this.data.length)
			throw new TraCIException("Cannot access data with index %d", index);
		return this.data[index];
	}

	public Iterator<Pair<TraCIDataType, Object>> itemIterator() {
		return new Iter(this, null);
	}

	public Iterator<Pair<TraCIDataType, Object>> itemIterator(TraCIDataType typeAssertion) {
		return new Iter(this, typeAssertion);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		CompoundObject that = (CompoundObject) o;
		return Arrays.equals(type, that.type) &&
				Arrays.equals(data, that.data);
	}

	@Override
	public int hashCode() {
		int result = Arrays.hashCode(type);
		result = 31 * result + Arrays.hashCode(data);
		return result;
	}

	private class Iter implements Iterator<Pair<TraCIDataType, Object>> {

		private final CompoundObject compoundObject;
		private final Predicate<TraCIDataType> typeAssertionTest;
		private final TraCIDataType typeAssertion;
		private int curr;

		Iter(CompoundObject compoundObject, TraCIDataType typeAssertion) {
			this.compoundObject = compoundObject;
			this.typeAssertion = typeAssertion;
			if (typeAssertion != null) {
				this.typeAssertionTest = traCIDataType -> traCIDataType.equals(typeAssertion);
			} else {
				this.typeAssertionTest = traCIDataType -> true;
			}
			this.curr = 0;
		}

		@Override
		public boolean hasNext() {
			return curr < compoundObject.size();
		}



		@Override
		public Pair<TraCIDataType, Object> next() {
			Pair<TraCIDataType, Object> p = Pair.of(compoundObject.type[curr], compoundObject.data[curr]);
			if (! this.typeAssertionTest.test(p.getLeft())) {
				throw new TraCIException("Type mismatch in CompoundObject. Expected '%s' but found '%s'",
						this.typeAssertion.name(), p.getLeft().name());
			}
			curr++;
			return p;
		}
	}

}
