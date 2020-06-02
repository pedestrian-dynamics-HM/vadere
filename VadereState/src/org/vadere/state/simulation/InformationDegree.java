package org.vadere.state.simulation;

// @author Christina Mayr

import org.jetbrains.annotations.NotNull;

import static java.lang.Integer.max;

public class InformationDegree {

    private final double percentageInformed;
    private final int numberPedsInformed;
    private final int numberPedsAll;


    public InformationDegree( int numberPedsInformed, int numberPedsAll) {

        this.numberPedsInformed = numberPedsInformed;
        this.numberPedsAll = numberPedsAll;
        if (numberPedsAll > 0){
            this.percentageInformed = ((double) numberPedsInformed) / numberPedsAll;}
        else {
            this.percentageInformed = 0;}
    }


    public String[] getValueString(){
        String[] valueLine = {""+this.numberPedsInformed, ""+this.numberPedsAll, ""+this.percentageInformed};
        return valueLine;
    }

    public double getPercentageInformed() {
        return percentageInformed;
    }
}
