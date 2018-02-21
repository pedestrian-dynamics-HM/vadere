package org.vadere.state.attributes.processor;

/**
 * @author Mario Teixeira Parente
 *
 */

public class AttributesPedestrianOverlapProcessor extends AttributesProcessor {
    private double pedRadius = 0.2;

    public double getPedRadius() {
        return pedRadius;
    }

	public void setPedRadius(double pedRadius) {
    	checkSealed();
		this.pedRadius = pedRadius;
	}
}
