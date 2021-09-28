package org.vadere.simulator.control.external.models;

import org.json.JSONException;
import org.json.JSONObject;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;

import java.time.temporal.ValueRange;


public class CtlCommand {

    private int commandId = 0;
    private int stimulusId = -1;
    JSONObject rawCommand;
    VShape space;
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
        } catch (JSONException ignored) { }

        return time;
    }

    public VShape getSpace(){
        // only radius

        VShape shape;
        double x = 0;
        double y = 0;
        double radius = 0;
        double height = 0;
        double width = 0;
        try {
            JSONObject space = rawCommand.getJSONObject("space");
            x = space.getDouble("x");
            y = space.getDouble("y");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        try {
            JSONObject space = rawCommand.getJSONObject("space");
            radius = space.getDouble("radius");

            if (radius > 0) {
                this.space = new VCircle(x, y, radius);
            }
        } catch (JSONException ignored) {}

        try {
            JSONObject space = rawCommand.getJSONObject("space");
            height = space.getDouble("height");
            width = space.getDouble("width");

            if ((height > 0) && (width > 0)) {
                this.space = new VRectangle(x, y, width, height);
            }
        } catch (JSONException ignored) {}

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

    public int getCommandId() {
        try {
            commandId = rawCommand.getInt("commandId");
        } catch (JSONException ignored) { }
        return commandId;
    }

    public int getStimulusId() {
        try {
            stimulusId = rawCommand.getInt("stimulusId");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return stimulusId;
    }



}
