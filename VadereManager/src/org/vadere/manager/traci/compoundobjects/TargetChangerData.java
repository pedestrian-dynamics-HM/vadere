package org.vadere.manager.traci.compoundobjects;

import org.vadere.manager.traci.TraCIDataType;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VPolygon;

import java.awt.*;
import java.util.ArrayList;

public class TargetChangerData extends GenericCompoundObject{

    String id;
    ArrayList<String> points;
    double reachDist;
    int nextTargetIsPedestrian;
    String nextTarget;
    double prob;

    TargetChangerData(CompoundObject o) {
        super(o, 6);
    }

    @Override
    protected void init(CompoundObject o) {
        id = (String)o.getData(0, TraCIDataType.STRING);
        points = (ArrayList<String>)o.getData(1, TraCIDataType.STRING_LIST);
        reachDist = (Double)o.getData(2, TraCIDataType.DOUBLE);
        nextTargetIsPedestrian = (Integer)o.getData(3, TraCIDataType.INTEGER);
        nextTarget = (String)o.getData(4, TraCIDataType.STRING);
        prob = (Double)o.getData(5, TraCIDataType.DOUBLE);
    }

    public String getId(){ return id; }

    public int getIdAsInt(){ return Integer.parseInt(id); }

    public ArrayList<String> getPoints(){ return points; }

    public VPolygon getPointsAsVPolygon(){

        VPolygon pointsAsVPolygon = new VPolygon();
        for(int i = 0; i < points.size(); i += 2){
            double x = Double.parseDouble(points.get(i));
            double y = Double.parseDouble(points.get(i + 1));
            VPoint p = new VPoint(x, y);
            pointsAsVPolygon.getPoints().add(p);
        }

        return pointsAsVPolygon;
    }

    public double getReachDist(){ return reachDist; }

    public int getNextTargetIsPedestrian(){ return nextTargetIsPedestrian; }

    public boolean getNextTargetIsPedestrianAsBool(){
        if(nextTargetIsPedestrian == 0){
            return false;
        } else {
            return true;
        }
    }

    public String getNextTarget(){ return nextTarget; }

    public int getNextTargetAsInt(){ return Integer.parseInt(nextTarget); }

    public double getProb(){ return prob; }
}
