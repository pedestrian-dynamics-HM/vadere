package org.vadere.simulator.control.external.models;

import org.json.JSONException;
import org.json.JSONObject;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VShape;


public class CtlCommand {

    JSONObject rawCommand;
    VCircle space;
    Double time;
    JSONObject pedCommand;


    public CtlCommand(JSONObject command){
        rawCommand = command;
    };

    public CtlCommand(String command){
        rawCommand = new JSONObject(command);
    };


    public String getModelName(){
        return rawCommand.getString("model");
    }

    public double getExecTime(){
        double time = Double.POSITIVE_INFINITY;

        try {
            time = rawCommand.getDouble("time");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return time;
    }

    public VCircle getSpace(){
        // only radius

        VShape shape;
        double x = 0;
        double y = 0;
        double radius = 0;
        try {
            JSONObject space = rawCommand.getJSONObject("space");
            x = space.getDouble("x");
            y = space.getDouble("y");
            radius = space.getDouble("radius");

            if (radius>=0) {
                this.space = new VCircle(x, y, radius);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return this.space;

    }

    public boolean isSpaceBounded(){
        return (getSpace() != null);
    }




    public JSONObject getPedCommand(){
        try{
            pedCommand = rawCommand.getJSONObject("command");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return pedCommand;

    }





}
