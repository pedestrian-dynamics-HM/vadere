package org.vadere.state.attributes.scenario;

import org.lwjgl.system.CallbackI;
import org.vadere.state.attributes.AttributesEmbedShape;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VShape;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;


public class AttributesAerosolCloud extends AttributesEmbedShape {

    private int id;
    private VShape shape;
    private double area;
    private double height;
    private VPoint center;
    private ArrayList<VPoint> vertices;
    private double creationTime;
    private double halfLife;
    private double initialPathogenLoad;
    private double currentPathogenLoad;
    private boolean hasReachedLifeEnd;

    // Constructors
    public AttributesAerosolCloud() {
        VPoint center = new VPoint(0, 0);
        double radius = 0.75;
        this.id = AttributesEmbedShape.ID_NOT_SET;
        this.shape = new VCircle(center, radius);
        this.height = 1.0;
        this.area = radius * radius * Math.PI;
        this.center = center;
        this.vertices = new ArrayList<>(Arrays.asList(new VPoint(0, 0), new VPoint(0, 0)));
        this.creationTime = -1;
        this.halfLife = -1;
        this.initialPathogenLoad = -1;
        this.currentPathogenLoad = -1;
        this.hasReachedLifeEnd = false;
    }

    public AttributesAerosolCloud(VShape shape, double creationTime){
        this();
        this.shape = shape;
        this.creationTime = creationTime;
    }

    public AttributesAerosolCloud(int id, VShape shape, double area, double height, VPoint center, ArrayList<VPoint> vertices, double creationTime, double halfLife, double initialPathogenLoad, double currentPathogenLoad, boolean hasReachedLifeEnd) {
        this.id = id;
        this.shape = shape;
        this.area = area;
        this.height = height;
        this.center = center;
        this.vertices = vertices;
        this.creationTime = creationTime;
        this.halfLife = halfLife;
        this.initialPathogenLoad = initialPathogenLoad;
        this.currentPathogenLoad = currentPathogenLoad;
        this.hasReachedLifeEnd = hasReachedLifeEnd;
    }

    // Getter
    public int getId() { return id; }

    @Override
    public VShape getShape() { // ToDo: check if this works instead of public VShape getShape() {return shape;}
        return shape;
    }

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

    public double getCreationTime() {
        return creationTime;
    }

    public double getHalfLife() {
        return halfLife;
    }

    public double getInitialPathogenLoad() {
        return initialPathogenLoad;
    }

    public double getCurrentPathogenLoad() {
        return currentPathogenLoad;
    }

    public boolean getHasReachedLifeEnd() {
        return hasReachedLifeEnd;
    }

    // Setter
    public void setId(int id) {
        checkSealed();
        this.id = id;
    }

    @Override
    public void setShape(VShape shape) {
        this.shape = shape;
    }

    public void setArea(double area) { this.area = area; }

    public void setCenter(VPoint center) {
        this.center = center;
    }
    public void setVertices(ArrayList<VPoint> vertices) {
        this.vertices = vertices;
    }

    public void setCreationTime(double creationTime) {
        this.creationTime = creationTime;
    }

    public void setHasReachedLifeEnd(boolean hasReachedLifeEnd) {
        this.hasReachedLifeEnd = hasReachedLifeEnd;
    }

    public void setCurrentPathogenLoad(double currentPathogenLoad) {
        this.currentPathogenLoad = currentPathogenLoad;
    }
}