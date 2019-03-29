package org.vadere.state.attributes.processor;

import org.vadere.util.geometry.shapes.VPoint;

/**
 * @author Marion Gödel
 *
 */

public class AttributesQueueWidthProcessor extends AttributesProcessor {


    private VPoint referencePoint = new VPoint(0,0);
    private double maxDist = 5.0;
    private VPoint direction = new VPoint(0, 1);

    public VPoint getReferencePoint(){ return referencePoint; }
    public double getMaxDist(){ return maxDist; }
    public VPoint getDirection(){ return direction; }


    public void setReferencePoint(VPoint referencePoint){
        checkSealed();
        this.referencePoint = referencePoint;
    }
    public void setReferencePoint(double x, double y){
        setReferencePoint(new VPoint(x,y));
    }

    public void setMaxDist(double maxDist) {
        checkSealed();
        this.maxDist = maxDist;
    }

    public void setDirection(VPoint direction){
        if( direction.distanceToOrigin() != 1.0 || Math.max(Math.abs(direction.getX()), Math.abs(direction.getY())) != 1.0){
            System.out.println("Warning! Only the following vectors are allowed: [1,0],[-1,0],[0,1],[0,-1]. It is continued with [1,0].");
            direction = new VPoint(1, 0);
        }
        this.direction = direction;
    }

    public void setDirection(double x, double y){
        checkSealed();
        setDirection(new VPoint(x,y));
    }
}

