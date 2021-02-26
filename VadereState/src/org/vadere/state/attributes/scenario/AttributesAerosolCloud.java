package org.vadere.state.attributes.scenario;

import org.vadere.state.attributes.AttributesEmbedShape;
import org.vadere.util.geometry.shapes.VShape;


public class AttributesAerosolCloud extends AttributesEmbedShape {

    private VShape shape;
    private int id;
    private double aerosolPersistenceTime;
    private double aerosolPersistenceStart;
    private double aerosolCloudRadius;
    // private double pathogenLoad;

    // Constructors
    public AttributesAerosolCloud(){};

    public AttributesAerosolCloud(int id) {
        this.id = id;
    }

    public AttributesAerosolCloud(int id, VShape shape, double aerosolPersistenceTime, double aerosolPersistenceStart, double aerosolCloudRadius) {
        this.shape = shape;
        this.id = id;
        this.aerosolPersistenceTime = aerosolPersistenceTime;
        this.aerosolPersistenceStart = aerosolPersistenceStart;
        this.aerosolCloudRadius = aerosolCloudRadius;
    }

    // Getter
    @Override
    public VShape getShape() {
        return shape;
    }

    public int getId() {
        checkSealed();
        return id;
    }
    public double getAerosolPersistenceTime() { return aerosolPersistenceTime; }
    public double getAerosolPersistenceStart() { return aerosolPersistenceStart; }
    public double getAerosolCloudRadius() { return aerosolCloudRadius; }

    // Setter
    @Override
    public void setShape(VShape shape) {
        this.shape = shape;
    }

    public void setId(int id) { this.id = id; }
    public void setAerosolPersistenceTime(double aerosolPersistenceTime) { this.aerosolPersistenceTime = aerosolPersistenceTime; }
    public void setAerosolPersistenceStart(double aerosolPersistenceStart) { this.aerosolPersistenceStart = aerosolPersistenceStart; }
    public void setAerosolCloudRadius(double aerosolCloudRadius) { this.aerosolCloudRadius = aerosolCloudRadius; }



}
