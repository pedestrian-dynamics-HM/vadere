package org.vadere.manager.traci.compoundobjects;

import org.vadere.manager.traci.TraCIDataType;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VPolygon;

import java.util.ArrayList;

public class WaitingAreaData extends GenericCompoundObject {

    String id;
    ArrayList<String> points;

    public WaitingAreaData(CompoundObject o) { super(o, 2); }

    @Override
    protected void init(CompoundObject o) {
        id = (String)o.getData(0, TraCIDataType.STRING);
        points = (ArrayList<String>)o.getData(1, TraCIDataType.STRING_LIST);
    }

    public String getId() {
        return id;
    }

    public int getIdAsInt() {
        return Integer.parseInt(id);
    }

    public ArrayList<String> getPoints() {
        return points;
    }

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
}
