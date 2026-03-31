package org.vadere.state.attributes.models.restaurant;


import org.vadere.annotation.factories.attributes.ModelAttributeClass;
import org.vadere.state.attributes.Attributes;

import java.util.ArrayList;


public class AttributesSeatGroup extends Attributes{
    private int tableTargetId;
    private ArrayList<Integer> seatTargetIds;
    private double lengthOfStay;

    private static final int INVALID_ID = -1;

    public AttributesSeatGroup() {
        this.tableTargetId = INVALID_ID;
        this.seatTargetIds = new ArrayList<>();
        this.lengthOfStay = 0;
    }

    public AttributesSeatGroup(int tableTargetId, ArrayList<Integer> seatTargetIds, double lengthOfStay) {
        this.tableTargetId = tableTargetId;
        this.seatTargetIds = seatTargetIds;
        this.lengthOfStay = lengthOfStay;
    }

    public int getTableTargetId() { return tableTargetId; }

    public ArrayList<Integer> getSeatTargetIds() { return seatTargetIds; }

    public double getLengthOfStay() { return lengthOfStay; }



}
