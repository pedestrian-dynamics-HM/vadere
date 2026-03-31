package org.vadere.state.attributes.models.airflow;

import org.vadere.annotation.factories.attributes.ModelAttributeClass;
import org.vadere.state.attributes.Attributes;

@ModelAttributeClass
public class AttributesLinearAirFlowModel extends Attributes {

    /**
     * Describes the speed of the airflow shifting the AerosolClouds
     * Unit: m/s
     */
    private double airflowSpeed;

    /**
     * Describes the direction of the airflow
     * Unit: point of the compass
     */
    private AirflowDirection airflowDirection;

    public AttributesLinearAirFlowModel() {
        airflowSpeed = 0.1;
        airflowDirection = AirflowDirection.N;
    }

    public double getAirflowSpeed() {
        return airflowSpeed;
    }

    public double getAirflowDirection()  {
        return airflowDirection.angle;
    }

    public enum AirflowDirection {
        N(Math.PI * 1/2),
        NO(Math.PI * 1/4),
        O(0),
        SO(Math.PI * 7/4),
        S(Math.PI * 3/2),
        SW(Math.PI * 5/4),
        W(Math.PI),
        NW(Math.PI * 3/4);

        final double angle;

        AirflowDirection(double angle) {
            this.angle = angle;
        }
    }
}
