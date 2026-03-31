package org.vadere.state.attributes.models.airflow;

public class AttributesInOutLet {

    private String side;

    private double start;

    private double width;

    public AttributesInOutLet() {
        side = "south";
        start = 0;
        width = 1;
    }

    public AttributesInOutLet(String side, double start, double width) {
        this.side = side;
        this.start = start;
        this.width = width;
    }

    public String getSide() {
        return side;
    }

    public double getStart() {
        return start;
    }

    public double getWidth() {
        return width;
    }
}
