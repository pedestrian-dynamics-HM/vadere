package org.vadere.state.scenario;

import org.jetbrains.annotations.NotNull;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.scenario.AttributesMeasurementArea;
import org.vadere.state.types.ScenarioElementType;
import org.vadere.util.geometry.shapes.VPolygon;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

public class MeasurementArea extends ScenarioElement<AttributesMeasurementArea> {


	public MeasurementArea(){
		this(new AttributesMeasurementArea());
	}

	public MeasurementArea(@NotNull  AttributesMeasurementArea attributes) {
		this.attributes = attributes;
	}

	public MeasurementArea(MeasurementArea measurementArea){
		this(new AttributesMeasurementArea(measurementArea.getId(), measurementArea.getShape()));
	}


	public boolean isRectangular(){
		VShape shape = attributes.getShape();
		if (shape instanceof VRectangle)
			return true;
		if (shape instanceof VPolygon){
			return ((VPolygon)shape).isRectangular();
		}
		return false;
	}

	public VRectangle asVRectangle(){
		VShape shape = attributes.getShape();
		if (shape instanceof VRectangle)
			return (VRectangle) shape;
		if (shape instanceof VPolygon){
			VRectangle rectangle = ((VPolygon)shape).asVRectangle();
			setShape(rectangle);
			return rectangle;
		}
		return null;
	}

	public VPolygon asPolygon() {
		VShape shape = attributes.getShape();
		if (shape instanceof VRectangle || shape instanceof VPolygon) {
			return new VPolygon(shape);
		}
		return null;
	}

	/**
	 * Compare {@link MeasurementArea}s based on their shape.
	 * Important {@link VPolygon} != {@link VRectangle} even if all points are the same.
	 * @param other
	 * @return
	 */
	public boolean compareByShape(MeasurementArea other){
		return this.getShape().equals(other.getShape());
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
	public AttributesMeasurementArea getAttributes() {
		return attributes;
	}

	@Override
	public void setAttributes(AttributesMeasurementArea attributes) {
		this.attributes = attributes;
	}

	public void setId(int id){
		(getAttributes()).setId(id);
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
