package org.vadere.state.scenario;

import org.jetbrains.annotations.NotNull;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.scenario.AttributesAerosolCloud;
import org.vadere.state.types.ScenarioElementType;
import org.vadere.util.geometry.shapes.*;

import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * This class models aerosolClouds. AerosolClouds represent one mode of transmission how pathogen can spread among
 * pedestrians. They are created, updated and deleted in the InfectionModel:
 *
 * <ul>
 *     <li>Creation: infectious pedestrians emit pathogen, i.e. an aerosolCloud is created. Its shape and position
 *     depend on the pedestrian's trajectory and respiratory cycle. The shape can either be circular or elliptical.</li>
 *     <li>Update: An aerosolCloud can change its extent (shape) and pathogenLoad. The corresponding methods are defined
 *     in this class. The pathogen concentration equals pathogenLoad / volume. </li>
 *     <li>Deletion: The InfectionModel deletes an aerosolCloud once it has reached a minimum pathogen
 *     concentration</li>
 * </ul>
 */
public class AerosolCloud extends InfectiousParticleDispersion {

    private AttributesAerosolCloud attributes;

    final static int numberOfNodesAlongShapeBound = 20;

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

    public double getHeight() {
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

    public VPoint getCenter() {
        return attributes.getCenter();
    }

    public ArrayList<VPoint> getVertices() {
        return attributes.getVertices();
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

//    /*
//     * Calculates the pathogenLevel inside an aerosolCloud assuming a Gaussian distribution in x and y direction; the
//     * aerosolCloud's radial component equals n times the standard deviation
//     *
//     * Currently, the method is not used but may be helpful in future work.
//     */
//    public double calculatePathogenLevelAtPosition(VPoint position) {
//        double pathogenLevel;
//        double theta = 0;
//        if (!attributes.getShape().contains(position)) {
//            // pathogenLevel outside shape is 0
//            pathogenLevel = 0.0;
//        } else {
//            double xStd = -1;
//            double yStd = -1;
//            VShape shape = attributes.getShape();
//            VPoint center = attributes.getCenter();
//            AffineTransform transform = new AffineTransform();
//
//            if (shape instanceof VPolygon) {
//                VPoint vertex1 = attributes.getVertices().get(0);
//                VPoint vertex2 = attributes.getVertices().get(1);
//                theta = Math.atan2(vertex2.y - vertex1.y, vertex2.x - vertex1.x); // orientation of connecting line between vertex1 and vertex2
//                transform.rotate(-theta);
//                transform.translate(-center.x, -center.y);
//
//                // transform aerosolCloud (translate center to origin, change orientation (if not type VCircle)
//                VShape transformedShape = new VPolygon(transform.createTransformedShape(shape));
//                xStd = transformedShape.getBounds2D().getMaxX(); // xStd equals length of semi-Axis along x
//                yStd = transformedShape.getBounds2D().getMaxY(); // yStd equals length of semi-Axis along y
//
//            } else if (shape instanceof VCircle) {
//                // transform aerosolCloud (translate center to origin, change orientation (if not type VCircle)
//                transform.translate(-center.x, -center.y);
//                VCircle circle = (VCircle) shape;
//                double radius = circle.getRadius();
//                xStd = radius;
//                yStd = radius;
//                theta = 0.0;
//            } else {
//                logger.errorf(">>>>>>>>>>>calculatePathogenLevel: shape of aerosolCloud with Id %i is neither VPolygon nor VCircle.", this.getId());
//            }
//
//            // transform position
//            VPoint translatedPosition = new VPoint(position.x - center.x, position.y - center.y);
//            translatedPosition.rotate(-theta);
//
//            // assumption: the pathogen concentration is normally distributed along x and y (gaussian ellipsoid)
//            double n = 3.0; // the distance between boundary and center of the shape represents n times standard deviation of the
//            pathogenLevel = normalPathogenDistribution(xStd / n, yStd / n, translatedPosition.x, translatedPosition.y);
//        }
//        return pathogenLevel;
//    }
//
//    private double normalPathogenDistribution(double xStd, double yStd, double x, double y) {
//        return 1.0 / (2.0 * Math.PI * xStd * yStd) * (Math.exp(-1.0 / 2.0 * ((x * x) / (xStd * xStd) + (y * y) / (yStd * yStd))));
//    }

    /*
     * This method increases the shape of a circular aerosolCloud about deltaRadius; In case of an elliptical
     * aerosolCloud, the area is increased about the same cross-sectional area as if its shape were circular and changed
     * by deltaRadius.
     */
    public void increaseShape(double deltaRadius) {
        if (deltaRadius > 0.0) {

            VPoint center = attributes.getCenter();
            VPoint vertex1 = attributes.getVertices().get(0);
            double radius = Math.sqrt(attributes.getArea() / Math.PI);

            // define new cross-sectional area
            double newArea = Math.pow((radius + deltaRadius), 2) * Math.PI;
            attributes.setArea(newArea);

            // define new shape and vertices
            if (center.distance(vertex1) > radius) {
                increaseEllipticalShape(radius, deltaRadius, newArea, center, vertex1);
            } else if (center.distance(vertex1) <= radius) {
                increaseCircularShape(newArea, center);
            }

            // define new center
            VPoint newCenter = new VPoint((attributes.getVertices().get(0).x + attributes.getVertices().get(1).x) / 2, (attributes.getVertices().get(0).y + attributes.getVertices().get(1).y) / 2);
            attributes.setCenter(newCenter);
        }
    }

    private void increaseCircularShape(double newArea, VPoint center) {
        VShape newShape = createTransformedAerosolCloudShape(center, center, newArea);
        attributes.setShape(newShape);
        attributes.setVertices(new ArrayList<>(Arrays.asList(center, center)));
    }

    private void increaseEllipticalShape(double radius, double deltaRadius, double newArea, VPoint center, VPoint vertex1) {
        Vector2D semiAxis1 = new Vector2D(center.x - vertex1.x, center.y - vertex1.y);
        double lengthSemiAxis1 = semiAxis1.getLength();
        double lengthSemiAxis2 = attributes.getArea() / Math.PI / lengthSemiAxis1;
        Vector2D unitAxis1 = semiAxis1.normalize(1);
        double deltaSemiAxis;

        // deltaSemiAxis = increaseEllipseAxisProportionally(deltaRadius, lengthSemiAxis1, lengthSemiAxis2);
        deltaSemiAxis = increaseEllipseAxisEqually(deltaRadius, lengthSemiAxis1, lengthSemiAxis2, radius);

        // define new vertices
        Vector2D newSemiAxis1 = unitAxis1.multiply(lengthSemiAxis1 + deltaSemiAxis);
        VPoint newVertex1 =  newSemiAxis1.add(center);
        VPoint newVertex2 =  newSemiAxis1.multiply(-1.0).add(center);

        VShape newShape = createTransformedAerosolCloudShape(newVertex1, newVertex2, newArea);

        attributes.setShape(newShape);
        attributes.setVertices(new ArrayList<>(Arrays.asList(newVertex1, newVertex2)));
    }

    /*
     * Returns deltaAxis in m by which an elliptical aerosolCloud will be increased equally in the direction of its
     * semi-major and semi-minor axes. The resulting cross sectional area equals the area of a circle whose radius is
     * increased by deltaRadius.
     */
    private double increaseEllipseAxisEqually(double deltaRadius, double lengthSemiAxis1, double lengthSemiAxis2, double radius) {
        // increase equally to all sides
        double axisSum = lengthSemiAxis1 + lengthSemiAxis2;
        /* Let 2 * a and 2 * b be the principal diameters of an ellipse; circle with radius so that
         * ellipseArea = circleArea
         * ellipseArea = a * b * PI
         * circeArea = radius^2 * PI
         *
         * Now increase circle by deltaRadius: increasedCircleArea = (radius + deltaRadius)^2 * PI
         * and increase the ellipse equally in direction of a and b by deltaAxes:
         * increasedEllipseArea = (a + deltaAxes) * (b + deltaAxes) * PI
         *
         * Under the condition (increasedEllipseArea = increasedCircleArea), we have:
         * (a + deltaAxes) * (b + deltaAxes) * PI = (radius + deltaRadius)^2 * PI
         * solve for deltaAxes and obtain ...
         */
        double deltaAxes =  ((-axisSum) + Math.sqrt(Math.pow(axisSum, 2) + 4.0 * (2.0 * deltaRadius * radius + deltaRadius * deltaRadius))) / 2;

        return deltaAxes;
    }

// Method currently not used but maybe helpful for future work
//    /*
//     * Returns deltaAxis in m by which an elliptical aerosolCloud will be increased in the direction of its
//     * major axis. The corresponding minor axis is increased proportionally, i.e. the resulting cross sectional area
//     * of the aerosolCloud equals the area of a circle whose radius is increased by deltaRadius.
//     */
//    private double increaseEllipseAxisProportionally(double deltaRadius, double lengthSemiAxis1, double lengthSemiAxis2) {
//        // increase proportionally to all sides
//        // deltas for semiAxes, where (I) and (II) hold true
//        // (I) lengthSemiAxis1 / lengthSemiAxis2 = newAxis1 / newAxis2 (relation between semiAxis1 and semiAxis2 stays the same)
//        // (II) (radius + deltaRadius)^2 * PI = (lengthSemiAxis1 + deltaSemiAxis1) * (lengthSemiAxis2 + deltaAxis2) * PI (new area of an elliptical aerosolCloud equals the new area of a circular aerosolCloud)
//        double w = Math.sqrt(1.0 + 2 * deltaRadius / Math.sqrt(lengthSemiAxis1 * lengthSemiAxis2) + (deltaRadius * deltaRadius) / (lengthSemiAxis1 * lengthSemiAxis2));
//        double deltaSemiAxis1 = lengthSemiAxis1 * w - lengthSemiAxis1;
//        return  deltaSemiAxis1;
//    }

    public static VShape createTransformedAerosolCloudShape(VPoint vertex1, VPoint vertex2, double area) {
        VPoint center = new VPoint((vertex1.x + vertex2.x) / 2.0, (vertex1.y + vertex2.y) / 2.0);
        double majorAxis = vertex1.distance(vertex2);
        double minorAxis = 4.0 * area / (majorAxis * Math.PI);

        // ellipse parameters
        double semiMajorAxis = majorAxis / 2.0;
        double semiMinorAxis = minorAxis / 2.0;
        double eccentricity = Math.sqrt(semiMajorAxis * semiMajorAxis - semiMinorAxis * semiMinorAxis) / semiMajorAxis;
        VShape shape;

        if (majorAxis <= minorAxis) {
            // return circle (approximated circle with edges)
            double radius = Math.sqrt(area / Math.PI);
            // Use VPolygon with points that yield a circular shape instead of VCircle (shape = new VCircle(new VPoint(center.getX(), center.getY()), radius);) to keep the shape consistent
            Path2D path = new Path2D.Double();
            path.moveTo(radius, 0);
            for (double angle = 0.0; angle < 2.0 * Math.PI; angle += 2.0 * Math.PI / numberOfNodesAlongShapeBound) {
                path.lineTo(Math.cos(angle) * radius, Math.sin(angle) * radius);
            }
            path.closePath();
            VShape circle = new VPolygon(path);
            AffineTransform transform = new AffineTransform();
            transform.translate(center.getX(), center.getY());

            shape = new VPolygon(transform.createTransformedShape(circle));

        } else {
            // return ellipse (approximated ellipse with edges)
            Path2D path = new Path2D.Double();
            path.moveTo(semiMajorAxis, 0); // define stating point
            for (double angle = 0.0; angle < 2.0 * Math.PI; angle += 2.0 * Math.PI / numberOfNodesAlongShapeBound) {
                double radius = semiMinorAxis / Math.sqrt(1 - Math.pow(eccentricity * Math.cos(angle), 2)); // radius(angle) from ellipse center to its bound
                path.lineTo(Math.cos(angle) * radius, Math.sin(angle) * radius); // convert polar to cartesian coordinates
            }
            path.closePath();
            VShape ellipse = new VPolygon(path);
            double theta = Math.atan2(vertex2.y - vertex1.y, vertex2.x - vertex1.x); // get orientation of shape
            AffineTransform transform = new AffineTransform();
            transform.translate(center.getX(), center.getY());
            transform.rotate(theta);

            shape = new VPolygon(transform.createTransformedShape(ellipse));
        }
        return shape;
    }
}
