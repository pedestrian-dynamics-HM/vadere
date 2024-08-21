package org.vadere.state.attributes.models.airflow;

import org.vadere.state.attributes.Attributes;

public class AttributesLinearAirFlowModel extends Attributes {

    /**
     * Describes the speed of the wind shifting the AerosolClouds
     * Unit: m/s
     */
    private double windSpeed;

    /**
     * Describes the direction of the wind
     * Unit: point of the compass
     */
    private WindDirection windDirection;

    public AttributesLinearAirFlowModel() {
        windSpeed = 0.1;
        windDirection = WindDirection.N;
    }

    public double getWindSpeed() {
        return windSpeed;
    }

    public double getWindDirection()  {
        return windDirection.angle;
    }

    public enum WindDirection {
        N(Math.PI * 1/2),
        NO(Math.PI * 1/4),
        O(0),
        SO(Math.PI * 7/4),
        S(Math.PI * 3/2),
        SW(Math.PI * 5/4),
        W(Math.PI),
        NW(Math.PI * 3/4);

        final double angle;

        WindDirection(double angle) {
            this.angle = angle;
        }
    }
}
