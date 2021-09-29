package org.vadere.state.attributes.scenario;

import org.vadere.state.scenario.AerosolCloud;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VShape;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * This class defines the attributes of an {@link AerosolCloud}.
 */
public class AttributesAerosolCloud extends AttributesParticleDispersion {

    private double area;
    private double height;
    private VPoint center;
    private ArrayList<VPoint> vertices;
    private double halfLife;

    // Constructors
    public AttributesAerosolCloud() {
        super();
        VPoint center = new VPoint(0, 0);
        double radius = 0.75;
        this.height = 1.0;
        this.area = radius * radius * Math.PI;
        this.center = center;
        this.vertices = new ArrayList<>(Arrays.asList(new VPoint(0, 0), new VPoint(0, 0)));
        this.halfLife = 100;
    }

    public AttributesAerosolCloud(VShape shape, double creationTime){
        super(creationTime, shape);
    }

    public AttributesAerosolCloud(int id, VShape shape, double currentPathogenLoad){
        super(id, shape, currentPathogenLoad);
    }

    public AttributesAerosolCloud(int id, VShape shape, double area, double height, VPoint center, ArrayList<VPoint> vertices, double creationTime, double halfLife, double initialPathogenLoad, double currentPathogenLoad) {
        super(id, creationTime, initialPathogenLoad, currentPathogenLoad, shape);
        this.area = area;
        this.height = height;
        this.center = center;
        this.vertices = vertices;
        this.halfLife = halfLife;
    }

    // Getter

    public double getArea() {
        return  area;
    }

    public double getHeight() {
        return height;
    }

    public VPoint getCenter() {
        return center;
    }

    public ArrayList<VPoint> getVertices() {
        return vertices;
    }

    public double getHalfLife() { return halfLife; }

    // Setter

    public void setArea(double area) {
        this.area = area;
    }

    public void setCenter(VPoint center) {
        this.center = center;
    }

    public void setVertices(ArrayList<VPoint> vertices) {
        this.vertices = vertices;
    }
}