package org.vadere.state.traci;

import java.util.LinkedList;

/**
 * Builder class to create any combination atomar data types combined in a @{@link CompoundObject}.
 * See static methods on how the builder is used. Ensure that the number of {@link
 * #add(TraCIDataType)} calls is equal to the number of arguments to the {@link #build(Object...)}.
 */
public class CompoundObjectBuilder {

	private LinkedList<TraCIDataType> types;

	public CompoundObjectBuilder() {
		this.types = new LinkedList<>();
	}

	static public CompoundObjectBuilder builder() {
		return new CompoundObjectBuilder();
	}

	static public CompoundObject json(String json) {

		return CompoundObjectBuilder.builder()
				.rest()
				.add(TraCIDataType.STRING)
				.build(json);
	}

	public CompoundObjectBuilder rest() {
		types.clear();
		return this;
	}

	public CompoundObjectBuilder add(TraCIDataType type) {
		types.add(type);
		return this;
	}

	public CompoundObjectBuilder add(TraCIDataType type, int count) {
		for (int i = 0; i < count; i++) {
			types.add(type);
		}
		return this;
	}

	public CompoundObject build(Object... data) {
		CompoundObject obj = new CompoundObject(data.length);
		if (types.size() == data.length) {
			int idx = 0;
			for (TraCIDataType type : types) {
				obj.add(type, data[idx]);
				idx++;
			}

		} else {
			throw new TraCIException("CompoundObjectBuilder error. Number of Types does not match" +
					" received number of data items");
		}

		return obj;
	}


}
