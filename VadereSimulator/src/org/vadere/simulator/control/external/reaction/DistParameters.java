package org.vadere.simulator.control.external.reaction;

import org.apache.commons.math.distribution.DiscreteDistribution;
import org.apache.commons.math3.util.Pair;
import org.apache.tools.ant.types.selectors.SelectSelector;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Collections;
import java.util.LinkedList;

public class DistParameters {

    static String key = "isBernoulliParameterCertain";
    private JSONObject command = new JSONObject();

    // default
    private boolean isParameterFixed;
    private LinkedList<Double> params = new LinkedList<Double>(Collections.singletonList(1.0));
    private String type = "none";



    // constructor
    public DistParameters(JSONObject obj){
        command = obj;
    }

    public DistParameters(Double bernoulliParameter){
        params = new LinkedList<Double>(Collections.singletonList(bernoulliParameter));
    }

    public DistParameters(){

    }



    // getter
    public boolean isBernoulliParameterFixed() {

        if (isBernoulliParameterDefined()){
            this.isParameterFixed = (boolean) command.get(key);
        }
        return this.isParameterFixed;
    }


    public Pair<String, LinkedList> getBernoulliDist() {
        return Pair.create(getDistType(), getDistributionParameters());
    }


    public String getDistType() {

        String keyType = "DistributionType";

        if (isBernoulliParameterDefined()) {

            if (isBernoulliParameterFixed()) {
                type = BernoulliParameterDistributions.FIXED.toString();}
            else{

                JSONObject parameterSet = command.getJSONObject("BernoulliParameter");
                if (parameterSet.has(keyType)) {
                    type = parameterSet.getString(keyType);
                }
            }
        }

        return type;
    }



    private LinkedList<Double> getBernoulliParameterValueAsList() {
        LinkedList<Double> params = new LinkedList<>();

        if (isBernoulliParameterDefined()) {
            double singleVal = command.getDouble("BernoulliParameter");
            params.add(singleVal);
        }

        return params;
    }


    public LinkedList<Double> getDistributionParameters() {

        if (isBernoulliParameterDefined()){

            if (isBernoulliParameterFixed()){
                this.params = getBernoulliParameterValueAsList();
            }
            else {
                LinkedList<Double> params = new LinkedList<>();
                JSONObject parameterSet = command.getJSONObject("BernoulliParameter");
                JSONArray targetList = (JSONArray) parameterSet.get("DistributionParameters");
                for (int i = 0; i < targetList.length(); i++) {
                    params.add(targetList.getDouble(i));
                    this.params = params;
                }
            }

        }

        return params;
    }



    private boolean isBernoulliParameterDefined(){
        if (command.isEmpty()){
            return false;
        }
        return command.has(key);
    }


}
