package org.vadere.state.attributes.models;

import org.vadere.annotation.factories.attributes.ModelAttributeClass;
import org.vadere.state.attributes.Attributes;

import java.util.ArrayList;

@ModelAttributeClass
public class AttributeSIR extends Attributes {

    // initial r-Value of SIR model
    private double initialR = 0.8;
    private ArrayList<Integer> infectionZoneIds = new ArrayList<>();




    public double getInitialR() {
        return initialR;
    }

    public void setInitialR(double initialR) {
        // all attribute setter must have this check to ensure they are not changed during simulation
        checkSealed();
        this.initialR = initialR;
    }

    public void setInfectionZoneIds(ArrayList<Integer> infectionZoneIds) {
        checkSealed();
        this.infectionZoneIds = infectionZoneIds;
    }

    public ArrayList<Integer> getInfectionZoneIds() {
        return infectionZoneIds;
    }
}

