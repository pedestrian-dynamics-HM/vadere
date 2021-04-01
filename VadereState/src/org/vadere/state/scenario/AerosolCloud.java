package org.vadere.state.scenario;

import org.jetbrains.annotations.NotNull;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.scenario.AttributesAerosolCloud;
import org.vadere.state.types.ScenarioElementType;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VPolygon;
import org.vadere.util.geometry.shapes.VShape;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;

public class AerosolCloud extends ScenarioElement {

    private AttributesAerosolCloud attributes;

    // ToDo: implement AerosolCloudListener (or remove commented code)
    // private final Collection<AerosolCloudListener> aerosolCloudListeners = new LinkedList<>();

    // Constructors
    public AerosolCloud() { this(new AttributesAerosolCloud()); }

    public AerosolCloud(@NotNull AttributesAerosolCloud attributes) {
        this.attributes = attributes;
    }

    public AerosolCloud(AerosolCloud aerosolCloud){
        this(new AttributesAerosolCloud(aerosolCloud.getId(), aerosolCloud.getShape(), aerosolCloud.attributes.getShapeParameters(), aerosolCloud.getCreationTime(),
                aerosolCloud.getHalfLife(), aerosolCloud.getInitialPathogenLoad(), aerosolCloud.getHasReachedLifeEnd()));
    }


    // Getter
    @Override
    public VShape getShape() {     // ToDo check of one must use VShape instead -> attributesAerosolCloud
        return attributes.getShape();
    }

    public ArrayList<VPoint> getShapeParameters() {     // ToDo check of one must use VShape instead -> attributesAerosolCloud
        return attributes.getShapeParameters();
    }

    @Override
    public int getId() {
        return attributes.getId();
    }

    @Override
    public ScenarioElementType getType() {
        return ScenarioElementType.AEROSOL_CLOUD;
    }

    @Override
    public Attributes getAttributes() {
        return attributes;
    }

    public double getHalfLife() { return attributes.getHalfLife(); }
    public double getCreationTime() { return attributes.getCreationTime(); }
    public double getPathogenDensity() { return attributes.getPathogenDensity(); }
    public double getInitialPathogenLoad() { return attributes.getInitialPathogenLoad(); }
    public boolean getHasReachedLifeEnd() { return attributes.getHasReachedLifeEnd(); }

    // Setter
    @Override
    public void setShape(VShape newShape) {
        attributes.setShape(newShape);
    }

    @Override
    public void setAttributes(Attributes attributes) {
        this.attributes = (AttributesAerosolCloud) attributes;
    }

    public void setId(int id){
        ((AttributesAerosolCloud)getAttributes()).setId(id);
    }

    public void setCreationTime(double creationTime) { attributes.setCreationTime(creationTime); }
    public void setHalfLife(double halfLife) { attributes.setHalfLife(halfLife); }
    public void setPathogenDensity(double pathogenDensity) { attributes.setPathogenDensity(pathogenDensity); }
    public void setInitialPathogenLoad(double initialPathogenLoad) { attributes.setInitialPathogenLoad(initialPathogenLoad); }
    public void setHasReachedLifeEnd(boolean hasReachedLifeEnd) { attributes.setHasReachedLifeEnd(hasReachedLifeEnd); }


    // Other methods
    @Override
    public AerosolCloud clone() {
        return new AerosolCloud(((AttributesAerosolCloud) attributes.clone()));
    }

    // ToDo: implement AerosolCloudListener (or remove commented code)
//    /** Models can register a target listener. */
//    public void addListener(AerosolCloudListener listener) {
//        aerosolCloudListeners.add(listener);
//    }
//
//    public boolean removeListener(AerosolCloudListener listener) {
//        return aerosolCloudListeners.remove(listener);
//    }
//
//    /** Returns an unmodifiable collection. */
//    public Collection<AerosolCloudListener> getAerosolCloudListeners() {
//        return Collections.unmodifiableCollection(aerosolCloudListeners);
//    }


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((attributes == null) ? 0 : attributes.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof AerosolCloud)) {
            return false;
        }
        AerosolCloud other = (AerosolCloud) obj;
        if (attributes == null) {
            if (other.attributes != null) {
                return false;
            }
        } else if (!attributes.equals(other.attributes)) {
            return false;
        }
        return true;
    }

    // ToDo: move parts of this method to higher level to avoid calling the method for the same aerosolCloud multiple times
    public double calculatePathogenLevel(VPoint position){
        double pathogenLevel = -1;
        double theta = 0;
        if(!attributes.getShape().contains(position)){
            // error: position must be within shape
        } else {
            double xStd = -1;
            double yStd = -1;
            VShape shape = attributes.getShape();
            VPoint center = attributes.getShapeParameters().get(0);
            AffineTransform transform = new AffineTransform();

            if (shape instanceof VPolygon) {
                VPoint vertex1 = attributes.getShapeParameters().get(1);
                VPoint vertex2 = attributes.getShapeParameters().get(2);
                theta = Math.atan2(vertex2.y - vertex1.y, vertex2.x - vertex1.x); // orientation of connecting line between vertex1 and vertex2
                transform.rotate(-theta);
                transform.translate(-center.x, -center.y);

                // transform aerosolCloud (translate center to origin, change orientation (if not type VCircle)
                VShape transformedShape = new VPolygon(transform.createTransformedShape(shape));
                xStd = transformedShape.getBounds2D().getMaxX(); // xStd equals length of semi-Axis along x
                yStd = transformedShape.getBounds2D().getMaxY(); // yStd equals length of semi-Axis along y

            } else if (shape instanceof VCircle) {
                // transform aerosolCloud (translate center to origin, change orientation (if not type VCircle)
                transform.translate(-center.x, -center.y);
                VCircle circle = (VCircle) shape;
                double radius = circle.getRadius();
                xStd = radius;
                yStd = radius;
                theta = 0.0;
            }
            // ToDo: add else (shape is not instance of VCircle or Polygon)

            // transform position
            VPoint translatedPosition = new VPoint(position.x - center.x, position.y - center.y);
            translatedPosition.rotate(-theta);

            // apply distribution function
            // ToDo: ellipse: the distribution along the major axis should have a constant part
            // (maybe a better shape would be two segments of a circle (180°) connected by a rectangle)
            double n = 3.0; // the distance between boundary and center of the shape represents n times standard deviation of the
            pathogenLevel = twoDimensionalNormalDistZeroMeanZeroCorrelation(xStd / n, yStd / n, translatedPosition.x, translatedPosition.y);
            // pathogenLevel = twoDimensionalNormalDistribution(0.0,  xStd / n,  0.0,  yStd / n,  translatedPosition.x, translatedPosition.y);
        }
        return pathogenLevel;
    }

    // ToDo remove or move method to utils
    private double twoDimensionalNormalDistribution(double xMean, double xStd, double yMean, double yStd, double x, double y) {
        double g = 0.0; // correlation coefficient
        return (1.0 / (2.0 * Math.PI * xStd * yStd * Math.sqrt(1.0 - g * g))) * Math.exp(-1.0 / (2. * (1.0 - g * g)) * ((x - xMean) * (x - xMean) / (xStd * xStd) + (y - yMean) * (y - yMean) / (yStd * yStd) - (2.0 * g * (x - yMean) * (y - xMean)) / (xStd * yStd)));
    }
    // ToDo move method to utils
    private double twoDimensionalNormalDistZeroMeanZeroCorrelation(double xStd, double yStd, double x, double y) {
        return 1.0 / (2.0 * Math.PI * xStd * yStd) * (Math.exp(-1.0 / 2.0 * ((x * x) / (xStd * xStd) + (y * y) / (yStd * yStd))));
    }
}
