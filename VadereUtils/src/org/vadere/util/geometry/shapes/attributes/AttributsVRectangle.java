package org.vadere.util.geometry.shapes.attributes;

import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.reflection.VadereAttribute;

public class AttributsVRectangle extends AttributesVShape{
    @VadereAttribute
    private Double x;
    @VadereAttribute
    private Double y;
    @VadereAttribute
    private Double width;
    @VadereAttribute
    private Double height;


    public AttributsVRectangle(VRectangle rectangle){
        this.x = rectangle.getX();
        this.y = rectangle.getY();
        this.width = rectangle.getWidth();
        this.height = rectangle.getHeight();
    }

    public Double getX() {
        return x;
    }

    public void setX(Double x) {
        checkSealed();
        this.x = x;
    }

    public Double getY() {
        return y;
    }

    public void setY(Double y) {
        checkSealed();
        this.y = y;
    }

    public Double getWidth() {
        return width;
    }

    public void setWidth(Double width) {
        checkSealed();
        this.width = width;
    }

    public Double getHeight() {
        return height;
    }

    public void setHeight(Double height) {
        checkSealed();
        this.height = height;
    }
}
