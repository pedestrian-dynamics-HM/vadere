package org.vadere.state.attributes.models.airflow;

import org.vadere.state.attributes.Attributes;

public class AttributesBounds extends Attributes {
    private double xmin;
    private double xmax;
    private double ymin;
    private double ymax;

    public AttributesBounds() {
        this.xmin = 0.0;
        this.xmax = 1000.0;
        this.ymin = 0.0;
        this.ymax = 1000.0;
    }

    public AttributesBounds(double xmin, double xmax, double ymin, double ymax) {
        this.xmin = xmin;
        this.xmax = xmax;
        this.ymin = ymin;
        this.ymax = ymax;
    }

    public double getXmin() { return xmin; }
    public void setXmin(double xmin) { this.xmin = xmin; }
    public double getXmax() { return xmax; }
    public void setXmax(double xmax) { this.xmax = xmax; }
    public double getYmin() { return ymin; }
    public void setYmin(double ymin) { this.ymin = ymin; }
    public double getYmax() { return ymax; }
    public void setYmax(double ymax) { this.ymax = ymax; }
} 