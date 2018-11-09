package org.vadere.state.scenario;

import org.jetbrains.annotations.NotNull;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.scenario.AttributesObstacle;
import org.vadere.state.types.ScenarioElementType;
import org.vadere.util.geometry.shapes.VShape;

public class Obstacle extends ScenarioElement {

	private AttributesObstacle attributes;

	public Obstacle(@NotNull AttributesObstacle attributes) {
		this.attributes = attributes;
	}
	
	@Override
	public void setShape(VShape newShape) {
		attributes.setShape(newShape);
	}

	@Override
	public VShape getShape() {
		return attributes.getShape();
	}

	@Override
	public int getId() {
		return attributes.getId();
	}

	public void setId(int id){
		((AttributesObstacle)getAttributes()).setId(id);
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
		if (!(obj instanceof Obstacle)) {
			return false;
		}
		Obstacle other = (Obstacle) obj;
		if (attributes == null) {
			if (other.attributes != null) {
				return false;
			}
		} else if (!attributes.equals(other.attributes)) {
			return false;
		}
		return true;
	}

	@Override
	public ScenarioElementType getType() {
		return ScenarioElementType.OBSTACLE;
	}

	@Override
	public Attributes getAttributes() {
		return attributes;
	}

	@Override
	public void setAttributes(Attributes attributes) {
		this.attributes = (AttributesObstacle) attributes;
	}


	@Override
	public Obstacle clone() {
		return new Obstacle((AttributesObstacle) attributes.clone());
	}
}
