package org.vadere.state.scenario;

import org.jetbrains.annotations.NotNull;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.scenario.AttributesMeasurementArea;
import org.vadere.state.types.ScenarioElementType;
import org.vadere.util.geometry.shapes.VShape;

import java.util.Objects;

public class MeasurementArea extends ScenarioElement {

	private AttributesMeasurementArea attributes;

	public MeasurementArea(){
		this(new AttributesMeasurementArea());
	}

	public MeasurementArea(@NotNull  AttributesMeasurementArea attributes) {
		this.attributes = attributes;
	}

	public MeasurementArea(MeasurementArea measurementArea){
		this(new AttributesMeasurementArea(measurementArea.getId(), measurementArea.getShape()));
	}

	@Override
	public VShape getShape() {
		return attributes.getShape();
	}

	@Override
	public void setShape(VShape newShape) {
		attributes.setShape(newShape);
	}

	@Override
	public int getId() {
		return attributes.getId();
	}

	@Override
	public ScenarioElementType getType() {
		return ScenarioElementType.MEASUREMENT_AREA;
	}

	@Override
	public ScenarioElement clone() {
		return new MeasurementArea(((AttributesMeasurementArea) attributes.clone()));
	}

	@Override
	public Attributes getAttributes() {
		return attributes;
	}

	@Override
	public void setAttributes(Attributes attributes) {
		this.attributes = (AttributesMeasurementArea)attributes;
	}

	public void setId(int id){
		((AttributesMeasurementArea)getAttributes()).setId(id);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((attributes == null) ? 0 : attributes.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof MeasurementArea)) {
			return false;
		}
		MeasurementArea other = (MeasurementArea) obj;
		if (attributes == null) {
			if (other.attributes != null) {
				return false;
			}
		} else if (!attributes.equals(other.attributes)) {
			return false;
		}
		return true;
	}
}
