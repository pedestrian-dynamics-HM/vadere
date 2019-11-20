package org.vadere.manager.traci.compoundobjects;

import org.vadere.manager.traci.TraCIDataType;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VPolygon;

import java.util.ArrayList;

public class WaitingAreaData extends GenericCompoundObject {

    String id;
    double startTime, endTime, waitTimeBetweenRepetition, time;
    int repeat;
    ArrayList<String> points;

    public WaitingAreaData(CompoundObject o) { super(o, 7); }

    public double getStartTime() {
        return startTime;
    }

    public double getEndTime() {
        return endTime;
    }

    public double getWaitTimeBetweenRepetition() {
        return waitTimeBetweenRepetition;
    }

    public int getRepeat() {
        return repeat;
    }

    public boolean getRepeatAsBool(){
        if(repeat == 0){
            return false;
        }else{
            return true;
        }
    }

    @Override
    protected void init(CompoundObject o) {
        id = (String)o.getData(0, TraCIDataType.STRING);
        startTime = (double)o.getData(1, TraCIDataType.DOUBLE);
        endTime = (double)o.getData(2, TraCIDataType.DOUBLE);
        repeat = (int)o.getData(3, TraCIDataType.INTEGER);
        waitTimeBetweenRepetition = (double)o.getData(4, TraCIDataType.DOUBLE);
        time = (double)o.getData(5, TraCIDataType.DOUBLE);
        points = (ArrayList<String>)o.getData(6, TraCIDataType.STRING_LIST);
    }

    public String getId() {
        return id;
    }

    public int getIdAsInt() {
        return Integer.parseInt(id);
    }

    public double getTime() {
        return time;
    }

    public ArrayList<String> getPoints() {
        return points;
    }

    public VPolygon getPointsAsVPolygon(){

        ArrayList<VPoint> vps = new ArrayList<VPoint>();
        for(int i = 0; i < points.size(); i += 2){
            double x = Double.parseDouble(points.get(i));
            double y = Double.parseDouble(points.get(i + 1));
            VPoint p = new VPoint(x, y);
            vps.add(p);
        }

        VPolygon pointsAsVPolygon = GeometryUtils.polygonFromPoints2D(vps);

        return pointsAsVPolygon;
    }
}
