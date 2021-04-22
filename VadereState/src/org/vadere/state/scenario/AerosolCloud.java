package org.vadere.state.scenario;

import org.jetbrains.annotations.NotNull;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.scenario.AttributesAerosolCloud;
import org.vadere.state.types.ScenarioElementType;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VPolygon;
import org.vadere.util.geometry.shapes.VShape;

import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.Arrays;

import static org.vadere.util.opencl.CLDemo.logger;


public class AerosolCloud extends ScenarioElement {

    private AttributesAerosolCloud attributes;


    // ToDo: implement AerosolCloudListener (or remove commented code)
    // private final Collection<AerosolCloudListener> aerosolCloudListeners = new LinkedList<>();


    // Constructors
    public AerosolCloud() {
        this(new AttributesAerosolCloud());
    }

    public AerosolCloud(@NotNull AttributesAerosolCloud attributes) {
        this.attributes = attributes;
    }

    // Getter
    @Override
    public VShape getShape() {
        return attributes.getShape();
    }

    public double getArea() {
        return attributes.getArea();
    }

    public double getHeigth() {
        return attributes.getHeight();
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

    public double getHalfLife() {
        return attributes.getHalfLife();
    }

    public double getCreationTime() {
        return attributes.getCreationTime();
    }

    public double getInitialPathogenLoad() {
        return attributes.getInitialPathogenLoad();
    }

    public double getCurrentPathogenLoad() {
        return attributes.getCurrentPathogenLoad();
    }

    public boolean getHasReachedLifeEnd() {
        return attributes.getHasReachedLifeEnd();
    }


    // Setter
    @Override
    public void setShape(VShape newShape) {
        attributes.setShape(newShape);
    }

    public void setArea(double area) {
        attributes.setArea(area);
    }

    @Override
    public void setAttributes(Attributes attributes) {
        this.attributes = (AttributesAerosolCloud) attributes;
    }

    public void setId(int id) {
        ((AttributesAerosolCloud) getAttributes()).setId(id);
    }

    public void setCreationTime(double creationTime) {
        attributes.setCreationTime(creationTime);
    }

    public void setHasReachedLifeEnd(boolean hasReachedLifeEnd) {
        attributes.setHasReachedLifeEnd(hasReachedLifeEnd);
    }

    public void setCurrentPathogenLoad(double currentPathogenLoad) {
        attributes.setCurrentPathogenLoad(currentPathogenLoad);
    }

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
            return other.attributes == null;
        } else return attributes.equals(other.attributes);
    }

    /**
     * Calculates the pathogenLevel inside an aerosolCloud assuming a Gaussian distribution in x and y direction; the
     * aerosolCloud's radial component equals n times the standard deviation
     */
    public double calculatePathogenLevelAtPosition(VPoint position) {
        double pathogenLevel;
        double theta = 0;
        if (!attributes.getShape().contains(position)) {
            // pathogenLevel outside shape is 0
            pathogenLevel = 0.0;
        } else {
            double xStd = -1;
            double yStd = -1;
            VShape shape = attributes.getShape();
            VPoint center = attributes.getCenter();
            AffineTransform transform = new AffineTransform();

            if (shape instanceof VPolygon) {
                VPoint vertex1 = attributes.getVertices().get(0);
                VPoint vertex2 = attributes.getVertices().get(1);
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
            } else {
                logger.errorf(">>>>>>>>>>>calculatePathogenLevel: shape of aerosolCloud with Id %i is neither VPolygon nor VCircle.", this.getId());
            }

            // transform position
            VPoint translatedPosition = new VPoint(position.x - center.x, position.y - center.y);
            translatedPosition.rotate(-theta);

            // assumption: the pathogen concentration is normally distributed along x and y (gaussian ellipsoid)
            double n = 3.0; // the distance between boundary and center of the shape represents n times standard deviation of the
            pathogenLevel = normalPathogenDistribution(xStd / n, yStd / n, translatedPosition.x, translatedPosition.y);
        }
        return pathogenLevel;
    }

    private double normalPathogenDistribution(double xStd, double yStd, double x, double y) {
        return 1.0 / (2.0 * Math.PI * xStd * yStd) * (Math.exp(-1.0 / 2.0 * ((x * x) / (xStd * xStd) + (y * y) / (yStd * yStd))));
    }

    public void increaseShape(double deltaRadius) {
        if (deltaRadius > 0.0) {

            VShape shape = attributes.getShape();
            VPoint center = attributes.getCenter();
            VPoint vertex1 = attributes.getVertices().get(0);
            VPoint vertex2 = attributes.getVertices().get(1);

            if (shape instanceof VPolygon) {
                // get length of oldAxis1 (semi-axis between vertex1 and vertex2) and oldAxis2 (corresponding perpendicular semi-axis)
                double oldAxis1 = Math.sqrt(Math.pow((vertex1.x - center.x), 2) + Math.pow((vertex1.y - center.y), 2));
                double oldAxis2 = attributes.getArea() / Math.PI / oldAxis1;
                // define new vertices and area
                VPoint newVertex1 = new VPoint(vertex1.x + deltaRadius * (vertex1.x - center.x) / oldAxis1, vertex1.y + deltaRadius * (vertex1.y - center.y) / oldAxis1);
                VPoint newVertex2 = new VPoint(vertex2.x - deltaRadius * (vertex2.x - center.x) / oldAxis1, vertex2.y - deltaRadius * (vertex2.y - center.y) / oldAxis1);
                double newArea = (oldAxis1 + deltaRadius) * (oldAxis2 + deltaRadius) * Math.PI;
                VShape newShape = createTransformedAerosolCloudShape(newVertex1, newVertex2, newArea);

                attributes.setShape(newShape);
                attributes.setArea(newArea);
                attributes.setCenter(center);
                attributes.setVertices(new ArrayList<>(Arrays.asList(newVertex1, newVertex2)));
            } else if (shape instanceof VCircle) {
                double newArea = Math.pow((((VCircle) shape).getRadius() + deltaRadius), 2) * Math.PI;
                VShape newShape = createTransformedAerosolCloudShape(vertex1, vertex2, newArea);
                attributes.setShape(newShape);
                attributes.setArea(newArea);
            }
        }
    }

    public static VShape createTransformedAerosolCloudShape(VPoint vertex1, VPoint vertex2, double area) {
        VPoint center = new VPoint((vertex1.x + vertex2.x) / 2.0, (vertex1.y + vertex2.y) / 2.0);
        double majorAxis = vertex1.distance(vertex2);
        double minorAxis = 4.0 * area / (majorAxis * Math.PI);

        // ellipse parameters
        double a = majorAxis / 2.0; // semi-major axis
        double b = minorAxis / 2.0; // semi-minor axis
        double c = Math.sqrt(a * a - b * b);
        double e = c / a; // eccentricity
        VShape shape;
        int numberOfNodesAlongBound = 50;

        if (majorAxis <= minorAxis) {
            // return circle, i.e. ellipse with (a'=b')
            shape = new VCircle(new VPoint(center.getX(), center.getY()), Math.sqrt(area / Math.PI));
        } else {
            // return polygon (approximated ellipse with edges)
            Path2D path = new Path2D.Double();
            path.moveTo(a, 0); // define stating point
            for (double angle = 0.0; angle < 2.0 * Math.PI; angle += 2.0 * Math.PI / numberOfNodesAlongBound) {
                double radius = b / Math.sqrt(1 - Math.pow(e * Math.cos(angle), 2)); // radius(angle) from ellipse center to its bound
                path.lineTo(Math.cos(angle) * radius, Math.sin(angle) * radius); // convert polar to cartesian coordinates
            }
            path.closePath();
            VShape polygon = new VPolygon(path);
            double theta = Math.atan2(vertex2.y - vertex1.y, vertex2.x - vertex1.x); // get orientation of shape
            AffineTransform transform = new AffineTransform();
            transform.translate(center.getX(), center.getY());
            transform.rotate(theta);

            shape = new VPolygon(transform.createTransformedShape(polygon));
        }
        return shape;
    }
}
