package org.vadere.state.attributes.models.airflow;

public class AttributesInOutLet {

    private String side;

    private double start;

    private double end;

    public AttributesInOutLet() {
        side = "bottom";
        start = 0;
        end = 2;
    }

    public String getSide() {
        return side;
    }

    public double getStart() {
        return start;
    }

    public double getEnd() {
        return end;
    }
}
