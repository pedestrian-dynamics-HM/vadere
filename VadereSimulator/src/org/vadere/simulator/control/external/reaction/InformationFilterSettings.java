package org.vadere.simulator.control.external.reaction;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;

public class InformationFilterSettings {

    int options = -1;
    HashMap<Integer, DistParameters> dist = new HashMap<>();
    private JSONObject rawCommand;
    private boolean isReactingToRecurringInformation = false;
    private boolean isReactingToFirstInformationOnly = true;

    public InformationFilterSettings(){}

    public InformationFilterSettings(String command) {
        this(new JSONObject(command));
    }


    public InformationFilterSettings(JSONObject command) {
        rawCommand = command;
    }


    private void setDists() {

        if (isOneOption()) {
            dist.put(0, new DistParameters(this.rawCommand));
        } else {

            JSONArray multipleDists = rawCommand.getJSONArray("reactionProbabilities");
            try {
                if (multipleDists.length() != getNrOptions()) {
                    throw new Exception("Number of options does not fit array size");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            for (int i = 0; i < multipleDists.length(); i++) {
                dist.put(i, new DistParameters(multipleDists.getJSONObject(i)));
            }
        }
    }

    public boolean isReactingToRecurringInformation(){
        String key = "isReactingToRecurringInformation";
        if (rawCommand != null) {
            if (rawCommand.has(key)) {
                return rawCommand.getBoolean(key);
            }
        }
        return isReactingToRecurringInformation;
    }

    public boolean isReactingToFirstInformationOnly(){
        String key = "isReactingToFirstInformationOnly";
        if (rawCommand != null) {
            if (rawCommand.has(key)) {
                return rawCommand.getBoolean(key);
            }
        }
        return isReactingToFirstInformationOnly;
    }


    public int getNrOptions() {
        String key = "numberOfReactionProbabilities";
        if (rawCommand != null) {
            if (options == -1) {
                if (rawCommand.has(key)) {
                    options = rawCommand.getInt(key);
                } else {
                    options = 1;
                }
            }
        }
        return options;
    }

    public boolean isOneOption() {
        return (1 == getNrOptions());
    }


    public DistParameters getDistParameters(int index) {

        if (dist.size() == 0) {
            setDists();
        }

        if (getNrOptions() == 1)
            return dist.get(0);
        else
            return dist.get(index);
    }

    public DistParameters getDistParameters() {
        return getDistParameters(0);
    }

    public HashMap<Integer, DistParameters> getDist() {
        if (dist.isEmpty())
            setDists();
        return dist;
    }
}
