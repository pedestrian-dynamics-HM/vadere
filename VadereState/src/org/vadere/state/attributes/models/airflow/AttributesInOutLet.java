package org.vadere.state.attributes.models.airflow;

public class AttributesInOutLet {

    private String inletSide;

    private double inletStart;

    private double inletEnd;

    private String outletSide;

    private double outletStart;

    private double outletEnd;

    public AttributesInOutLet() {
        inletSide = "bottom";
        inletStart = 0;
        inletEnd = 2;
        outletSide = "right";
        outletStart = 0;
        outletEnd = 2;
    }

    public String getInletSide() {
        return inletSide;
    }

    public double getInletStart() {
        return inletStart;
    }

    public double getInletEnd() {
        return inletEnd;
    }

    public String getOutletSide() {
        return outletSide;
    }

    public double getOutletStart() {
        return outletStart;
    }

    public double getOutletEnd() {
        return outletEnd;
    }
}
