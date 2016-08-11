package org.vadere.simulator.projects;

import java.util.Collection;
import java.util.Map;

import org.vadere.simulator.models.Model;
import org.vadere.simulator.projects.dataprocessing.TimeStep;
import org.vadere.simulator.projects.dataprocessing.TimeStepData;
import org.vadere.state.attributes.Attributes;

import com.google.gson.JsonElement;

/**
 * Stores an attributes object.
 * 
 * 
 */
public class TimeStepAttributes extends TimeStep {

	private final Map<Class<? extends Model>, JsonElement> attributesModel;
	private final Attributes attributes;

	public TimeStepAttributes(
			final Collection<? extends TimeStepData> kinematics, double time,
			int step) {
		super(kinematics, time, step);
		this.attributes = null;
		this.attributesModel = null;
	}

	public TimeStepAttributes(final Map<Class<? extends Model>, JsonElement> attributesModel) {
		super(null, 0, 1);
		this.attributesModel = attributesModel;
		this.attributes = null;
	}

	public TimeStepAttributes(final Attributes attributes) {
		super(null, 0, 1);
		this.attributes = attributes;
		this.attributesModel = null;
	}

	public Attributes getAttributes() {
		return attributes;
	}

	public Map<Class<? extends Model>, JsonElement> getAttributesModel() {
		return attributesModel;
	}

}
