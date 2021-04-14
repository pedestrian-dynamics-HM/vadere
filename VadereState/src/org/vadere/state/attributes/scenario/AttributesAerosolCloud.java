package org.vadere.state.attributes.scenario;

import org.lwjgl.system.CallbackI;
import org.vadere.state.attributes.AttributesEmbedShape;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VShape;

import java.util.ArrayList;
import java.util.LinkedList;


public class AttributesAerosolCloud extends AttributesEmbedShape {

    private int id;
    private VShape shape;
    private double area;
    private ArrayList<VPoint> shapeParameters; // center, vertex1, vertex2 // ToDo: dirty parameter (may differ from actual shape)
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
        this.area = radius * radius * Math.PI;
        this.shapeParameters = new ArrayList<VPoint>();
        this.shapeParameters.add(0, center);
        this.shapeParameters.add(1, new VPoint(0, 0));
        this.shapeParameters.add(2, new VPoint(0, 0));
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

    public AttributesAerosolCloud(int id, VShape shape, double area, ArrayList<VPoint> shapeParameters, double creationTime, double halfLife, double initialPathogenLoad, double currentPathogenLoad, boolean hasReachedLifeEnd) {
        this.id = id;
        this.shape = shape;
        this.area = area;
        this.shapeParameters = shapeParameters;
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

    public double getArea() { return  area; }

    public ArrayList<VPoint> getShapeParameters() { // ToDo: check if this works instead of public VShape getShape() {return shape;}
        return shapeParameters;
    }

    public double getCreationTime() { return creationTime; }

    public double getHalfLife() { return halfLife; }

    public double getInitialPathogenLoad() { return initialPathogenLoad; }

    public double getCurrentPathogenLoad() { return currentPathogenLoad; }

    public boolean getHasReachedLifeEnd() { return hasReachedLifeEnd; }

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

    public void setShapeParameters(ArrayList<VPoint> shapeParameters) {
        this.shapeParameters = shapeParameters;
    }

    public void setCreationTime(double creationTime) { this.creationTime = creationTime; }

    public void setHasReachedLifeEnd(boolean hasReachedLifeEnd) { this.hasReachedLifeEnd = hasReachedLifeEnd; }

    public void setCurrentPathogenLoad(double currentPathogenLoad) { this.currentPathogenLoad = currentPathogenLoad; }
}